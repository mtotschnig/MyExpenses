package org.totschnig.myexpenses.activity;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.SubMenu;
import android.widget.Toast;

import com.dropbox.core.android.Auth;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.WebDavBackendProviderFactory;
import org.totschnig.myexpenses.task.SyncAccountTask;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;

import java.io.File;
import java.util.List;

import icepick.Icepick;
import icepick.State;

import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_URL;
import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_USERNAME;
import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE;
import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_FALLBACK_TO_CLASS1;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_DROPBOX_SETUP;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_FETCH_SYNC_ACCOUNT_DATA;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_WEBDAV_TEST_LOGIN;

public abstract class SyncBackendSetupActivity extends ProtectedFragmentActivity
    implements EditTextDialog.EditTextDialogListener {
  protected List<SyncBackendProviderFactory> backendProviders;
  @State
  int selectedFactoryId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    backendProviders = ServiceLoader.load(this);
    Icepick.restoreInstanceState(this, savedInstanceState);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Icepick.saveInstanceState(this, outState);
  }

  //LocalFileBackend
  public void onFinishEditDialog(Bundle args) {
    String filePath = args.getString(EditTextDialog.KEY_RESULT);
    File baseFolder = new File(filePath);
    if (!baseFolder.isDirectory()) {
      Toast.makeText(this, "No directory " + filePath, Toast.LENGTH_SHORT).show();
      return;
    }
    String accountName = getSyncBackendProviderFactoryByIdOrThrow(R.id.SYNC_BACKEND_LOCAL).buildAccountName(filePath);
    Bundle bundle = new Bundle(1);
    bundle.putString(KEY_SYNC_PROVIDER_URL, filePath);
    createAccount(accountName, null, null, bundle);
  }

  //WebDav
  public void onFinishWebDavSetup(Bundle data) {
    String userName = data.getString(AccountManager.KEY_ACCOUNT_NAME);
    String password = data.getString(AccountManager.KEY_PASSWORD);
    String url = data.getString(KEY_SYNC_PROVIDER_URL);
    String certificate = data.getString(KEY_WEB_DAV_CERTIFICATE);
    String accountName = getSyncBackendProviderFactoryByIdOrThrow(R.id.SYNC_BACKEND_WEBDAV).buildAccountName(url);

    Bundle bundle = new Bundle();
    bundle.putString(KEY_SYNC_PROVIDER_URL, url);
    bundle.putString(KEY_SYNC_PROVIDER_USERNAME, userName);
    if (certificate != null) {
      bundle.putString(KEY_WEB_DAV_CERTIFICATE, certificate);
    }
    if (data.getBoolean(KEY_WEB_DAV_FALLBACK_TO_CLASS1)) {
      bundle.putString(KEY_WEB_DAV_FALLBACK_TO_CLASS1, "1");
    }
    createAccount(accountName, password, null, bundle);
  }

  //Dropbox
  @Override
  protected void onResume() {
    super.onResume();
    if (selectedFactoryId == R.id.SYNC_BACKEND_DROPBOX) {
      startTaskExecution(TaskExecutionFragment.TASK_DROPBOX_SETUP, new Bundle(), R.string.progress_dialog_checking_sync_backend);
    }
  }

  public void startSetup(SyncBackendProviderFactory factory) {
    selectedFactoryId = factory.getId();
    factory.startSetup(this);
  }

  //Google Drive
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == SYNC_BACKEND_SETUP_REQUEST && resultCode == RESULT_OK && intent != null) {
        createAccount(intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME), null,
            null, intent.getBundleExtra(AccountManager.KEY_USERDATA));
    }
  }

  protected void createAccount(String accountName, String password, String authToken, Bundle bundle) {
    Bundle args = new Bundle();
    args.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
    args.putString(AccountManager.KEY_PASSWORD, password);
    args.putString(AccountManager.KEY_AUTHTOKEN, authToken);
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
      case TASK_DROPBOX_SETUP: {
        if (result.success) {
          String accountName = getSyncBackendProviderFactoryByIdOrThrow(R.id.SYNC_BACKEND_DROPBOX).buildAccountName((String) result.extra[0]);
          createAccount(accountName, null, Auth.getOAuth2Token(), null);
        } else {
          Toast.makeText(this, result.print(this), Toast.LENGTH_LONG).show();
        }
      }
    }
  }

  public void addSyncProviderMenuEntries(SubMenu subMenu) {
    for (SyncBackendProviderFactory factory: backendProviders) {
      subMenu.add(Menu.NONE, factory.getId(), Menu.NONE, factory.getLabel());
    }
  }

  public @Nullable
  SyncBackendProviderFactory getSyncBackendProviderFactoryById(int id) {
    try {
      return getSyncBackendProviderFactoryByIdOrThrow(id);
    } catch (IllegalStateException e) {
      return null;
    }
  }

  public @NonNull
  SyncBackendProviderFactory getSyncBackendProviderFactoryByIdOrThrow(int id) throws IllegalStateException {
    for (SyncBackendProviderFactory factory: backendProviders) {
      if (factory.getId() == id) {
        return factory;
      }
    }
    throw new IllegalStateException();
  }

  protected SetupWebdavDialogFragment getWebdavFragment() {
    return (SetupWebdavDialogFragment) getSupportFragmentManager().findFragmentByTag(
        WebDavBackendProviderFactory.WEBDAV_SETUP);
  }
}
