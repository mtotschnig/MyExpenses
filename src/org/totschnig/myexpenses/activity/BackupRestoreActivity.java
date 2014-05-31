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
import java.util.ArrayList;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.BackupSourcesDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

public class BackupRestoreActivity extends ProtectedFragmentActivityNoAppCompat
  implements ConfirmationDialogListener {
  private File backupFile;

  public static String KEY_BACKUPFILE = "backupFile";

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    if (savedInstanceState == null) {
      backupFile = MyApplication.requireBackupFile();
      if (backupFile != null) {
        if (getIntent().getAction().equals("myexpenses.intent.backup")) {
          if (backupFile.exists()) {
            Toast.makeText(getBaseContext(),"Backup folder "+backupFile.getPath() + "already exists.", Toast.LENGTH_LONG).show();
            finish();
          }
          MessageDialogFragment.newInstance(
              R.string.menu_backup,
              getString(R.string.warning_backup,backupFile.getAbsolutePath()),
              new MessageDialogFragment.Button(android.R.string.yes, R.id.BACKUP_COMMAND, null),
              null,
              MessageDialogFragment.Button.noButton())
            .show(getSupportFragmentManager(),"BACKUP");
        } else {
          BackupSourcesDialogFragment.newInstance().show(getSupportFragmentManager(), "GRISBI_SOURCES");
        }
      } else {
        Toast.makeText(getBaseContext(),getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
      }
    } else {
      backupFile = (File) savedInstanceState.getSerializable(KEY_BACKUPFILE);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_BACKUPFILE, backupFile);
  };

  private void showRestoreDialog(Uri fileUri) {
    Bundle b = new Bundle();
    b.putInt(ConfirmationDialogFragment.KEY_TITLE,
        R.string.pref_restore_title);
    b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
        getString(R.string.warning_restore,fileUri.getPath()));
    b.putInt(ConfirmationDialogFragment.KEY_COMMAND,
        R.id.RESTORE_COMMAND);
    b.putParcelable(TaskExecutionFragment.KEY_FILE_PATH, fileUri);
    ConfirmationDialogFragment.newInstance(b)
      .show(getSupportFragmentManager(),"RESTORE");
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command,tag))
      return true;
    switch(command) {
    case R.id.BACKUP_COMMAND:
      if (Utils.checkAppFolderWarning()) {
        doBackup();
      } else {
        Bundle b = new Bundle();
        b.putInt(ConfirmationDialogFragment.KEY_TITLE,
            R.string.dialog_title_attention);
        b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
            getString(R.string.warning_app_folder_will_be_deleted_upon_uninstall));
        b.putInt(ConfirmationDialogFragment.KEY_COMMAND,
            R.id.BACKUP_COMMAND_DO);
        b.putString(ConfirmationDialogFragment.KEY_PREFKEY,
            MyApplication.PREFKEY_APP_FOLDER_WARNING_SHOWN);
        ConfirmationDialogFragment.newInstance(b)
         .show(getSupportFragmentManager(),"APP_FOLDER_WARNING");
      }
      return true;
  }
  return false;
  }

  protected void doBackup() {
    if (Utils.isExternalStorageAvailable()) {
      startTaskExecution(TaskExecutionFragment.TASK_BACKUP, null, backupFile, R.string.menu_backup);
    } else {
      Toast.makeText(getBaseContext(),getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
      finish();
    }
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
    Result r = (Result) result;
    switch(taskId) {
    case TaskExecutionFragment.TASK_RESTORE:
      String msg = r.print(this);
      Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
      if (((Result) result).success) {
        MyApplication.getInstance().initContribEnabled();
        //if the backup is password protected, we want to force the password check
        //is it not enough to set mLastPause to zero, since it would be overwritten by the callings activity onpause
        //hence we need to set isLocked if necessary
        MyApplication.getInstance().resetLastPause();
        MyApplication.getInstance().shouldLock(this);
  
        setResult(RESULT_FIRST_USER);
      }
      break;
    case TaskExecutionFragment.TASK_BACKUP:
      Toast.makeText(getBaseContext(), getString(r.getMessage(),backupFile.getPath()), Toast.LENGTH_LONG).show();
      if (((Result) result).success && MyApplication.getInstance().getSettings()
          .getBoolean(MyApplication.PREFKEY_PERFORM_SHARE,false)) {
        ArrayList<File> files = new ArrayList<File>();
        files.add((File) backupFile);
          Utils.share(this,files,
              MyApplication.getInstance().getSettings().getString(MyApplication.PREFKEY_SHARE_TARGET,"").trim(),
              "application/zip");
      }
    }
    finish();
  }

  @Override
  public void onProgressUpdate(Object progress) {
    Toast.makeText(getBaseContext(),getString((Integer) progress), Toast.LENGTH_LONG).show();
  }

  public void onSourceSelected(Uri mUri) {
    showRestoreDialog(mUri);
  }

  @Override
  public boolean dispatchCommand(int command, Bundle args) {
    switch(command) {
    case R.id.BACKUP_COMMAND_DO:
      doBackup();
      return true;
    case R.id.RESTORE_COMMAND:
      getSupportFragmentManager().beginTransaction()
        .add(TaskExecutionFragment.newInstanceRestore(args),
            "ASYNC_TASK")
        .add(ProgressDialogFragment.newInstance(
            R.string.pref_restore_title),"PROGRESS")
        .commit();
    }
    return false;
  }
}