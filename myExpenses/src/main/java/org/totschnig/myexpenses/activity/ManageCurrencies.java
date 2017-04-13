package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Utils;

import java.util.Currency;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;

public class ManageCurrencies extends ProtectedFragmentActivity implements
    ConfirmationDialogFragment.ConfirmationDialogCheckedListener {

  private static final String KEY_NUMBER_FRACTION_DIGITS = "number_fraction_digtis";
  String currency;
  int numberFractionDigits;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      currency = savedInstanceState.getString(KEY_CURRENCY);
      numberFractionDigits = savedInstanceState.getInt(KEY_NUMBER_FRACTION_DIGITS);
    }
    setTheme(MyApplication.getThemeIdEditDialog());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.currency_list);
    setupToolbar(true);
    getSupportActionBar().setTitle(R.string.pref_custom_currency_title);
  }

  public void onFinishCurrencyEdit(String currency, String symbol, int numberFractionDigits) {
    this.currency = currency;
    handleSymbolUpdate(symbol);
    this.numberFractionDigits = numberFractionDigits;
    if (this.numberFractionDigits < 0 || this.numberFractionDigits > 8) {
      Toast.makeText(this, getString(R.string.validation_error_number_out_of_range, 0, 8), Toast.LENGTH_LONG).show();
    } else {
      handleFractionDigitsUpdate();
    }
  }

  private void handleSymbolUpdate(String symbol) {
    if (Money.storeCustomSymbol(currency, symbol)) {
      refresh();
    }
  }

  private void handleFractionDigitsUpdate() {
    int oldValue = Money.getFractionDigits(Currency.getInstance(this.currency));
    if (oldValue != this.numberFractionDigits) {
      if (Account.count(KEY_CURRENCY + "=?", new String[]{this.currency}) > 0) {
        String message = getString(R.string.warning_change_fraction_digits_1);
        int delta = oldValue - this.numberFractionDigits;
        message += " " + getString(
            delta > 0 ? R.string.warning_change_fraction_digits_2_multiplied :
                R.string.warning_change_fraction_digits_2_divided,
            Utils.pow(10, Math.abs(delta)));
        if (delta > 0) {
          message += " " + getString(R.string.warning_change_fraction_digits_3);
        }
        Bundle b = new Bundle();
        b.putInt(ConfirmationDialogFragment.KEY_TITLE,
            R.string.dialog_title_information);
        b.putString(ConfirmationDialogFragment.KEY_MESSAGE, message);
        b.putInt(ConfirmationDialogFragment.KEY_CHECKBOX_LABEL,
            R.string.warning_change_fraction_digits_checkbox_label);
        ConfirmationDialogFragment.newInstance(b)
            .show(getSupportFragmentManager(), "CHANGE_FRACTION_DIGITS");
      } else {
        apply();
      }
    }
  }

  protected void changeFractionDigitsDo() {
    startTaskExecution(TaskExecutionFragment.TASK_CHANGE_FRACTION_DIGITS,
        new String[]{currency}, numberFractionDigits, R.string.progress_dialog_saving);
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    Toast.makeText(this, getString(R.string.change_fraction_digits_result, (Integer) o, currency), Toast.LENGTH_LONG).show();
    refresh();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(KEY_CURRENCY, currency);
    outState.putInt(KEY_NUMBER_FRACTION_DIGITS, numberFractionDigits);
  }

  @Override
  public void onPositive(Bundle args, boolean checked) {
    if (checked) {
      changeFractionDigitsDo();
    } else {
      apply();
    }
  }

  @Override
  public void onNegative(Bundle args) {
  }

  @Override
  public void onDismissOrCancel(Bundle args) {
  }

  private void apply() {
    Money.storeCustomFractionDigits(currency, numberFractionDigits);
    getContentResolver().notifyChange(TransactionProvider.TEMPLATES_URI, null, false);
    getContentResolver().notifyChange(TransactionProvider.TRANSACTIONS_URI, null, false);
    getContentResolver().notifyChange(TransactionProvider.ACCOUNTS_URI, null, false);
    getContentResolver().notifyChange(TransactionProvider.UNCOMMITTED_URI, null, false);
    refresh();
  }

  private void refresh() {
    ((ArrayAdapter) ((ListFragment) getSupportFragmentManager().findFragmentById(R.id.currency_list))
        .getListAdapter()).notifyDataSetChanged();
  }
}
