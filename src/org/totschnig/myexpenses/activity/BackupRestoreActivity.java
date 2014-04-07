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
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.BackupListDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

public class BackupRestoreActivity extends ProtectedFragmentActivityNoAppCompat {
  private String fileName;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    if (savedInstanceState == null) {
      if (Utils.isExternalStorageAvailable()) {
        if (getIntent().getAction().equals("myexpenses.intent.backup")) {
          MessageDialogFragment.newInstance(
              R.string.menu_backup,
              R.string.warning_backup,
              new MessageDialogFragment.Button(android.R.string.yes, R.id.BACKUP_COMMAND, null),
              null,
              MessageDialogFragment.Button.noButton())
            .show(getSupportFragmentManager(),"BACKUP");
        } else {
          openBrowse();
          //Toast.makeText(getBaseContext(),getString(R.string.restore_no_backup_found), Toast.LENGTH_LONG).show();
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
        getString(R.string.warning_restore,fileName),
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
        finish();
      }
      break;
    case R.id.RESTORE_COMMAND:
      getSupportFragmentManager().beginTransaction()
        .add(TaskExecutionFragment.newInstanceRestore(fileName),
            "ASYNC_TASK")
        .add(ProgressDialogFragment.newInstance(
            R.string.pref_restore_title),"PROGRESS")
        .commit();
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
    String msg = ((Result) result).print(this);
    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
    if (taskId == TaskExecutionFragment.TASK_RESTORE &&
        ((Result) result).success) {
      setResult(RESULT_FIRST_USER);
    }
    finish();
  }
  @Override
  public void onProgressUpdate(Object progress) {
    Toast.makeText(getBaseContext(),getString((Integer) progress), Toast.LENGTH_LONG).show();
  }

  public void openBrowse() {
    BackupListDialogFragment.newInstance()
      .show(getSupportFragmentManager(),"BACKUP_LIST");
  }
  
  //inspired by Financisto
  public static String[] listBackups(
      BackupListDialogFragment backupListDialogFragment) {
    File appDir = Utils.requireAppDir();
    String[] files = appDir.list(new FilenameFilter(){
      @Override
      public boolean accept(File dir, String filename) {
        return filename.matches("backup-\\d\\d\\d\\d\\d\\d\\d\\d-\\d\\d\\d\\d\\d\\d");
      }
    });
    if (files != null) {
      Arrays.sort(files, new Comparator<String>(){
        @Override
        public int compare(String s1, String s2) {
          return s2.compareTo(s1);
        }
      });
      return files;
    } else {
      return new String[0];
    }
  }
  public void onSourceSelected(String string) {
    fileName = string;
    showRestoreDialog();
  }
}