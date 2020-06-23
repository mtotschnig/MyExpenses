package org.totschnig.myexpenses;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.service.DailyScheduler;
import org.totschnig.myexpenses.sync.GenericAccountService;

import java.util.Objects;

public class ExecutionTrigger extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    switch (Objects.requireNonNull(intent.getAction())) {
      case "TRIGGER_SYNC": {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(GenericAccountService.GetAccount(intent.getStringExtra("ACCOUNT")),
            TransactionProvider.AUTHORITY, bundle);
        break;
      }
      case "TRIGGER_PLANNER": {
        DailyScheduler.updatePlannerAlarms(context, true, true);
      }
    }

  }
}
