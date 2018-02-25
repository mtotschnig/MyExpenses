package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.util.Result;

import java.io.Serializable;

import static org.totschnig.myexpenses.util.PermissionHelper.hasExternalReadPermission;

public class LocalFileBackendProviderFactory extends SyncBackendProviderFactory {

  public static final String LABEL = "Local";

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
    return LABEL;
  }

  @Override
  public void startSetup(ProtectedFragmentActivity context) {
    Bundle args = new Bundle();
    args.putString(EditTextDialog.KEY_DIALOG_TITLE, "Local backend: Directory path");
    EditTextDialog.newInstance(args)
        .show(context.getSupportFragmentManager(), "LOCAL_BACKEND_DIRECTORY_PATH");
  }

  @Override
  public int getId() {
    return R.id.SYNC_BACKEND_LOCAL;
  }

  @Override
  public Intent getRepairIntent(Activity manageSyncBackends) {
    return null;
  }

  @Override
  public boolean startRepairTask(ManageSyncBackends activity, Intent data) {
    return false;
  }

  @Override
  public Result handleRepairTask(Serializable mExtra) {
    return null;
  }

  @Override
  public void init() {
  }
}
