package org.totschnig.myexpenses.task;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.util.Result;

import java.util.List;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_PASSWORD;
import static android.accounts.AccountManager.KEY_USERDATA;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;

public class SyncAccountTask extends AsyncTask<Void, Void, Result> {

  public static final String KEY_RETURN_REMOTE_DATA_LIST = "returnRemoteDataList";
  private final TaskExecutionFragment taskExecutionFragment;
  private final String accountName;
  private final String password;
  private final Bundle userData;
  private final boolean create;
  /**
   * if true returns list of backups and sync accounts from backend,
   * if false returns number of local accounts linked to this backend
   */
  private final boolean shouldReturnRemoteDataList;

  SyncAccountTask(TaskExecutionFragment taskExecutionFragment, Bundle args, boolean create) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.accountName = args.getString(KEY_ACCOUNT_NAME);
    this.password = args.getString(KEY_PASSWORD);
    this.userData = args.getBundle(KEY_USERDATA);
    this.shouldReturnRemoteDataList = args.getBoolean(KEY_RETURN_REMOTE_DATA_LIST);
    this.create = create;
  }

  @Override
  protected Result doInBackground(Void... params) {
    Account account = GenericAccountService.GetAccount(accountName);
    if (create) {
      AccountManager accountManager = AccountManager.get(MyApplication.getInstance());
      ContribFeature.SYNCHRONIZATION.recordUsage();
      if (accountManager.addAccountExplicitly(account, null, userData)) {
        accountManager.setPassword(account, password);
        GenericAccountService.activateSync(account);
      } else {
        return Result.FAILURE;
      }
    }
    return new Result(true, 0, buildResultExtra());
  }

  @Nullable
  private Object[] buildResultExtra() {
    if (shouldReturnRemoteDataList) {
      SyncBackendProvider syncBackendProvider;
      List<String> syncAccounts;
      List<String> backups;
      try {
        syncBackendProvider = SyncBackendProviderFactory.get(taskExecutionFragment.getActivity(),
            GenericAccountService.GetAccount(accountName)).getOrThrow();
        syncAccounts = syncBackendProvider.getRemoteAccountList().map(AccountMetaData::label).collect(Collectors.toList());
        backups = syncBackendProvider.getStoredBackups();
      } catch (Throwable throwable) {
        return null;
      }
      return new Object[]{accountName, backups, syncAccounts};
    } else {
      return new Object[]{accountName, org.totschnig.myexpenses.model.Account.count(
          KEY_SYNC_ACCOUNT_NAME + " IS NULL", null)};
    }
  }

  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT, result);
    }
  }
}
