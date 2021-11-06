package org.totschnig.myexpenses.activity

import android.content.Intent
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.viewmodel.data.Debt

abstract class DebtActivity: ProtectedFragmentActivity() {

    fun editDebt(debt: Debt) {
        startActivity(Intent(this, DebtEdit::class.java).apply {
            putExtra(DatabaseConstants.KEY_PAYEEID, debt.payeeId)
            putExtra(DatabaseConstants.KEY_PAYEE_NAME, debt.payeeName)
            putExtra(DatabaseConstants.KEY_DEBT_ID, debt.id)
        })
    }

    fun deleteDebt(debt:Debt, count: Int) {
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

    abstract fun deleteDebtDo(debtId: Long)
}