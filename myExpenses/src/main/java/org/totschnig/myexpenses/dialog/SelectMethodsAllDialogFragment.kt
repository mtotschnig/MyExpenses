package org.totschnig.myexpenses.dialog

import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.Criteria
import org.totschnig.myexpenses.provider.filter.MethodCriteria

class SelectMethodsAllDialogFragment: SelectFilterDialog(true) {
    override fun makeCriteria(label: String, vararg ids: Long): Criteria =
            if (ids.size == 1 && ids[0] == -1L) MethodCriteria() else MethodCriteria(label, *ids)

    override fun getDialogTitle() = R.string.search_method

    override fun getUri() = TransactionProvider.METHODS_URI

    override fun getColumn() = KEY_LABEL

    override fun getSelectionArgs() = null

    override fun getSelection() = null
}