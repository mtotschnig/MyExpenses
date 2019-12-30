package org.totschnig.myexpenses.viewholder

import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefHandler

class TransferViewHolder(viewBinding: OneExpenseBinding) : TransactionViewHolder<Transfer>(viewBinding) {
    override fun bind(transaction: Transfer, isCalendarPermissionPermanentlyDeclined: Boolean, prefHandler: PrefHandler) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, prefHandler)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}