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
import org.totschnig.myexpenses.util.ContribUtils
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.licence.LicenceHandler
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

    override fun onHandleWork(intent: Intent) {
        val action = intent.action
        if (ACTION_AUTO_BACKUP == action) {
            val syncAccount = prefHandler.getString(PrefKey.AUTO_BACKUP_CLOUD, null)
            val result: Result<DocumentFile> =
                doBackup(this, prefHandler.getString(PrefKey.EXPORT_PASSWORD, null), syncAccount)
            result.onSuccess {
                val remaining = ContribFeature.AUTO_BACKUP.recordUsage(prefHandler, licenceHandler)
                if (remaining < 1) {
                    ContribUtils.showContribNotification(this, ContribFeature.AUTO_BACKUP)
                }
            }.onFailure {
                val notificationTitle = TextUtils.concatResStrings(
                    this,
                    " ",
                    R.string.app_name,
                    R.string.contrib_feature_auto_backup_label
                )
                prefHandler.putBoolean(PrefKey.AUTO_BACKUP, false)
                val content = "${it.message} ${getString(R.string.warning_auto_backup_deactivated)}"
                val preferenceIntent = Intent(this, MyPreferenceActivity::class.java)
                val builder =
                    NotificationBuilderWrapper.defaultBigTextStyleBuilder(this, notificationTitle, content)
                        .setContentIntent(
                            PendingIntent.getActivity(
                                this, 0,
                                preferenceIntent, PendingIntent.FLAG_CANCEL_CURRENT
                            )
                        )
                val notification = builder.build()
                notification.flags = Notification.FLAG_AUTO_CANCEL
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
                    NotificationBuilderWrapper.NOTIFICATION_AUTO_BACKUP, notification
                )
            }
        } else if (ACTION_SCHEDULE_AUTO_BACKUP == action) {
            DailyScheduler.updateAutoBackupAlarms(this)
        }
    }

    companion object {
        const val ACTION_AUTO_BACKUP = BuildConfig.APPLICATION_ID + ".ACTION_AUTO_BACKUP"
        const val ACTION_SCHEDULE_AUTO_BACKUP =
            BuildConfig.APPLICATION_ID + ".ACTION_SCHEDULE_AUTO_BACKUP"

        /**
         * Unique job ID for this service.
         */
        const val JOB_ID = 1000

        /**
         * Convenience method for enqueuing work in to this service.
         */
        @JvmStatic
        fun enqueueWork(context: Context?, work: Intent?) {
            enqueueWork(context!!, AutoBackupService::class.java, JOB_ID, work!!)
        }
    }
}