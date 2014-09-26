package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.view.Menu;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.EditTextDialog.EditTextDialogListener;
import org.totschnig.myexpenses.fragment.FolderList;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 12/23/11 12:53 AM
 *
 */
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
      //((FolderList) getSupportFragmentManager().findFragmentById(R.id.folder_list))
      //  .createNewFolder(args.getString(EditTextDialog.KEY_RESULT));
    }

    @Override
    public void onCancelEditDialog() {
    }
}
