package org.totschnig.myexpenses.activity;

import java.util.Currency;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class ManageCurrencies extends ProtectedFragmentActivity implements
    EditTextDialogListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
      setTheme(MyApplication.getThemeId());
      super.onCreate(savedInstanceState);
      setContentView(R.layout.currency_list);
    }
    @Override
    public void onFinishEditDialog(Bundle args) {
      String currency = args.getString(DatabaseConstants.KEY_CURRENCY);
      try {
        int result = Integer.parseInt(args.getString(EditTextDialog.KEY_RESULT));
        if (result<0 ||result>8) {
          throw new IllegalArgumentException();
        }
        if (Money.fractionDigits(Currency.getInstance(currency))!=result) {
          startTaskExecution(TaskExecutionFragment.TASK_CHANGE_FRACTION_DIGITS,
              new String[] {currency}, result,R.string.progress_dialog_saving);

        }
      } catch (IllegalArgumentException e) {
        Toast.makeText(this, R.string.warning_fraction_digits_out_of_range, Toast.LENGTH_LONG).show();
      }
      //((FolderList) getSupportFragmentManager().findFragmentById(R.id.folder_list))
      //  .createNewFolder(args.getString(EditTextDialog.KEY_RESULT));
    }

    @Override
    public void onCancelEditDialog() {
    }
    @Override
    public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    ((ArrayAdapter) ((ListFragment) getSupportFragmentManager().findFragmentById(R.id.currency_list))
        .getListAdapter()).notifyDataSetChanged();
    }
}
