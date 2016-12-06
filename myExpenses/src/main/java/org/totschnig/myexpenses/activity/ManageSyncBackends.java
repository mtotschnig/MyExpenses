package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.widget.Toast;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.fragment.SyncBackendList;
import org.totschnig.myexpenses.sync.GenericAccountService;

import java.io.File;

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
    String filePath = args.getString(EditTextDialog.KEY_RESULT);
    File baseFolder = new File(filePath);
    if (!baseFolder.isDirectory()) {
      Toast.makeText(this, "No directory " + filePath, Toast.LENGTH_SHORT).show();
      return;
    }
    String accountName = args.getString(GenericAccountService.KEY_SYNC_PROVIDER_LABEL) + " - "
        + filePath;
    Bundle bundle = new Bundle(1);
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_ID,
        args.getString(GenericAccountService.KEY_SYNC_PROVIDER_ID));
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_URI, filePath);
    if(((MyApplication) getApplicationContext()).createSyncAccount(accountName, bundle)) {
      ((SyncBackendList) getSupportFragmentManager().findFragmentById(R.id.backend_list)).loadData();
    }
  }

  @Override
  public void onCancelEditDialog() {

  }
}
