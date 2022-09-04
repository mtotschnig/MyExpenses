package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.SparseBooleanArray
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import eltos.simpledialogfragment.input.SimpleInputDialog
import icepick.State
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.ACTION_SELECT_MAPPING
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.BaseMyExpenses
import org.totschnig.myexpenses.activity.CONFIRM_MAP_TAG_REQUEST
import org.totschnig.myexpenses.activity.FILTER_CATEGORY_REQUEST
import org.totschnig.myexpenses.activity.FILTER_PAYEE_REQUEST
import org.totschnig.myexpenses.activity.FILTER_TAGS_REQUEST
import org.totschnig.myexpenses.activity.MAP_ACCOUNT_REQUEST
import org.totschnig.myexpenses.activity.MAP_CATEGORY_REQUEST
import org.totschnig.myexpenses.activity.MAP_METHOD_REQUEST
import org.totschnig.myexpenses.activity.MAP_PAYEE_REQUEST
import org.totschnig.myexpenses.activity.MAP_TAG_REQUEST
import org.totschnig.myexpenses.activity.ManageCategories
import org.totschnig.myexpenses.activity.ManageParties
import org.totschnig.myexpenses.activity.ManageTags
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.dialog.AmountFilterDialog
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.DateFilterDialog
import org.totschnig.myexpenses.dialog.TransactionDetailFragment
import org.totschnig.myexpenses.dialog.select.SelectCrStatusDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMethodDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMultipleAccountDialogFragment.Companion.newInstance
import org.totschnig.myexpenses.dialog.select.SelectSingleAccountDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectSingleMethodDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectTransferAccountDialogFragment
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.CheckTransferAccountOfSplitPartsHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_SAME_CURRENCY
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.TextUtils.withAmountColor
import org.totschnig.myexpenses.util.asTrueSequence
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.KEY_ROW_IDS
import org.totschnig.myexpenses.viewmodel.data.Tag
import se.emilsjolander.stickylistheaders.StickyListHeadersListView
import kotlin.math.sign

const val KEY_REPLACE = "replace"

class TransactionList : BaseTransactionList() {

    @JvmField
    @State
    var selectedTransactionSum: Long = 0

    @JvmField
    @State
    var selectedTransactionSumFormatted: String? = null

    private fun handleTagResult(intent: Intent) {
        lifecycleScope.launchWhenResumed {
            ConfirmTagDialogFragment().also {
                it.arguments = Bundle().apply {
                    putParcelableArrayList(
                        KEY_TAG_LIST,
                        intent.getParcelableArrayListExtra(KEY_TAG_LIST)
                    )
                }
                it.setTargetFragment(this@TransactionList, CONFIRM_MAP_TAG_REQUEST)
            }.show(parentFragmentManager, "CONFIRM")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode != Activity.RESULT_CANCELED) {
            when (requestCode) {
                CONFIRM_MAP_TAG_REQUEST -> {
                    intent?.let {
                        viewModel.tag(
                            binding.list.checkedItemIds,
                            it.getParcelableArrayListExtra(KEY_TAG_LIST)!!,
                            it.getBooleanExtra(KEY_REPLACE, false)
                        )
                    }
                    finishActionMode()
                }
                MAP_TAG_REQUEST -> {
                    handleTagResult(intent!!)
                }
                else -> {
                    super.onActivityResult(requestCode, resultCode, intent)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mAccount == null || activity == null) {
            return false
        }
        val command = item.itemId
        if (command == R.id.FILTER_CATEGORY_COMMAND) {
            if (!removeFilter(command)) {
                val i = Intent(activity, ManageCategories::class.java)
                i.action = ACTION_SELECT_FILTER
                startActivityForResult(i, FILTER_CATEGORY_REQUEST)
            }
            return true
        } else if (command == R.id.FILTER_TAG_COMMAND) {
            if (!removeFilter(command)) {
                val i = Intent(activity, ManageTags::class.java)
                i.action = ACTION_SELECT_FILTER
                startActivityForResult(i, FILTER_TAGS_REQUEST)
            }
            return true
        } else if (command == R.id.FILTER_AMOUNT_COMMAND) {
            if (!removeFilter(command)) {
                AmountFilterDialog.newInstance(mAccount.currencyUnit)
                    .show(requireActivity().supportFragmentManager, "AMOUNT_FILTER")
            }
            return true
        } else if (command == R.id.FILTER_DATE_COMMAND) {
            if (!removeFilter(command)) {
                DateFilterDialog.newInstance()
                    .show(requireActivity().supportFragmentManager, "DATE_FILTER")
            }
            return true
        } else if (command == R.id.FILTER_COMMENT_COMMAND) {
            if (!removeFilter(command)) {
                SimpleInputDialog.build()
                    .title(R.string.search_comment)
                    .pos(R.string.menu_search)
                    .neut()
                    .show(this, FILTER_COMMENT_DIALOG)
            }
            return true
        } else if (command == R.id.FILTER_STATUS_COMMAND) {
            if (!removeFilter(command)) {
                SelectCrStatusDialogFragment.newInstance()
                    .show(requireActivity().supportFragmentManager, "STATUS_FILTER")
            }
            return true
        } else if (command == R.id.FILTER_PAYEE_COMMAND) {
            if (!removeFilter(command)) {
                val i = Intent(activity, ManageParties::class.java)
                i.action = ACTION_SELECT_FILTER
                i.putExtra(DatabaseConstants.KEY_ACCOUNTID, mAccount.id)
                startActivityForResult(i, FILTER_PAYEE_REQUEST)
            }
            return true
        } else if (command == R.id.FILTER_METHOD_COMMAND) {
            if (!removeFilter(command)) {
                SelectMethodDialogFragment.newInstance(mAccount.id)
                    .show(requireActivity().supportFragmentManager, "METHOD_FILTER")
            }
            return true
        } else if (command == R.id.FILTER_TRANSFER_COMMAND) {
            if (!removeFilter(command)) {
                SelectTransferAccountDialogFragment.newInstance(mAccount.id)
                    .show(requireActivity().supportFragmentManager, "TRANSFER_FILTER")
            }
            return true
        } else if (command == R.id.FILTER_ACCOUNT_COMMAND) {
            if (!removeFilter(command)) {
                newInstance(mAccount.currencyUnit.code)
                    .show(requireActivity().supportFragmentManager, "ACCOUNT_FILTER")
            }
            return true
        } else if (command == R.id.PRINT_COMMAND) {
            val ctx = requireActivity() as MyExpenses
            if (hasItems) {
                AppDirHelper.checkAppDir(requireContext()).onSuccess {
                    ctx.contribFeatureRequested(ContribFeature.PRINT, null)
                }.onFailure {
                    ctx.showDismissibleSnackBar(it.safeMessage)
                }
            } else {
                ctx.showExportDisabledCommand()
            }
            return true
        } else if (command == R.id.SYNC_COMMAND) {
            mAccount.requestSync()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onHeaderLongClick(
        l: StickyListHeadersListView?, header: View?,
        itemPosition: Int, headerId: Long, currentlySticky: Boolean
    ): Boolean {
        val ctx = requireActivity() as MyExpenses
        headerData?.get(headerId)?.get(6)?.let {
            if (it > 0) {
                ctx.contribFeatureRequested(ContribFeature.DISTRIBUTION, headerId.toInt())
            } else {
                ctx.showSnackBar(R.string.no_mapped_transactions)
            }
        }
        return true
    }

    private fun warnSealedAccount(sealedAccount: Boolean, sealedDebt: Boolean, multiple: Boolean) {
        val resIds = mutableListOf<Int>()
        if (multiple) {
            resIds.add(R.string.warning_account_for_transaction_is_closed)
        }
        if (sealedAccount) {
            resIds.add(R.string.object_sealed)
        }
        if (sealedDebt) {
            resIds.add(R.string.object_sealed_debt)
        }
        (requireActivity() as ProtectedFragmentActivity).showSnackBar(
            concatResStrings(
                requireContext(),
                " ",
                *resIds.toIntArray()
            )
        )
    }

    override fun checkSealed(itemIds: LongArray, onChecked: Runnable) {
        (requireActivity() as BaseMyExpenses).buildCheckSealedHandler().check(itemIds) { result ->
            lifecycleScope.launchWhenResumed {
                result.onSuccess {
                    if (it.first && it.second) {
                        onChecked.run()
                    } else {
                        warnSealedAccount(!it.first, !it.second, itemIds.size > 1)
                    }
                }.onFailure(showFailure)
            }
        }
    }

    val showFailure: (exception: Throwable) -> Unit = {
        (requireActivity() as ProtectedFragmentActivity).showSnackBar(it.message!!)
    }

    override fun dispatchCommandMultiple(
        command: Int,
        positions: SparseBooleanArray,
        itemIds: LongArray
    ): Boolean {
        if (super.dispatchCommandMultiple(command, positions, itemIds)) {
            return true
        }
        val ctx = activity as MyExpenses? ?: return false
        if (command == R.id.DELETE_COMMAND) {
            var hasReconciled = false
            var hasNotVoid = false
            for (i in 0 until positions.size()) {
                if (positions.valueAt(i)) {
                    val pos = positions.keyAt(i)
                    if (mTransactionsCursor.moveToPosition(pos)) {
                        val status = enumValueOrDefault(
                            mTransactionsCursor.getString(columnIndexCrStatus),
                            CrStatus.UNRECONCILED
                        )
                        if (status == CrStatus.RECONCILED) {
                            hasReconciled = true
                        }
                        if (status != CrStatus.VOID) {
                            hasNotVoid = true
                        }
                        if (hasNotVoid && hasReconciled) break
                    } else {
                        CrashHandler.report(IllegalStateException("Move to position $pos failed (count = ${mTransactionsCursor.count}"))
                        return true
                    }
                }
            }
            val finalHasReconciled = hasReconciled
            val finalHasNotVoid = hasNotVoid
            checkSealed(itemIds) {
                var message = resources.getQuantityString(
                    R.plurals.warning_delete_transaction,
                    itemIds.size,
                    itemIds.size
                )
                if (finalHasReconciled) {
                    message += " " + getString(R.string.warning_delete_reconciled)
                }
                val b = Bundle()
                b.putInt(
                    ConfirmationDialogFragment.KEY_TITLE,
                    R.string.dialog_title_warning_delete_transaction
                )
                b.putString(ConfirmationDialogFragment.KEY_MESSAGE, message)
                b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.DELETE_COMMAND_DO)
                b.putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
                    R.id.CANCEL_CALLBACK_COMMAND
                )
                b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.menu_delete)
                if (finalHasNotVoid) {
                    b.putString(
                        ConfirmationDialogFragment.KEY_CHECKBOX_LABEL,
                        getString(R.string.mark_void_instead_of_delete)
                    )
                }
                b.putLongArray(KEY_ROW_IDS, itemIds)
                showConfirmationDialog(b, "DELETE_TRANSACTION")
            }
            return true
        } else if (command == R.id.SPLIT_TRANSACTION_COMMAND) {
            checkSealed(itemIds) {
                ctx.contribFeatureRequested(
                    ContribFeature.SPLIT_TRANSACTION,
                    itemIds
                )
            }
        } else if (command == R.id.UNGROUP_SPLIT_COMMAND) {
            checkSealed(itemIds) {
                val b = Bundle()
                b.putString(
                    ConfirmationDialogFragment.KEY_MESSAGE,
                    getString(R.string.warning_ungroup_split_transactions)
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                    R.id.UNGROUP_SPLIT_COMMAND
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
                    R.id.CANCEL_CALLBACK_COMMAND
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                    R.string.menu_ungroup_split_transaction
                )
                b.putLongArray(TaskExecutionFragment.KEY_LONG_IDS, itemIds)
                showConfirmationDialog(b, "UNSPLIT_TRANSACTION")
            }
            return true
        } else if (command == R.id.UNDELETE_COMMAND) {
            checkSealed(itemIds) {
                viewModel.undeleteTransactions(itemIds).observe(
                    viewLifecycleOwner
                ) { result: Int ->
                    if (result == 0) (requireActivity() as BaseActivity)
                        .showDeleteFailureFeedback(null)
                }
            }
        } else if (command == R.id.REMAP_CATEGORY_COMMAND) {
            checkSealed(itemIds) {
                val i = Intent(activity, ManageCategories::class.java)
                i.action = ACTION_SELECT_MAPPING
                startActivityForResult(i, MAP_CATEGORY_REQUEST)
            }
            return true
        } else if (command == R.id.MAP_TAG_COMMAND) {
            checkSealed(itemIds) {
                val i = Intent(activity, ManageTags::class.java)
                i.action = ACTION_SELECT_MAPPING
                startActivityForResult(i, MAP_TAG_REQUEST)
            }
            return true
        } else if (command == R.id.REMAP_PAYEE_COMMAND) {
            checkSealed(itemIds) {
                val i = Intent(activity, ManageParties::class.java)
                i.action = ACTION_SELECT_MAPPING
                startActivityForResult(i, MAP_PAYEE_REQUEST)
            }
            return true
        } else if (command == R.id.REMAP_METHOD_COMMAND) {
            checkSealed(itemIds) {
                var hasExpense = false
                var hasIncome = false
                val accountTypes: MutableSet<String> = HashSet()
                for (i in 0 until positions.size()) {
                    if (positions.valueAt(i)) {
                        mTransactionsCursor.moveToPosition(positions.keyAt(i))
                        val amount = mTransactionsCursor.getLong(
                            mTransactionsCursor.getColumnIndexOrThrow(KEY_AMOUNT)
                        )
                        if (amount > 0) hasIncome = true
                        if (amount < 0) hasExpense = true
                        accountTypes.add(
                            mTransactionsCursor.getString(
                                mTransactionsCursor.getColumnIndexOrThrow(
                                    DatabaseConstants.KEY_ACCOUNT_TYPE
                                )
                            )
                        )
                    }
                }
                var type = 0
                if (hasExpense && !hasIncome) type = -1 else if (hasIncome && !hasExpense) type = 1
                lifecycleScope.launchWhenResumed {
                    val dialogFragment = SelectSingleMethodDialogFragment.newInstance(
                        R.string.menu_remap,
                        R.string.remap_empty_list,
                        accountTypes.toTypedArray(),
                        type
                    )
                    dialogFragment.setTargetFragment(this@TransactionList, MAP_METHOD_REQUEST)
                    dialogFragment.show(requireActivity().supportFragmentManager, "REMAP_METHOD")
                }
            }
            return true
        } else if (command == R.id.REMAP_ACCOUNT_COMMAND) {
            checkSealed(itemIds) {
                val excludedIds: MutableList<Long> = ArrayList()
                val splitIds: MutableList<Long> = ArrayList()
                if (!mAccount.isAggregate) {
                    excludedIds.add(mAccount.id)
                }
                for (i in 0 until positions.size()) {
                    if (positions.valueAt(i)) {
                        mTransactionsCursor.moveToPosition(positions.keyAt(i))
                        val transferAccount = DbUtils.getLongOr0L(
                            mTransactionsCursor,
                            DatabaseConstants.KEY_TRANSFER_ACCOUNT
                        )
                        if (transferAccount != 0L) {
                            excludedIds.add(transferAccount)
                        }
                        if (DatabaseConstants.SPLIT_CATID == DbUtils.getLongOrNull(
                                mTransactionsCursor,
                                DatabaseConstants.KEY_CATID
                            )
                        ) {
                            splitIds.add(
                                DbUtils.getLongOr0L(
                                    mTransactionsCursor,
                                    DatabaseConstants.KEY_ROWID
                                )
                            )
                        }
                    }
                }
                CheckTransferAccountOfSplitPartsHandler(requireActivity().contentResolver).check(
                    splitIds
                ) { result ->
                    lifecycleScope.launchWhenResumed {
                        result.onSuccess {
                            excludedIds.addAll(it)
                            val dialogFragment =
                                SelectSingleAccountDialogFragment.newInstance(
                                    R.string.menu_remap,
                                    R.string.remap_empty_list,
                                    excludedIds
                                )
                            dialogFragment.setTargetFragment(
                                this@TransactionList,
                                MAP_ACCOUNT_REQUEST
                            )
                            dialogFragment.show(
                                requireActivity().supportFragmentManager,
                                "REMAP_ACCOUNT"
                            )
                        }.onFailure(showFailure)
                    }
                }
            }
            return true
        } else if (command == R.id.LINK_TRANSFER_COMMAND) {
            checkSealed(itemIds) {
                val b = Bundle()
                b.putString(
                    ConfirmationDialogFragment.KEY_MESSAGE,
                    getString(R.string.warning_link_transfer) + " " + getString(R.string.continue_confirmation)
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                    R.id.LINK_TRANSFER_COMMAND
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
                    R.id.CANCEL_CALLBACK_COMMAND
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                    R.string.menu_create_transfer
                )
                b.putLongArray(KEY_ROW_IDS, itemIds)
                showConfirmationDialog(b, "LINK_TRANSFER")
            }
            return true
        }
        return false
    }

    override fun onFinishActionMode() {
        super.onFinishActionMode()
        selectedTransactionSum = 0
    }

    override fun setTitle(mode: ActionMode, lv: AbsListView) {
        val count = lv.checkedItemCount
        if (count > 1) {
            mAccount?.let {
                selectedTransactionSumFormatted =
                    currencyFormatter.convAmount(selectedTransactionSum, it.currencyUnit)
            }
            mode.title = TextUtils.concat(
                count.toString(), " ", setColor(selectedTransactionSumFormatted)
            )
        } else {
            super.setTitle(mode, lv)
        }
    }

    private fun setColor(text: String?) =
        text?.withAmountColor(resources, selectedTransactionSum.sign) ?: ""

    override fun onSelectionChanged(position: Int, checked: Boolean) {
        if (mTransactionsCursor.moveToPosition(position)) {
            val amount =
                mTransactionsCursor.getLong(mTransactionsCursor.getColumnIndexOrThrow(KEY_AMOUNT))
            val shouldCount = if (isTransferAtPosition(position) && mAccount.isAggregate) {
                if (mAccount.isHomeAggregate) false else mTransactionsCursor.getInt(
                    mTransactionsCursor.getColumnIndexOrThrow(KEY_IS_SAME_CURRENCY)
                ) != 1
            } else true
            if (shouldCount) {
                if (checked) {
                    selectedTransactionSum += amount
                } else {
                    selectedTransactionSum -= amount
                }
            }
        }
    }


    override fun configureMenu(menu: Menu, lv: AbsListView) {
        super.configureMenu(menu, lv)
        val checkedItemPositions = lv.checkedItemPositions
        var hasSplit = false
        var hasVoid = false
        var hasNotSplit = false
        var hasTransfer = false
        var canLinkAsTransfer = false
        for (i in 0 until checkedItemPositions.size()) {
            if (checkedItemPositions.valueAt(i)) {
                if (isSplitAtPosition(checkedItemPositions.keyAt(i))) {
                    hasSplit = true
                } else {
                    hasNotSplit = true
                }
                if (hasSplit && hasNotSplit) {
                    break
                }
            }
        }
        for (i in 0 until checkedItemPositions.size()) {
            if (checkedItemPositions.valueAt(i)) {
                if (isVoidAtPosition(checkedItemPositions.keyAt(i))) {
                    hasVoid = true
                    break
                }
            }
        }
        for (i in 0 until checkedItemPositions.size()) {
            if (checkedItemPositions.valueAt(i)) {
                if (isTransferAtPosition(checkedItemPositions.keyAt(i))) {
                    hasTransfer = true
                    break
                }
            }
        }
        if (lv.checkedItemCount == 2 && !hasSplit && !hasTransfer) {
            val checked = checkedItemPositions.asTrueSequence().toList()
            canLinkAsTransfer = checked.size == 2 && canLinkPositions(checked[0], checked[1])
        }

        with(menu) {
            findItem(R.id.CREATE_TEMPLATE_COMMAND).isVisible = lv.checkedItemCount == 1
            findItem(R.id.SPLIT_TRANSACTION_COMMAND).isVisible = !hasSplit && !hasVoid
            findItem(R.id.UNGROUP_SPLIT_COMMAND).isVisible = !hasNotSplit && !hasVoid
            findItem(R.id.UNDELETE_COMMAND).isVisible = hasVoid
            findItem(R.id.EDIT_COMMAND).isVisible = lv.checkedItemCount == 1 && !hasVoid
            findItem(R.id.REMAP_ACCOUNT_COMMAND).isVisible =
                ((activity as? MyExpenses)?.accountCount ?: 0) > 1
            findItem(R.id.REMAP_PAYEE_COMMAND).isVisible = !hasTransfer
            findItem(R.id.REMAP_CATEGORY_COMMAND).isVisible = !hasTransfer && !hasSplit
            findItem(R.id.REMAP_METHOD_COMMAND).isVisible = !hasTransfer
            findItem(R.id.LINK_TRANSFER_COMMAND).isVisible = canLinkAsTransfer
        }
    }

    private fun canLinkPositions(position1: Int, position2: Int): Boolean {
        if (mTransactionsCursor != null && columnIndexAccountId > -1) {
            if (mTransactionsCursor.moveToPosition(position1)) {
                val accountId1 = mTransactionsCursor.getLong(columnIndexAccountId)
                val amount1 = mTransactionsCursor.getLong(columnIndexAmount)
                val currency1 = currencyAtPosition
                if (mTransactionsCursor.moveToPosition(position2)) {
                    //we either have two transactions with different currencies or with the same amount
                    return accountId1 != mTransactionsCursor.getLong(columnIndexAccountId) &&
                            (amount1 == -mTransactionsCursor.getLong(columnIndexAmount) || currency1 != currencyAtPosition)
                }
            }
        }
        return false
    }

    private val currencyAtPosition: String?
        get() = columnIndexCurrency.takeIf { it > -1 }?.let { mTransactionsCursor.getString(it) }

    override fun showDetails(transactionId: Long) {
        lifecycleScope.launchWhenResumed {
            with(parentFragmentManager) {
                if (findFragmentByTag(TransactionDetailFragment::class.java.name) == null) {
                    TransactionDetailFragment.newInstance(transactionId)
                        .show(this, TransactionDetailFragment::class.java.name)
                }
            }
        }
    }

    override fun showConfirmationDialog(bundle: Bundle?, tag: String?) {
        lifecycleScope.launchWhenResumed {
            ConfirmationDialogFragment.newInstance(bundle).show(parentFragmentManager, tag)
        }
    }

    /**
     * reimplement DbConstants.budgetColumn outside of Database
     */
    override fun resolveBudget(headerId: Int) =
        budgetAmounts?.takeIf { it.isNotEmpty() }?.let { budgetAmounts ->
            budgetAmounts.find { it.first == headerId } ?:
            budgetAmounts.lastOrNull { !it.third && it.first < headerId }
        }?.second
}

class ConfirmTagDialogFragment : DialogFragment() {
    val tagList
        get() = requireArguments().getParcelableArrayList<Tag>(KEY_TAG_LIST)!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val isEmpty = tagList.size == 0
        val dialog = MaterialDialog(requireContext())
            .title(R.string.menu_tag)
            .message(
                text = if (isEmpty) getString(R.string.dialog_multi_tag_clear) else getString(
                    R.string.dialog_multi_tag,
                    tagList.joinToString(", ") { tag -> tag.label })
            )
            .negativeButton(android.R.string.cancel)
        return if (isEmpty) {
            dialog.positiveButton(R.string.menu_remove) { confirm(true) }
        } else {
            dialog.listItemsSingleChoice(R.array.multi_tag_options) { _, index, _ -> confirm(index == 1) }
                .positiveButton(R.string.menu_tag)
        }
    }

    private fun confirm(replace: Boolean) {
        targetFragment?.onActivityResult(
            CONFIRM_MAP_TAG_REQUEST,
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(KEY_REPLACE, replace)
                putParcelableArrayListExtra(KEY_TAG_LIST, tagList)
            })
    }
}