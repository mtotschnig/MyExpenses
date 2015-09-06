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

import java.util.Calendar;
import java.util.Date;

import ru.orangesoftware.financisto2.utils.MyPreferences;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 12/16/11 12:54 AM
 */
public class DailyAutoBackupScheduler {

    private final int hh;
    private final int mm;
    private final long now;

    public static void scheduleNextAutoBackup(Context context) {
        if (true/*MyPreferences.isAutoBackupEnabled(context)*/) {
            int hhmm = 1700;//MyPreferences.getAutoBackupTime(context);
            int hh = hhmm/100;
            int mm = hhmm - 100*hh;
            new DailyAutoBackupScheduler(hh, mm, System.currentTimeMillis()).scheduleBackup(context);
        }
    }
    
    public DailyAutoBackupScheduler(int hh, int mm, long now) {
        this.hh = hh;
        this.mm = mm;
        this.now = now;
    }

    public void scheduleBackup(Context context) {
        AlarmManager service = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(context);
        Date scheduledTime = getScheduledTime();
        service.set(AlarmManager.RTC_WAKEUP, scheduledTime.getTime(), pendingIntent);
        Log.i("Financisto", "Next auto-backup scheduled at "+scheduledTime);
    }

    private PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(ScheduledAlarmReceiver.SCHEDULED_BACKUP);
        return PendingIntent.getBroadcast(context, -100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public Date getScheduledTime() {
        Calendar c = Calendar.getInstance();
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
