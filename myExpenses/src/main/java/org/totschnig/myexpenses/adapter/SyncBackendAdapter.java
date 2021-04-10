package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.annimon.stream.Exceptional;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.sync.json.AccountMetaData;

import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

public class SyncBackendAdapter extends BaseExpandableListAdapter {

  public enum SyncState {
    SYNCED_TO_THIS,
    SYNCED_TO_OTHER,
    UNSYNCED,
    UNKNOWN,
    ERROR
  }

  private List<Pair<String, Boolean>> syncAccounts;
  private final SparseArray<List<Exceptional<AccountMetaData>>> accountMetaDataMap = new SparseArray<>();
  private final LayoutInflater layoutInflater;
  private Map<String, String> localAccountInfo;
  private final CurrencyContext currencyContext;

  public SyncBackendAdapter(Context context, CurrencyContext currencyContext, List<Pair<String, Boolean>> syncAccounts) {
    this.layoutInflater = LayoutInflater.from(context);
    this.syncAccounts = syncAccounts;
    this.currencyContext = currencyContext;
  }

  @Override
  public Object getChild(int groupPosition, int childPosititon) {
    List<Exceptional<AccountMetaData>> childList = getChildList(groupPosition);
    return childList != null ? childList.get(childPosititon) : null;
  }

  private List<Exceptional<AccountMetaData>> getChildList(int groupPosition) {
    return accountMetaDataMap.get(groupPosition);
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    return childPosition;
  }

  @Override
  public View getChildView(int groupPosition, final int childPosition,
                           boolean isLastChild, View convertView, ViewGroup parent) {
    Exceptional<AccountMetaData> accountMetaData = (Exceptional<AccountMetaData>) getChild(groupPosition, childPosition);

    if (convertView == null) {
      convertView = layoutInflater.inflate(R.layout.sync_account_row, parent, false);
    }
    ImageView syncStateView = convertView.findViewById(R.id.state);
    final TextView labelTextView = convertView.findViewById(R.id.label);
    if (accountMetaData.isPresent()) {
      labelTextView.setText(accountMetaData.get().toString());
      convertView.findViewById(R.id.color1).setBackgroundColor(accountMetaData.get().color());
      SyncState syncState = getSyncState(groupPosition, childPosition);
      switch (syncState) {
        case UNKNOWN:
          syncStateView.setVisibility(View.GONE);
          break;
        case SYNCED_TO_THIS:
          syncStateView.setVisibility(View.VISIBLE);
          syncStateView.setImageResource(R.drawable.ic_sync);
          break;
        case UNSYNCED:
        case SYNCED_TO_OTHER:
          syncStateView.setVisibility(View.VISIBLE);
          syncStateView.setImageResource(R.drawable.ic_action_sync_unlink);
          break;
      }
    } else {
      syncStateView.setVisibility(View.GONE);
      labelTextView.setText(accountMetaData.getException().getMessage());
    }
    return convertView;
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    List<Exceptional<AccountMetaData>> childList = getChildList(groupPosition);
    return childList != null ? childList.size() : 0;
  }

  @Override
  public Object getGroup(int groupPosition) {
    return syncAccounts.get(groupPosition);
  }

  public String getBackendLabel(int groupPosition) {
    return ((Pair<String, Boolean>) getGroup(groupPosition)).first;
  }

  @Override
  public int getGroupCount() {
    return syncAccounts.size();
  }

  @Override
  public long getGroupId(int groupPosition) {
    return groupPosition;
  }

  @Override
  public View getGroupView(int groupPosition, boolean isExpanded,
                           View convertView, ViewGroup parent) {
    final Pair<String, Boolean> group = (Pair<String, Boolean>) getGroup(groupPosition);
    String headerTitle = group.first;
    if (convertView == null) {
      convertView = layoutInflater.inflate(R.layout.sync_backend_row, parent, false);
    }

    ((TextView) convertView.findViewById(R.id.label)).setText(headerTitle);
    convertView.findViewById(R.id.state).setVisibility(group.second ? View.VISIBLE : View.GONE);

    return convertView;
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }

  public void setAccountList(List<Pair<String, Boolean>> accountList) {
    syncAccounts = accountList;
    accountMetaDataMap.clear();
    notifyDataSetChanged();
  }

  public void setAccountMetadata(int groupPosition, List<Exceptional<AccountMetaData>> accountMetaDataList) {
    accountMetaDataMap.put(groupPosition, accountMetaDataList);
    notifyDataSetChanged();
  }

  public boolean hasAccountMetadata(int groupPosition) {
    return accountMetaDataMap.get(groupPosition) != null;
  }

  public void setLocalAccountInfo(Map<String, String> uuid2syncMap) {
    localAccountInfo = uuid2syncMap;
    notifyDataSetChanged();
  }

  public SyncState getSyncState(long packedPosition) {
    return getSyncState(ExpandableListView.getPackedPositionGroup(packedPosition), ExpandableListView.getPackedPositionChild(packedPosition));
  }

  private SyncState getSyncState(int groupPosition, int childPosition) {
    String syncAccount = getBackendLabel(groupPosition);
    Exceptional<AccountMetaData> accountMetaData = (Exceptional<AccountMetaData>) getChild(groupPosition, childPosition);
    if (accountMetaData.isPresent()) {
      String uuid = accountMetaData.get().uuid();
      if (localAccountInfo != null && localAccountInfo.containsKey(uuid)) {
        if (localAccountInfo.get(uuid) == null) {
          return SyncState.UNSYNCED;
        }
        return syncAccount.equals(localAccountInfo.get(uuid)) ?
            SyncState.SYNCED_TO_THIS : SyncState.SYNCED_TO_OTHER;
      }
      return SyncState.UNKNOWN;
    } else {
      return SyncState.ERROR;
    }
  }

  @Nullable
  public Account getAccountForSync(long packedPosition) {
    int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
    final Exceptional<AccountMetaData> child = (Exceptional<AccountMetaData>) getChild(groupPosition,
        ExpandableListView.getPackedPositionChild(packedPosition));
    if (child.isPresent()) {
      Account account = child.get().toAccount(currencyContext);
      account.setSyncAccountName(getBackendLabel(groupPosition));
      return account;
    } else {
      return null;
    }
  }

  public String getSyncAccountName(long packedPosition) {
    int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
    return getBackendLabel(groupPosition);
  }
}
