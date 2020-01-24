package org.totschnig.myexpenses.delegate

import android.os.Bundle
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.Account

//Transaction or Split
abstract class MainDelegate<T : ITransaction>(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, prefHandler: PrefHandler, isTemplate: Boolean) : TransactionDelegate<T>(viewBinding, dateEditBinding, prefHandler, isTemplate) {

    override fun bind(transaction: T, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, savedInstanceState: Bundle?, recurrence: Plan.Recurrence?, currencyExtra: String?) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance, savedInstanceState, recurrence, currencyExtra)
        viewBinding.Category.setOnClickListener { host.startSelectCategory() }
    }

    override fun buildTransaction(forSave: Boolean, currencyContext: CurrencyContext, accountId: Long): T? {
        val amount = validateAmountInput(forSave)
        if (amount == null) { //Snackbar is shown in validateAmountInput
            return null
        }
        return buildMainTransaction(accountId).apply {
            this.amount = Money(currentAccount()!!.currency, amount)
            payee = viewBinding.Payee.text.toString()
            this.methodId = this@MainDelegate.methodId
            val originalAmount = validateAmountInput(viewBinding.OriginalAmount, false)
            val selectedItem = viewBinding.OriginalAmount.selectedCurrency
            if (selectedItem != null && originalAmount != null) {
                val currency = selectedItem.code()
                PrefKey.LAST_ORIGINAL_CURRENCY.putString(currency)
                this.originalAmount = Money(currencyContext[currency], originalAmount)
            } else {
                this.originalAmount = null
            }
            val equivalentAmount = validateAmountInput(viewBinding.EquivalentAmount, false)
            this.equivalentAmount = if (equivalentAmount == null) null else Money(Utils.getHomeCurrency(), if (isIncome) equivalentAmount else equivalentAmount.negate())
        }
    }

    override fun updateAccount(account: Account) {
        super.updateAccount(account)
        if (!isSplitPart) {
            host.loadMethods(account)
        }
    }

    override fun populateFields(transaction: T, prefHandler: PrefHandler, newInstance: Boolean) {
        super.populateFields(transaction, prefHandler, newInstance)
        if (!isSplitPart)
            viewBinding.Payee.setText(transaction.payee)
    }

    abstract fun buildMainTransaction(accountId: Long): T
}