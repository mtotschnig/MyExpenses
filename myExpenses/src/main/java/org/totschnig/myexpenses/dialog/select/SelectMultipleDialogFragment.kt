package org.totschnig.myexpenses.dialog.select

import android.content.DialogInterface

abstract class SelectMultipleDialogFragment(withNullItem: Boolean) :
    SelectFromTableDialogFragment(withNullItem) {
    protected abstract fun onResult(
        labelList: List<String>,
        itemIds: LongArray,
        which: Int
    ): Boolean

    override fun onClick(dialog: DialogInterface, which: Int) {
        dataViewModel.selection?.takeIf { it.isNotEmpty() }?.also { selection ->
            if (onResult(
                    selection.map { it.label },
                    selection.map { it.id }.toLongArray(),
                    which
                )
            ) {
                dismiss()
            }
        } ?: run { dismiss() }
    }
}