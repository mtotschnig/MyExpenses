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

import static org.totschnig.myexpenses.service.AutoBackupService.ACTION_AUTO_BACKUP;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScheduleReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (ACTION_AUTO_BACKUP.equals(action)) {
      requestAutoBackup(context);
    }
  }

  private void requestAutoBackup(Context context) {
    Intent serviceIntent = new Intent(context, AutoBackupService.class);
    serviceIntent.setAction(ACTION_AUTO_BACKUP);
    AutoBackupService.enqueueWork(context, serviceIntent);
  }
}
