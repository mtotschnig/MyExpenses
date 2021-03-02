package org.totschnig.myexpenses.delegate

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import icepick.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.AccountAdapter
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.ITransfer
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.ui.MyTextWatcher
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.viewmodel.data.Account
import java.math.BigDecimal

class TransferDelegate(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, methodRowBinding: MethodRowBinding, prefHandler: PrefHandler, isTemplate: Boolean) :
        TransactionDelegate<ITransfer>(viewBinding, dateEditBinding, methodRowBinding, prefHandler, isTemplate) {

    private var transferAccountSpinner = SpinnerHelper(viewBinding.TransferAccount)

    init {
        createTransferAccountAdapter()
    }

    override val operationType = TransactionsContract.Transactions.TYPE_TRANSFER

    private val lastExchangeRateRelevantInputs = intArrayOf(INPUT_EXCHANGE_RATE, INPUT_AMOUNT)
    private lateinit var transferAccountsAdapter: AccountAdapter

    @JvmField
    @State
    var mTransferAccountId: Long? = null

    @JvmField
    @State
    var transferPeer: Long? = null

    override val helpVariant: ExpenseEdit.HelpVariant
        get() = when {
            isTemplate -> ExpenseEdit.HelpVariant.templateTransfer
            isSplitPart -> ExpenseEdit.HelpVariant.splitPartTransfer
            else -> ExpenseEdit.HelpVariant.transfer
        }

    override val typeResId = R.string.transfer
    override val editResId = R.string.menu_edit_transfer
    override val editPartResId = R.string.menu_edit_split_part_transfer


    override fun bind(transaction: ITransfer?, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, savedInstanceState: Bundle?, recurrence: Plan.Recurrence?, withAutoFill: Boolean) {
        if (transaction != null) {
            mTransferAccountId = transaction.transferAccountId
            transferPeer = transaction.transferPeer
        }
        viewBinding.Amount.addTextChangedListener(LinkedTransferAmountTextWatcher(true))
        viewBinding.TransferAmount.addTextChangedListener(LinkedTransferAmountTextWatcher(false))
        viewBinding.ERR.ExchangeRate.setExchangeRateWatcher(LinkedExchangeRateTextWatcher())
        viewBinding.Amount.hideTypeButton()
        viewBinding.CategoryRow.visibility = View.GONE
        viewBinding.TransferAccountRow.visibility = View.VISIBLE
        viewBinding.AccountLabel.setText(R.string.transfer_from_account)
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance, savedInstanceState, recurrence, withAutoFill)
        hideRowsSpecificToMain()
        if (rowId != 0L) {
            configureTransferDirection()
        }
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

    override fun createAdapters(newInstance: Boolean, withAutoFill: Boolean) {
        createStatusAdapter()
        if (newInstance) {
            createOperationTypeAdapter()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        super.onItemSelected(parent, view, position, id)
        when (parent.id) {
            R.id.TransferAccount -> {
                mTransferAccountId = transferAccountSpinner.selectedItemId
                configureTransferInput()
            }
        }
    }

    override fun setAccount(currencyExtra: String?) {
        super.setAccount(currencyExtra)
        val selectedPosition = setTransferAccountFilterMap()
        transferAccountSpinner.setSelection(selectedPosition)
        mTransferAccountId = transferAccountSpinner.selectedItemId
        configureTransferInput()
    }

    private fun setTransferAccountFilterMap(): Int {
        val fromAccount = mAccounts[accountSpinner.selectedItemPosition]
        val list = mutableListOf<Account>()
        var position = 0
        var selectedPosition = 0
        for (i in mAccounts.indices) {
            val account = mAccounts[i]
            if (fromAccount.id != account.id) {
                list.add(account)
                if (mTransferAccountId != null && mTransferAccountId == account.id) {
                    selectedPosition = position
                }
                position++
            }
        }
        transferAccountsAdapter.clear()
        transferAccountsAdapter.addAll(list)
        return selectedPosition
    }

    private fun transferAccount() = getAccountFromSpinner(transferAccountSpinner)

    private fun configureTransferInput() {
        val transferAccount = transferAccount()
        val currentAccount = currentAccount()
        if (transferAccount == null || currentAccount == null) {
            return
        }
        val currency = currentAccount.currency
        val transferAccountCurrencyUnit = transferAccount.currency
        val isSame = currency == transferAccountCurrencyUnit
        setVisibility(viewBinding.TransferAmountRow, !isSame)
        setVisibility(viewBinding.ERR.root as ViewGroup, !isSame /*&& mTransaction !is Template*/)
        addCurrencyToInput(viewBinding.TransferAmountLabel, viewBinding.TransferAmount, transferAccountCurrencyUnit, R.string.amount)
        viewBinding.TransferAmount.setFractionDigits(transferAccountCurrencyUnit.fractionDigits)
        viewBinding.ERR.ExchangeRate.setCurrencies(currency, transferAccountCurrencyUnit)
        //TODO check history of this dead code
        val bundle = Bundle(2)
        bundle.putStringArray(DatabaseConstants.KEY_CURRENCY, arrayOf(currency.code, transferAccountCurrencyUnit.code))
    }

    private fun createTransferAccountAdapter() {
        transferAccountsAdapter = AccountAdapter(context)
        transferAccountsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        transferAccountSpinner.adapter = transferAccountsAdapter
        transferAccountSpinner.setOnItemSelectedListener(this)
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
            var isMain: Boolean) : MyTextWatcher() {

        override fun afterTextChanged(s: Editable) {
            if (isProcessingLinkedAmountInputs) return
            isProcessingLinkedAmountInputs = true
            if (isTemplate) {
                (if (isMain) viewBinding.TransferAmount else viewBinding.Amount).clear()
            } else if (viewBinding.ERR.root.visibility == View.VISIBLE) {
                val currentFocus = if (isMain) INPUT_AMOUNT else INPUT_TRANSFER_AMOUNT
                if (lastExchangeRateRelevantInputs[0] != currentFocus) {
                    lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0]
                    lastExchangeRateRelevantInputs[0] = currentFocus
                }
                if (lastExchangeRateRelevantInputs[1] == INPUT_EXCHANGE_RATE) {
                    applyExchangeRate(if (isMain) viewBinding.Amount else viewBinding.TransferAmount,
                            if (isMain) viewBinding.TransferAmount else viewBinding.Amount,
                            viewBinding.ERR.ExchangeRate.getRate(!isMain))
                } else {
                    updateExchangeRates()
                }
            }
            isProcessingLinkedAmountInputs = false
        }
    }

    private fun applyExchangeRate(from: AmountInput, to: AmountInput, rate: BigDecimal?) {
        val input = validateAmountInput(from, showToUser = false, ifPresent = true)
        to.setAmount(if (rate != null && input != null) input.multiply(rate) else BigDecimal(0), false)
    }

    private fun updateExchangeRates() {
        val amount = validateAmountInput(viewBinding.Amount, showToUser = false, ifPresent = true)
        val transferAmount = validateAmountInput(viewBinding.TransferAmount, showToUser = false, ifPresent = true)
        viewBinding.ERR.ExchangeRate.calculateAndSetRate(amount, transferAmount)
    }

    override fun buildTransaction(forSave: Boolean, currencyContext: CurrencyContext, accountId: Long): ITransfer? {
        val amount = validateAmountInput(forSave)
        val currentAccount = currentAccount()!!
        val transferAccount = transferAccount()!!
        val isSame = currentAccount.currency == transferAccount.currency
        val transferAmount: BigDecimal?
        transferAmount = if (isSame && amount != null) {
            amount.negate()
        } else {
            validateAmountInput(viewBinding.TransferAmount, forSave, true)?.let {
                if (isIncome) it.negate() else it
            }
        }
        return if (isTemplate) {
            if (amount == null && transferAmount == null) {
                return null
            }
            buildTemplate(accountId).apply {
                if (amount != null) {
                    this.amount = Money(currentAccount.currency, amount)
                    setTransferAccountId(transferAccount.id)
                } else if (!isSame && transferAmount != null) {
                    this.accountId = transferAccount.id
                    setTransferAccountId(currentAccount.id)
                    this.amount = Money(transferAccount.currency, transferAmount)
                    viewBinding.Amount.setError(null)
                }
            }
        } else {
            if (amount == null || transferAmount == null) {
                return null
            }
            Transfer(accountId, transferAccount.id, parentId).apply {
                transferPeer = this@TransferDelegate.transferPeer
                setAmountAndTransferAmount(
                        Money(currentAccount.currency, amount),
                        Money(transferAccount.currency, transferAmount))
            }
        }
    }

    override fun updateAccount(account: Account) {
        super.updateAccount(account)
        transferAccountSpinner.setSelection(setTransferAccountFilterMap())
        mTransferAccountId = transferAccountSpinner.selectedItemId
        configureTransferInput()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val transferAccountId = transferAccountSpinner.selectedItemId
        if (transferAccountId != AdapterView.INVALID_ROW_ID) {
            mTransferAccountId = transferAccountId
            super.onSaveInstanceState(outState)
        }
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

    companion object {
        private const val INPUT_EXCHANGE_RATE = 1
        private const val INPUT_AMOUNT = 2
        private const val INPUT_TRANSFER_AMOUNT = 3
    }

}