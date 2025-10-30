package org.totschnig.myexpenses.delegate

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.TooltipCompat
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
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.TransactionEditData

class SplitDelegate(
    viewBinding: OneExpenseBinding,
    dateEditBinding: DateEditBinding,
    methodRowBinding: MethodRowBinding,
    isTemplate: Boolean
) :
    MainDelegate(
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

    @State
    var splitParts: ArrayList<TransactionEditData> = ArrayList()

    override fun bind(
        transaction: TransactionEditData?,
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

        if ((transaction?.amount?.amountMinor ?: 0L) != 0L) {
            userSetAmount = true
        }

        viewBinding.CategoryRow.isVisible = false
        viewBinding.SplitRow.isVisible = true
        host.registerForContextMenu(viewBinding.list)
        missingRecurrenceFeature = if (!withTypeSpinner || prefHandler.getBoolean(
                PrefKey.NEW_SPLIT_TEMPLATE_ENABLED,
                true
            )
        ) super.missingRecurrenceFeature() else ContribFeature.SPLIT_TEMPLATE
        with(viewBinding.CREATEPARTCOMMAND) {
            val helpText = concatResStrings(
                context, ". ",
                R.string.menu_create_split_part_category, R.string.menu_create_split_part_transfer
            )
            TooltipCompat.setTooltipText(this, helpText)
            contentDescription = helpText
            setOnClickListener {
                host.createRow(unsplitAmount?.amountMajor)
            }
        }
        viewBinding.unsplitLine.setOnClickListener {
            unsplitAmountFormatted?.let {
                host.copyToClipboard(it)
            }
        }

        transaction?.splitParts?.let { parts ->
            splitParts.addAll(parts)
        }
    }

    override fun prepareForNew(): Boolean {
        super.prepareForNew()
        splitParts.clear()
        showSplits(splitParts)
        return true
    }

    fun addSplitParts(parts: ArrayList<TransactionEditData>) {
        parts.forEach { part ->
            val existingIndex = splitParts.indexOfFirst { it.uuid == part.uuid }
            if (existingIndex != -1) {
                splitParts[existingIndex] = part
            } else {
                splitParts.add(part)
            }
        }

        showSplits(splitParts)
    }

    fun removeSplitPart(position: Int) {
        splitParts.removeAt(position)
        showSplits(splitParts)
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

    override fun buildMainTransaction(account: Account): TransactionEditData =
        super.buildMainTransaction(account).copy(
            categoryId = DatabaseConstants.SPLIT_CATID,
            splitParts = splitParts
        )

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
        showSplits(splitParts)
    }

    override fun updateAccount(account: Account, isInitialSetup: Boolean) {
        requireAdapter()
        if (adapter.itemCount > 0 && splitParts.any { it.transferEditData?.transferAccountId == account.id }) {
            accountSpinner.setSelection(accountAdapter.getPosition(accountId!!))
            host.showSnackBar(
                host.getString(
                    R.string.warning_cannot_move_split_transaction,
                    account.label
                )
            )
        } else {
            super.updateAccount(account, isInitialSetup)
            splitParts = splitParts.mapTo(ArrayList(splitParts.size)) {
                it.copy(accountId = account.id)
            }
            adapter.currencyUnit = account.currency
            //noinspection NotifyDataSetChanged
            adapter.notifyDataSetChanged()
            updateBalance()
        }
    }

    override fun missingRecurrenceFeature() = missingRecurrenceFeature

    fun showSplits(transactions: MutableList<TransactionEditData>) {
        adapter.submitList(transactions.map { part ->
            SplitPartRVAdapter.SplitPart(
                uuid = part.uuid,
                amount = part.amount,
                comment = part.comment,
                categoryPath = part.categoryPath,
                transferAccount = part.transferEditData?.transferAccountId?.let { transferAccountId ->
                    mAccounts.find { it.id == transferAccountId }
                }?.label,
                debtLabel = debts.find { it.id == part.debtId }?.label,
                tags = part.tags,
                icon = part.categoryIcon,
            )
        })
        viewBinding.empty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        viewBinding.list.visibility = if (transactions.isEmpty()) View.GONE else View.VISIBLE
        transactionSum = transactions.sumOf { it.amount.amountMinor }
        updateBalance()
    }
}