package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Utils;

import java.util.Currency;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;

public class ManageCurrencies extends ProtectedFragmentActivity implements
    EditTextDialogListener, ConfirmationDialogFragment.ConfirmationDialogCheckedListener {

  private static final String KEY_RESULT = "result";
  String mCurrency;
  int mResult;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      mCurrency = savedInstanceState.getString(KEY_CURRENCY);
      mResult = savedInstanceState.getInt(KEY_RESULT);
    }
    setTheme(MyApplication.getThemeIdEditDialog());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.currency_list);
    setupToolbar(true);
    getSupportActionBar().setTitle(R.string.pref_custom_currency_title);
  }

  @Override
  public void onFinishEditDialog(Bundle args) {
    mCurrency = args.getString(KEY_CURRENCY);
    try {
      mResult = Integer.parseInt(args.getString(EditTextDialog.KEY_RESULT));
      if (mResult < 0 || mResult > 8) {
        throw new IllegalArgumentException();
      }
      int oldValue = Money.getFractionDigits(Currency.getInstance(mCurrency));
      if (oldValue != mResult) {
        if (Account.count(KEY_CURRENCY + "=?", new String[]{mCurrency}) > 0) {
          String message = getString(R.string.warning_change_fraction_digits_1);
          int delta = oldValue - mResult;
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
    } catch (IllegalArgumentException e) {
      Toast.makeText(this, R.string.warning_fraction_digits_out_of_range, Toast.LENGTH_LONG).show();
    }
  }

  protected void changeFractionDigitsDo() {
    startTaskExecution(TaskExecutionFragment.TASK_CHANGE_FRACTION_DIGITS,
        new String[]{mCurrency}, mResult, R.string.progress_dialog_saving);
  }

  @Override
  public void onCancelEditDialog() {
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    Toast.makeText(this, getString(R.string.change_fraction_digits_result, (Integer) o, mCurrency), Toast.LENGTH_LONG).show();
    refresh();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(KEY_CURRENCY, mCurrency);
    outState.putInt(KEY_RESULT, mResult);
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
    Money.storeCustomFractionDigits(mCurrency, mResult);
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
