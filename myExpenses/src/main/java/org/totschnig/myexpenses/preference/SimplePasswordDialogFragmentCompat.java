package org.totschnig.myexpenses.preference;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;

import org.totschnig.myexpenses.R;

public class SimplePasswordDialogFragmentCompat extends PreferenceDialogFragmentCompat {

  private TextInputEditText passwordInput;

  public static SimplePasswordDialogFragmentCompat newInstance(String key) {
    SimplePasswordDialogFragmentCompat fragment = new SimplePasswordDialogFragmentCompat();
    Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
    passwordInput = view.findViewById(R.id.passwordEdit);
    String currentValue = ((SimplePasswordPreference) getPreference()).getValue();
    if (currentValue != null) {
      passwordInput.setText(currentValue);
    }
  }

  @Override
  public void onDialogClosed(boolean positiveResult) {
    final SimplePasswordPreference preference = (SimplePasswordPreference) getPreference();

    if (!positiveResult) {
      return;
    }
    preference.setValue(passwordInput.getText().toString());
  }
}
