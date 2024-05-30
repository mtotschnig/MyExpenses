package org.totschnig.myexpenses.activity

import android.view.MenuItem
import eltos.simpledialogfragment.input.SimpleInputDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.AmountFilterDialog
import org.totschnig.myexpenses.dialog.DateFilterDialog
import org.totschnig.myexpenses.dialog.select.SelectCrStatusDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMethodDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMultipleAccountDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectTransferAccountDialogFragment
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.filter.AccountCriterion
import org.totschnig.myexpenses.provider.filter.AmountCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.DateCriterion
import org.totschnig.myexpenses.provider.filter.IdCriterion
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.provider.filter.TransferCriterion
import org.totschnig.myexpenses.util.checkMenuIcon
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.viewmodel.SumInfoLoaded

class FilterHandler(private val activity: BaseMyExpenses) {
    fun configureSearchMenu(searchMenu: MenuItem) {
        with(activity) {
            searchMenu.setEnabledAndVisible((sumInfo as? SumInfoLoaded)?.hasItems == true)
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
                    filterItem.setEnabledAndVisible(enabled || c != null)
                    if (c != null) {
                        filterItem.isChecked = true
                        filterItem.title = c.prettyPrint(this)
                    }
                }
            }
        }
    }

    fun handleFilter(itemId: Int, edit: Criterion<*>? = null): Boolean {
        with(activity) {
            if (accountCount == 0) return false
            if (edit == null && removeFilter(itemId)) return true
            val accountId = currentAccount?.id ?: return false
            when (itemId) {
                R.id.FILTER_CATEGORY_COMMAND -> getCategory.launch(
                    accountId to edit as? CategoryCriterion
                )

                R.id.FILTER_PAYEE_COMMAND -> getPayee.launch(
                    accountId to edit as? PayeeCriterion
                )

                R.id.FILTER_TAG_COMMAND -> getTags.launch(
                    accountId to edit as? TagCriterion
                )

                R.id.FILTER_AMOUNT_COMMAND -> AmountFilterDialog.newInstance(
                    currentAccount!!.currencyUnit, edit as? AmountCriterion
                ).show(supportFragmentManager, "AMOUNT_FILTER")

                R.id.FILTER_DATE_COMMAND -> DateFilterDialog.newInstance(
                    edit as? DateCriterion
                ).show(supportFragmentManager, "DATE_FILTER")

                R.id.FILTER_COMMENT_COMMAND -> SimpleInputDialog.build()
                    .title(R.string.search_comment)
                    .pos(R.string.menu_search)
                    .text((edit as? CommentCriterion)?.searchString)
                    .neut()
                    .show(this, FILTER_COMMENT_DIALOG)

                R.id.FILTER_STATUS_COMMAND -> SelectCrStatusDialogFragment.newInstance(
                    edit as? CrStatusCriterion
                ).show(supportFragmentManager, "STATUS_FILTER")

                R.id.FILTER_METHOD_COMMAND -> SelectMethodDialogFragment.newInstance(
                    accountId, edit as? MethodCriterion
                ).show(supportFragmentManager, "METHOD_FILTER")

                R.id.FILTER_TRANSFER_COMMAND -> SelectTransferAccountDialogFragment.newInstance(
                    accountId, edit as? TransferCriterion
                ).show(supportFragmentManager, "TRANSFER_FILTER")

                R.id.FILTER_ACCOUNT_COMMAND -> SelectMultipleAccountDialogFragment.newInstance(
                    if (currentAccount!!.isHomeAggregate) null else currentAccount!!.currency, edit as? AccountCriterion
                )
                    .show(supportFragmentManager, "ACCOUNT_FILTER")

                else -> return false
            }
            return true
        }
    }

    private inline fun <reified I : IdCriterion, C : PickObjectContract<I>>
            buildLauncher(createContract: () -> C) =
        activity.registerForActivityResult(createContract()) { criterion ->
            criterion?.let { activity.addFilterCriterion(it) }
        }

    private val getCategory = buildLauncher(::PickCategoryContract)
    private val getPayee = buildLauncher(::PickPayeeContract)
    private val getTags = buildLauncher(::PickTagContract)

    companion object {
        const val FILTER_COMMENT_DIALOG = "dialogFilterComment"
    }
}