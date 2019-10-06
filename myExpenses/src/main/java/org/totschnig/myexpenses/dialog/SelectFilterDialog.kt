package org.totschnig.myexpenses.dialog

import android.text.TextUtils
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.filter.Criteria

abstract class SelectFilterDialog(withNullItem: Boolean) : SelectFromTableDialogFragment(withNullItem) {
    abstract protected fun makeCriteria(label: String, vararg ids: Long): Criteria
    abstract protected fun getCommand(): Int
    override fun onResult(labelList: List<String>, itemIds: LongArray, which: Int): Boolean {
        if (itemIds.size == 1 || itemIds.indexOf(-1L) == -1) {
            (getActivity() as Host).addFilterCriteria(
                    getCommand(),
                    makeCriteria(TextUtils.join(",", labelList), *itemIds))
            return true
        } else {
            showSnackbar(R.string.unmapped_filter_only_single)
            return false
        }
    }
    interface Host {
        fun addFilterCriteria(id: Int, c: Criteria)
    }
}