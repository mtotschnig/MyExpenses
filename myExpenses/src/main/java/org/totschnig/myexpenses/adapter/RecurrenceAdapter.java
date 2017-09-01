package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Plan;

import java.util.Arrays;

public class RecurrenceAdapter extends ArrayAdapter<Plan.Recurrence> {

  public RecurrenceAdapter(Context context, Plan.Recurrence... excludedValues) {
    super(context, android.R.layout.simple_spinner_item,
        excludedValues == null ? Plan.Recurrence.values() :
        Stream.of(Plan.Recurrence.values())
            .filter(value -> Arrays.asList(excludedValues).indexOf(value) == -1)
            .toArray(size -> new Plan.Recurrence[size]));
    setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View result = super.getView(position, convertView, parent);
    ((TextView) result).setText(getItem(position).getLabel(getContext()));
    return result;
  }

  @Override
  public View getDropDownView(int position, View convertView, ViewGroup parent) {
    View result = super.getDropDownView(position, convertView, parent);
    ((TextView) result).setText(getItem(position).getLabel(getContext()));
    return result;
  }
}
