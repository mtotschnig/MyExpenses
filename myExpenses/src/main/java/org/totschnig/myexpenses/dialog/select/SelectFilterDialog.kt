package org.totschnig.myexpenses.dialog.select

import android.text.TextUtils
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID

abstract class SelectFilterDialog<T : Criterion<*>>(withNullItem: Boolean) : SelectMultipleDialogFragment(withNullItem) {
    protected abstract fun makeCriteria(label: String, vararg ids: Long): T

    override fun onResult(labelList: List<String>, itemIds: LongArray, which: Int) =
            if (itemIds.size == 1 || itemIds.indexOf(NULL_ITEM_ID) == -1) {
                (activity as Host).addFilterCriterion(
                        makeCriteria(TextUtils.join(",", labelList), *itemIds))
                true
            } else {
                showSnackBar(R.string.unmapped_filter_only_single)
                false
            }

    interface Host {
        fun addFilterCriterion(c: Criterion<*>)
    }
}