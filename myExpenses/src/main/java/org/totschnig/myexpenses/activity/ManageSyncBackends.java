package org.totschnig.myexpenses.activity;

import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.fragment.SyncBackendList;
import org.totschnig.myexpenses.sync.GenericAccountService;

public class ManageSyncBackends extends ProtectedFragmentActivity implements
    EditTextDialog.EditTextDialogListener {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdEditDialog());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_sync_backends);
    setupToolbar(true);
    setTitle(R.string.pref_manage_sync_backends_title);
  }

  //LocalFileBackend
  @Override
  public void onFinishEditDialog(Bundle args) {
    if(((MyApplication) getApplicationContext()).createSyncAccount(
        args.getString(EditTextDialog.KEY_RESULT),
        args.getString(GenericAccountService.KEY_SYNC_PROVIDER))) {
      ((SyncBackendList) getSupportFragmentManager().findFragmentById(R.id.backend_list)).loadData();
    }
  }

  @Override
  public void onCancelEditDialog() {

  }
}
