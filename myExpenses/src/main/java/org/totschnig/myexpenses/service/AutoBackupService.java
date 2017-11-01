/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package org.totschnig.myexpenses.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.provider.DocumentFile;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.AccountPreference;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.util.BackupUtils;
import org.totschnig.myexpenses.util.ContribUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP;
import static org.totschnig.myexpenses.util.NotificationBuilderWrapper.NOTIFICATION_AUTO_BACKUP;

public class AutoBackupService extends JobIntentService {

  private static final String TAG = AutoBackupService.class.getSimpleName();
  public static final String ACTION_AUTO_BACKUP = BuildConfig.APPLICATION_ID + ".ACTION_AUTO_BACKUP";
  public static final String ACTION_SCHEDULE_AUTO_BACKUP = BuildConfig.APPLICATION_ID + ".ACTION_SCHEDULE_AUTO_BACKUP";

  /**
   * Unique job ID for this service.
   */
  static final int JOB_ID = 1000;

  /**
   * Convenience method for enqueuing work in to this service.
   */
  static void enqueueWork(Context context, Intent work) {
    enqueueWork(context, AutoBackupService.class, JOB_ID, work);
  }

  @Override
  protected void onHandleWork(Intent intent) {
    if (intent == null) {
      return;
    }
    String action = intent.getAction();
    if (ACTION_AUTO_BACKUP.equals(action)) {
      Result result = BackupUtils.doBackup();
      if (result.success) {
        int remaining = ContribFeature.AUTO_BACKUP.recordUsage();
        if (remaining < 1) {
          ContribUtils.showContribNotification(this, ContribFeature.AUTO_BACKUP);
        }
        String syncAccount = PrefKey.AUTO_BACUP_CLOUD.getString(AccountPreference.SYNCHRONIZATION_NONE);
        if (!syncAccount.equals(AccountPreference.SYNCHRONIZATION_NONE)) {
          Bundle bundle = new Bundle();
          bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
          bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
          DocumentFile documentFile = (DocumentFile) result.extra[0];
          bundle.putString(SyncAdapter.KEY_UPLOAD_AUTO_BACKUP_URI, documentFile.getUri().toString());
          bundle.putString(SyncAdapter.KEY_UPLOAD_AUTO_BACKUP_NAME, documentFile.getName());
          ContentResolver.requestSync(GenericAccountService.GetAccount(syncAccount), TransactionProvider.AUTHORITY, bundle);
        }
      } else {
        String notifTitle = Utils.concatResStrings(this, " ", R.string.app_name, R.string.contrib_feature_auto_backup_label);
        AUTO_BACKUP.putBoolean(false);
        String content = result.print(this) + " " + getString(R.string.warning_auto_backup_deactivated);
        Intent preferenceIntent = new Intent(this, MyPreferenceActivity.class);
        NotificationCompat.Builder builder =
            new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notification_sigma)
                .setContentTitle(notifTitle)
                .setContentText(content)
                .setContentIntent(PendingIntent.getActivity(this, 0, preferenceIntent, PendingIntent.FLAG_CANCEL_CURRENT))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(
            NOTIFICATION_AUTO_BACKUP, notification);
      }
    } else if (ACTION_SCHEDULE_AUTO_BACKUP.equals(action)) {
      DailyAutoBackupScheduler.updateAutoBackupAlarms(this);
    }
  }

}
