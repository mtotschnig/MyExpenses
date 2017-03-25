package org.totschnig.myexpenses.sync;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.dialog.EditTextDialog;

public class LocalFileBackendProviderFactory extends SyncBackendProviderFactory {

  @NonNull
  @Override
  protected LocalFileBackendProvider _fromAccount(Context context, Account account, AccountManager accountManager) {
    //before API 16, we need to check for write access
    if (!(hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
        hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE))) {
      throw new IllegalStateException("LocalFileBackendProvider needs READ_EXTERNAL_STORAGE permission");
    }
    return new LocalFileBackendProvider(context, accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL));
  }

  @Override
  public String getLabel() {
    return "Local";
  }

  @Override
  public void startSetup(ManageSyncBackends context) {
    Bundle args = new Bundle();
    args.putString(EditTextDialog.KEY_DIALOG_TITLE, "Local backend: Directory path");
    args.putString(GenericAccountService.KEY_SYNC_PROVIDER_LABEL, getLabel());
    EditTextDialog.newInstance(args)
        .show(context.getSupportFragmentManager(), "LOCAL_BACKEND_DIRECTORY_PATH");
  }

  private boolean hasPermission(String permission) {
    return ContextCompat.checkSelfPermission(MyApplication.getInstance(), permission) ==
        PackageManager.PERMISSION_GRANTED;
  }
}
