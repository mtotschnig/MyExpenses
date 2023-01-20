package org.totschnig.myexpenses.service

import android.accounts.AccountManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.NotificationBuilderWrapper.NOTIFICATION_AUTO_BACKUP
import timber.log.Timber

class GenericAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                requestScheduleAutoBackup(context)
            }
            AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION -> try {
                if ((context.applicationContext as MyApplication).appComponent.prefHandler().getInt(PrefKey.CURRENT_VERSION, 0) > 0) {
                    Account.checkSyncAccounts(context)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
            AutoBackupService.ACTION_BACKUP_PURGE_CANCEL -> {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(
                    NOTIFICATION_AUTO_BACKUP
                )
            }
            AutoBackupService.ACTION_BACKUP_PURGE -> {
                val serviceIntent = Intent(context, AutoBackupService::class.java)
                serviceIntent.action = AutoBackupService.ACTION_BACKUP_PURGE
                AutoBackupService.enqueueWork(context, serviceIntent)
            }
        }
    }

    private fun requestScheduleAutoBackup(context: Context) {
        val serviceIntent = Intent(context, AutoBackupService::class.java)
        serviceIntent.action = AutoBackupService.ACTION_SCHEDULE_AUTO_BACKUP
        AutoBackupService.enqueueWork(context, serviceIntent)
    }
}