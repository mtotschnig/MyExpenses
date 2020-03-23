package org.totschnig.myexpenses.dialog.select

import android.text.TextUtils
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.filter.Criteria

abstract class SelectFilterDialog(withNullItem: Boolean) : SelectMultipleDialogFragment(withNullItem) {
    abstract protected fun makeCriteria(label: String, vararg ids: Long): Criteria

    override fun onResult(labelList: List<String>, itemIds: LongArray, which: Int): Boolean {
        if (itemIds.size == 1 || itemIds.indexOf(NULL_ITEM_ID) == -1) {
            (activity as Host).addFilterCriteria(
                    makeCriteria(TextUtils.join(",", labelList), *itemIds))
            return true
        } else {
            showSnackbar(R.string.unmapped_filter_only_single)
            return false
        }
    }

    interface Host {
        fun addFilterCriteria(c: Criteria)
    }
}