package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment;
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException;

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
    return new WebDavBackendProvider(account, accountManager);
  }

  @Override
  public void startSetup(ManageSyncBackends context) {
    SetupWebdavDialogFragment webdavDialogFragment = new SetupWebdavDialogFragment();
    webdavDialogFragment.setCancelable(false);
    webdavDialogFragment.show(context.getSupportFragmentManager(), WEBDAV_SETUP);
  }
}
