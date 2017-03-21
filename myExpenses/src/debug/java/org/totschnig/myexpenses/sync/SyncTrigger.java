package org.totschnig.myexpenses.sync;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.totschnig.myexpenses.provider.TransactionProvider;

public class SyncTrigger extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Bundle bundle = new Bundle();
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
    ContentResolver.requestSync(GenericAccountService.GetAccount(intent.getStringExtra("ACCOUNT")),
        TransactionProvider.AUTHORITY, bundle);
  }
}
