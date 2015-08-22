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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.BackupListDialogFragment;
import org.totschnig.myexpenses.dialog.BackupSourcesDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.ImportSourceDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;

import static org.totschnig.myexpenses.task.RestoreTask.KEY_DIR_NAME_LEGACY;

import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.FileUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.provider.DocumentFile;
import android.view.Window;
import android.widget.Toast;

public class BackupRestoreActivity extends ProtectedFragmentActivityNoAppCompat
    implements ConfirmationDialogListener {
  public static final String KEY_RESTORE_PLAN_STRATEGY = "restorePlanStrategy";

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    String action = getIntent().getAction();
    if (action != null && action.equals("myexpenses.intent.backup")) {
      Result appDirStatus = Utils.checkAppDir();
      if (!appDirStatus.success) {
        abort(appDirStatus.print(this));
        return;
      }
      MessageDialogFragment.newInstance(
          R.string.menu_backup,
          getString(R.string.warning_backup,
              FileUtils.getPath(this, Utils.getAppDir().getUri())),
          new MessageDialogFragment.Button(android.R.string.yes,
              R.id.BACKUP_COMMAND, null), null,
          MessageDialogFragment.Button.noButton())
          .show(getSupportFragmentManager(), "BACKUP");
    } else {
      if (getIntent().getBooleanExtra("legacy", false)) {
        Result appDirStatus = Utils.checkAppDir();
        if (appDirStatus.success) {
          openBrowse();
        } else {
          abort(appDirStatus.print(this));
        }
      } else {
        BackupSourcesDialogFragment.newInstance().show(
            getSupportFragmentManager(), "GRISBI_SOURCES");
      }
    }
  }

  private void abort(String message) {
    Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    setResult(RESULT_CANCELED);
    finish();
  }

  private void showRestoreDialog(Uri fileUri, int restorePlanStrategie) {
    Bundle b = new Bundle();
    b.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.pref_restore_title);
    b.putString(
        ConfirmationDialogFragment.KEY_MESSAGE,
        getString(R.string.warning_restore,
            DialogUtils.getDisplayName(fileUri)));
    b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
        R.id.RESTORE_COMMAND);
    b.putInt(KEY_RESTORE_PLAN_STRATEGY, restorePlanStrategie);
    b.putParcelable(TaskExecutionFragment.KEY_FILE_PATH, fileUri);
    ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(),
        "RESTORE");
  }

  /**
   * Legacy version for backups stored in application directory
   *
   * @param dir
   */
  private void showRestoreDialog(String dir) {
    Bundle b = new Bundle();
    b.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.pref_restore_title);
    b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
        getString(R.string.warning_restore, dir));
    b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
        R.id.RESTORE_COMMAND);
    b.putString(KEY_DIR_NAME_LEGACY, dir);
    ConfirmationDialogFragment.newInstance(b).show(getSupportFragmentManager(),
        "RESTORE");
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag))
      return true;
    switch (command) {
      case R.id.BACKUP_COMMAND:
        if (Utils.checkAppFolderWarning()) {
          doBackup();
        } else {
          Bundle b = new Bundle();
          b.putInt(ConfirmationDialogFragment.KEY_TITLE,
              R.string.dialog_title_attention);
          b.putString(
              ConfirmationDialogFragment.KEY_MESSAGE,
              getString(R.string.warning_app_folder_will_be_deleted_upon_uninstall));
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
              R.id.BACKUP_COMMAND_DO);
          b.putString(ConfirmationDialogFragment.KEY_PREFKEY,
              MyApplication.PrefKey.APP_FOLDER_WARNING_SHOWN.getKey());
          ConfirmationDialogFragment.newInstance(b).show(
              getSupportFragmentManager(), "APP_FOLDER_WARNING");
        }
        return true;
    }
    return false;
  }

  protected void doBackup() {
    Result appDirStatus = Utils.checkAppDir();
    if (appDirStatus.success) {
      startTaskExecution(TaskExecutionFragment.TASK_BACKUP, null, null,
          R.string.menu_backup);
    } else {
      Toast.makeText(getBaseContext(), appDirStatus.print(this),
          Toast.LENGTH_LONG).show();
      finish();
    }
  }

  @Override
  public void onPostExecute(int taskId, Object result) {
    super.onPostExecute(taskId, result);
    Result r = (Result) result;
    switch (taskId) {
      case TaskExecutionFragment.TASK_RESTORE:
        String msg = r.print(this);
        if (msg != null) {
          Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
        }
        if (r.success) {
          //MyApplication.getInstance().resetContribEnabled();
          // if the backup is password protected, we want to force the password
          // check
          // is it not enough to set mLastPause to zero, since it would be
          // overwritten by the callings activity onpause
          // hence we need to set isLocked if necessary
          MyApplication.getInstance().resetLastPause();
          MyApplication.getInstance().shouldLock(this);

          setResult(RESULT_FIRST_USER);
        }
        break;
      case TaskExecutionFragment.TASK_BACKUP:
        if (!r.success) {
          Toast.makeText(getBaseContext(),
              r.print(this), Toast.LENGTH_LONG)
              .show();
        } else {
          Uri backupFileUri = (Uri) r.extra[0];
          Toast.makeText(getBaseContext(),
              getString(r.getMessage(), FileUtils.getPath(this, backupFileUri)), Toast.LENGTH_LONG)
              .show();
          if (MyApplication.PrefKey.PERFORM_SHARE.getBoolean(false)) {
            ArrayList<Uri> uris = new ArrayList<>();
            uris.add(backupFileUri);
            Utils.share(this, uris,
                MyApplication.PrefKey.SHARE_TARGET.getString("").trim(),
                "application/zip");
          }
        }
    }
    finish();
  }

  @Override
  public void onProgressUpdate(Object progress) {
    Toast.makeText(getBaseContext(), ((Result) progress).print(this),
        Toast.LENGTH_LONG).show();
  }

  public void onSourceSelected(Uri mUri, int restorePlanStrategie) {
    showRestoreDialog(mUri, restorePlanStrategie);
  }

  /**
   * Legacy callback from BackupListDialogFragment for backups stored in
   * application directory
   *
   * @param dirOrFile
   */
  public void onSourceSelected(String dirOrFile, int restorePlanStrategie) {
    if (dirOrFile.endsWith(".zip")) {
      showRestoreDialog(Uri.fromFile(new File(Utils.getAppDir().getUri().getPath(), dirOrFile)),
          restorePlanStrategie);
    } else {
      showRestoreDialog(dirOrFile);
    }
  }

  @Override
  public void onPositive(Bundle args) {
    switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
      case R.id.BACKUP_COMMAND_DO:
        doBackup();
        break;
      case R.id.RESTORE_COMMAND:
        getSupportFragmentManager()
            .beginTransaction()
            .add(TaskExecutionFragment.newInstanceRestore(args), ProtectionDelegate.ASYNC_TAG)
            .add(ProgressDialogFragment.newInstance(R.string.pref_restore_title),
                ProtectionDelegate.PROGRESS_TAG).commit();
        break;
    }
  }

  public void openBrowse() {
    String[] backups = listBackups();
    if (backups.length == 0) {
      Toast.makeText(getBaseContext(),
          getString(R.string.restore_no_backup_found), Toast.LENGTH_LONG)
          .show();
      finish();
    } else {
      BackupListDialogFragment.newInstance(backups).show(
          getSupportFragmentManager(), "BACKUP_LIST");
    }
  }

  // inspired by Financisto
  public static String[] listBackups() {
    DocumentFile appDir = Utils.getAppDir();
    if (appDir.getUri().getScheme().equals("file")) {
      String[] files = new File(appDir.getUri().getPath()).list(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
          // backup-yyyMMdd-HHmmss
          return filename
              .matches("backup-\\d\\d\\d\\d\\d\\d\\d\\d-\\d\\d\\d\\d\\d\\d") ||
              filename.endsWith(".zip");
        }
      });
      if (files != null) {
        Arrays.sort(files, new Comparator<String>() {
          @Override
          public int compare(String s1, String s2) {
            return s2.compareTo(s1);
          }
        });
        return files;
      }
    }
    return new String[0];
  }

  @Override
  public void onNegative(Bundle args) {
    setResult(RESULT_CANCELED);
    finish();
  }

  @Override
  public void onDismissOrCancel(Bundle args) {
    setResult(RESULT_CANCELED);
    finish();
  }

  @Override
  public void onMessageDialogDismissOrCancel() {
    setResult(RESULT_CANCELED);
    finish();
  }

}