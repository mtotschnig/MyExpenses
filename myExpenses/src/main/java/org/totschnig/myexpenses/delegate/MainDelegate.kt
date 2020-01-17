package org.totschnig.myexpenses.delegate

import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils

//Transaction or Split
abstract class MainDelegate<T : ITransaction>(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, prefHandler: PrefHandler, isTemplate: Boolean) : TransactionDelegate<T>(viewBinding, dateEditBinding, prefHandler, isTemplate) {

    override fun bind(transaction: T, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, recurrence: Plan.Recurrence?, plan: Plan?) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance, recurrence, plan)
        viewBinding.Category.setOnClickListener { (context as ExpenseEdit).startSelectCategory() }
    }

    override fun buildTransaction(forSave: Boolean, currencyContext: CurrencyContext, accountId: Long): T? {
        val amount = validateAmountInput(forSave)
        if (amount == null) { //Snackbar is shown in validateAmountInput
            return null
        }
        return buildMainTransaction(accountId).apply {
            this.amount = Money(currentAccount()!!.currencyUnit, amount)
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

    abstract fun buildMainTransaction(accountId: Long): T
}