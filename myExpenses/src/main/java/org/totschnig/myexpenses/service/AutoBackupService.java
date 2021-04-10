/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Michael Totschnig - extended for My Expenses
 ******************************************************************************/
package org.totschnig.myexpenses.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.AccountPreference;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.util.BackupUtils;
import org.totschnig.myexpenses.util.ContribUtils;
import org.totschnig.myexpenses.util.NotificationBuilderWrapper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.documentfile.provider.DocumentFile;

import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP;
import static org.totschnig.myexpenses.util.NotificationBuilderWrapper.NOTIFICATION_AUTO_BACKUP;

public class AutoBackupService extends JobIntentService {

  private static final String TAG = AutoBackupService.class.getSimpleName();
  public static final String ACTION_AUTO_BACKUP = BuildConfig.APPLICATION_ID + ".ACTION_AUTO_BACKUP";
  public static final String ACTION_SCHEDULE_AUTO_BACKUP = BuildConfig.APPLICATION_ID + ".ACTION_SCHEDULE_AUTO_BACKUP";

  @Inject
  PrefHandler prefHandler;
  @Inject
  LicenceHandler licenceHandler;

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
  public void onCreate() {
    super.onCreate();
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  protected void onHandleWork(@NonNull Intent intent) {
    String action = intent.getAction();
    if (ACTION_AUTO_BACKUP.equals(action)) {
      Result<DocumentFile> result = BackupUtils.doBackup(prefHandler.getString(PrefKey.EXPORT_PASSWORD, null), this);
      if (result.isSuccess()) {
        int remaining = ContribFeature.AUTO_BACKUP.recordUsage(prefHandler, licenceHandler);
        if (remaining < 1) {
          ContribUtils.showContribNotification(this, ContribFeature.AUTO_BACKUP);
        }
        String syncAccount = PrefKey.AUTO_BACKUP_CLOUD.getString(AccountPreference.SYNCHRONIZATION_NONE);
        if (!syncAccount.equals(AccountPreference.SYNCHRONIZATION_NONE)) {
          final DocumentFile backupFile = result.getExtra();
          if (backupFile != null) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            String backupFileName = backupFile.getName();
            if (backupFileName == null) {
              CrashHandler.report(String.format("Could not get name from uri %s", backupFile.getUri()));
              backupFileName = "backup-" + new SimpleDateFormat("yyyMMdd", Locale.US)
                  .format(new Date());
            }
            DbUtils.storeSetting(getContentResolver(), SyncAdapter.KEY_UPLOAD_AUTO_BACKUP_NAME, backupFileName);
            DbUtils.storeSetting(getContentResolver(), SyncAdapter.KEY_UPLOAD_AUTO_BACKUP_URI, backupFile.getUri().toString());
            ContentResolver.requestSync(GenericAccountService.getAccount(syncAccount), TransactionProvider.AUTHORITY, bundle);
          }
        }
      } else {
        String notifTitle = TextUtils.concatResStrings(this, " ", R.string.app_name, R.string.contrib_feature_auto_backup_label);
        AUTO_BACKUP.putBoolean(false);
        String content = result.print(this) + " " + getString(R.string.warning_auto_backup_deactivated);
        Intent preferenceIntent = new Intent(this, MyPreferenceActivity.class);
        NotificationBuilderWrapper builder =
            NotificationBuilderWrapper.defaultBigTextStyleBuilder(this, notifTitle, content)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                    preferenceIntent, PendingIntent.FLAG_CANCEL_CURRENT));
        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(
            NOTIFICATION_AUTO_BACKUP, notification);
      }
    } else if (ACTION_SCHEDULE_AUTO_BACKUP.equals(action)) {
      DailyScheduler.updateAutoBackupAlarms(this);
    }
  }

}
