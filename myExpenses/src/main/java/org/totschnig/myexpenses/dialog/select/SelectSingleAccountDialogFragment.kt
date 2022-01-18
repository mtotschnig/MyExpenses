package org.totschnig.myexpenses.dialog.select

import android.net.Uri
import org.jetbrains.annotations.NotNull
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.TransactionProvider

class SelectSingleAccountDialogFragment : SelectSingleDialogFragment() {
    override val uri: Uri = TransactionProvider.ACCOUNTS_URI
    override val column: String = DatabaseConstants.KEY_LABEL
    override val selection: String
        get() = "$KEY_SEALED = 0 " + (arguments?.getLongArray(KEY_EXCLUDED_IDS)?.let {
            "AND $KEY_ROWID NOT IN (${it.joinToString()})"
        } ?: "")

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