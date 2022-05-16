package org.totschnig.myexpenses.delegate

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.view.View
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.ui.MyTextWatcher
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.SplitPartListViewModel
import org.totschnig.myexpenses.viewmodel.data.Account

class SplitDelegate(
    viewBinding: OneExpenseBinding,
    dateEditBinding: DateEditBinding,
    methodRowBinding: MethodRowBinding,
    isTemplate: Boolean
) :
    MainDelegate<ISplit>(
        viewBinding,
        dateEditBinding,
        methodRowBinding,
        isTemplate
    ) {

    override val operationType = TransactionsContract.Transactions.TYPE_SPLIT

    override val helpVariant: ExpenseEdit.HelpVariant
        get() = if (isTemplate) ExpenseEdit.HelpVariant.templateSplit else ExpenseEdit.HelpVariant.split
    override val typeResId = R.string.split_transaction
    override val editResId = R.string.menu_edit_split
    override val shouldAutoFill = false
    private var missingRecurrenceFeature: ContribFeature? = null
    lateinit var adapter: SplitPartRVAdapter
    private var transactionSum: Long = 0

    override fun bind(
        transaction: ISplit?,
        newInstance: Boolean,
        savedInstanceState: Bundle?,
        recurrence: Plan.Recurrence?,
        withAutoFill: Boolean
    ) {
        super.bind(
            transaction,
            newInstance,
            savedInstanceState,
            recurrence,
            withAutoFill
        )
        viewBinding.Amount.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                updateBalance()
            }
        })
        viewBinding.CategoryRow.visibility = View.GONE
        viewBinding.SplitRow.visibility = View.VISIBLE
        missingRecurrenceFeature = if (!newInstance || prefHandler.getBoolean(
                PrefKey.NEW_SPLIT_TEMPLATE_ENABLED,
                true
            )
        ) super.missingRecurrenceFeature() else ContribFeature.SPLIT_TEMPLATE
    }

    override fun buildMainTransaction(accountId: Long): ISplit =
        if (isTemplate) buildTemplate(accountId) else SplitTransaction(accountId)

    override fun prepareForNew() {
        super.prepareForNew()
        rowId = SplitTransaction.getNewInstance(accountId!!).id
        host.viewModel.loadSplitParts(rowId, isTemplate)
    }

    override fun configureType() {
        super.configureType()
        updateBalance()
    }

    private fun updateBalance() {
        unsplitAmountFormatted?.let {
            viewBinding.end.text = it
        }
    }

    val unsplitAmountFormatted: String?
        get() = unsplitAmount?.let { currencyFormatter.formatMoney(it) }

    val unsplitAmount: Money?
        get() = host.amount?.let {
            Money(it.currencyUnit, it.amountMinor - transactionSum)
        }

    val splitComplete: Boolean
        get() = host.amount?.amountMinor?.minus(transactionSum) == 0L

    private fun requireAdapter() {
        if (!::adapter.isInitialized) {
            adapter = SplitPartRVAdapter(
                context,
                currentAccount()!!.currency,
                currencyFormatter
            ) { view, _ -> host.openContextMenu(view) }
            viewBinding.list.adapter = adapter
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun updateAccount(account: Account) {
        super.updateAccount(account)
        requireAdapter()
        adapter.currencyUnit = account.currency
        adapter.notifyDataSetChanged()
        updateBalance()
        if (adapter.itemCount > 0) { //call background task for moving parts to new account
            host.startMoveSplitParts(rowId, account.id)
        }
    }

    fun onUncommitedSplitPartsMoved(success: Boolean) {
        val account = mAccounts[accountSpinner.selectedItemPosition]
        if (success) {
            super.updateAccount(account)
        } else {
            for ((index, a) in mAccounts.withIndex()) {
                if (a.id == accountId) {
                    accountSpinner.setSelection(index)
                    break
                }
            }
            host.showSnackBar(
                host.getString(
                    R.string.warning_cannot_move_split_transaction,
                    account.label
                )
            )
        }
    }

    override fun missingRecurrenceFeature() = missingRecurrenceFeature

    fun showSplits(transactions: List<SplitPartListViewModel.Transaction>) {
        adapter.submitList(transactions)
        viewBinding.empty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        viewBinding.list.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
        transactionSum = transactions.sumOf { it.amountRaw }
        updateBalance()
    }
}