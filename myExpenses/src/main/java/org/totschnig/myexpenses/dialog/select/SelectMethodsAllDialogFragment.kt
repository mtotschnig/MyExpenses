package org.totschnig.myexpenses.dialog.select

import android.net.Uri
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.Criteria
import org.totschnig.myexpenses.provider.filter.MethodCriteria

class SelectMethodsAllDialogFragment : SelectFilterDialog(true) {
    override fun makeCriteria(label: String, vararg ids: Long): Criteria =
        if (ids.size == 1 && ids[0] == NULL_ITEM_ID) MethodCriteria() else MethodCriteria(
            label,
            *ids
        )

    override val uri: Uri = TransactionProvider.METHODS_URI

    override val column: String
        get() = PaymentMethod.localizedLabelSqlColumn(context, KEY_LABEL)

    override val dialogTitle: Int = R.string.search_method

}