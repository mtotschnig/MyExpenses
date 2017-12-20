package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.dropbox.core.android.Auth;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.util.Result;

import java.io.Serializable;

public class DropboxProviderFactory extends SyncBackendProviderFactory {
  public static final String LABEL = "Dropbox";
  private static final String APP_KEY = "09ctg08r5gnsh5c";

  @NonNull
  @Override
  protected SyncBackendProvider _fromAccount(Context context, Account account, AccountManager accountManager) throws SyncBackendProvider.SyncParseException {
    return new DropboxBackendProvider(context);
  }

  @Override
  public String getLabel() {
    return LABEL;
  }

  @Override
  public void startSetup(FragmentActivity activity) {
    Auth.startOAuth2Authentication(activity, APP_KEY);
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
