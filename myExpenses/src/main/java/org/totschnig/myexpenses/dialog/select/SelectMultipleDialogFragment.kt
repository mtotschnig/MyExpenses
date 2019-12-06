package org.totschnig.myexpenses.dialog.select

import android.content.DialogInterface
import android.database.Cursor
import androidx.appcompat.app.AlertDialog
import java.util.*

abstract class SelectMultipleDialogFragment(withNullItem: Boolean): SelectFromTableDialogFragment(withNullItem) {
    protected abstract fun onResult(labelList: List<String>, itemIds: LongArray, which: Int): Boolean

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (activity == null) {
            return
        }
        val listView = (dialog as AlertDialog).listView
        val positions = listView.checkedItemPositions
        val itemIds = listView.checkedItemIds
        var shouldDismiss = true
        if (itemIds.size > 0) {
            val labelList = ArrayList<String>()
            for (i in 0 until positions.size()) {
                if (positions.valueAt(i)) {
                    val cursor = adapter.getItem(positions.keyAt(i)) as Cursor
                    labelList.add(cursor.getString(cursor.getColumnIndex(column)))
                }
            }
            shouldDismiss = onResult(labelList, itemIds, which)
        }
        if (shouldDismiss) {
            dismiss()
        }
    }
}