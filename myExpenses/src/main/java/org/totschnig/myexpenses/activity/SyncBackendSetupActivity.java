package org.totschnig.myexpenses.activity;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.SubMenu;
import android.widget.Toast;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.WebDavBackendProviderFactory;
import org.totschnig.myexpenses.task.SyncAccountTask;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;

import java.io.File;
import java.util.List;

import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_LABEL;
import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_URL;
import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_USERNAME;
import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE;
import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_FALLBACK_TO_CLASS1;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_FETCH_SYNC_ACCOUNT_DATA;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_WEBDAV_TEST_LOGIN;

public abstract class SyncBackendSetupActivity extends ProtectedFragmentActivity
    implements EditTextDialog.EditTextDialogListener {
  //LocalFileBackend
  public void onFinishEditDialog(Bundle args) {
    String filePath = args.getString(EditTextDialog.KEY_RESULT);
    File baseFolder = new File(filePath);
    if (!baseFolder.isDirectory()) {
      Toast.makeText(this, "No directory " + filePath, Toast.LENGTH_SHORT).show();
      return;
    }
    String accountName = args.getString(KEY_SYNC_PROVIDER_LABEL) + " - "
        + filePath;
    Bundle bundle = new Bundle(1);
    bundle.putString(KEY_SYNC_PROVIDER_URL, filePath);
    createAccount(accountName, null, bundle);
  }

  //WebDav
  public void onFinishWebDavSetup(Bundle data) {
    String userName = data.getString(AccountManager.KEY_ACCOUNT_NAME);
    String password = data.getString(AccountManager.KEY_PASSWORD);
    String url = data.getString(KEY_SYNC_PROVIDER_URL);
    String certificate = data.getString(KEY_WEB_DAV_CERTIFICATE);
    String accountName = WebDavBackendProviderFactory.LABEL + " - " + url;

    Bundle bundle = new Bundle();
    bundle.putString(KEY_SYNC_PROVIDER_URL, url);
    bundle.putString(KEY_SYNC_PROVIDER_USERNAME, userName);
    if (certificate != null) {
      bundle.putString(KEY_WEB_DAV_CERTIFICATE, certificate);
    }
    if (data.getBoolean(KEY_WEB_DAV_FALLBACK_TO_CLASS1)) {
      bundle.putString(KEY_WEB_DAV_FALLBACK_TO_CLASS1, "1");
    }
    createAccount(accountName, password, bundle);
  }

  //Google Drive
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == SYNC_BACKEND_SETUP_REQUEST && resultCode == RESULT_OK && intent != null) {
        createAccount(intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME), null,
            intent.getBundleExtra(AccountManager.KEY_USERDATA));
    }
  }

  protected void createAccount(String accountName, String password, Bundle bundle) {
    Bundle args = new Bundle();
    args.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
    args.putString(AccountManager.KEY_PASSWORD, password);
    args.putParcelable(AccountManager.KEY_USERDATA, bundle);
    args.putBoolean(SyncAccountTask.KEY_RETURN_REMOTE_DATA_LIST, createAccountTaskShouldReturnDataList());
    getSupportFragmentManager()
        .beginTransaction()
        .add(TaskExecutionFragment.newInstanceWithBundle(args, TASK_CREATE_SYNC_ACCOUNT), ASYNC_TAG)
        .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_fetching_data_from_sync_backend), PROGRESS_TAG)
        .commit();
  }

  public void fetchAccountData(String accountName) {
    Bundle args = new Bundle();
    args.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
    args.putBoolean(SyncAccountTask.KEY_RETURN_REMOTE_DATA_LIST, createAccountTaskShouldReturnDataList());
    getSupportFragmentManager()
        .beginTransaction()
        .add(TaskExecutionFragment.newInstanceWithBundle(args, TASK_FETCH_SYNC_ACCOUNT_DATA), ASYNC_TAG)
        .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_fetching_data_from_sync_backend), PROGRESS_TAG)
        .commit();
  }

  protected boolean createAccountTaskShouldReturnDataList() {
    return false;
  }

  public void onCancelEditDialog() {

  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    Result result = (Result) o;
    switch (taskId) {
      case TASK_WEBDAV_TEST_LOGIN: {
        getWebdavFragment().onTestLoginResult(result);
        break;
      }
    }
  }

  public void addSyncProviderMenuEntries(SubMenu subMenu, List<SyncBackendProviderFactory> backendProviders) {
    for (SyncBackendProviderFactory factory: backendProviders) {
      subMenu.add(Menu.NONE, factory.getId(), Menu.NONE, factory.getLabel());
    }
  }

  public @Nullable
  SyncBackendProviderFactory getSyncBackendProviderFactoryById(
      List<SyncBackendProviderFactory> backendProviders, int id) {
    for (SyncBackendProviderFactory factory: backendProviders) {
      if (factory.getId() == id) {
        return factory;
      }
    }
    return null;
  }

  protected SetupWebdavDialogFragment getWebdavFragment() {
    return (SetupWebdavDialogFragment) getSupportFragmentManager().findFragmentByTag(
        WebDavBackendProviderFactory.WEBDAV_SETUP);
  }
}
