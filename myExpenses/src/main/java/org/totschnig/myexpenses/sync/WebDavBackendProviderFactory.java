package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment;

public class WebDavBackendProviderFactory extends SyncBackendProviderFactory {

  @Override
  public String getLabel() {
    return "WebDAV";
  }

  @Override
  protected SyncBackendProvider _fromAccount(Account account, AccountManager accountManager) {
    return new WebDavBackendProvider();
  }

  @Override
  public int getId() {
    return R.id.CREATE_BACKEND_WEBDAV_COMMAND;
  }

  @Override
  public void startSetup(ManageSyncBackends context) {
    SetupWebdavDialogFragment webdavDialogFragment = new SetupWebdavDialogFragment();
    webdavDialogFragment.show(context.getSupportFragmentManager(), "WEBDAV_SETUP");
  }
}
