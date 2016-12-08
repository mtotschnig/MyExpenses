package org.totschnig.myexpenses.activity;

import android.accounts.AccountManager;
import android.os.Bundle;
import android.widget.Toast;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.fragment.SyncBackendList;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.WebDavBackendProviderFactory;

import java.io.File;

import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE;

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
    Bundle bundle = new Bundle(2);
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_ID,
        args.getString(GenericAccountService.KEY_SYNC_PROVIDER_ID));
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, filePath);
    createAccount(accountName, null, bundle);
  }

  //WebDav
  public void onFinishWebDavSetup(Bundle data) {
    String userName = data.getString(AccountManager.KEY_ACCOUNT_NAME);
    String password = data.getString(AccountManager.KEY_PASSWORD);
    String url = data.getString(GenericAccountService.KEY_SYNC_PROVIDER_URL);
    String certificate = data.getString(KEY_WEB_DAV_CERTIFICATE);
    String accountName = WebDavBackendProviderFactory.LABEL + " - " + url;

    Bundle bundle = new Bundle();
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_ID, String.valueOf(R.id.CREATE_BACKEND_WEBDAV_COMMAND));
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, url);
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_USERNAME, userName);
    if (certificate != null) {
      bundle.putString(KEY_WEB_DAV_CERTIFICATE, certificate);
    }
    createAccount(accountName, password, bundle);
  }

  private void createAccount(String accountName, String password, Bundle bundle) {
    if(((MyApplication) getApplicationContext()).createSyncAccount(accountName, password, bundle)) {
      ((SyncBackendList) getSupportFragmentManager().findFragmentById(R.id.backend_list)).loadData();
    }
  }

  @Override
  public void onCancelEditDialog() {

  }
}
