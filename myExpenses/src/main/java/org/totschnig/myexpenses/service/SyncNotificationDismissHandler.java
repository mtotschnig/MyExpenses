package org.totschnig.myexpenses.service;

import android.accounts.Account;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;

import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncAdapter;

public class SyncNotificationDismissHandler extends IntentService {
  public SyncNotificationDismissHandler() {
    super("SyncNotificationDismissHandler");
  }

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (intent != null) {
      String accountName = intent.getStringExtra(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME);
      if (accountName != null) {
        Account account = GenericAccountService.getAccount(accountName);
        if (!ContentResolver.isSyncActive(account, TransactionProvider.AUTHORITY)) {
          Bundle bundle = new Bundle();
          bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
          bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
          bundle.putBoolean(SyncAdapter.KEY_NOTIFICATION_CANCELLED, true);
          ContentResolver.requestSync(account, TransactionProvider.AUTHORITY, bundle);
        }
      }
    }
  }
}
