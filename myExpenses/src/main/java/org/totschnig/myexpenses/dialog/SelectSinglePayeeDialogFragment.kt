package org.totschnig.myexpenses.dialog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.AbsListView.CHOICE_MODE_SINGLE
import androidx.appcompat.app.AlertDialog
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.TransactionProvider

class SelectSinglePayeeDialogFragment : SelectFromTableDialogFragment(false) {

    override fun getUri(): Uri  = TransactionProvider.PAYEES_URI

    override fun getColumn() = DatabaseConstants.KEY_PAYEE_NAME

    override fun onResult(labelList: MutableList<String>, itemIds: LongArray, which: Int): Boolean {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, Intent().apply {
                putExtra(KEY_LABEL, labelList[0])
                putExtra(KEY_PAYEEID, itemIds[0])
            })
        }
        return true
    }

    override fun getChoiceMode() = CHOICE_MODE_SINGLE

    companion object {
        @JvmStatic
        fun newInstance(dialogTitle: Int): SelectSinglePayeeDialogFragment = SelectSinglePayeeDialogFragment().apply {
            arguments = Bundle().apply { putInt(KEY_DIALOG_TITLE, dialogTitle) }
        }
    }
}