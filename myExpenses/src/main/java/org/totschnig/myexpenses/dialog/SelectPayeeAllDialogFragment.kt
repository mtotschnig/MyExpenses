package org.totschnig.myexpenses.dialog

import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.Criteria
import org.totschnig.myexpenses.provider.filter.NullCriteria
import org.totschnig.myexpenses.provider.filter.PayeeCriteria

class SelectPayeeAllDialogFragment: SelectFilterDialog(true) {
    override fun makeCriteria(label: String, vararg ids: Long): Criteria =
            if (ids.size == 1 && ids[0] == -1L) NullCriteria(KEY_PAYEEID) else PayeeCriteria(label, *ids)

    override fun getCommand() = R.id.FILTER_PAYEE_COMMAND

    override fun getDialogTitle() = R.string.search_payee

    override fun getUri() = TransactionProvider.PAYEES_URI

    override fun getColumn() = KEY_PAYEE_NAME

    override fun getSelectionArgs() = null

    override fun getSelection() = null
}