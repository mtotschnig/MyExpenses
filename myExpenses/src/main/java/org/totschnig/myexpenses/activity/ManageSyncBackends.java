package org.totschnig.myexpenses.activity;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.Toast;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.SelectUnSyncedAccountDialogFragment;
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment;
import org.totschnig.myexpenses.fragment.SyncBackendList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.WebDavBackendProviderFactory;
import org.totschnig.myexpenses.util.Result;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_LABEL;
import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_URL;
import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_SYNC_PROVIDER_USERNAME;
import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE;
import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_FALLBACK_TO_CLASS1;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_LINK_LOCAL;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_LINK_REMOTE;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_LINK_SAVE;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_REMOVE_BACKEND;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_UNLINK;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_WEBDAV_TEST_LOGIN;

public class ManageSyncBackends extends ProtectedFragmentActivity implements
    EditTextDialog.EditTextDialogListener, ContribIFace {

  private static final String KEY_PACKED_POSITION = "packedPosition";
  private Account newAccount;

  private List<SyncBackendProviderFactory> backendProviders;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    backendProviders = ServiceLoader.load(this);
    setTheme(MyApplication.getThemeIdEditDialog());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_sync_backends);
    setupToolbar(true);
    setTitle(R.string.pref_manage_sync_backends_title);
    if (savedInstanceState == null && !ContribFeature.SYNCHRONIZATION.isAvailable()) {
      contribFeatureRequested(ContribFeature.SYNCHRONIZATION, null);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.sync_backend, menu);
    SubMenu createSubMenu = menu.findItem(R.id.CREATE_COMMAND).getSubMenu();
    for (int i = 0, backendProvidersSize = backendProviders.size(); i < backendProvidersSize; i++) {
      createSubMenu.add(Menu.NONE, i, Menu.NONE, backendProviders.get(i).getLabel());
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() < backendProviders.size()) {
      contribFeatureRequested(ContribFeature.SYNCHRONIZATION, item.getItemId());
      return true;
    }
    return super.onOptionsItemSelected(item);
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
      return;
    }
  }

  @Override
  public void onCancelEditDialog() {

  }

  @Override
  public void onPositive(Bundle args) {
    switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
      case R.id.SYNC_UNLINK_COMMAND: {
        startTaskExecution(TASK_SYNC_UNLINK,
            new String[]{args.getString(DatabaseConstants.KEY_UUID)}, null, 0);
        break;
      }
      case R.id.SYNC_REMOVE_BACKEND_COMMAND: {
        startTaskExecution(TASK_SYNC_REMOVE_BACKEND,
            new String[]{args.getString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)}, null, 0);
        break;
      }
      case R.id.SYNC_LINK_COMMAND_LOCAL_DO: {
        Account account = getListFragment().getAccountForSync(args.getLong(KEY_PACKED_POSITION));
        startTaskExecution(TASK_SYNC_LINK_LOCAL,
            new String[]{account.uuid}, account.getSyncAccountName(), 0);
        break;
      }
      case R.id.SYNC_LINK_COMMAND_REMOTE_DO: {
        Account account = getListFragment().getAccountForSync(args.getLong(KEY_PACKED_POSITION));
        startTaskExecution(TASK_SYNC_LINK_REMOTE,
            null, account, 0);
        break;
      }
    }
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch (command) {
      case R.id.SYNC_LINK_COMMAND_LOCAL: {
        Bundle b = new Bundle();
        b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
            getString(R.string.dialog_confirm_sync_link_local));
        b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.SYNC_LINK_COMMAND_LOCAL_DO);
        b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.dialog_command_sync_link_local);
        b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, android.R.string.cancel);
        b.putLong(KEY_PACKED_POSITION, (Long) tag);
        ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(), "SYNC_LINK_LOCAL");
        break;
      }
      case R.id.SYNC_LINK_COMMAND_REMOTE: {
        Bundle b = new Bundle();
        b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
            getString(R.string.dialog_confirm_sync_link_remote));
        b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.SYNC_LINK_COMMAND_REMOTE_DO);
        b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.dialog_command_sync_link_remote);
        b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, android.R.string.cancel);
        b.putLong(KEY_PACKED_POSITION, (Long) tag);
        ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(), "SYNC_LINK_REMOTE");
        break;
      }
    }
    return super.dispatchCommand(command, tag);
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
      case TASK_CREATE_SYNC_ACCOUNT: {
        if (result.success) {
          getListFragment().reloadAccountList();
          if (result.extra != null) {
            showSelectUnsyncedAccount((String) result.extra[0]);
          }
        }
        break;
      }
      case TASK_SYNC_REMOVE_BACKEND: {
        if (result.success) {
          getListFragment().reloadAccountList();
        }
        break;
      }
      case TASK_SYNC_LINK_SAVE: {
        Toast.makeText(this, result.print(this), Toast.LENGTH_LONG).show();
        //fall through
      }
      case TASK_SYNC_UNLINK:
      case TASK_SYNC_LINK_LOCAL:
      case TASK_SYNC_LINK_REMOTE: {
        if (result.success) {
          getListFragment().reloadLocalAccountInfo();
        }
        break;
      }
    }
  }

  protected void showSelectUnsyncedAccount(String accountName) {
    //if we were called from AccountEdit, we do not show the unsynced account selection
    //since we suppose that user wants to create one account for the account he is editing
    if (getCallingActivity() == null) {
      SelectUnSyncedAccountDialogFragment.newInstance(accountName)
          .show(getSupportFragmentManager(), "SELECT_UNSYNCED");
    }
  }

  private SyncBackendList getListFragment() {
    return (SyncBackendList) getSupportFragmentManager().findFragmentById(R.id.backend_list);
  }

  private SetupWebdavDialogFragment getWebdavFragment() {
    return (SetupWebdavDialogFragment) getSupportFragmentManager().findFragmentByTag(
        WebDavBackendProviderFactory.WEBDAV_SETUP);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.SYNC_DOWNLOAD_COMMAND:
        if (PrefKey.NEW_ACCOUNT_ENABLED.getBoolean(true)) {
          newAccount = getListFragment().getAccountForSync(
              ((ExpandableListContextMenuInfo) item.getMenuInfo()).packedPosition);
          startDbWriteTask(false);
        } else {
          contribFeatureRequested(ContribFeature.ACCOUNTS_UNLIMITED, null);
        }
        return true;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  public Model getObject() {
    return newAccount;
  }

  @Override
  public void contribFeatureCalled(ContribFeature feature, Serializable tag) {
    if (tag instanceof Integer) {
      feature.recordUsage();
      backendProviders.get((Integer) tag).startSetup(this);
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {

  }

}
