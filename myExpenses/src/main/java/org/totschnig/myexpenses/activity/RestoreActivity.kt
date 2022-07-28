package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.RestoreViewModel

abstract class RestoreActivity: ProtectedFragmentActivity() {
    private val restoreViewModel: RestoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((applicationContext as MyApplication).appComponent) {
            inject(restoreViewModel)
        }
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
                        result.onFailure {
                            progressDialogFragment?.appendToMessage(it.safeMessage)
                        }
                        progressDialogFragment?.onTaskCompleted()
                        onPostRestoreTask(it)
                        restoreViewModel.resultProcessed()
                    }
                }
            }
        }
    }

    protected fun doRestore(args: Bundle) {
        supportFragmentManager
            .beginTransaction()
            .add(
                ProgressDialogFragment.newInstance(getString(R.string.pref_restore_title), true),
                PROGRESS_TAG
            ).commitNow()
        restoreViewModel.startRestore(args)
    }

    protected open fun onPostRestoreTask(result: Result<Unit>) {
        if (result.isSuccess) {
            licenceHandler.reset()
            // if the backup is password protected, we want to force the password
            // check
            // is it not enough to set mLastPause to zero, since it would be
            // overwritten by the callings activity onpause
            // hence we need to set isLocked if necessary
            val myApplication = requireApplication()
            myApplication.resetLastPause()
            if (myApplication.shouldLock(this)) {
                myApplication.isLocked = true
            }
        }
    }
}