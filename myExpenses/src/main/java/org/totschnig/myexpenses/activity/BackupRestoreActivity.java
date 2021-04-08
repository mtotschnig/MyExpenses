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

import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.BackupListDialogFragment;
import org.totschnig.myexpenses.dialog.BackupSourcesDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.task.RestoreTask;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.ShareUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.io.FileUtils;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import icepick.State;
import timber.log.Timber;

import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_DEVICE_LOCK_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_LEGACY;
import static org.totschnig.myexpenses.task.RestoreTask.KEY_PASSWORD;

public class BackupRestoreActivity extends ProtectedFragmentActivity
    implements ConfirmationDialogListener, SimpleDialog.OnDialogResultListener {
  public static final String FRAGMENT_TAG = "BACKUP_SOURCE";
  private static final String DIALOG_TAG_PASSWORD = "PASSWORD";

  private boolean calledFromOnboarding = false;

  public static final String ACTION_BACKUP = "BACKUP";
  public static final String ACTION_RESTORE = "RESTORE";
  public static final String ACTION_RESTORE_LEGACY = "RESTORE_LEGACY";

  @State
  int taskResult = RESULT_OK;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ComponentName callingActivity = getCallingActivity();
    if (callingActivity != null && Utils.getSimpleClassNameFromComponentName(callingActivity)
        .equals(OnboardingActivity.class.getSimpleName())) {
      calledFromOnboarding = true;
      Timber.i("Called from onboarding");
    }
    if (savedInstanceState != null) {
      return;
    }
    String action = getIntent().getAction();
    switch (action == null ? "" : action) {
      case ACTION_BACKUP: {
        Result appDirStatus = AppDirHelper.checkAppDir(this);
        if (!appDirStatus.isSuccess()) {
          abort(appDirStatus.print(this));
          return;
        }
        DocumentFile appDir = AppDirHelper.getAppDir(this);
        if (appDir == null) {
          abort(getString(R.string.io_error_appdir_null));
          return;
        }
        boolean isProtected = !TextUtils.isEmpty(prefHandler.getString(PrefKey.EXPORT_PASSWORD, null));
        StringBuilder message = new StringBuilder();
        message.append(getString(R.string.warning_backup, FileUtils.getPath(this, appDir.getUri())))
            .append(" ");
        if (isProtected) {
          message.append(getString(R.string.warning_backup_protected)).append(" ");
        } else if (prefHandler.getBoolean(PROTECTION_LEGACY, false) || prefHandler.getBoolean(PROTECTION_DEVICE_LOCK_SCREEN, false)) {
          message.append(unencryptedBackupWarning()).append(" ");
        }
        message.append(getString(R.string.continue_confirmation));
        MessageDialogFragment.newInstance(
            getString(isProtected ? R.string.dialog_title_backup_protected : R.string.menu_backup),
            message.toString(),
            new MessageDialogFragment.Button(R.string.response_yes,
                R.id.BACKUP_COMMAND, null), null,
            MessageDialogFragment.noButton(), isProtected ? R.drawable.ic_lock : 0)
            .show(getSupportFragmentManager(), "BACKUP");
        break;
      }
      case ACTION_RESTORE_LEGACY: {
        Result appDirStatus = AppDirHelper.checkAppDir(this);
        if (appDirStatus.isSuccess()) {
          openBrowse();
        } else {
          abort(appDirStatus.print(this));
        }
        break;
      }
      case ACTION_RESTORE: {
        BackupSourcesDialogFragment.newInstance().show(
            getSupportFragmentManager(), FRAGMENT_TAG);
        break;

      }
    }
  }

  private void abort(String message) {
    showMessage(message);
  }

  private void showRestoreDialog(Uri fileUri, int restorePlanStrategy) {
    Bundle bundle = buildRestoreArgs(fileUri, restorePlanStrategy);
    bundle.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.pref_restore_title);
    final String message = getString(R.string.warning_restore, DialogUtils.getDisplayName(fileUri))
        + " " + getString(R.string.continue_confirmation);
    bundle.putString(
        ConfirmationDialogFragment.KEY_MESSAGE,
        message);
    bundle.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
        R.id.RESTORE_COMMAND);
    ConfirmationDialogFragment.newInstance(bundle).show(getSupportFragmentManager(),
        "RESTORE");
  }

  private Bundle buildRestoreArgs(Uri fileUri, int restorePlanStrategie) {
    Bundle bundle = new Bundle();
    bundle.putInt(RestoreTask.KEY_RESTORE_PLAN_STRATEGY, restorePlanStrategie);
    bundle.putParcelable(TaskExecutionFragment.KEY_FILE_PATH, fileUri);
    return bundle;
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag))
      return true;
    if (command == R.id.BACKUP_COMMAND) {
      if (AppDirHelper.checkAppFolderWarning(this)) {
        doBackup();
      } else {
        Bundle b = new Bundle();
        b.putInt(ConfirmationDialogFragment.KEY_TITLE,
            R.string.dialog_title_attention);
        b.putCharSequence(
            ConfirmationDialogFragment.KEY_MESSAGE,
            Utils.getTextWithAppName(this, R.string.warning_app_folder_will_be_deleted_upon_uninstall));
        b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
            R.id.BACKUP_COMMAND_DO);
        b.putString(ConfirmationDialogFragment.KEY_PREFKEY,
            PrefKey.APP_FOLDER_WARNING_SHOWN.getKey());
        ConfirmationDialogFragment.newInstance(b).show(
            getSupportFragmentManager(), "APP_FOLDER_WARNING");
      }
      return true;
    }
    return false;
  }

  protected void doBackup() {
    Result appDirStatus = AppDirHelper.checkAppDir(this);//TODO this check leads to strict mode violation, can we get rid of it ?
    if (appDirStatus.isSuccess()) {
      startTaskExecution(TaskExecutionFragment.TASK_BACKUP, null, prefHandler.getString(PrefKey.EXPORT_PASSWORD, null),
          R.string.menu_backup, true);
    } else {
      abort(appDirStatus.print(this));
    }
  }

  @Override
  public void onPostExecute(int taskId, Object result) {
    super.onPostExecute(taskId, result);
    Result<DocumentFile> r = (Result<DocumentFile>) result;
    if (taskId == TaskExecutionFragment.TASK_BACKUP) {
      if (!r.isSuccess()) {
        onProgressUpdate(r.print(this));
      } else {
        Uri backupFileUri = r.getExtra().getUri();
        onProgressUpdate(getString(r.getMessage(), FileUtils.getPath(this, backupFileUri)));
        if (PrefKey.PERFORM_SHARE.getBoolean(false)) {
          ArrayList<Uri> uris = new ArrayList<>();
          uris.add(backupFileUri);
          Result shareResult = ShareUtils.share(this, uris,
              PrefKey.SHARE_TARGET.getString("").trim(),
              "application/zip");
          if (!shareResult.isSuccess()) {
            onProgressUpdate(shareResult.print(this));
          }
        }
      }
    }
  }

  @Override
  protected boolean shouldKeepProgress(int taskId) {
    return true;
  }

  @Override
  protected void onPostRestoreTask(Result result) {
    super.onPostRestoreTask(result);
    onProgressUpdate(result);
    if (result.isSuccess()) {
      taskResult = RESULT_RESTORE_OK;
    }
  }

  public void onSourceSelected(Uri mUri, int restorePlanStrategy) {
    if (calledFromOnboarding) {
      final Bundle args = buildRestoreArgs(mUri, restorePlanStrategy);
      if (FileUtils.getPath(this, mUri).endsWith("enc")) {
        SimpleFormDialog.build().msg(R.string.backup_is_encrypted)
            .fields(Input.password(KEY_PASSWORD).required())
            .extra(args)
            .show(this, DIALOG_TAG_PASSWORD);
      } else {
        doRestore(args);
      }
    } else {
      showRestoreDialog(mUri, restorePlanStrategy);
    }
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (DIALOG_TAG_PASSWORD.equals(dialogTag)) {
      if (which == BUTTON_POSITIVE) {
        doRestore(extras);
      } else {
        abort();
      }
      return true;
    }
    return false;
  }

  @Override
  public void onPositive(Bundle args) {
    int anInt = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE);
    if (anInt == R.id.BACKUP_COMMAND_DO) {
      doBackup();
    } else if (anInt == R.id.RESTORE_COMMAND) {
      doRestore(args);
    }
  }

  public void openBrowse() {
    if (hasBackups()) {
      BackupListDialogFragment.newInstance().show(
          getSupportFragmentManager(), FRAGMENT_TAG);
    } else {
      abort(getString(R.string.restore_no_backup_found));
    }
  }

  public boolean hasBackups() {
    DocumentFile appDir = AppDirHelper.getAppDir(this);
    return appDir != null && Stream.of(appDir.listFiles())
        .anyMatch(documentFile -> {
          final String name = documentFile.getName();
          return name != null && name.endsWith(".zip");
        });
  }

  @Override
  public void onNegative(Bundle args) {
    abort();
  }

  public void abort() {
    setResult(RESULT_CANCELED);
    finish();
  }

  @Override
  public void onDismissOrCancel(Bundle args) {
    abort();
  }

  @Override
  public void onProgressDialogDismiss() {
    setResult(taskResult);
    finish();
  }

  @Override
  public void onMessageDialogDismissOrCancel() {
    abort();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR) {
      if (!PermissionHelper.allGranted(grantResults)) {
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment instanceof DialogUtils.CalendarRestoreStrategyChangedListener) {
          ((DialogUtils.CalendarRestoreStrategyChangedListener) fragment).onCalendarPermissionDenied();
        }
      }
      return;
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  protected int getSnackbarContainerId() {
    return  android.R.id.content;
  }
}