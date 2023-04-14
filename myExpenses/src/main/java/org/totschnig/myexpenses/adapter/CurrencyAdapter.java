package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import androidx.annotation.NonNull;

public class CurrencyAdapter extends ArrayAdapter<Currency> {
  public CurrencyAdapter(@NonNull Context context, int resource) {
    super(context, resource, android.R.id.text1);
    setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
  }
}
