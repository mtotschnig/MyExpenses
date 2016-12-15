package org.totschnig.myexpenses.sync;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.dialog.EditTextDialog;

public class LocalFileBackendProviderFactory extends SyncBackendProviderFactory {

  @Override
  protected LocalFileBackendProvider _fromAccount(Account account, AccountManager accountManager) {
    if (ContextCompat.checkSelfPermission(MyApplication.getInstance(), Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
      throw new IllegalStateException("LocalFileBackendProvider needs READ_EXTERNAL_STORAGE permission");
    }
    return new LocalFileBackendProvider(accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL));
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
