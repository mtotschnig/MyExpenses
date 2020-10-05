package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.DropboxSetup;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.util.Result;

import java.io.Serializable;

import androidx.annotation.NonNull;

import static org.totschnig.myexpenses.activity.ConstantsKt.SYNC_BACKEND_SETUP_REQUEST;

public class DropboxProviderFactory extends SyncBackendProviderFactory {
  public static final String LABEL = "Dropbox";

  @NonNull
  @Override
  protected SyncBackendProvider _fromAccount(Context context, Account account, AccountManager accountManager) {
    return new DropboxBackendProvider(context, accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL));
  }

  @Override
  public String getLabel() {
    return LABEL;
  }

  @Override
  public void startSetup(ProtectedFragmentActivity activity) {
    activity.startActivityForResult(new Intent(activity, DropboxSetup.class),
        SYNC_BACKEND_SETUP_REQUEST);
  }

  @Override
  public int getId() {
    return R.id.SYNC_BACKEND_DROPBOX;
  }

  @Override
  public Intent getRepairIntent(Activity activity) {
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
