package org.totschnig.myexpenses.task;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.AsyncTask;
import android.os.Bundle;

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
import static org.totschnig.myexpenses.sync.GenericAccountService.Authenticator.AUTH_TOKEN_TYPE;

public class SyncAccountTask extends AsyncTask<Void, Void, Result> {

  public static final String KEY_RETURN_REMOTE_DATA_LIST = "returnRemoteDataList";
  private final TaskExecutionFragment taskExecutionFragment;
  private final String accountName;
  private final String password;
  private final Bundle userData;
  private final String authToken;
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
    this.authToken = args.getString(AccountManager.KEY_AUTHTOKEN);
    this.shouldReturnRemoteDataList = args.getBoolean(KEY_RETURN_REMOTE_DATA_LIST);
    this.create = create;
  }

  @Override
  protected Result doInBackground(Void... params) {
    Account account = GenericAccountService.GetAccount(accountName);
    if (create) {
      AccountManager accountManager = AccountManager.get(MyApplication.getInstance());
      ContribFeature.SYNCHRONIZATION.recordUsage();
      if (accountManager.addAccountExplicitly(account, password, userData)) {
        if (authToken != null) {
          accountManager.setAuthToken(account, AUTH_TOKEN_TYPE, authToken);
        }
        GenericAccountService.activateSync(account);
      } else {
        return Result.FAILURE;
      }
    }
    return buildResult();
  }

  private Result buildResult() {
    if (shouldReturnRemoteDataList) {
      SyncBackendProvider syncBackendProvider;
      List<AccountMetaData> syncAccounts;
      List<String> backups;
      try {
        syncBackendProvider = SyncBackendProviderFactory.get(taskExecutionFragment.getActivity(),
            GenericAccountService.GetAccount(accountName)).getOrThrow();
        Result result = syncBackendProvider.setUp(authToken);
        if (!result.success) {
          return result;
        }
        syncAccounts = syncBackendProvider.getRemoteAccountList().collect(Collectors.toList());
        backups = syncBackendProvider.getStoredBackups();
      } catch (Throwable throwable) {
        return new Result(false, throwable.getMessage());
      }
      return new Result(true, 0, accountName, backups, syncAccounts);
    } else {
      return new Result(true, 0, accountName, org.totschnig.myexpenses.model.Account.count(
          KEY_SYNC_ACCOUNT_NAME + " IS NULL", null));
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
