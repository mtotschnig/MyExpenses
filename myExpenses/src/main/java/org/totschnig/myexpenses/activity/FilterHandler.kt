package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID

class FilterHandler(val activity: BaseMyExpenses) {
    fun handleFilter(itemId: Int): Boolean {
        with(activity) {
            when (itemId) {
                R.id.FILTER_CATEGORY_COMMAND -> {
                    if (!viewModel.removeFilter(itemId, currentAccount.id)) {
                        getCategory.launch(Unit)
                    }
                }
                else -> return false
            }
            return true
        }
    }

    private val getCategory =
        activity.registerForActivityResult(PickObjectContract(FILTER_CATEGORY_REQUEST)) {}

    private inner class PickObjectContract(private val requestKey: String) :
        ActivityResultContract<Unit, Unit>() {
        override fun createIntent(context: Context, input: Unit) =
            Intent(
                context, when (requestKey) {
                    FILTER_CATEGORY_REQUEST -> ManageCategories::class.java
                    else -> throw IllegalArgumentException()
                }
            ).apply {
                action = ACTION_SELECT_FILTER
            }

        override fun parseResult(resultCode: Int, intent: Intent?) {
            if (resultCode == Activity.RESULT_OK) {
                intent?.extras?.let { onResultSingle(requestKey, it) }
            }
            if (resultCode == Activity.RESULT_FIRST_USER) {
                intent?.extras?.let { onResultMultiple(requestKey, it) }
            }
        }
    }

    private fun onResultMultiple(requestKey: String, result: Bundle) {
        val rowIds = result.getLongArray(DatabaseConstants.KEY_ROWID)
        val label = result.getString(DatabaseConstants.KEY_LABEL)
        if (rowIds != null && label != null) {
            if (requestKey == FILTER_CATEGORY_REQUEST) {
                addCategoryFilter(label, *rowIds)
            }
        }
    }

    private fun onResultSingle(requestKey: String, result: Bundle) {
        val rowId = result.getLong(DatabaseConstants.KEY_ROWID)
        val label = result.getString(DatabaseConstants.KEY_LABEL)
        if (rowId != 0L && label != null) {
            if (requestKey == FILTER_CATEGORY_REQUEST) {
                addCategoryFilter(label, rowId)
            }
        }
    }

    private fun addCategoryFilter(label: String, vararg catIds: Long) {
        with(activity) {
            viewModel.addFilterCriteria(
                if (catIds.size == 1 && catIds[0] == NULL_ITEM_ID) CategoryCriterion() else
                    CategoryCriterion(label, *catIds),
                currentAccount.id
            )
        }
    }

    companion object {
        const val FILTER_CATEGORY_REQUEST = "filterCategory"
    }
}