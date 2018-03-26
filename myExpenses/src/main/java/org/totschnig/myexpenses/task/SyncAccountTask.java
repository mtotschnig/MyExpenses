package org.totschnig.myexpenses.task;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.AsyncTask;
import android.os.Bundle;

import com.annimon.stream.Collectors;
import com.annimon.stream.Exceptional;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.json.AccountMetaData;

import java.util.List;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_PASSWORD;
import static android.accounts.AccountManager.KEY_USERDATA;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.sync.GenericAccountService.Authenticator.AUTH_TOKEN_TYPE;

public class SyncAccountTask extends AsyncTask<Void, Void, Exceptional<SyncAccountTask.Result>> {

  public static final String KEY_RETURN_REMOTE_DATA_LIST = "returnRemoteDataList";
  private final TaskExecutionFragment taskExecutionFragment;
  private final String accountName;
  private final String password;
  private final Bundle userData;
  private final String authToken;
  private final boolean create;
  /**
   * if true returns list of backups and sync accounts from backend,
   * if false returns number of local accounts that are still unsynced
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
  protected Exceptional<Result> doInBackground(Void... params) {
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
        return Exceptional.of(new Exception("Error while adding account"));
      }
    }
    return buildResult();
  }

  private Exceptional<Result> buildResult() {
    final int localUnsynced = org.totschnig.myexpenses.model.Account.count(
        KEY_SYNC_ACCOUNT_NAME + " IS NULL", null);
    List<AccountMetaData> syncAccounts = null;
    List<String> backups = null;
    if (shouldReturnRemoteDataList) {
      SyncBackendProvider syncBackendProvider;

      Account account = GenericAccountService.GetAccount(accountName);
      try {
        syncBackendProvider = SyncBackendProviderFactory.get(taskExecutionFragment.getActivity(),
            account).getOrThrow();
        Exceptional<Void> result = syncBackendProvider.setUp(authToken);
        if (!result.isPresent()) {
          return Exceptional.of(result.getException());
        }
        syncAccounts = syncBackendProvider.getRemoteAccountList(account).collect(Collectors.toList());
        backups = syncBackendProvider.getStoredBackups(account);
      } catch (Throwable throwable) {
        return Exceptional.of(throwable);
      }
    }
    List<AccountMetaData> finalSyncAccounts = syncAccounts;
    List<String> finalBackups = backups;
    return Exceptional.of(() -> new Result(accountName, finalSyncAccounts, finalBackups, localUnsynced));
  }

  @Override
  protected void onPostExecute(Exceptional result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT, result);
    }
  }
  public static class Result {
    public Result(String accountName, List<AccountMetaData> syncAccounts, List<String> backups, int localUnsynced) {
      this.accountName = accountName;
      this.syncAccounts = syncAccounts;
      this.backups = backups;
      this.localUnsynced = localUnsynced;
    }

    public final String accountName;
    public final  List<AccountMetaData> syncAccounts;
    public final  List<String> backups;
    public final int localUnsynced;
  }
}
