package org.totschnig.myexpenses.task;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.AsyncTask;
import android.os.Bundle;

import com.annimon.stream.Collectors;
import com.annimon.stream.Exceptional;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.IOException;
import java.util.List;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_PASSWORD;
import static android.accounts.AccountManager.KEY_USERDATA;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.sync.GenericAccountService.AUTH_TOKEN_TYPE;
import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_ENCRYPTED;
import static org.totschnig.myexpenses.sync.GenericAccountService.KEY_PASSWORD_ENCRYPTION;

public class SyncAccountTask extends AsyncTask<Void, Void, Exceptional<SyncAccountTask.Result>> {

  public static final String KEY_RETURN_REMOTE_DATA_LIST = "returnRemoteDataList";
  private final TaskExecutionFragment taskExecutionFragment;
  private final String accountName;
  private final String password;
  private final Bundle userData;
  private final String authToken;
  private final boolean create;
  private final String encryptionPassword;
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
    this.encryptionPassword = args.getString(KEY_PASSWORD_ENCRYPTION);
    this.create = create;
  }

  @Override
  protected Exceptional<Result> doInBackground(Void... params) {
    Account account = GenericAccountService.getAccount(accountName);
    if (create) {
      final MyApplication application = MyApplication.getInstance();
      AccountManager accountManager = AccountManager.get(application);
      if (accountManager.addAccountExplicitly(account, password, userData)) {
        if (authToken != null) {
          accountManager.setAuthToken(account, AUTH_TOKEN_TYPE, authToken);
        }
        if (encryptionPassword != null) {
          accountManager.setUserData(account, KEY_ENCRYPTED, Boolean.toString(true));
        }
        GenericAccountService.storePassword(application.getContentResolver(), accountName, encryptionPassword);
        final Exceptional<Result> result = buildResult();
        if (result.isPresent()) {
          GenericAccountService.activateSync(account, application.getAppComponent().prefHandler());
        } else {
          //we try to remove a failed account immediately, otherwise user would need to do it, before
          //being able to try again
          final AccountManagerFuture<Boolean> accountManagerFuture = accountManager.removeAccount(account, null, null);
          try {
            accountManagerFuture.getResult();
          } catch (OperationCanceledException | AuthenticatorException | IOException e) {
            CrashHandler.report(e);
          }
        }
        return result;
      } else {
        return Exceptional.of(new Exception("Error while adding account"));
      }
    } else {
      return buildResult();
    }
  }

  private Exceptional<Result> buildResult() {
    final int localUnsynced = org.totschnig.myexpenses.model.Account.count(
        KEY_SYNC_ACCOUNT_NAME + " IS NULL", null);
    Account account = GenericAccountService.getAccount(accountName);
    SyncBackendProvider syncBackendProvider;
    try {
      syncBackendProvider = SyncBackendProviderFactory.get(taskExecutionFragment.getActivity(),
          account, create).getOrThrow();
      final List<AccountMetaData> syncAccounts = shouldReturnRemoteDataList ? syncBackendProvider.getRemoteAccountList()
          .filter(Exceptional::isPresent)
          .map(Exceptional::get)
          .collect(Collectors.toList()) : null;
      final List<String> backups = shouldReturnRemoteDataList ? syncBackendProvider.getStoredBackups() : null;
      syncBackendProvider.tearDown();
      return Exceptional.of(() -> new Result(accountName, syncAccounts, backups, localUnsynced));
    } catch (Throwable throwable) {
      if (!(throwable instanceof IOException || throwable instanceof SyncBackendProvider.EncryptionException)) {
        SyncAdapter.log().e(throwable);
      }
      return Exceptional.of(throwable);
    }
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
