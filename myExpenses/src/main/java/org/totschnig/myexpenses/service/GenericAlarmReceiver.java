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

import org.totschnig.myexpenses.model.Account;

import timber.log.Timber;

public class GenericAlarmReceiver extends BroadcastReceiver {

  static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
  static final String ACCOUNT_CHANGED = "android.accounts.LOGIN_ACCOUNTS_CHANGED";

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (BOOT_COMPLETED.equals(action)) {
      requestScheduleAutoBackup(context);
      requestSchedulePlanExecutor(context);
    } else if (ACCOUNT_CHANGED.equals(action)) {
      try {
        Account.checkSyncAccounts(context);
      } catch (Exception e) {
        Timber.e(e);
      }
    }
  }

  private void requestSchedulePlanExecutor(Context context) {
    Intent serviceIntent = new Intent(context, PlanExecutor.class);
    serviceIntent.setAction(PlanExecutor.ACTION_SCHEDULE_EXECUTE_PLANS);
    PlanExecutor.enqueueWork(context, serviceIntent);
  }

  private void requestScheduleAutoBackup(Context context) {
    Intent serviceIntent = new Intent(context, AutoBackupService.class);
    serviceIntent.setAction(AutoBackupService.ACTION_SCHEDULE_AUTO_BACKUP);
    AutoBackupService.enqueueWork(context, serviceIntent);
  }
}
