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
import android.util.Log;

import org.totschnig.myexpenses.MyApplication;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 12/16/11 12:54 AM
 */
public class DailyAutoBackupScheduler {

    private final int hh;
    private final int mm;

    public static void updateAutoBackupAlarms(Context context) {
        if (MyApplication.PrefKey.AUTO_BACKUP.getBoolean(false) &&
            MyApplication.PrefKey.AUTO_BACKUP_DIRTY.getBoolean(true)) {
            scheduleAutoBackup(context);
        } else {
            cancelAutoBackup(context);
        }
    }
    
    public DailyAutoBackupScheduler(int hh, int mm) {
        this.hh = hh;
        this.mm = mm;
    }

    public static void scheduleAutoBackup(Context context) {
        AlarmManager service = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(context);
        Date scheduledTime = getScheduledTime();
        service.set(AlarmManager.RTC_WAKEUP, scheduledTime.getTime(), pendingIntent);
        Log.i("DEBUG", "Next auto-backup scheduled at "+scheduledTime);
    }

    public static void cancelAutoBackup(Context context) {
        AlarmManager service = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(context);
        service.cancel(pendingIntent);
        Log.i("DEBUG", "auto-backup canceled ");
    }

    private static PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(ScheduledAlarmReceiver.SCHEDULED_BACKUP);
        return PendingIntent.getBroadcast(context, -100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static Date getScheduledTime() {
        int hhmm = MyApplication.PrefKey.AUTO_BACKUP_TIME.getInt(600);
        int hh = hhmm/100;
        int mm = hhmm - 100*hh;
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
