/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package org.totschnig.myexpenses.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;

public class GenericAlarmReceiver extends BroadcastReceiver {

  static final String PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";
  static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
  static final String SCHEDULED_BACKUP = BuildConfig.APPLICATION_ID + ".SCHEDULED_BACKUP";
  static final String ACCOUNT_CHANGED = "android.accounts.LOGIN_ACCOUNTS_CHANGED";

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    String dataString = intent.getDataString();
    if (PACKAGE_REPLACED.equals(action)) {
      if (("package:" + BuildConfig.APPLICATION_ID).equals(dataString)) {
        requestScheduleAutoBackup(context);
        MyApplication.getInstance().initPlanner();
      }
    } else if (BOOT_COMPLETED.equals(action)) {
      requestScheduleAutoBackup(context);
      //no need to explicitly calling initPlanner, since instantiating MyApplication will allready trigger
      //start of Planner
      //MyApplication.getInstance().initPlanner();
    } else if (SCHEDULED_BACKUP.equals(action)) {
      requestAutoBackup(context);
    } else if (ACCOUNT_CHANGED.equals(action)) {
      Account.checkSyncAccounts(context);
    }
  }

  private void requestScheduleAutoBackup(Context context) {
    Intent serviceIntent = new Intent(context, AutoBackupService.class);
    serviceIntent.setAction(AutoBackupService.ACTION_SCHEDULE_AUTO_BACKUP);
    WakefulIntentService.sendWakefulWork(context, serviceIntent);
  }

  private void requestAutoBackup(Context context) {
    Intent serviceIntent = new Intent(context, AutoBackupService.class);
    serviceIntent.setAction(AutoBackupService.ACTION_AUTO_BACKUP);
    WakefulIntentService.sendWakefulWork(context, serviceIntent);
  }

}
