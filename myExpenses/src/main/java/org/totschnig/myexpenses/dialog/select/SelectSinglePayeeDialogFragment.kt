package org.totschnig.myexpenses.dialog.select

import android.net.Uri
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider

class SelectSinglePayeeDialogFragment : SelectSingleDialogFragment() {

    override fun getUri(): Uri  = TransactionProvider.PAYEES_URI

    override fun getColumn() = DatabaseConstants.KEY_PAYEE_NAME

    companion object {
        @JvmStatic
        fun newInstance(dialogTitle: Int): SelectSinglePayeeDialogFragment = SelectSinglePayeeDialogFragment().apply {
            arguments = buildArguments(dialogTitle)
        }
    }
}