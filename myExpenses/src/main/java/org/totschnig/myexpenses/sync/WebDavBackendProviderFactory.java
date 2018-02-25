package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment;
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException;
import org.totschnig.myexpenses.util.Result;

import java.io.Serializable;

public class WebDavBackendProviderFactory extends SyncBackendProviderFactory {

  public static final String LABEL = "WebDAV";
  public static final String WEBDAV_SETUP = "WEBDAV_SETUP";

  @Override
  public String getLabel() {
    return LABEL;
  }

  @NonNull
  @Override
  protected SyncBackendProvider _fromAccount(Context context, Account account, AccountManager accountManager) throws SyncParseException {
    return new WebDavBackendProvider(context, account, accountManager);
  }

  @Override
  public void startSetup(ProtectedFragmentActivity context) {
    SetupWebdavDialogFragment webdavDialogFragment = new SetupWebdavDialogFragment();
    webdavDialogFragment.setCancelable(false);
    webdavDialogFragment.show(context.getSupportFragmentManager(), WEBDAV_SETUP);
  }

  @Override
  public int getId() {
    return R.id.SYNC_BACKEND_WEBDAV;
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
