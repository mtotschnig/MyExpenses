package org.totschnig.myexpenses.activity;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Menu;
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
    public boolean onCreateOptionsMenu(Menu menu) {
    // TODO overriden because help menu is not defined
    return true;
    }
    @Override
    public void onFinishEditDialog(Bundle args) {
      String currency = args.getString(DatabaseConstants.KEY_CURRENCY);
      try {
        int result = Integer.parseInt(args.getString(EditTextDialog.KEY_RESULT));
        if (result<0 ||result>8) {
          throw new IllegalArgumentException();
        }
        SharedPreferencesCompat.apply(
            MyApplication.getInstance().getSettings().edit()
            .putInt(currency+Money.KEY_CUSTOM_FRACTION_DIGITS, result));
        ((ArrayAdapter) ((ListFragment) getSupportFragmentManager().findFragmentById(R.id.currency_list))
          .getListAdapter()).notifyDataSetChanged();
      } catch (IllegalArgumentException e) {
        Toast.makeText(this, R.string.warning_fraction_digits_out_of_range, Toast.LENGTH_LONG).show();
      }
      //((FolderList) getSupportFragmentManager().findFragmentById(R.id.folder_list))
      //  .createNewFolder(args.getString(EditTextDialog.KEY_RESULT));
    }

    @Override
    public void onCancelEditDialog() {
    }
}
