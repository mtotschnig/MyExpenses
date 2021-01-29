package org.totschnig.myexpenses.dialog.select

import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider

class SelectSingleMethodDialogFragment : SelectSingleDialogFragment() {
    override fun getUri() = TransactionProvider.METHODS_URI.buildUpon()
            .appendPath(TransactionProvider.URI_SEGMENT_TYPE_FILTER)
            .appendPath(arguments?.getString(KEY_SIGNUM))
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_ACCOUNTY_TYPE_LIST,
                    arguments?.getStringArray(KEY_ACCOUNT_TYPES)?.joinToString(separator = ";"))
            .build()

    override fun getColumn() = DatabaseConstants.KEY_LABEL

    companion object {
        const val KEY_SIGNUM = "signum"
        const val KEY_ACCOUNT_TYPES = "accountTypes"
        @JvmStatic
        fun newInstance(dialogTitle: Int, remap_empty_list: Int, accountTypes: Array<String>, signum: Int) = SelectSingleMethodDialogFragment().apply {
            arguments = buildArguments(dialogTitle, remap_empty_list).apply {
                putStringArray(KEY_ACCOUNT_TYPES, accountTypes)
                putString(KEY_SIGNUM, signum.toString())
            }
        }
    }
}