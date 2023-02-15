package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import eltos.simpledialogfragment.input.SimpleInputDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.AmountFilterDialog
import org.totschnig.myexpenses.dialog.DateFilterDialog
import org.totschnig.myexpenses.dialog.select.SelectCrStatusDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMethodDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMultipleAccountDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectTransferAccountDialogFragment
import org.totschnig.myexpenses.fragment.TagList.Companion.KEY_TAG_LIST
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.checkMenuIcon
import org.totschnig.myexpenses.viewmodel.SumInfoLoaded
import org.totschnig.myexpenses.viewmodel.data.Tag

class FilterHandler(private val activity: BaseMyExpenses) {
    fun configureSearchMenu(searchMenu: MenuItem) {
        with(activity) {
            val sumInfoIsLoaded = sumInfo is SumInfoLoaded
            Utils.menuItemSetEnabledAndVisible(searchMenu, sumInfoIsLoaded)
            (sumInfo as? SumInfoLoaded)?.let { sumInfo ->
                val whereFilter = currentFilter.whereFilter
                searchMenu.isChecked = !whereFilter.isEmpty
                checkMenuIcon(searchMenu)
                val filterMenu = searchMenu.subMenu!!
                for (i in 0 until filterMenu.size()) {
                    val filterItem = filterMenu.getItem(i)
                    var enabled = true
                    when (filterItem.itemId) {
                        R.id.FILTER_CATEGORY_COMMAND -> {
                            enabled = sumInfo.mappedCategories
                        }
                        R.id.FILTER_STATUS_COMMAND -> {
                            enabled =
                                currentAccount!!.isAggregate || currentAccount!!.type != AccountType.CASH
                        }
                        R.id.FILTER_PAYEE_COMMAND -> {
                            enabled = sumInfo.mappedPayees
                        }
                        R.id.FILTER_METHOD_COMMAND -> {
                            enabled = sumInfo.mappedMethods
                        }
                        R.id.FILTER_TRANSFER_COMMAND -> {
                            enabled = sumInfo.hasTransfers
                        }
                        R.id.FILTER_TAG_COMMAND -> {
                            enabled = sumInfo.hasTags
                        }
                        R.id.FILTER_ACCOUNT_COMMAND -> {
                            enabled = currentAccount!!.isAggregate
                        }
                    }
                    val c: Criterion<*>? = whereFilter[filterItem.itemId]
                    Utils.menuItemSetEnabledAndVisible(filterItem, enabled || c != null)
                    if (c != null) {
                        filterItem.isChecked = true
                        filterItem.title = c.prettyPrint(this)
                    }
                }
            }
        }
    }

    fun handleFilter(itemId: Int): Boolean {
        with(activity) {
            if (accountCount == 0) return false
            if (removeFilter(itemId)) return true
            val accountId = currentAccount!!.id
            when (itemId) {
                R.id.FILTER_CATEGORY_COMMAND -> getCategory.launch(accountId)
                R.id.FILTER_PAYEE_COMMAND -> getPayee.launch(accountId)
                R.id.FILTER_TAG_COMMAND -> getTags.launch(accountId)
                R.id.FILTER_AMOUNT_COMMAND -> AmountFilterDialog.newInstance(currentAccount!!.currency)
                    .show(supportFragmentManager, "AMOUNT_FILTER")
                R.id.FILTER_DATE_COMMAND -> DateFilterDialog.newInstance()
                    .show(supportFragmentManager, "DATE_FILTER")
                R.id.FILTER_COMMENT_COMMAND -> SimpleInputDialog.build()
                    .title(R.string.search_comment)
                    .pos(R.string.menu_search)
                    .neut()
                    .show(this, FILTER_COMMENT_DIALOG)
                R.id.FILTER_STATUS_COMMAND -> SelectCrStatusDialogFragment.newInstance()
                    .show(supportFragmentManager, "STATUS_FILTER")
                R.id.FILTER_METHOD_COMMAND -> SelectMethodDialogFragment.newInstance(accountId)
                    .show(supportFragmentManager, "METHOD_FILTER")
                R.id.FILTER_TRANSFER_COMMAND -> SelectTransferAccountDialogFragment.newInstance(
                    accountId
                ).show(supportFragmentManager, "TRANSFER_FILTER")
                R.id.FILTER_ACCOUNT_COMMAND -> SelectMultipleAccountDialogFragment.newInstance(
                    currentAccount!!.currency.code
                )
                    .show(supportFragmentManager, "ACCOUNT_FILTER")
                else -> return false
            }
            return true
        }
    }

    private val getCategory =
        activity.registerForActivityResult(PickObjectContract(FILTER_CATEGORY_REQUEST)) {}
    private val getPayee =
        activity.registerForActivityResult(PickObjectContract(FILTER_PAYEE_REQUEST)) {}
    private val getTags =
        activity.registerForActivityResult(PickObjectContract(FILTER_TAGS_REQUEST)) {}

    private inner class PickObjectContract(private val requestKey: String) :
        ActivityResultContract<Long, Unit>() {
        override fun createIntent(context: Context, input: Long) =
            Intent(
                context, when (requestKey) {
                    FILTER_CATEGORY_REQUEST -> ManageCategories::class.java
                    FILTER_PAYEE_REQUEST -> ManageParties::class.java
                    FILTER_TAGS_REQUEST -> ManageTags::class.java
                    else -> throw IllegalArgumentException()
                }
            ).apply {
                action = Action.SELECT_FILTER.name
                putExtra(DatabaseConstants.KEY_ACCOUNTID, input)
            }

        override fun parseResult(resultCode: Int, intent: Intent?) {
            if (resultCode == Activity.RESULT_OK) {
                if (requestKey == FILTER_TAGS_REQUEST) {
                    intent?.getParcelableArrayListExtra<Tag>(KEY_TAG_LIST)?.let { tagList ->
                        val ids = tagList.map { it.id }.toLongArray()
                        val labels = tagList.joinToString { it.label }
                        activity.addFilterCriterion(TagCriterion(labels, *ids))
                    }
                } else {
                    intent?.extras?.let {
                        val rowId = it.getLong(DatabaseConstants.KEY_ROWID)
                        val label = it.getString(DatabaseConstants.KEY_LABEL)
                        if (rowId != 0L && label != null) {
                            when (requestKey) {
                                FILTER_CATEGORY_REQUEST -> addCategoryFilter(label, rowId)
                                FILTER_PAYEE_REQUEST -> addPayeeFilter(label, rowId)
                            }
                        }
                        Unit
                    }
                }
            }
            if (resultCode == Activity.RESULT_FIRST_USER) {
                intent?.extras?.let {
                    val rowIds = it.getLongArray(DatabaseConstants.KEY_ROWID)
                    val label = it.getString(DatabaseConstants.KEY_LABEL)
                    if (rowIds != null && label != null) {
                        when (requestKey) {
                            FILTER_CATEGORY_REQUEST -> addCategoryFilter(label, *rowIds)
                            FILTER_PAYEE_REQUEST -> addPayeeFilter(label, *rowIds)
                        }
                    }
                    Unit
                }
            }
        }
    }

    private fun addCategoryFilter(label: String, vararg catIds: Long) {
        with(activity) {
            addFilterCriterion(
                if (catIds.size == 1 && catIds[0] == NULL_ITEM_ID) CategoryCriterion() else
                    CategoryCriterion(label, *catIds)
            )
        }
    }

    private fun addPayeeFilter(label: String, vararg catIds: Long) {
        with(activity) {
            addFilterCriterion(
                if (catIds.size == 1 && catIds[0] == NULL_ITEM_ID) PayeeCriterion() else
                    PayeeCriterion(label, *catIds)
            )
        }
    }

    companion object {
        const val FILTER_CATEGORY_REQUEST = "filterCategory"
        const val FILTER_PAYEE_REQUEST = "filterPayee"
        const val FILTER_TAGS_REQUEST = "filterTags"
        const val FILTER_COMMENT_DIALOG = "dialogFilterComment"
    }
}