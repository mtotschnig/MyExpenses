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

package org.totschnig.myexpenses.activity;

import java.io.File;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

public class Backup extends ProtectedFragmentActivityNoAppCompat {

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    if (savedInstanceState == null) {
      if (Utils.isExternalStorageAvailable()) {
        if (getIntent().getAction().equals("myexpenses.intent.backup")) {
          File backupDb = MyApplication.getBackupDbFile();
          int message = backupDb.exists() ? R.string.warning_backup_exists : R.string.warning_backup;
          MessageDialogFragment.newInstance(
              R.string.menu_backup,
              message,
              new MessageDialogFragment.Button(android.R.string.yes, R.id.BACKUP_COMMAND, null),
              null,
              MessageDialogFragment.Button.noButton())
            .show(getSupportFragmentManager(),"BACKUP");
        } else {
          //restore
          if (MyApplication.backupExists()) {
            showRestoreDialog();
          } else {
            Toast.makeText(getBaseContext(),getString(R.string.restore_no_backup_found), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
          }
        }
      }
      else {
        Toast.makeText(getBaseContext(),getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
      }
    }
  }
  private void showRestoreDialog() {
    MessageDialogFragment.newInstance(
        R.string.pref_restore_title,
        R.string.warning_restore,
        new MessageDialogFragment.Button(android.R.string.yes, R.id.RESTORE_COMMAND, null),
        null,
        MessageDialogFragment.Button.noButton())
      .show(getSupportFragmentManager(),"BACKUP");
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command,tag))
      return true;
    switch(command) {
    case R.id.BACKUP_COMMAND:
      if (Utils.isExternalStorageAvailable()) {
        startTaskExecution(TaskExecutionFragment.TASK_BACKUP, null, null, R.string.menu_backup);
      } else {
        Toast.makeText(getBaseContext(),getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
      }
      finish();
      break;
    case R.id.RESTORE_COMMAND:
      if (MyApplication.backupExists()) {
        startTaskExecution(TaskExecutionFragment.TASK_RESTORE, null, null, R.string.pref_restore_title);
      } else {
        Toast.makeText(getBaseContext(),getString(R.string.restore_no_backup_found), Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
      }      
  }
  return true;
  }
  @Override
  public void onMessageDialogDismissOrCancel() {
    //super.onMessageDialogDismissOrCancel();
    setResult(RESULT_CANCELED);
    finish();
  }
  @Override
  public void onPostExecute(int taskId,Object result) {
    super.onPostExecute(taskId,result);
    switch(taskId) {
    case TaskExecutionFragment.TASK_BACKUP:
      String msg = ((Result) result).print(this);
      Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
      break;
    case TaskExecutionFragment.TASK_RESTORE:
      if ((Boolean) result) {
        setResult(RESULT_FIRST_USER);
      }
      break;
    }
    finish();
  }
  @Override
  public void onProgressUpdate(Object progress) {
    Toast.makeText(getBaseContext(),getString((Integer) progress), Toast.LENGTH_LONG).show();
  }
}
