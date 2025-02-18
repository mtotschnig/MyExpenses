package org.totschnig.myexpenses.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.TimePreference
import org.totschnig.myexpenses.provider.doBackup
import org.totschnig.myexpenses.provider.listOldBackups
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.viewmodel.BackupViewModel
import timber.log.Timber
import java.util.concurrent.TimeUnit

abstract class BaseAutoBackupWorker(context: Context, workerParameters: WorkerParameters) :
    NotifyingBaseWorker(context, workerParameters) {

    override val notificationId = NotificationBuilderWrapper.NOTIFICATION_AUTO_BACKUP
    override val channelId: String = NotificationBuilderWrapper.CHANNEL_ID_AUTO_BACKUP
    override val notificationTitleResId = R.string.pref_auto_backup_title
}

class AutoBackupWorker(context: Context, workerParameters: WorkerParameters) :
    BaseAutoBackupWorker(context, workerParameters) {

    companion object {
        const val ACTION_BACKUP_PURGE_CANCEL = "BACKUP_PURGE_CANCEL"
        const val ACTION_BACKUP_PURGE = "BACKUP_PURGE"

        private fun WorkManager.cancelWork() = cancelUniqueWork(WORK_NAME)
        private fun WorkManager.enqueue(initialDelayMillis: Long?): Operation {
            Timber.w("enqueuing $WORK_NAME with initialDelayMillis $initialDelayMillis")
            return enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<AutoBackupWorker>()
                    .apply {
                        initialDelayMillis?.let {
                            setInitialDelay(it, TimeUnit.MILLISECONDS)
                        }
                    }.build()
            )
        }

        private const val WORK_NAME = "AutoBackupService"

        fun enqueueOrCancel(context: Context, prefHandler: PrefHandler) {
            val workManager = WorkManager.getInstance(context)
            if (prefHandler.getBoolean(PrefKey.AUTO_BACKUP, false) &&
                prefHandler.getBoolean(PrefKey.AUTO_BACKUP_DIRTY, true)
            ) {
                workManager.enqueue(
                    TimePreference.getScheduledTime(
                        prefHandler, PrefKey.AUTO_BACKUP_TIME
                    )
                )
            } else {
                workManager.cancelWork()
            }
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelWork()
        }

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueue(null)
        }
    }

    override suspend fun doWork(): Result {
        Timber.w("now doWork $WORK_NAME")
        doBackup(applicationContext, prefHandler, true).onSuccess { (_, oldBackups) ->
            if (oldBackups.isNotEmpty()) {
                val requireConfirmation =
                    prefHandler.getBoolean(PrefKey.PURGE_BACKUP_REQUIRE_CONFIRMATION, true)
                if (requireConfirmation) {
                    val builder = buildMessage(
                        "${wrappedContext.getString(R.string.dialog_title_purge_backups)} (${oldBackups.size})"
                    )
                    builder.addAction(
                        0,
                        0,
                        wrappedContext.getString(R.string.menu_delete),
                        PendingIntent.getBroadcast(
                            applicationContext,
                            0,
                            Intent(applicationContext, BackupPurgeReceiver::class.java).setAction(
                                ACTION_BACKUP_PURGE
                            ),
                            //noinspection InlinedApi
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    builder.addAction(
                        0,
                        0,
                        wrappedContext.getString(android.R.string.cancel),
                        PendingIntent.getBroadcast(
                            applicationContext,
                            0,
                            Intent(applicationContext, BackupPurgeReceiver::class.java).setAction(
                                ACTION_BACKUP_PURGE_CANCEL
                            ),
                            //noinspection InlinedApi
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    notify(builder.build())
                } else {
                    notify(
                        buildMessage(
                            BackupViewModel.purgeResult2Message(wrappedContext,
                                oldBackups.map { it.delete() }
                            )
                        ).build()
                    )
                }
            }
        }.onFailure {
            prefHandler.putBoolean(PrefKey.AUTO_BACKUP, false)
            val content =
                "${it.message} ${wrappedContext.getString(R.string.warning_auto_backup_deactivated)}"
            val preferenceIntent = PreferenceActivity.getIntent(
                applicationContext, prefHandler.getKey(PrefKey.CATEGORY_BACKUP_RESTORE)
            )
            val builder = buildMessage(content)
                .setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        0,
                        preferenceIntent,
                        //noinspection InlinedApi
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            val notification = builder.build()
            notification.flags = Notification.FLAG_AUTO_CANCEL
            notify(notification)
        }
        return Result.success()
    }
}

class BackupPurgeWorker(context: Context, workerParameters: WorkerParameters) :
    BaseAutoBackupWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        AppDirHelper.getAppDir(applicationContext).onSuccess { appDir ->
            notify(
                buildMessage(
                    BackupViewModel.purgeResult2Message(
                        applicationContext,
                        listOldBackups(appDir, prefHandler).map { it.delete() })
                ).build()
            )
        }
        return Result.success()
    }
}