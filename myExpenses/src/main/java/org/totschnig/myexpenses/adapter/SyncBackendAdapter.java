package org.totschnig.myexpenses.adapter;

import android.accounts.Account;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.sync.json.AccountMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncBackendAdapter extends BaseExpandableListAdapter {

  private List<SyncAccount> syncAccounts;
  private LayoutInflater layoutInflater;

  public SyncBackendAdapter(Context context, List<SyncAccount> syncAccounts) {
    this.layoutInflater = LayoutInflater.from(context);
    this.syncAccounts = syncAccounts;
  }

  @Override
  public Object getChild(int groupPosition, int childPosititon) {
    return syncAccounts.get(groupPosition).getChildList().get(childPosititon);
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    return childPosition;
  }

  @Override
  public View getChildView(int groupPosition, final int childPosition,
                           boolean isLastChild, View convertView, ViewGroup parent) {

    final String childText = ((AccountMetaData) getChild(groupPosition, childPosition)).label();

    if (convertView == null) {
      convertView = layoutInflater.inflate(R.layout.sync_backend_row, null);
    }

    ((TextView) convertView.findViewById(R.id.label)).setText(childText);
    return convertView;
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    return ((SyncAccount) getGroup(groupPosition)).getChildList().size();
  }

  @Override
  public Object getGroup(int groupPosition) {
    return syncAccounts.get(groupPosition);
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
    String headerTitle = ((SyncAccount) getGroup(groupPosition)).name;
    if (convertView == null) {
      convertView = layoutInflater.inflate(R.layout.sync_backend_row, null);
    }

    ((TextView) convertView.findViewById(R.id.label)).setText(headerTitle);


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

  public void setData(List<SyncAccount> accountList) {
    syncAccounts = accountList;
    notifyDataSetChanged();
  }

  public static class SyncAccount {
    public final String name;
    private final List<AccountMetaData> childList = new ArrayList<>();

    public SyncAccount(Account account) {
      this.name = account.name;
      childList.add(AccountMetaData.builder().setLabel("DEBUG").setUuid("DEBUG").setColor(-1).setCurrency("EUR").build());
    }

    List<AccountMetaData> getChildList() {
      return Collections.unmodifiableList(childList);
    }
  }
}
