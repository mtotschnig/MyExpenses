package org.totschnig.myexpenses.task;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.AsyncTask;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.util.Result;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_PASSWORD;
import static android.accounts.AccountManager.KEY_USERDATA;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;

public class CreateSyncAccountTask extends AsyncTask<Void, Void, Result> {

  private final TaskExecutionFragment taskExecutionFragment;
  private final String accountName;
  private final String password;
  private final Bundle userData;

  CreateSyncAccountTask(TaskExecutionFragment taskExecutionFragment, Bundle args) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.accountName = args.getString(KEY_ACCOUNT_NAME);
    this.password = args.getString(KEY_PASSWORD);
    this.userData = args.getBundle(KEY_USERDATA);
  }

  @Override
  protected Result doInBackground(Void... params) {
    Account newAccount = GenericAccountService.GetAccount(accountName);
    AccountManager accountManager = AccountManager.get(MyApplication.getInstance());
    if (accountManager.addAccountExplicitly(newAccount, null, userData)) {
      accountManager.setPassword(newAccount, password);
      GenericAccountService.activateSync(newAccount);
      //we pass the accountName back, only if there are unlinked accounts
      return org.totschnig.myexpenses.model.Account.count(KEY_SYNC_ACCOUNT_NAME + " IS NULL", null) > 0 ?
          new Result(true, 0, accountName) : Result.SUCCESS;
    }
    return Result.FAILURE;
  }

  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT, result);
    }
  }
}
