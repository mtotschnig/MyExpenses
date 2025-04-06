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

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.dialog.QifImportDialogFragment
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeatureNotAvailableException
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.QifImportViewModel

class QifImport : ProtectedFragmentActivity() {

    private val importViewModel: QifImportViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            QifImportDialogFragment.newInstance().show(supportFragmentManager, "QIF_IMPORT_SOURCE")
        }
        injector.inject(importViewModel)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                importViewModel.publishProgress.collect { progress ->
                    progress?.let {
                        progressDialogFragment?.appendToMessage(progress)
                    }
                }
            }
        }
    }

    fun onSourceSelected(
        mUri: Uri,
        qifDateFormat: QifDateFormat,
        accountId: Long,
        currency: String,
        withTransactions: Boolean,
        withCategories: Boolean,
        withParties: Boolean,
        encoding: String?,
        autoFillCategories: Boolean
    ) {
        supportFragmentManager.beginTransaction()
            .add(
                ProgressDialogFragment.newInstance(
                    getString(R.string.pref_import_title, "QIF"),
                    null,
                    ProgressDialog.STYLE_SPINNER,
                    true
                ), PROGRESS_TAG
            )
            .commitNow()
        importViewModel.importData(mUri, qifDateFormat, accountId, currencyContext[currency], withTransactions,
            withCategories, withParties, encoding, autoFillCategories).observe(this) {
                it.onFailure {
                    if (it !is ContribFeatureNotAvailableException) {
                        CrashHandler.report(it)
                    }
                    progressDialogFragment?.appendToMessage(it.safeMessage)
                }
                progressDialogFragment?.onTaskCompleted()
        }

    }

    override fun onProgressDialogDismiss() {
        finish()
    }

    override fun onMessageDialogDismissOrCancel() {
        finish()
    }
}