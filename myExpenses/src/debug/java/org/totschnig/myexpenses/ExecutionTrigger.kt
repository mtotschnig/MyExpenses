package org.totschnig.myexpenses

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.service.DailyScheduler
import org.totschnig.myexpenses.sync.GenericAccountService
import java.util.*

class ExecutionTrigger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (Objects.requireNonNull(intent.action)) {
            "TRIGGER_SYNC" -> {
                val bundle = Bundle().apply {
                    putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                    putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                }
                ContentResolver.requestSync(GenericAccountService.getAccount(intent.getStringExtra("ACCOUNT")),
                        TransactionProvider.AUTHORITY, bundle)
            }
            "TRIGGER_PLANNER" -> {
                DailyScheduler.updatePlannerAlarms(context, true, true)
            }
        }
    }
}