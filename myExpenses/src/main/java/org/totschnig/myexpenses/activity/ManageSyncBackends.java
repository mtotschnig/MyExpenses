package org.totschnig.myexpenses.activity;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.Toast;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment;
import org.totschnig.myexpenses.fragment.SyncBackendList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.WebDavBackendProviderFactory;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;

import java.io.File;

import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE;

public class ManageSyncBackends extends ProtectedFragmentActivity implements
    EditTextDialog.EditTextDialogListener {

  private Account newAccount;

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
    Bundle args = new Bundle();
    args.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
    args.putString(AccountManager.KEY_PASSWORD, password);
    args.putParcelable(AccountManager.KEY_USERDATA, bundle);
    getSupportFragmentManager()
        .beginTransaction()
        .add(TaskExecutionFragment.newInstanceWithBundle(args, TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT), ProtectionDelegate.ASYNC_TAG)
        .commit();
  }

  @Override
  public void onCancelEditDialog() {

  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    Result result = (Result) o;
    switch (taskId) {
      case TaskExecutionFragment.TASK_WEBDAV_TEST_LOGIN:
        getWebdavFragment().onTestLoginResult(result);
        break;
      case TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT:
        if (result.success) {
          getListFragment().loadData();
        }
    }
  }

  private SyncBackendList getListFragment() {
    return (SyncBackendList) getSupportFragmentManager().findFragmentById(R.id.backend_list);
  }

  private SetupWebdavDialogFragment getWebdavFragment() {
    return (SetupWebdavDialogFragment) getSupportFragmentManager().findFragmentByTag(WebDavBackendProviderFactory.WEBDAV_SETUP);
  }


  @Override
  public boolean onContextItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case R.id.SYNC_DOWNLOAD_COMMAND:
        newAccount = getListFragment().getAccountForSync(
            ((ExpandableListContextMenuInfo) item.getMenuInfo()).packedPosition);
        startDbWriteTask(false);
        return true;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  public void onPostExecute(Object result) {
    super.onPostExecute(result);
    requestSync(newAccount.getSyncAccountName(), newAccount.getId());
  }

  public void requestSync(String syncAccountName, Long id) {
    Bundle bundle = new Bundle();
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
    if (!id.equals(0L)) {
      bundle.putLong(DatabaseConstants.KEY_ACCOUNTID, id);
    }
    ContentResolver.requestSync(GenericAccountService.GetAccount(syncAccountName),
        TransactionProvider.AUTHORITY, bundle);
  }

  @Override
  public Model getObject() {
    return newAccount;
  }
  
}
