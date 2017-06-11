package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import java.util.List;

//https://stackoverflow.com/a/6022474/1199911
public class MultiSpinner extends android.support.v7.widget.AppCompatSpinner implements
    DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnCancelListener {

  private List<String> items;
  private boolean[] selected;
  private String defaultText;
  private MultiSpinnerListener listener;

  public MultiSpinner(Context context) {
    super(context);
  }

  public MultiSpinner(Context arg0, AttributeSet arg1) {
    super(arg0, arg1);
  }

  public MultiSpinner(Context arg0, AttributeSet arg1, int arg2) {
    super(arg0, arg1, arg2);
  }

  @Override
  public void onClick(DialogInterface dialog, int which, boolean isChecked) {
    selected[which] = isChecked;
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    // refresh text on spinner
    StringBuilder spinnerBuffer = new StringBuilder();
    boolean someUnselected = false;
    for (int i = 0; i < items.size(); i++) {
      if (selected[i]) {
        spinnerBuffer.append(items.get(i));
        spinnerBuffer.append(", ");
      } else {
        someUnselected = true;
      }
    }
    String spinnerText;
    if (someUnselected) {
      spinnerText = spinnerBuffer.toString();
      if (spinnerText.length() > 2)
        spinnerText = spinnerText.substring(0, spinnerText.length() - 2);
    } else {
      spinnerText = defaultText;
    }
    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
        android.R.layout.simple_spinner_item,
        new String[]{spinnerText});
    setAdapter(adapter);
    if (listener != null) {
      listener.onItemsSelected(selected);
    }
  }

  @Override
  public boolean performClick() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setMultiChoiceItems(
        items.toArray(new CharSequence[items.size()]), selected, this);
    builder.setPositiveButton(android.R.string.ok,
        (dialog, which) -> dialog.cancel());
    builder.setOnCancelListener(this);
    builder.show();
    return true;
  }

  public void setItems(List<String> items, String allText,
                       MultiSpinnerListener listener) {
    this.items = items;
    this.defaultText = allText;
    this.listener = listener;

    // all selected by default
    selected = new boolean[items.size()];
    for (int i = 0; i < selected.length; i++)
      selected[i] = true;

    // all text on the spinner
    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
        android.R.layout.simple_spinner_item, new String[]{allText});
    setAdapter(adapter);
  }

  public interface MultiSpinnerListener {
    void onItemsSelected(boolean[] selected);
  }
}