package org.totschnig.myexpenses.dialog.select

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.AbsListView
import androidx.appcompat.app.AlertDialog
import org.totschnig.myexpenses.provider.DatabaseConstants

abstract class SelectSingleDialogFragment : SelectFromTableDialogFragment(false) {
    override fun onClick(dialog: DialogInterface, which: Int) {
        if (activity == null || which != AlertDialog.BUTTON_POSITIVE) {
            return
        }
        val listView = (dialog as AlertDialog).listView
        val cursor = adapter.getItem(listView.checkedItemPosition) as Cursor
        onResult(cursor.getString(cursor.getColumnIndex(column)), listView.checkedItemIds[0])
        dismiss()
    }

    fun onResult(label: String, itemId: Long) {
        targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, Intent().apply {
            putExtra(DatabaseConstants.KEY_LABEL, label)
            putExtra(DatabaseConstants.KEY_ROWID, itemId)
        })
    }

    override fun getChoiceMode() = AbsListView.CHOICE_MODE_SINGLE

    companion object {
        internal fun buildArguments(dialogTitle: Int) = Bundle().apply { putInt(KEY_DIALOG_TITLE, dialogTitle) }
    }
}