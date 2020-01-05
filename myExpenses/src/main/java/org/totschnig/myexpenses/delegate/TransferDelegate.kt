package org.totschnig.myexpenses.delegate

import android.text.Editable
import android.view.View
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.ui.MyTextWatcher
import java.math.BigDecimal

class TransferDelegate(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, prefHandler: PrefHandler) : TransactionDelegate<Transfer>(viewBinding, dateEditBinding, prefHandler) {
    override fun bind(transaction: Transfer, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance)
        viewBinding.Amount.addTextChangedListener(LinkedTransferAmountTextWatcher(true))
        viewBinding.TransferAmount.addTextChangedListener(LinkedTransferAmountTextWatcher(false))
        viewBinding.ERR.ExchangeRate.setExchangeRateWatcher(LinkedExchangeRateTextWatcher())
    }
    private inner class LinkedTransferAmountTextWatcher(
            /**
             * true if we are linked to from amount
             */
            var isMain: Boolean) : MyTextWatcher() {

        override fun afterTextChanged(s: Editable) {
            if (isProcessingLinkedAmountInputs) return
            isProcessingLinkedAmountInputs = true
/*            if (mTransaction is Template) {
                (if (isMain) rootBinding.TransferAmount else amountInput).clear()
            } else */if (exchangeRateRow.visibility == View.VISIBLE) {
                val currentFocus = if (isMain) ExpenseEdit.INPUT_AMOUNT else ExpenseEdit.INPUT_TRANSFER_AMOUNT
                if (lastExchangeRateRelevantInputs[0] != currentFocus) {
                    lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0]
                    lastExchangeRateRelevantInputs[0] = currentFocus
                }
                if (lastExchangeRateRelevantInputs[1] == ExpenseEdit.INPUT_EXCHANGE_RATE) {
                    applyExchangeRate(if (isMain) amountInput else rootBinding.TransferAmount,
                            if (isMain) rootBinding.TransferAmount else amountInput,
                            exchangeRateEdit.getRate(!isMain))
                } else {
                    updateExchangeRates(rootBinding.TransferAmount)
                }
            }
            isProcessingLinkedAmountInputs = false
        }
    }

    private inner class LinkedExchangeRateTextWatcher : ExchangeRateEdit.ExchangeRateWatcher {
        override fun afterExchangeRateChanged(rate: BigDecimal, inverse: BigDecimal) {
            if (isProcessingLinkedAmountInputs) return
            isProcessingLinkedAmountInputs = true
            val constant: AmountInput?
            val variable: AmountInput?
            val exchangeFactor: BigDecimal
            if (lastExchangeRateRelevantInputs[0] != ExpenseEdit.INPUT_EXCHANGE_RATE) {
                lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0]
                lastExchangeRateRelevantInputs[0] = ExpenseEdit.INPUT_EXCHANGE_RATE
            }
            if (lastExchangeRateRelevantInputs[1] == ExpenseEdit.INPUT_AMOUNT) {
                constant = amountInput
                variable = rootBinding.TransferAmount
                exchangeFactor = rate
            } else {
                constant = rootBinding.TransferAmount
                variable = amountInput
                exchangeFactor = inverse
            }
            applyExchangeRate(constant, variable, exchangeFactor)
            isProcessingLinkedAmountInputs = false
        }
    }
}