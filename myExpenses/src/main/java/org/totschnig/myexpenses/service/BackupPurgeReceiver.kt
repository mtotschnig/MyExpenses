package org.totschnig.myexpenses.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.totschnig.myexpenses.util.NotificationBuilderWrapper.NOTIFICATION_AUTO_BACKUP

class BackupPurgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AutoBackupWorker.ACTION_BACKUP_PURGE_CANCEL -> {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(
                    NOTIFICATION_AUTO_BACKUP
                )
            }
            AutoBackupWorker.ACTION_BACKUP_PURGE -> {
                WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<BackupPurgeWorker>().build())
            }
        }
    }
}