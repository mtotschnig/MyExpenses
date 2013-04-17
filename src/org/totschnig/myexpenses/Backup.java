/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Toast;

public class Backup extends Activity implements ContribIFace {
  static final int BACKUP_DIALOG_ID = 1;
  static final int BACKUP_COMMAND_ID = 1;
  static final int RESTORE_DIALOG_ID = 2;
  static final int RESTORE_COMMAND_ID = 2;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      if (Utils.isExternalStorageAvailable()) {
        if (getIntent().getAction().equals("myexpenses.intent.backup")) {
          showDialog(BACKUP_DIALOG_ID);
        }
        else {
          //restore
          if (MyApplication.backupExists()) {
            if (Utils.isContribEnabled(this)) {
              showDialog(RESTORE_DIALOG_ID);
            }
            else {
              showDialog(R.id.CONTRIB_DIALOG_ID);
            }
          } else {
            Toast.makeText(getBaseContext(),getString(R.string.restore_no_backup_found), Toast.LENGTH_LONG).show();
            finish();
          }
        }
      }
      else {
        Toast.makeText(getBaseContext(),getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
        finish();
      }
    }
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
    case BACKUP_DIALOG_ID:
      File backupDb = MyApplication.getBackupDbFile();
      int message = backupDb.exists() ? R.string.warning_backup_exists : R.string.warning_backup;
      return Utils.createMessageDialog(new ContextThemeWrapper(this, MyApplication.getThemeId()) {
        public void onDialogButtonClicked(View v) {
          dismissDialog(BACKUP_DIALOG_ID);
          if (v.getId() == BACKUP_COMMAND_ID) {
            if (Utils.isExternalStorageAvailable()) {
              if (MyApplication.getInstance().backup()) {
                Toast.makeText(getBaseContext(),getString(R.string.backup_success), Toast.LENGTH_LONG).show();
              } else {
                Toast.makeText(getBaseContext(),getString(R.string.backup_failure), Toast.LENGTH_LONG).show();
              }
            } else {
              Toast.makeText(getBaseContext(),getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
            }
          }
          finish();
        }
      },message,BACKUP_COMMAND_ID,null)
      .setOnCancelListener(new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
          finish();
        }
      })
      .create();
    case RESTORE_DIALOG_ID:
      return Utils.createMessageDialog(new ContextThemeWrapper(this, MyApplication.getThemeId()) {
        public void onDialogButtonClicked(View v) {
          dismissDialog(RESTORE_DIALOG_ID);
          if (v.getId() == RESTORE_COMMAND_ID) {
            Utils.recordUsage("restore");
            if (MyApplication.backupExists()) {
              MyApplication.backupRestore();
              Intent i = getBaseContext().getPackageManager()
                  .getLaunchIntentForPackage( getBaseContext().getPackageName() );
              i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(i);
            } else {
              Toast.makeText(getBaseContext(),getString(R.string.restore_no_backup_found), Toast.LENGTH_LONG).show();
            }
          }
          finish();
        }
      },R.string.warning_restore,RESTORE_COMMAND_ID,null)
      .setOnCancelListener(new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
          finish();
        }
      })
      .create();
    case R.id.CONTRIB_DIALOG_ID:
      return Utils.contribDialog(this,"restore");
    }
    return null;
  }
  public void contribCallback(int command) {
    showDialog(RESTORE_DIALOG_ID);
  }
}
