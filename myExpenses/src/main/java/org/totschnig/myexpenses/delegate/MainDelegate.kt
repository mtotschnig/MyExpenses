package org.totschnig.myexpenses.delegate

import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils

abstract class MainDelegate<T : Transaction>(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, prefHandler: PrefHandler) : TransactionDelegate<T>(viewBinding, dateEditBinding, prefHandler) {

    override fun bind(transaction: T, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, recurrence: Plan.Recurrence?) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance, recurrence)
        viewBinding.Category.setOnClickListener { (context as ExpenseEdit).startSelectCategory() }
    }

    override fun buildTransaction(forSave: Boolean, currencyContext: CurrencyContext): T? {
        val amount = validateAmountInput(forSave)
        if (amount == null) { //Snackbar is shown in validateAmountInput
            return null
        }
        return buildMainTransaction().apply {
            this.amount = Money(currentAccount()!!.currencyUnit, amount)
            this.catId = this@MainDelegate.catId
            payee = viewBinding.Payee.text.toString()
            this.methodId = this@MainDelegate.methodId
            //TODO
            if (true /*mIsMainTransaction*/) {
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
    }

    override fun updateAccount(account: Account) {
        super.updateAccount(account)
        //TODO
        /* if (!isSplitPart) {
             loadMethods(account)
         }
         if (mOperationType == TransactionsContract.Transactions.TYPE_SPLIT) {
             val splitPartList = findSplitPartList()
             splitPartList?.updateAccount(account)
         }*/
    }

    abstract fun buildMainTransaction(): T
}