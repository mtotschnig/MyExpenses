package org.totschnig.myexpenses.dialog.select

import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.Criteria
import org.totschnig.myexpenses.provider.filter.PayeeCriteria

class SelectPayeeFilterDialog: SelectFilterDialog(true) {
    override fun makeCriteria(label: String, vararg ids: Long): Criteria =
            if (ids.size == 1 && ids[0] == NULL_ITEM_ID) PayeeCriteria() else PayeeCriteria(label, *ids)

    override fun getDialogTitle() = R.string.search_payee

    override fun getUri() = TransactionProvider.PAYEES_URI

    override fun getColumn() = KEY_PAYEE_NAME

}