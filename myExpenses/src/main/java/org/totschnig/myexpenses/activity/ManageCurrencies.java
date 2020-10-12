package org.totschnig.myexpenses.activity;

import android.os.Bundle;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditCurrencyDialog;

import androidx.fragment.app.Fragment;

public class ManageCurrencies extends ProtectedFragmentActivity  {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.currency_list);
    setupToolbar(true);
    getSupportActionBar().setTitle(R.string.pref_custom_currency_title);
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    if (command == R.id.CREATE_COMMAND) {
      EditCurrencyDialog.newInstance(null).show(getSupportFragmentManager(), "NEW_CURRENCY");
    }
    return false;
  }

  @Override
  public Fragment getCurrentFragment() {
    return getSupportFragmentManager().findFragmentById(R.id.currency_list);
  }
}
