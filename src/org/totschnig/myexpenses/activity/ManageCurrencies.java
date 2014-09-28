package org.totschnig.myexpenses.activity;

import java.util.Currency;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class ManageCurrencies extends ProtectedFragmentActivity implements
    EditTextDialogListener,ConfirmationDialogListener {

    private static final String KEY_RESULT = "result";
    String mCurrency;
    int mResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      if (savedInstanceState!=null) {
        mCurrency = savedInstanceState.getString(KEY_CURRENCY);
        mResult = savedInstanceState.getInt(KEY_RESULT);
      }
      setTheme(MyApplication.getThemeId());
      super.onCreate(savedInstanceState);
      setContentView(R.layout.currency_list);
    }
    @Override
    public void onFinishEditDialog(Bundle args) {
      mCurrency = args.getString(KEY_CURRENCY);
      try {
        mResult = Integer.parseInt(args.getString(EditTextDialog.KEY_RESULT));
        if (mResult<0 ||mResult>8) {
          throw new IllegalArgumentException();
        }
        int oldValue = Money.fractionDigits(Currency.getInstance(mCurrency));
        if (oldValue!=mResult) {
          if (oldValue > mResult &&
              Account.count(KEY_CURRENCY+"=?", new String[]{mCurrency})>0) {
            //if we are reducing the number we warn
            Bundle b = new Bundle();
            b.putInt(ConfirmationDialogFragment.KEY_TITLE,
                R.string.dialog_title_information);
            b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
                getString(R.string.warning_change_fraction_digits,mCurrency));
            ConfirmationDialogFragment.newInstance(b)
              .show(getSupportFragmentManager(),"CHANGE_FRACTION_DIGITS");
          } else {
            changeFractionDigitsDo(mResult);
          }
        }
      } catch (IllegalArgumentException e) {
        Toast.makeText(this, R.string.warning_fraction_digits_out_of_range, Toast.LENGTH_LONG).show();
      }
      //((FolderList) getSupportFragmentManager().findFragmentById(R.id.folder_list))
      //  .createNewFolder(args.getString(EditTextDialog.KEY_RESULT));
    }
    protected void changeFractionDigitsDo(int result) {
      startTaskExecution(TaskExecutionFragment.TASK_CHANGE_FRACTION_DIGITS,
          new String[] {mCurrency}, result,R.string.progress_dialog_saving);
    }

    @Override
    public void onCancelEditDialog() {
    }
    @Override
    public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    Toast.makeText(this, getString(R.string.change_fraction_digits_result,(Integer)o,mCurrency),Toast.LENGTH_LONG).show();
    ((ArrayAdapter) ((ListFragment) getSupportFragmentManager().findFragmentById(R.id.currency_list))
        .getListAdapter()).notifyDataSetChanged();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(KEY_CURRENCY, mCurrency);
    outState.putInt(KEY_RESULT, mResult);
    }
    @Override
    public void onPositive(Bundle args) {
      changeFractionDigitsDo(mResult);
    }
    @Override
    public void onNegative(Bundle args) {
    }
    @Override
    public void onDismissOrCancel(Bundle args) {
    }
}
