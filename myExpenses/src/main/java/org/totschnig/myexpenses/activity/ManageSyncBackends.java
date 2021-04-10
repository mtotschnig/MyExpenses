package org.totschnig.myexpenses.activity;

import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import com.annimon.stream.Exceptional;
import com.dropbox.core.android.Auth;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.select.SelectUnSyncedAccountDialogFragment;
import org.totschnig.myexpenses.fragment.SyncBackendList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.task.SyncAccountTask;
import org.totschnig.myexpenses.util.Result;

import java.io.Serializable;

import icepick.State;

import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_REPAIR_SYNC_BACKEND;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_LINK_LOCAL;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_LINK_REMOTE;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_LINK_SAVE;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_REMOVE_BACKEND;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_UNLINK;

public class ManageSyncBackends extends SyncBackendSetupActivity implements ContribIFace {

  private static final int REQUEST_REPAIR_INTENT= 1;

  private static final String KEY_ACCOUNT = "account";
  public static final String ACTION_REFRESH_LOGIN = "refreshLogin";
  private Account newAccount;

  @State
  String dropBoxTokenRequestPendingForAccount = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_sync_backends);
    setupToolbar(true);
    setTitle(R.string.pref_manage_sync_backends_title);
    if (savedInstanceState == null) {
      if (!licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION)) {
        contribFeatureRequested(ContribFeature.SYNCHRONIZATION, null);
      }
      sanityCheck();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (dropBoxTokenRequestPendingForAccount != null) {
      final String accessToken = Auth.getOAuth2Token();
      if (accessToken != null) {
        AccountManager.get(this).setAuthToken(
            GenericAccountService.getAccount(dropBoxTokenRequestPendingForAccount),
            GenericAccountService.AUTH_TOKEN_TYPE,
            accessToken);
      } else {
        showSnackbar("Dropbox Oauth Token is null");
      }
      dropBoxTokenRequestPendingForAccount = null;
    } else {
      if (ACTION_REFRESH_LOGIN.equals(getIntent().getAction())) {
        requestDropboxAccess(getIntent().getStringExtra(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME));
      }
    }
  }

  private void sanityCheck() {
    for (SyncBackendProviderFactory factory: backendProviders) {
      Intent repairIntent = factory.getRepairIntent(this);
      if (repairIntent != null) {
        startActivityForResult(repairIntent, REQUEST_REPAIR_INTENT);
        //for the moment we handle only one problem at one time
        break;
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.sync_backend, menu);
    addSyncProviderMenuEntries(menu.findItem(R.id.CREATE_COMMAND).getSubMenu());
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (getSyncBackendProviderFactoryById(item.getItemId()) != null) {
      contribFeatureRequested(ContribFeature.SYNCHRONIZATION, item.getItemId());
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onPositive(Bundle args) {
    int anInt = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE);
    if (anInt == R.id.SYNC_UNLINK_COMMAND) {
      startTaskExecution(TASK_SYNC_UNLINK,
          new String[]{args.getString(DatabaseConstants.KEY_UUID)}, null, 0);
      return;
    } else if (anInt == R.id.SYNC_REMOVE_BACKEND_COMMAND) {
      startTaskExecution(TASK_SYNC_REMOVE_BACKEND,
          new String[]{args.getString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)}, null, 0);
      return;
    } else if (anInt == R.id.SYNC_LINK_COMMAND_LOCAL_DO) {
      Account account = (Account) args.getSerializable(KEY_ACCOUNT);
      startTaskExecution(TASK_SYNC_LINK_LOCAL,
          new String[]{account.getUuid()}, account.getSyncAccountName(), 0);
      return;
    } else if (anInt == R.id.SYNC_LINK_COMMAND_REMOTE_DO) {
      Account account = (Account) args.getSerializable(KEY_ACCOUNT);
      startTaskExecution(TASK_SYNC_LINK_REMOTE,
          null, account, 0);
      return;
    }
    super.onPositive(args);
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    if (command == R.id.SYNC_LINK_COMMAND_LOCAL) {
      Bundle b = new Bundle();
      b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
          getString(R.string.dialog_confirm_sync_link_local));
      b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.SYNC_LINK_COMMAND_LOCAL_DO);
      b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.dialog_command_sync_link_local);
      b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, android.R.string.cancel);
      b.putSerializable(KEY_ACCOUNT, (Account) tag);
      ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(), "SYNC_LINK_LOCAL");
      return true;
    } else if (command == R.id.SYNC_LINK_COMMAND_REMOTE) {
      Bundle b = new Bundle();
      b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
          getString(R.string.dialog_confirm_sync_link_remote));
      b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.SYNC_LINK_COMMAND_REMOTE_DO);
      b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.dialog_command_sync_link_remote);
      b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, android.R.string.cancel);
      b.putSerializable(KEY_ACCOUNT, (Account) tag);
      ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(), "SYNC_LINK_REMOTE");
      return true;
    } else if (command == R.id.TRY_AGAIN_COMMAND) {
      sanityCheck();
      return true;
    }
    return false;
  }

  @Override //DbWriteFragment
  public void onPostExecute(Uri result) {
    super.onPostExecute(result);
    if (result == null) {
      showSnackbar(String.format("There was an error saving account %s", newAccount.getLabel()));
    }
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    switch (taskId) {
      case TASK_CREATE_SYNC_ACCOUNT: {
        Exceptional<SyncAccountTask.Result> result = (Exceptional<SyncAccountTask.Result>) o;
        getListFragment().reloadAccountList();
        if (result.isPresent()) {
          if (result.get().localUnsynced > 0) {
            showSelectUnsyncedAccount(result.get().accountName);
          }
        } else {
          showSnackbar(result.getException().getMessage());
        }
        break;
      }
      case TASK_SYNC_REMOVE_BACKEND: {
        Result result = (Result) o;
        if (result.isSuccess()) {
          getListFragment().reloadAccountList();
        }
        break;
      }
      case TASK_SYNC_LINK_SAVE: {
        Result result = (Result) o;
        showDismissibleSnackbar(result.print(this));
        //fall through
      }
      case TASK_SYNC_UNLINK:
      case TASK_SYNC_LINK_LOCAL:
      case TASK_SYNC_LINK_REMOTE: {
        Result result = (Result) o;
        if (!result.isSuccess()) {
          showSnackbar(result.print(this));
        }
        break;
      }
      case TASK_REPAIR_SYNC_BACKEND: {
        Result result = (Result) o;
        String resultPrintable = result.print(this);
        if (result.isSuccess()) {
          showSnackbar(resultPrintable);
        } else {
          Bundle b = new Bundle();
          b.putString(ConfirmationDialogFragment.KEY_MESSAGE, resultPrintable);
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.TRY_AGAIN_COMMAND);
          b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.button_label_try_again);
          ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(), "REPAIR_SYNC_FAILURE");
        }
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

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.SYNC_DOWNLOAD_COMMAND) {
      if (PrefKey.NEW_ACCOUNT_ENABLED.getBoolean(true)) {
        newAccount = getListFragment().getAccountForSync(
            ((ExpandableListContextMenuInfo) item.getMenuInfo()).packedPosition);
        if (newAccount != null) {
          startDbWriteTask();
        }
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
      startSetup((Integer) tag);
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_REPAIR_INTENT && resultCode == RESULT_OK) {
      for (SyncBackendProviderFactory factory: backendProviders) {
        if (factory.startRepairTask(this, data)) {
          break;
        }
      }
    }
  }

  public void requestDropboxAccess(String accountName) {
    dropBoxTokenRequestPendingForAccount = accountName;
    //TODO generalize for other backends
    getSyncBackendProviderFactoryByIdOrThrow(R.id.SYNC_BACKEND_DROPBOX).startSetup(this);
  }
}
