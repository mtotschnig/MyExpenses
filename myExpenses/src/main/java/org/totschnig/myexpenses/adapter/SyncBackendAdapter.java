package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.sync.json.AccountMetaData;

import java.util.List;

public class SyncBackendAdapter extends BaseExpandableListAdapter {

  private List<String> syncAccounts;
  private SparseArray<List<AccountMetaData>> accountMetaDataMap = new SparseArray<>();
  private LayoutInflater layoutInflater;

  public SyncBackendAdapter(Context context, List<String> syncAccounts) {
    this.layoutInflater = LayoutInflater.from(context);
    this.syncAccounts = syncAccounts;
  }

  @Override
  public Object getChild(int groupPosition, int childPosititon) {
    List<AccountMetaData> childList = getChildList(groupPosition);
    return childList != null ? childList.get(childPosititon) : null ;
  }

  private List<AccountMetaData> getChildList(int groupPosition) {
    return accountMetaDataMap.get(groupPosition);
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
      convertView = layoutInflater.inflate(R.layout.sync_backend_row, parent, false);
    }

    ((TextView) convertView.findViewById(R.id.label)).setText(childText);
    return convertView;
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    List<AccountMetaData> childList = getChildList(groupPosition);
    return childList != null ? childList.size() : 0;
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
    String headerTitle = (String) getGroup(groupPosition);
    if (convertView == null) {
      convertView = layoutInflater.inflate(R.layout.sync_backend_row, parent, false);
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

  public void setAccountList(List<String> accountList) {
    syncAccounts = accountList;
    notifyDataSetChanged();
  }

  public void setAccountMetadata(int groupPosition, List<AccountMetaData> accountMetaDataList) {
    accountMetaDataMap.put(groupPosition, accountMetaDataList);
    notifyDataSetChanged();
  }

  public boolean hasAccountMetdata(int groupPosition) {
    return accountMetaDataMap.get(groupPosition) != null;
  }
}
