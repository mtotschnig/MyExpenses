package org.totschnig.myexpenses.dialog.select

import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.addToSelection
import org.totschnig.myexpenses.dialog.confirmFilter
import org.totschnig.myexpenses.provider.filter.IdCriterion
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID
import org.totschnig.myexpenses.provider.filter.criterion

abstract class SelectFilterDialog<T : IdCriterion>(
    withNullItem: Boolean,
    private val typeParameterClass: Class<T>
) :
    SelectMultipleDialogFragment(withNullItem) {
    protected abstract fun makeCriteria(label: String, vararg ids: Long): T

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        arguments?.criterion(typeParameterClass)?.let { criterion ->
            if (criterion.values.isEmpty()) {
                dataViewModel.selectionState.addToSelection(NULL_ITEM_ID)
            } else {
                criterion.values.forEach {
                    dataViewModel.selectionState.addToSelection(it)
                }
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onResult(labelList: List<String>, itemIds: LongArray, which: Int) =
        if (itemIds.size == 1 || itemIds.indexOf(NULL_ITEM_ID) == -1) {
            parentFragmentManager.confirmFilter(
                requestKey,
                makeCriteria(TextUtils.join(",", labelList), *itemIds)
            )
            true
        } else {
            showSnackBar(R.string.unmapped_filter_only_single)
            false
        }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        parentFragmentManager.confirmFilter(requestKey, null)
    }
}