package org.totschnig.myexpenses.adapter;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;

import java.util.List;

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

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View result = super.getView(position, convertView, parent);
    ((TextView) result).setText(getLabelResid(getItem(position)));
    return result;
  }

  @Override
  public View getDropDownView(int position, View convertView, ViewGroup parent) {
    View result = super.getDropDownView(position, convertView, parent);
    ((TextView) result).setText(getLabelResid(getItem(position)));
    return result;
  }
  protected int getLabelResid(int operationType) {
    switch (operationType) {
      case MyExpenses.TYPE_SPLIT:
        return R.string.menu_create_split;
      case MyExpenses.TYPE_TRANSFER:
        return isTemplate ? R.string.menu_create_template_for_transfer :
            (isSplitPart ? R.string.menu_create_split_part_transfer : R.string.menu_create_transfer);
      default:
        return isTemplate ? R.string.menu_create_template_for_transaction :
            (isSplitPart ? R.string.menu_create_split_part_category : R.string.menu_create_transaction);
    }
  }
}
