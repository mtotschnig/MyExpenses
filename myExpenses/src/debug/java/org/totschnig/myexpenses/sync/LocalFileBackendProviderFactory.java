package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.dialog.EditTextDialog;

import static org.totschnig.myexpenses.util.PermissionHelper.hasExternalReadPermission;

public class LocalFileBackendProviderFactory extends SyncBackendProviderFactory {

  @NonNull
  @Override
  protected LocalFileBackendProvider _fromAccount(Context context, Account account, AccountManager accountManager) {
    //before API 16, we need to check for write access
    if (!hasExternalReadPermission(context)) {
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


}
