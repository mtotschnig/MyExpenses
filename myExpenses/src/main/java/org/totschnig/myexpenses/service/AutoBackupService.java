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
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.GenericTask;
import org.totschnig.myexpenses.util.Result;

import java.util.Date;

public class AutoBackupService extends WakefulIntentService {

	private static final String TAG = AutoBackupService.class.getSimpleName();
    public static final String ACTION_AUTO_BACKUP = "org.totschnig.myexpenses.ACTION_AUTO_BACKUP";
    public static final String ACTION_SCHEDULE_AUTO_BACKUP = "org.totschnig.myexpenses.ACTION_SCHEDULE_AUTO_BACKUP";

    public AutoBackupService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("DEBUG", "Created AutoBackupService service ...");
    }

    @Override
	protected void doWakefulWork(Intent intent) {
        String action = intent.getAction();
        if (ACTION_AUTO_BACKUP.equals(action)) {
            Log.i("DEBUG","now doing backup");
            //TODO report on error
            Result result = GenericTask.doBackup();
            if (result.success) {
                ContribFeature.AUTO_BACKUP.recordUsage();
                //TODO if usage limit is exceeded inform user
            }
        }  else if (ACTION_SCHEDULE_AUTO_BACKUP.equals(action)) {
            DailyAutoBackupScheduler.updateAutoBackupAlarms(this);
        }
    }

}
