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
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import java.util.Date;

import androidx.annotation.Nullable;

import static android.content.Context.ALARM_SERVICE;
import static org.totschnig.myexpenses.preference.TimePreference.getScheduledTime;
import static org.totschnig.myexpenses.service.AutoBackupService.ACTION_AUTO_BACKUP;
import static org.totschnig.myexpenses.service.PlanExecutor.ACTION_EXECUTE_PLANS;
import static org.totschnig.myexpenses.service.PlanExecutor.KEY_FORCE_IMMEDIATE;
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
      if (licenceHandler.hasTrialAccessTo(ContribFeature.AUTO_BACKUP)) {
        scheduleAutoBackup(context);
      }
    } else {
      cancelAutoBackup(context);
    }
  }

  public static void scheduleAutoBackup(Context context) {
    cancelAutoBackup(context);
    AlarmManager service = (AlarmManager) context.getSystemService(ALARM_SERVICE);
    PendingIntent pendingIntent = getAutoBackupPendingIntent(context);
    Date scheduledTime = getScheduledTime(getPrefHandler(context), PrefKey.AUTO_BACKUP_TIME);
    service.set(AlarmManager.RTC, scheduledTime.getTime(), pendingIntent);
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
    return PendingIntent.getBroadcast(context, -100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public static void updatePlannerAlarms(Context context, boolean force, boolean now) {
    if (CALENDAR.hasPermission(context)) {
      PendingIntent pendingIntent = getPlannerPendingIntent(context, force);
      AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
      manager.set(AlarmManager.RTC, now ? System.currentTimeMillis() : getScheduledTime(getPrefHandler(context), PrefKey.PLANNER_EXECUTION_TIME).getTime(), pendingIntent);
    }
  }

  private static PendingIntent getPlannerPendingIntent(Context ctx, boolean force) {
    Bundle extras = new Bundle(1);
    extras.putBoolean(KEY_FORCE_IMMEDIATE, force);
    return createSchedulePendingIntent(ctx, ACTION_EXECUTE_PLANS, extras);
  }

  public static void cancelPlans(Context ctx) {
    PendingIntent pendingIntent = getPlannerPendingIntent(ctx, false);
    AlarmManager manager = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
    manager.cancel(pendingIntent);
  }
}
