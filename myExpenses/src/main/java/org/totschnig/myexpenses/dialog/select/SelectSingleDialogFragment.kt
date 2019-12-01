package org.totschnig.myexpenses.dialog.select

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.AbsListView
import androidx.appcompat.app.AlertDialog
import org.totschnig.myexpenses.provider.DatabaseConstants

abstract class SelectSingleDialogFragment : SelectFromTableDialogFragment(false) {
    override fun onResult(labelList: MutableList<String>, itemIds: LongArray, which: Int): Boolean {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, Intent().apply {
                putExtra(DatabaseConstants.KEY_LABEL, labelList[0])
                putExtra(DatabaseConstants.KEY_ROWID, itemIds[0])
            })
        }
        return true
    }

    override fun getChoiceMode() = AbsListView.CHOICE_MODE_SINGLE

    companion object {
        internal fun buildArguments(dialogTitle: Int) = Bundle().apply { putInt(KEY_DIALOG_TITLE, dialogTitle) }
    }
}