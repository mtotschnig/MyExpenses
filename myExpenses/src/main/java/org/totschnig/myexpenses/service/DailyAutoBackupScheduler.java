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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.preference.TimePreference;

import java.util.Calendar;
import java.util.Date;

/**
 * Original implementation based on Financisto
 */
public class DailyAutoBackupScheduler {

  private DailyAutoBackupScheduler() {
  }

  public static void updateAutoBackupAlarms(Context context) {
    if (PrefKey.AUTO_BACKUP.getBoolean(false) &&
        PrefKey.AUTO_BACKUP_DIRTY.getBoolean(true)) {
      final PrefHandler prefHandler = ((MyApplication) context.getApplicationContext()).getAppComponent().prefHandler();
      if (ContribFeature.AUTO_BACKUP.hasAccess() || ContribFeature.AUTO_BACKUP.usagesLeft(prefHandler) > 0) {
        scheduleAutoBackup(context);
      }
    } else {
      cancelAutoBackup(context);
    }
  }

  public static void scheduleAutoBackup(Context context) {
    cancelAutoBackup(context);
    AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    PendingIntent pendingIntent = createPendingIntent(context);
    Date scheduledTime = getScheduledTime();
    service.set(AlarmManager.RTC_WAKEUP, scheduledTime.getTime(), pendingIntent);
  }

  public static void cancelAutoBackup(Context context) {
    AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    PendingIntent pendingIntent = createPendingIntent(context);
    service.cancel(pendingIntent);
    pendingIntent.cancel();
  }

  private static PendingIntent createPendingIntent(Context context) {
    Intent intent = new Intent(context, ScheduledBackupReceiver.class);
    return PendingIntent.getBroadcast(context, -100, intent, PendingIntent.FLAG_ONE_SHOT);
  }

  private static Date getScheduledTime() {
    int hhmm = PrefKey.AUTO_BACKUP_TIME.getInt(TimePreference.DEFAULT_VALUE);
    int hh = hhmm / 100;
    int mm = hhmm - 100 * hh;
    Calendar c = Calendar.getInstance();
    long now = System.currentTimeMillis();
    c.setTimeInMillis(now);
    c.set(Calendar.HOUR_OF_DAY, hh);
    c.set(Calendar.MINUTE, mm);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    if (c.getTimeInMillis() < now) {
      c.add(Calendar.DAY_OF_MONTH, 1);
    }
    return c.getTime();
  }

}
