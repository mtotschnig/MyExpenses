package org.totschnig.myexpenses.delegate

import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.preference.PrefHandler

class SplitDelegate(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, prefHandler: PrefHandler) : TransactionDelegate<SplitTransaction>(viewBinding, dateEditBinding, prefHandler) {
    override fun bind(transaction: SplitTransaction, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}