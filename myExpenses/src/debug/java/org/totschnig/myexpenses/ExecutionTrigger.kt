package org.totschnig.myexpenses

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.service.AutoBackupService
import org.totschnig.myexpenses.service.AutoBackupService.Companion.enqueueWork
import org.totschnig.myexpenses.service.DailyScheduler
import org.totschnig.myexpenses.sync.GenericAccountService

class ExecutionTrigger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "TRIGGER_SYNC" -> GenericAccountService.requestSync(intent.getStringExtra("ACCOUNT")!!)
            "TRIGGER_PLANNER" -> DailyScheduler.updatePlannerAlarms(context, true, true)
            "TRIGGER_BACKUP" -> {
                val serviceIntent = Intent(context, AutoBackupService::class.java)
                serviceIntent.action = AutoBackupService.ACTION_AUTO_BACKUP
                enqueueWork(context, serviceIntent)
            }
        }
    }
}