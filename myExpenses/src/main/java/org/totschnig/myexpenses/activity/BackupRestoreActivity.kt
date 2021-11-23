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
package org.totschnig.myexpenses.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SimpleFormDialog
import icepick.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.BackupSourcesDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.DialogUtils.CalendarRestoreStrategyChangedListener
import org.totschnig.myexpenses.preference.AccountPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.task.RestoreTask
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.Result
import org.totschnig.myexpenses.util.ShareUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.io.FileUtils
import org.totschnig.myexpenses.viewmodel.BackupViewModel
import org.totschnig.myexpenses.viewmodel.BackupViewModel.BackupState
import org.totschnig.myexpenses.viewmodel.BackupViewModel.BackupState.Running

class BackupRestoreActivity : ProtectedFragmentActivity(), ConfirmationDialogListener,
    OnDialogResultListener {
    lateinit var backupViewModel: BackupViewModel

    @JvmField
    @State
    var taskResult = RESULT_OK
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backupViewModel = ViewModelProvider(this)[BackupViewModel::class.java]
        requireApplication().appComponent.inject(backupViewModel)
        backupViewModel.getBackupState().observe(this) { backupState: BackupState? ->
            val onDismissed: Snackbar.Callback = object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                    setResult(taskResult)
                    finish()
                }
            }
            when (backupState) {
                is Running -> {
                    showSnackbarIndefinite(R.string.menu_backup)
                }
                is BackupState.Error -> {
                    showDismissibleSnackbar(backupState.throwable.message!!, onDismissed)
                }
                is BackupState.Success -> {
                    var message = getString(R.string.backup_success, backupState.result.second)
                    if (prefHandler.getBoolean(PrefKey.PERFORM_SHARE, false)) {
                        val uris = ArrayList<Uri>()
                        uris.add(backupState.result.first.uri)
                        val shareResult = ShareUtils.share(
                            this, uris,
                            prefHandler.getString(PrefKey.SHARE_TARGET, "")!!.trim { it <= ' ' },
                            "application/zip"
                        )
                        if (!shareResult.isSuccess) {
                            message += " " + shareResult.print(this)
                        }
                    }
                    showDismissibleSnackbar(message, onDismissed)
                }
            }
        }
        if (savedInstanceState != null) {
            return
        }
        val action = intent.action
        when (action ?: "") {
            ACTION_BACKUP -> {
                val appDirStatus = AppDirHelper.checkAppDir(this)
                if (!appDirStatus.isSuccess) {
                    abort(appDirStatus.print(this))
                    return
                }
                val appDir = AppDirHelper.getAppDir(this)
                if (appDir == null) {
                    abort(getString(R.string.io_error_appdir_null))
                    return
                }
                val isProtected =
                    !TextUtils.isEmpty(prefHandler.getString(PrefKey.EXPORT_PASSWORD, null))
                val message = StringBuilder()
                message.append(
                    getString(
                        R.string.warning_backup,
                        FileUtils.getPath(this, appDir.uri)
                    )
                )
                    .append(" ")
                if (isProtected) {
                    message.append(getString(R.string.warning_backup_protected)).append(" ")
                } else if (prefHandler.getBoolean(
                        PrefKey.PROTECTION_LEGACY,
                        false
                    ) || prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)
                ) {
                    message.append(unencryptedBackupWarning()).append(" ")
                }
                message.append(getString(R.string.continue_confirmation))
                val bundle = Bundle()
                bundle.putInt(
                    ConfirmationDialogFragment.KEY_TITLE,
                    if (isProtected) R.string.dialog_title_backup_protected else R.string.menu_backup
                )
                bundle.putString(
                    ConfirmationDialogFragment.KEY_MESSAGE,
                    message.toString()
                )
                bundle.putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                    R.id.BACKUP_COMMAND
                )
                bundle.putInt(
                    ConfirmationDialogFragment.KEY_ICON,
                    if (isProtected) R.drawable.ic_lock else 0
                )
                val withSync = prefHandler.getString(
                    PrefKey.AUTO_BACKUP_CLOUD,
                    AccountPreference.SYNCHRONIZATION_NONE
                )
                if (withSync != AccountPreference.SYNCHRONIZATION_NONE) {
                    bundle.putString(
                        ConfirmationDialogFragment.KEY_CHECKBOX_LABEL,
                        getString(R.string.backup_save_to_sync_backend, withSync)
                    )
                    bundle.putBoolean(
                        ConfirmationDialogFragment.KEY_CHECKBOX_INITIALLY_CHHECKED,
                        prefHandler.getBoolean(PrefKey.SAVE_TO_SYNC_BACKEND_CHECKED, false)
                    )
                }
                ConfirmationDialogFragment.newInstance(bundle)
                    .show(supportFragmentManager, "BACKUP")
            }
            ACTION_RESTORE, Intent.ACTION_VIEW -> {
                BackupSourcesDialogFragment.newInstance(intent.data).show(
                    supportFragmentManager, FRAGMENT_TAG
                )
            }
        }
    }

    private fun abort(message: String) {
        showMessage(message)
    }

    private fun showRestoreDialog(bundle: Bundle) {
        bundle.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.pref_restore_title)
        val message = (getString(
            R.string.warning_restore,
            DialogUtils.getDisplayName(bundle.getParcelable(TaskExecutionFragment.KEY_FILE_PATH))
        )
                + " " + getString(R.string.continue_confirmation))
        bundle.putString(
            ConfirmationDialogFragment.KEY_MESSAGE,
            message
        )
        bundle.putInt(
            ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
            R.id.RESTORE_COMMAND
        )
        ConfirmationDialogFragment.newInstance(bundle).show(
            supportFragmentManager,
            "RESTORE"
        )
    }

    private fun buildRestoreArgs(fileUri: Uri, restorePlanStrategy: Int): Bundle {
        val bundle = Bundle()
        bundle.putInt(RestoreTask.KEY_RESTORE_PLAN_STRATEGY, restorePlanStrategy)
        bundle.putParcelable(TaskExecutionFragment.KEY_FILE_PATH, fileUri)
        return bundle
    }

    override fun shouldKeepProgress(taskId: Int): Boolean {
        return true
    }

    override fun onPostRestoreTask(result: Result<*>) {
        super.onPostRestoreTask(result)
        onProgressUpdate(result)
        if (result.isSuccess) {
            taskResult = RESULT_RESTORE_OK
        }
    }

    fun calledExternally(): Boolean {
        return Intent.ACTION_VIEW == intent.action
    }

    private fun calledFromOnboarding(): Boolean {
        val callingActivity = callingActivity
        return callingActivity != null && (Utils.getSimpleClassNameFromComponentName(callingActivity)
                == OnboardingActivity::class.java.simpleName)
    }

    fun onSourceSelected(mUri: Uri, restorePlanStrategy: Int) {
        val args = buildRestoreArgs(mUri, restorePlanStrategy)
        backupViewModel.isEncrypted(mUri).observe(this) {
            it.onFailure {
                showSnackbar(it.message ?: "ERROR")
            }.onSuccess {
                if (it) {
                    SimpleFormDialog.build().msg(R.string.backup_is_encrypted)
                        .fields(Input.password(RestoreTask.KEY_PASSWORD).text(prefHandler.getString(PrefKey.EXPORT_PASSWORD, "")).required())
                        .extra(args)
                        .show(this, DIALOG_TAG_PASSWORD)
                } else {
                    if (calledFromOnboarding()) {
                        doRestore(args)
                    } else {
                        showRestoreDialog(args)
                    }
                }
            }
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (DIALOG_TAG_PASSWORD == dialogTag) {
            if (which == OnDialogResultListener.BUTTON_POSITIVE) {
                if (calledFromOnboarding()) {
                    doRestore(extras)
                } else {
                    showRestoreDialog(extras)
                }
            } else {
                abort()
            }
            return true
        }
        return false
    }

    override fun onPositive(args: Bundle, checked: Boolean) {
        val command = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)
        if (command == R.id.BACKUP_COMMAND) {
            prefHandler.putBoolean(PrefKey.SAVE_TO_SYNC_BACKEND_CHECKED, checked)
            backupViewModel.doBackup(
                prefHandler.getString(PrefKey.EXPORT_PASSWORD, null),
                checked
            )
        } else if (command == R.id.RESTORE_COMMAND) {
            doRestore(args)
        }
    }

    override fun onNegative(args: Bundle) {
        abort()
    }

    fun abort() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onDismissOrCancel(args: Bundle) {
        abort()
    }

    override fun onProgressDialogDismiss() {
        if (calledExternally()) {
            restartAfterRestore()
        } else {
            setResult(taskResult)
            finish()
        }
    }

    override fun onMessageDialogDismissOrCancel() {
        abort()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR) {
            if (!PermissionHelper.allGranted(grantResults)) {
                (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? CalendarRestoreStrategyChangedListener)
                    ?.onCalendarPermissionDenied()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun getSnackbarContainerId(): Int {
        return android.R.id.content
    }

    companion object {
        const val FRAGMENT_TAG = "BACKUP_SOURCE"
        private const val DIALOG_TAG_PASSWORD = "PASSWORD"
        const val ACTION_BACKUP = "BACKUP"
        const val ACTION_RESTORE = "RESTORE"
    }
}