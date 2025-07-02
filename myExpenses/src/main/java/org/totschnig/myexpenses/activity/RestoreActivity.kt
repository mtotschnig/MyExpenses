package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.RestoreViewModel

abstract class RestoreActivity: ProtectedFragmentActivity() {
    private val restoreViewModel: RestoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(restoreViewModel)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                restoreViewModel.publishProgress.collect { progress ->
                    progress?.let {
                        progressDialogFragment?.appendToMessage(progress)
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                restoreViewModel.result.collect { result ->
                    result?.let {
                        result.onFailure { throwable ->
                            progressDialogFragment?.appendToMessage(throwable.safeMessage)
                        }
                        progressDialogFragment?.onTaskCompleted()
                        onPostRestoreTask(it)
                        restoreViewModel.resultProcessed()
                    }
                }
            }
        }

        restoreViewModel.permissionRequested.observe(this) {
            if (it != null) {
                checkPermissionsForPlaner()
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        super.onPermissionsGranted(requestCode, perms)
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR) {
            restoreViewModel.submitPermissionRequestResult(true)
        }
    }

    fun doWithEncryptionCheck(block: () -> Unit) {
        if (prefHandler.encryptDatabase && !isSqlCryptLoaded) {
            showMessage(
                "The module required for database encryption has not yet been downloaded from Play Store. Please try again!",
                null,
                null,
                MessageDialogFragment.Button(
                    R.string.button_label_close,
                    R.id.QUIT_COMMAND,
                    null
                ),
                false
            )
        } else {
           block()
        }
    }

    fun requireSqlCrypt() {
        if (!isSqlCryptLoaded) {
            featureManager.requestFeature(Feature.SQLCRYPT, this)
        }
    }

    private val isSqlCryptLoaded
        get() = featureManager.isFeatureInstalled(Feature.SQLCRYPT, this)

    protected fun doRestore(args: Bundle) {
        doWithEncryptionCheck {
            supportFragmentManager
                .beginTransaction()
                .add(
                    ProgressDialogFragment.newInstance(getString(R.string.pref_restore_title), true),
                    PROGRESS_TAG
                ).commitNow()
            restoreViewModel.startRestore(args)
        }
    }

    protected open fun onPostRestoreTask(result: Result<Unit>) {
        if (result.isSuccess) {
            licenceHandler.reset()
            // if the backup is password protected, we want to force the password
            // check
            // is it not enough to set mLastPause to zero, since it would be
            // overwritten by the callings activity onPause
            // hence we need to set isLocked if necessary
            val myApplication = requireApplication()
            myApplication.resetLastPause()
            if (myApplication.shouldLock(this)) {
                myApplication.lock()
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        super.onPermissionsDenied(requestCode, perms)
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR) {
           restoreViewModel.submitPermissionRequestResult(false)
        }
    }

    companion object {
        const val FRAGMENT_TAG_RESTORE = "RESTORE"
    }
}