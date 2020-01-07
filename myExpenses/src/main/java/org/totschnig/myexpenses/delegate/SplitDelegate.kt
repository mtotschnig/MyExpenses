package org.totschnig.myexpenses.delegate

import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.preference.PrefHandler

class SplitDelegate(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, prefHandler: PrefHandler) : MainDelegate<SplitTransaction>(viewBinding, dateEditBinding, prefHandler) {
    override val operationType = TransactionsContract.Transactions.TYPE_SPLIT

    override val helpVariant: ExpenseEdit.HelpVariant
        get() = ExpenseEdit.HelpVariant.split
    override val title = R.string.menu_edit_split

    override fun bind(transaction: SplitTransaction, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, recurrence: Plan.Recurrence?) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance, recurrence)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun buildMainTransaction() = SplitTransaction()
}