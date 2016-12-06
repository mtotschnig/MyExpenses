package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;

import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.dialog.EditTextDialog;

public class LocalFileBackendProviderFactory extends SyncBackendProviderFactory {

  @Override
  protected LocalFileBackendProvider _fromAccount(Account account, AccountManager accountManager) {
    return new LocalFileBackendProvider(accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URI));
  }

  @Override
  public String getLabel() {
    return "Local";
  }

  @Override
  public int getId() {
    return 0;
  }

  @Override
  public void startSetup(ManageSyncBackends context) {
    Bundle args = new Bundle();
    args.putString(EditTextDialog.KEY_DIALOG_TITLE, "Local backend: Directory path");
    args.putString(GenericAccountService.KEY_SYNC_PROVIDER_ID, String.valueOf(getId()));
    args.putString(GenericAccountService.KEY_SYNC_PROVIDER_LABEL, getLabel());
    EditTextDialog.newInstance(args)
        .show(context.getSupportFragmentManager(), "LOCAL_BACKEND_DIRECTORY_PATH");
  }
}
