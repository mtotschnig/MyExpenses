package org.totschnig.myexpenses.delegate

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.evernote.android.state.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.HELP_VARIANT_SPLIT
import org.totschnig.myexpenses.activity.HELP_VARIANT_TEMPLATE_SPLIT
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.SplitPart

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

    override val helpVariant: String
        get() = if (isTemplate) HELP_VARIANT_TEMPLATE_SPLIT else HELP_VARIANT_SPLIT
    override val typeResId = R.string.split_transaction
    override val editResId = R.string.menu_edit_split
    override val shouldAutoFill = false
    private var missingRecurrenceFeature: ContribFeature? = null
    lateinit var adapter: SplitPartRVAdapter
    private var transactionSum: Long = 0

    @State
    var userSetAmount: Boolean = false

    override fun bindUnsafe(
        transaction: ITransaction?,
        withTypeSpinner: Boolean,
        savedInstanceState: Bundle?,
        recurrence: Plan.Recurrence?,
        withAutoFill: Boolean,
        isCached: Boolean
    ) {
        super.bindUnsafe(
            transaction,
            withTypeSpinner,
            savedInstanceState,
            recurrence,
            withAutoFill,
            isCached
        )
        if (transaction?.amount?.amountMinor != 0L) {
            userSetAmount = true
        }
    }

    override fun bind(
        transaction: ISplit?,
        withTypeSpinner: Boolean,
        savedInstanceState: Bundle?,
        recurrence: Plan.Recurrence?,
        withAutoFill: Boolean
    ) {
        super.bind(
            transaction,
            withTypeSpinner,
            savedInstanceState,
            recurrence,
            withAutoFill
        )

        viewBinding.CategoryRow.isVisible = false
        viewBinding.SplitRow.isVisible = true
        host.registerForContextMenu(viewBinding.list)
        missingRecurrenceFeature = if (!withTypeSpinner || prefHandler.getBoolean(
                PrefKey.NEW_SPLIT_TEMPLATE_ENABLED,
                true
            )
        ) super.missingRecurrenceFeature() else ContribFeature.SPLIT_TEMPLATE
        with(viewBinding.CREATEPARTCOMMAND) {
            contentDescription = concatResStrings(
                context, ". ",
                R.string.menu_create_split_part_category, R.string.menu_create_split_part_transfer
            )
            setOnClickListener {
                host.createRow(unsplitAmount?.amountMajor)
            }
        }
        viewBinding.unsplitLine.setOnClickListener {
            unsplitAmountFormatted?.let {
                host.copyToClipboard(it)
            }
        }
    }

    override fun onAmountChanged() {
        super.onAmountChanged()
        if (!userSetAmount && !isProcessingLinkedAmountInputs) {
            userSetAmount = true
        }
        if (!isProcessingLinkedAmountInputs) {
            updateBalance()
        }
    }

    override fun buildMainTransaction(account: Account): ISplit =
        if (isTemplate) buildTemplate(account) else SplitTransaction(account.id)

    override fun prepareForNew(): Boolean {
        super.prepareForNew()
        val account = currentAccount()!!
        rowId = SplitTransaction.getNewInstance(
            context.contentResolver,
            account.id,
            account.currency,
            true
        ).id
        host.viewModel.loadSplitParts(rowId, isTemplate)
        return true
    }

    override fun configureType() {
        super.configureType()
        updateBalance()
    }

    private fun updateBalance() {
        if (userSetAmount) {
            val unsplitVisibility =
                if (unsplitAmount?.amountMinor?.equals(0L) == false) View.VISIBLE else View.GONE
            unsplitAmountFormatted?.let {
                viewBinding.end.text = it
            }
            viewBinding.unsplitSeparator.visibility = unsplitVisibility
            viewBinding.unsplitLine.visibility = unsplitVisibility
            viewBinding.BottomLine.visibility = unsplitVisibility
        } else if (transactionSum != 0L) {
            val existingValue = viewBinding.Amount.typedValue
            val newValue = Money(adapter.currencyUnit, transactionSum).amountMajor
            if (existingValue.compareTo(newValue) != 0) {
                isProcessingLinkedAmountInputs = true
                viewBinding.Amount.setAmount(newValue)
                isProcessingLinkedAmountInputs = false
            }
        }
    }

    val unsplitAmountFormatted: String?
        get() = unsplitAmount?.let { currencyFormatter.formatMoney(it) }

    private val unsplitAmount: Money?
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

    override fun updateAccount(account: Account, isInitialSetup: Boolean) {
        requireAdapter()
        if (adapter.itemCount > 0) { //call background task for moving parts to new account
            host.startMoveSplitParts(rowId, account.id)
        } else {
            updateAccountDo(account, isInitialSetup)
        }
    }

    private fun updateAccountDo(account: Account, isInitialSetup: Boolean) {
        super.updateAccount(account, isInitialSetup)
        adapter.currencyUnit = account.currency
        //noinspection NotifyDataSetChanged
        adapter.notifyDataSetChanged()
        updateBalance()
    }

    fun onUncommittedSplitPartsMoved(success: Boolean) {
        val account = getAccountFromSpinner(accountSpinner)!!
        if (success) {
            super.updateAccount(account, false)
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

    fun showSplits(transactions: List<SplitPart>) {
        adapter.submitList(transactions)
        viewBinding.empty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        viewBinding.list.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
        transactionSum = transactions.sumOf { it.amountRaw }
        updateBalance()
    }
}