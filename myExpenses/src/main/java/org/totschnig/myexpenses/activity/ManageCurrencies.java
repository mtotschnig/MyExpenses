package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditCurrencyDialog;

public class ManageCurrencies extends ProtectedFragmentActivity  {

  private static final String KEY_NUMBER_FRACTION_DIGITS = "number_fraction_digtis";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(getThemeIdEditDialog());
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
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    //showSnackbar(getString(R.string.change_fraction_digits_result, (Integer) o, currency), Snackbar.LENGTH_LONG);
    refreshList();
  }

  private void refreshList() {
    ((ArrayAdapter) ((ListFragment) getSupportFragmentManager().findFragmentById(R.id.currency_list))
        .getListAdapter()).notifyDataSetChanged();
  }
}
