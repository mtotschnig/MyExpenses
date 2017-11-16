package org.totschnig.myexpenses.adapter;


import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;

import java.util.List;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;

public class OperationTypeAdapter extends ArrayAdapter<Integer> {
  private boolean isTemplate;
  private boolean isSplitPart;

  public OperationTypeAdapter(Context context, List<Integer> allowedTypes,
                              boolean isTemplate, boolean isSplitPart) {
    super(context, android.R.layout.simple_spinner_item, allowedTypes);
    this.isTemplate = isTemplate;
    this.isSplitPart = isSplitPart;
    setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
  }

  @NonNull
  @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    View result = super.getView(position, convertView, parent);
    ((TextView) result).setText(getLabelResid(getItem(position)));
    return result;
  }

  @Override
  public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
    View result = super.getDropDownView(position, convertView, parent);
    ((TextView) result).setText(getLabelResid(getItem(position)));
    return result;
  }

  private int getLabelResid(Integer operationType) {
    switch (operationType) {
      case TYPE_SPLIT:
        return isTemplate ? R.string.menu_create_template_for_split : R.string.menu_create_split;
      case TYPE_TRANSFER:
        return isSplitPart ? R.string.menu_create_split_part_transfer :
            (isTemplate ? R.string.menu_create_template_for_transfer : R.string.menu_create_transfer);
      case TYPE_TRANSACTION:
        return isSplitPart ? R.string.menu_create_split_part_category :
            (isTemplate ? R.string.menu_create_template_for_transaction : R.string.menu_create_transaction);
      default:
        throw new IllegalStateException("Unknown operationtype " + operationType);
    }
  }
}
