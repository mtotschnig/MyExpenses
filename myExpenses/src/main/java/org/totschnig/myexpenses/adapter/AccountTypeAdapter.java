package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.AccountType;

import androidx.annotation.NonNull;

public class AccountTypeAdapter extends ArrayAdapter<AccountType>{

  private static final int TEXT_VIEW_RESOURCE_ID = android.R.id.text1;

  public AccountTypeAdapter(@NonNull Context context) {
    super(context, android.R.layout.simple_spinner_item, android.R.id.text1, AccountType.values());
    setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
  }
  @NonNull
  @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    View row = super.getView(position, convertView, parent);
    setText(position, row);
    return row;
  }

  @Override
  public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
    View row = super.getDropDownView(position, convertView, parent);
    setText(position, row);
    return row;
  }

  private void setText(int position, View row) {
    ((TextView) row.findViewById(TEXT_VIEW_RESOURCE_ID)).setText(getItem(position).toStringRes());
  }

}
