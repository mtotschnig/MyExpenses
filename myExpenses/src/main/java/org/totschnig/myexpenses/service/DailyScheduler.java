/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package org.totschnig.myexpenses.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import androidx.annotation.Nullable;

import static android.content.Context.ALARM_SERVICE;
import static org.totschnig.myexpenses.preference.TimePreference.getScheduledTime;
import static org.totschnig.myexpenses.service.AutoBackupService.ACTION_AUTO_BACKUP;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

/**
 * Original implementation based on Financisto
 */
public class DailyScheduler {

  private DailyScheduler() {
  }

  public static void updateAutoBackupAlarms(Context context) {
    final PrefHandler prefHandler = getPrefHandler(context);
    final LicenceHandler licenceHandler = getLicenceHandler(context);
    if (prefHandler.getBoolean(PrefKey.AUTO_BACKUP, false) &&
        prefHandler.getBoolean(PrefKey.AUTO_BACKUP_DIRTY, true)) {
      scheduleAutoBackup(context);
    } else {
      cancelAutoBackup(context);
    }
  }

  public static void scheduleAutoBackup(Context context) {
    cancelAutoBackup(context);
    AlarmManager service = (AlarmManager) context.getSystemService(ALARM_SERVICE);
    PendingIntent pendingIntent = getAutoBackupPendingIntent(context);
    long scheduledTime = getScheduledTime(getPrefHandler(context), PrefKey.AUTO_BACKUP_TIME);
    service.set(AlarmManager.RTC, scheduledTime, pendingIntent);
  }

  private static PrefHandler getPrefHandler(Context context) {
    final MyApplication applicationContext = (MyApplication) ((context instanceof MyApplication) ? context : context.getApplicationContext());
    final AppComponent appComponent = applicationContext.getAppComponent();
    return appComponent.prefHandler();
  }

  private static LicenceHandler getLicenceHandler(Context context) {
    final MyApplication applicationContext = (MyApplication) ((context instanceof MyApplication) ? context : context.getApplicationContext());
    final AppComponent appComponent = applicationContext.getAppComponent();
    return appComponent.licenceHandler();
  }

  public static void cancelAutoBackup(Context context) {
    AlarmManager service = (AlarmManager) context.getSystemService(ALARM_SERVICE);
    PendingIntent pendingIntent = getAutoBackupPendingIntent(context);
    service.cancel(pendingIntent);
    pendingIntent.cancel();
  }

  private static PendingIntent getAutoBackupPendingIntent(Context context) {
    return createSchedulePendingIntent(context, ACTION_AUTO_BACKUP, null);
  }

  private static PendingIntent createSchedulePendingIntent(Context context, String action, @Nullable Bundle extras) {
    Intent intent = new Intent(context, ScheduleReceiver.class);
    intent.setAction(action);
    if (extras != null) {
      intent.putExtras(extras);
    }
    //noinspection InlinedApi
    return PendingIntent.getBroadcast(context, -100, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  public static void updatePlannerAlarms(Context context, boolean force, boolean now) {
    if (CALENDAR.hasPermission(context)) {
      PlanExecutor.Companion.enqueueSelf(context, getPrefHandler(context), force);
    }
  }

}
