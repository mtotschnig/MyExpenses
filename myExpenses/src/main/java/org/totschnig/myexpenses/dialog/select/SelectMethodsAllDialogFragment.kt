package org.totschnig.myexpenses.dialog.select

import android.net.Uri
import android.os.Bundle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.localizedLabelSqlColumn
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.KEY_CRITERION
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID

class SelectMethodsAllDialogFragment :
    SelectFilterDialog<MethodCriterion>(true, MethodCriterion::class.java) {
    override fun makeCriteria(label: String, vararg ids: Long): MethodCriterion =
        if (ids.size == 1 && ids[0] == NULL_ITEM_ID) MethodCriterion() else MethodCriterion(
            label,
            *ids
        )

    override val uri: Uri = TransactionProvider.METHODS_URI

    override val column: String
        get() = localizedLabelSqlColumn(requireContext(), KEY_LABEL)

    override val dialogTitle: Int = R.string.search_method

    companion object {
        fun newInstance(methodCriterion: MethodCriterion?) =
            SelectMethodsAllDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_CRITERION, methodCriterion)
                }
            }
    }
}