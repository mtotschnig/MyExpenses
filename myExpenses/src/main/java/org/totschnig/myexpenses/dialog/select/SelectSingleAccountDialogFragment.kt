package org.totschnig.myexpenses.dialog.select

import android.net.Uri
import org.jetbrains.annotations.NotNull
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter

class SelectSingleAccountDialogFragment : SelectSingleDialogFragment() {
    override fun getUri(): Uri = TransactionProvider.ACCOUNTS_URI

    override fun getColumn() = DatabaseConstants.KEY_LABEL

    override fun getSelection() = "$KEY_SEALED = 0 " + (arguments?.getLongArray(KEY_EXCLUDED_IDS)?.let {
        "AND %s NOT %s".format(KEY_ROWID, WhereFilter.Operation.IN.getOp(it.size))
    } ?: "")

    override fun getSelectionArgs() = arguments?.getLongArray(KEY_EXCLUDED_IDS)
            ?.map(Long::toString)
            ?.toTypedArray()

    companion object {
        const val KEY_EXCLUDED_IDS = "excludedIds"
        @JvmStatic
        fun newInstance(dialogTitle: Int, emptyMessage: Int, excludedIds: List<Long>): @NotNull SelectSingleAccountDialogFragment = SelectSingleAccountDialogFragment().apply {
            arguments = buildArguments(dialogTitle, emptyMessage).apply {
                putLongArray(KEY_EXCLUDED_IDS, excludedIds.toLongArray())
            }
        }
    }
}