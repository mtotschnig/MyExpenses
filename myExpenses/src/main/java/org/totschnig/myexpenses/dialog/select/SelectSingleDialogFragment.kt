package org.totschnig.myexpenses.dialog.select

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.AbsListView
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import org.totschnig.myexpenses.provider.DatabaseConstants

abstract class SelectSingleDialogFragment : SelectFromTableDialogFragment(false) {
    override fun onClick(dialog: DialogInterface, which: Int) {
        if (activity == null || which != AlertDialog.BUTTON_POSITIVE) {
            return
        }
        buildExtras()?.let {
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, Intent().apply {
                putExtras(it)
            })
        }
        dismiss()
    }

    override fun getDialogTitle(): Int {
        return requireArguments().getInt(KEY_DIALOG_TITLE)
    }

    private fun buildExtras(): Bundle? {
        val listView = (dialog as AlertDialog).listView
        return listView.checkedItemPosition.takeIf { it != AdapterView.INVALID_POSITION }?.let {
            val item = adapter.getItem(it) as DataHolder
            Bundle().apply {
                putString(DatabaseConstants.KEY_LABEL, item.label)
                putLong(DatabaseConstants.KEY_ROWID, listView.checkedItemIds[0])
            }
        }
    }

    override fun getChoiceMode() = AbsListView.CHOICE_MODE_SINGLE

    companion object {
        internal fun buildArguments(dialogTitle: Int, emptyMessage: Int? = null) = Bundle().apply {
            putInt(KEY_DIALOG_TITLE, dialogTitle)
            if (emptyMessage != null) {
                putInt(KEY_EMPTY_MESSAGE, emptyMessage)
            }
        }
    }
}