package org.totschnig.myexpenses.dialog.select

import android.net.Uri
import org.totschnig.myexpenses.activity.RemapHandler.Companion.MAP_METHOD_REQUEST
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider

class SelectSingleMethodDialogFragment : SelectSingleDialogFragment() {
    override val uri: Uri
        get() = TransactionProvider.METHODS_URI.buildUpon()
            .appendPath(TransactionProvider.URI_SEGMENT_TYPE_FILTER)
            .appendPath(arguments?.getString(KEY_SIGNUM))
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_ACCOUNT_TYPE_LIST,
                    arguments?.getLongArray(KEY_ACCOUNT_TYPES)?.joinToString(separator = ";"))
            .build()

    override val column: String = DatabaseConstants.KEY_LABEL

    companion object {
        const val KEY_SIGNUM = "signum"
        const val KEY_ACCOUNT_TYPES = "accountTypes"
        @JvmStatic
        fun newInstance(dialogTitle: Int, emptyListMessage: Int, accountTypes: LongArray, signum: Int) = SelectSingleMethodDialogFragment().apply {
            arguments = buildArguments(dialogTitle, emptyListMessage, MAP_METHOD_REQUEST).apply {
                putLongArray(KEY_ACCOUNT_TYPES, accountTypes)
                putString(KEY_SIGNUM, signum.toString())
            }
        }
    }
}