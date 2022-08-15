/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 * Denis Solonenko - initial API and implementation
 * Michael Totschnig - extended for My Expenses
 */
package org.totschnig.myexpenses.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.doBackup
import org.totschnig.myexpenses.provider.listOldBackups
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ContribUtils
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.viewmodel.BackupViewModel
import javax.inject.Inject

class AutoBackupService : JobIntentService() {
    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var licenceHandler: LicenceHandler

    override fun onCreate() {
        super.onCreate()
        (application as MyApplication).appComponent.inject(this)
    }

    private val notificationTitle: String
        get() = TextUtils.concatResStrings(
            this,
            " ",
            R.string.app_name,
            R.string.contrib_feature_auto_backup_label
        )

    private fun notify(notification: Notification) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NotificationBuilderWrapper.NOTIFICATION_AUTO_BACKUP, notification)
    }

    private fun buildMessage(message: CharSequence) =
        NotificationBuilderWrapper.defaultBigTextStyleBuilder(this, notificationTitle, message)

    override fun onHandleWork(intent: Intent) {
        val action = intent.action
        when (action) {
            ACTION_AUTO_BACKUP -> {
                val syncAccount = prefHandler.getString(PrefKey.AUTO_BACKUP_CLOUD, null)
                val result: Result<Pair<DocumentFile, List<DocumentFile>>> =
                    doBackup(this, prefHandler, syncAccount)
                result.onSuccess { pair ->
                    if (pair.second.isNotEmpty()) {
                        val requireConfirmation =
                            prefHandler.getBoolean(PrefKey.PURGE_BACKUP_REQUIRE_CONFIRMATION, true)
                        if (requireConfirmation) {
                            val builder = buildMessage(
                                "${getString(R.string.dialog_title_purge_backups)} (${pair.second.size})"
                            )
                            builder.addAction(
                                0,
                                0,
                                getString(R.string.menu_delete),
                                PendingIntent.getBroadcast(
                                    this,
                                    0,
                                    Intent(this, GenericAlarmReceiver::class.java).setAction(
                                        ACTION_BACKUP_PURGE
                                    ),
                                    //noinspection InlinedApi
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                            )
                            builder.addAction(
                                0,
                                0,
                                getString(android.R.string.cancel),
                                PendingIntent.getBroadcast(
                                    this,
                                    0,
                                    Intent(this, GenericAlarmReceiver::class.java).setAction(
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
                                    BackupViewModel.purgeResult2Message(this,
                                        pair.second.map {
                                            it.delete()
                                        }
                                    )
                                ).build()
                            )
                        }
                    }
                }.onFailure {
                    prefHandler.putBoolean(PrefKey.AUTO_BACKUP, false)
                    val content =
                        "${it.message} ${getString(R.string.warning_auto_backup_deactivated)}"
                    val preferenceIntent = Intent(this, MyPreferenceActivity::class.java)
                    val builder = buildMessage(content)
                        .setContentIntent(
                            PendingIntent.getActivity(
                                this,
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
            }
            ACTION_SCHEDULE_AUTO_BACKUP -> {
                DailyScheduler.updateAutoBackupAlarms(this)
            }
            ACTION_BACKUP_PURGE -> {
                AppDirHelper.getAppDir(this)?.let { appDir ->
                    notify(
                        buildMessage(
                            BackupViewModel.purgeResult2Message(
                                this,
                                listOldBackups(appDir, prefHandler).map { it.delete() })
                        ).build()
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_AUTO_BACKUP = BuildConfig.APPLICATION_ID + ".ACTION_AUTO_BACKUP"
        const val ACTION_SCHEDULE_AUTO_BACKUP =
            BuildConfig.APPLICATION_ID + ".ACTION_SCHEDULE_AUTO_BACKUP"
        const val ACTION_BACKUP_PURGE_CANCEL = "BACKUP_PURGE_CANCEL"
        const val ACTION_BACKUP_PURGE = "BACKUP_PURGE"

        /**
         * Unique job ID for this service.
         */
        private const val JOB_ID = 1000

        /**
         * Convenience method for enqueuing work in to this service.
         */
        @JvmStatic
        fun enqueueWork(context: Context?, work: Intent?) {
            enqueueWork(context!!, AutoBackupService::class.java, JOB_ID, work!!)
        }
    }
}