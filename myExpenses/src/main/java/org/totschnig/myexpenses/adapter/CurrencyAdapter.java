package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.viewmodel.data.Currency;

public class CurrencyAdapter extends ArrayAdapter<Currency> {
  public CurrencyAdapter(@NonNull Context context) {
    super(context, android.R.layout.simple_spinner_item,
        android.R.id.text1);
    setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
  }
}
