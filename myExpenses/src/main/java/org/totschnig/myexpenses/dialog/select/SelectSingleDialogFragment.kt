package org.totschnig.myexpenses.dialog.select

import android.content.DialogInterface
import android.os.Bundle
import android.widget.AbsListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.setFragmentResult
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID

abstract class SelectSingleDialogFragment : SelectFromTableDialogFragment(false) {
    override fun onClick(dialog: DialogInterface, which: Int) {
        if (activity == null || which != AlertDialog.BUTTON_POSITIVE) {
            return
        }
        buildExtras()?.let {
            setFragmentResult(requireArguments().getString(KEY_REQUEST_KEY)!!, it)
        }
        dismiss()
    }

    override val dialogTitle: Int
        get() = requireArguments().getInt(KEY_DIALOG_TITLE)

    open fun buildExtras() = dataViewModel.selection?.getOrNull(0)?.let {
        Bundle().apply {
            putString(KEY_LABEL, it.label)
            putLong(KEY_ROWID, it.id)
        }
    }

    override val choiceMode: Int = AbsListView.CHOICE_MODE_SINGLE

    companion object {
        private const val KEY_REQUEST_KEY = "requestKey"
        internal fun buildArguments(
            dialogTitle: Int,
            emptyMessage: Int? = null,
            requestKey: String
        ) = Bundle().apply {
            putInt(KEY_DIALOG_TITLE, dialogTitle)
            if (emptyMessage != null) {
                putInt(KEY_EMPTY_MESSAGE, emptyMessage)
            }
            putString(KEY_REQUEST_KEY, requestKey)
        }
    }
}