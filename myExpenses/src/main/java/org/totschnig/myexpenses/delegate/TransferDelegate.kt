package org.totschnig.myexpenses.delegate

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.view.isVisible
import com.evernote.android.state.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.HELP_VARIANT_SPLIT_PART_TRANSFER
import org.totschnig.myexpenses.activity.HELP_VARIANT_TEMPLATE_TRANSFER
import org.totschnig.myexpenses.activity.HELP_VARIANT_TRANSFER
import org.totschnig.myexpenses.adapter.AccountAdapter
import org.totschnig.myexpenses.adapter.GroupedSpinnerAdapter
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.dialog.addAllAccounts
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.ITransfer
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model.isNullOr0
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.ui.MyTextWatcher
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.viewmodel.data.Account
import java.math.BigDecimal

class TransferDelegate(
    viewBinding: OneExpenseBinding,
    dateEditBinding: DateEditBinding,
    methodRowBinding: MethodRowBinding,
    isTemplate: Boolean
) :
    TransactionDelegate<ITransfer>(
        viewBinding,
        dateEditBinding,
        methodRowBinding,
        isTemplate
    ) {

    private var transferAccountSpinner = SpinnerHelper(viewBinding.TransferAccount)

    override val operationType = TransactionsContract.Transactions.TYPE_TRANSFER

    private val lastExchangeRateRelevantInputs = intArrayOf(INPUT_EXCHANGE_RATE, INPUT_AMOUNT)
    private lateinit var transferAccountsAdapter:  GroupedSpinnerAdapter<AccountFlag, Account>

    @State
    var transferAccountId: Long? = null

    @State
    var transferPeer: Long? = null

    @State
    var categoryVisible = false

    @State
    var passedInTransferAmount: Long? = null

    @State
    var passedInTransferAccountId: Long? = null

    override val helpVariant: String
        get() = when {
            isTemplate -> HELP_VARIANT_TEMPLATE_TRANSFER
            isSplitPart -> HELP_VARIANT_SPLIT_PART_TRANSFER
            else -> HELP_VARIANT_TRANSFER
        }

    override val typeResId = R.string.transfer
    override val editResId = R.string.menu_edit_transfer
    override val editPartResId = R.string.menu_edit_split_part_transfer


    override fun bind(
        transaction: ITransfer?,
        withTypeSpinner: Boolean,
        savedInstanceState: Bundle?,
        recurrence: Plan.Recurrence?,
        withAutoFill: Boolean
    ) {
        if (transaction != null) {
            transferAccountId = transaction.transferAccountId
            transferPeer = transaction.transferPeer
            passedInTransferAmount = transaction.transferAmount?.amountMinor
            passedInTransferAccountId = transaction.transferAccountId
            transaction.transferAmount?.let {
                viewBinding.TransferAmount.setFractionDigits(it.currencyUnit.fractionDigits)
            }
        }
        viewBinding.Amount.addTextChangedListener(LinkedTransferAmountTextWatcher(true))
        viewBinding.TransferAmount.addTextChangedListener(LinkedTransferAmountTextWatcher(false))
        viewBinding.ERR.ExchangeRate.setExchangeRateWatcher(LinkedExchangeRateTextWatcher())
        viewBinding.Amount.hideTypeButton()
        viewBinding.TransferAccountRow.isVisible = true
        viewBinding.AccountLabel.setText(R.string.transfer_from_account)
        super.bind(
            transaction,
            withTypeSpinner,
            savedInstanceState,
            recurrence,
            withAutoFill
        )
        if (catId != null && catId != prefHandler.defaultTransferCategory) categoryVisible = true
        hideRowsSpecificToMain()
        configureTransferDirection()
        configureCategoryVisibility()
    }

    override fun populateFields(transaction: ITransfer, withAutoFill: Boolean) {
        super.populateFields(transaction, withAutoFill)
        transaction.transferAmount?.let {
            viewBinding.TransferAmount.setAmount(it.amountMajor.abs())
            if (!isTemplate) {
                isProcessingLinkedAmountInputs = true
                updateExchangeRates()
                isProcessingLinkedAmountInputs = false
            }
        }

    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        super.onItemSelected(parent, view, position, id)
        when (parent.id) {
            R.id.TransferAccount -> {
                transferAccountId = transferAccountSpinner.selectedItemId
                configureTransferInput()
            }
        }
    }

    private fun setTransferAccountFilterMap() {
        val fromAccount = getAccountFromSpinner(accountSpinner)!!
        requireTransferAccountsAdapter()
        transferAccountsAdapter.addAllAccounts(mAccounts.filter { it.id != fromAccount.id })
    }

    fun transferAccount() = getAccountFromSpinner(transferAccountSpinner)

    private fun configureTransferInput() {
        val transferAccount = transferAccount()
        val currentAccount = currentAccount()
        if (transferAccount == null || currentAccount == null) {
            return
        }
        val currency = currentAccount.currency
        val transferAccountCurrencyUnit = transferAccount.currency
        val isSame = currency == transferAccountCurrencyUnit
        viewBinding.TransferAmountRow.isVisible = !isSame
        (viewBinding.ERR.root as ViewGroup).isVisible = !isSame && !isTemplate
        addCurrencyToInput(
            viewBinding.TransferAmountLabel,
            viewBinding.TransferAmount,
            transferAccountCurrencyUnit,
            R.string.amount
        )
        viewBinding.TransferAmount.setFractionDigits(transferAccountCurrencyUnit.fractionDigits)
        viewBinding.ERR.ExchangeRate.setCurrencies(currency, transferAccountCurrencyUnit)
    }

    private fun requireTransferAccountsAdapter() {
        if (!::transferAccountsAdapter.isInitialized) {
            transferAccountsAdapter = AccountAdapter(context)
            transferAccountSpinner.adapter = transferAccountsAdapter
            transferAccountSpinner.setOnItemSelectedListener(this)
        }
    }

    fun configureTransferDirection() {
        if (isIncome) {
            switchAccountViews()
        }
    }

    private fun switchAccountViews() {
        val accountSpinner = accountSpinner.spinner
        val transferAccountSpinner = transferAccountSpinner.spinner
        with(viewBinding.Table) {
            removeView(viewBinding.AmountRow)
            removeView(viewBinding.TransferAmountRow)
            if (isIncome) {
                if (accountSpinner.parent === viewBinding.AccountRow && transferAccountSpinner.parent === viewBinding.TransferAccountRow) {
                    viewBinding.AccountRow.removeView(accountSpinner)
                    viewBinding.TransferAccountRow.removeView(transferAccountSpinner)
                    viewBinding.AccountRow.addView(transferAccountSpinner)
                    viewBinding.TransferAccountRow.addView(accountSpinner)
                }
                addView(viewBinding.TransferAmountRow, 2)
                addView(viewBinding.AmountRow, 4)
            } else {
                if (accountSpinner.parent === viewBinding.TransferAccountRow && transferAccountSpinner.parent === viewBinding.AccountRow) {
                    viewBinding.AccountRow.removeView(transferAccountSpinner)
                    viewBinding.TransferAccountRow.removeView(accountSpinner)
                    viewBinding.AccountRow.addView(accountSpinner)
                    viewBinding.TransferAccountRow.addView(transferAccountSpinner)
                }
                addView(viewBinding.AmountRow, 2)
                addView(viewBinding.TransferAmountRow, 4)
            }
        }
    }

    private inner class LinkedTransferAmountTextWatcher(
        /**
         * true if we are linked to from amount
         */
        var isMain: Boolean
    ) : MyTextWatcher() {

        override fun afterTextChanged(s: Editable) {
            if (isProcessingLinkedAmountInputs) return
            isProcessingLinkedAmountInputs = true
            if (isTemplate) {
                (if (isMain) viewBinding.TransferAmount else viewBinding.Amount).clear()
            } else if (viewBinding.ERR.root.isVisible) {
                val currentFocus = if (isMain) INPUT_AMOUNT else INPUT_TRANSFER_AMOUNT
                if (lastExchangeRateRelevantInputs[0] != currentFocus) {
                    lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0]
                    lastExchangeRateRelevantInputs[0] = currentFocus
                }
                if (lastExchangeRateRelevantInputs[1] == INPUT_EXCHANGE_RATE) {
                    applyExchangeRate(
                        if (isMain) viewBinding.Amount else viewBinding.TransferAmount,
                        if (isMain) viewBinding.TransferAmount else viewBinding.Amount,
                        viewBinding.ERR.ExchangeRate.getRate(!isMain)
                    )
                } else {
                    updateExchangeRates()
                }
            }
            isProcessingLinkedAmountInputs = false
        }
    }

    private fun applyExchangeRate(from: AmountInput, to: AmountInput, rate: BigDecimal?) {
        val input = from.getAmount(showToUser = false)
        to.setAmount(
            if (rate != null && input != null) input.multiply(rate) else BigDecimal(0),
            false
        )
    }

    private fun updateExchangeRates() {
        val amount = viewBinding.Amount.getAmount(showToUser = false)
        val transferAmount =
            viewBinding.TransferAmount.getAmount(showToUser = false)
        viewBinding.ERR.ExchangeRate.calculateAndSetRate(amount, transferAmount)
    }

    override fun buildTransaction(
        forSave: Boolean,
        account: Account
    ): ITransfer? {
        val currentAccount = currentAccount()!!
        val transferAccount = transferAccount()!!
        val amount = validateAmountInput(forSave, currentAccount.currency).getOrNull()
        val isSame = currentAccount.currency == transferAccount.currency
        val transferAmount = if (isSame && amount != null) {
            amount.negate()
        } else {
            viewBinding.TransferAmount.getAmount(
                transferAccount.currency,
                showToUser = forSave
            ).getOrNull()?.let {
                if (isIncome) it.negate() else it
            }
        }
        return if (isTemplate) {
            if (amount == null && transferAmount == null) {
                null
            } else buildTemplate(account).apply {
                if (!amount.isNullOr0() || transferAmount.isNullOr0()) {
                    this.amount = amount ?: Money(currentAccount.currency, 0)
                    setTransferAccountId(transferAccount.id)
                } else if (!isSame && transferAmount != null) {
                    this.accountId = transferAccount.id
                    setTransferAccountId(currentAccount.id)
                    this.amount = transferAmount
                    viewBinding.Amount.setError(null)
                }
            }
        } else {
            if (amount == null || transferAmount == null) {
                null
            } else Transfer(account.id, transferAccount.id, parentId).apply {
                transferPeer = this@TransferDelegate.transferPeer
                setAmountAndTransferAmount(amount, transferAmount)
            }
        }
    }

    override fun updateAccount(account: Account, isInitialSetup: Boolean) {
        super.updateAccount(account, isInitialSetup)
        setTransferAccountFilterMap()
        if (accountId == transferAccountId) {
            transferAccountId = null
        }
        transferAccountId?.also {
            transferAccountSpinner.setSelection(transferAccountsAdapter.getPosition(it))
        } ?: run {
            transferAccountsAdapter.getFirstSelectable()?.let {
                transferAccountSpinner.setSelection(it.index)
                transferAccountId = it.value.itemId
            }
        }
        configureTransferInput()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        transferAccountSpinner.selectedItemId.takeIf {
            it != AdapterView.INVALID_ROW_ID
        }?.also {
            transferAccountId = it
        }
        super.onSaveInstanceState(outState)
    }

    fun invert() {
        viewBinding.Amount.toggle()
        switchAccountViews()
    }

    private inner class LinkedExchangeRateTextWatcher : ExchangeRateEdit.ExchangeRateWatcher {
        override fun afterExchangeRateChanged(rate: BigDecimal, inverse: BigDecimal) {
            if (isProcessingLinkedAmountInputs) return
            isProcessingLinkedAmountInputs = true
            val constant: AmountInput?
            val variable: AmountInput?
            val exchangeFactor: BigDecimal
            if (lastExchangeRateRelevantInputs[0] != INPUT_EXCHANGE_RATE) {
                lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0]
                lastExchangeRateRelevantInputs[0] = INPUT_EXCHANGE_RATE
            }
            if (lastExchangeRateRelevantInputs[1] == INPUT_AMOUNT) {
                constant = viewBinding.Amount
                variable = viewBinding.TransferAmount
                exchangeFactor = rate
            } else {
                constant = viewBinding.TransferAmount
                variable = viewBinding.Amount
                exchangeFactor = inverse
            }
            applyExchangeRate(constant, variable, exchangeFactor)
            isProcessingLinkedAmountInputs = false
        }
    }

    fun toggleCategory() {
        categoryVisible = !categoryVisible
        configureCategoryVisibility()
    }

    private fun configureCategoryVisibility() {
        viewBinding.CategoryRow.isVisible = categoryVisible
    }

    companion object {
        private const val INPUT_EXCHANGE_RATE = 1
        private const val INPUT_AMOUNT = 2
        private const val INPUT_TRANSFER_AMOUNT = 3
    }

}