package org.totschnig.myexpenses.viewholder

import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefHandler

class TransferViewHolder(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, prefHandler: PrefHandler) : TransactionViewHolder<Transfer>(viewBinding, dateEditBinding, prefHandler) {
    override fun bind(transaction: Transfer, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}