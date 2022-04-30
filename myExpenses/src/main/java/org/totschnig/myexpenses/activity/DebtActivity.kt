package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.ShareCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.Debt
import timber.log.Timber


abstract class DebtActivity : ProtectedFragmentActivity() {

    protected val debtViewModel: DebtViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as MyApplication).appComponent.inject(debtViewModel)
    }

    fun editDebt(debt: Debt) {
        startActivityForResult(Intent(this, DebtEdit::class.java).apply {
            putExtra(DatabaseConstants.KEY_PAYEEID, debt.payeeId)
            putExtra(DatabaseConstants.KEY_PAYEE_NAME, debt.payeeName)
            putExtra(DatabaseConstants.KEY_DEBT_ID, debt.id)
        }, DEBT_EDIT_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == DEBT_EDIT_REQUEST && resultCode == RESULT_FIRST_USER) {
            showSnackBar(R.string.object_sealed_debt)
        }
    }

    fun toggleDebt(debt: Debt) {
        if (debt.isSealed) {
            debtViewModel.reopenDebt(debt.id)
        } else {
            debtViewModel.closeDebt(debt.id)
        }
    }

    fun shareDebt(debt: Debt, exportFormat: DebtViewModel.ExportFormat) {
        showProgressSnackBar(getString(R.string.progress_dialog_printing, exportFormat.name))
        when(exportFormat) {
            DebtViewModel.ExportFormat.HTML -> debtViewModel.exportHtml(this, debt).observe(this) { uri ->
                dismissSnackBar()
/*                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/html")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                })*/
                ShareCompat.IntentBuilder(this)
                    .setType(exportFormat.mimeType)
                    .setSubject(debt.title(this))
                    .setStream(uri)
                    .startChooser()
            }
            DebtViewModel.ExportFormat.TXT -> debtViewModel.exportText(this, debt).observe(this) {
                dismissSnackBar()
                Timber.d("Debt Export: %s", it)
                ShareCompat.IntentBuilder(this)
                    .setType(exportFormat.mimeType)
                    .setSubject(debt.title(this))
                    .setText(it)
                    .startChooser()
            }
        }
    }

    fun deleteDebt(debt: Debt, count: Int) {
        MessageDialogFragment.newInstance(
            getString(R.string.dialog_title_delete_debt),
            "${
                resources.getQuantityString(
                    R.plurals.debt_mapped_transactions,
                    count,
                    debt.label,
                    count
                )
            } ${getString(R.string.continue_confirmation)}",
            MessageDialogFragment.Button(
                R.string.menu_delete,
                R.id.DELETE_DEBT_COMMAND,
                debt.id
            ),
            null,
            MessageDialogFragment.noButton(), 0
        )
            .show(supportFragmentManager, "DELETE_DEBT")
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        if (command == R.id.DELETE_DEBT_COMMAND) {
            deleteDebtDo(tag as Long)
            return true
        }
        return false
    }

    private fun deleteDebtDo(debtId: Long) {
        debtViewModel.deleteDebt(debtId).observe(this) {
            if (!it) {
                lifecycleScope.launchWhenResumed {
                    showSnackBar("ERROR", Snackbar.LENGTH_LONG, null)
                }
            }
        }
    }
}