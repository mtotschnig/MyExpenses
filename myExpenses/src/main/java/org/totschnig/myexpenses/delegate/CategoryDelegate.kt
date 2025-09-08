package org.totschnig.myexpenses.delegate

import android.os.Bundle
import android.text.TextUtils
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.shouldStartAutoFillWithFocus
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel
import org.totschnig.myexpenses.viewmodel.data.Account

class CategoryDelegate(
    viewBinding: OneExpenseBinding,
    dateEditBinding: DateEditBinding,
    methodRowBinding: MethodRowBinding,
    isTemplate: Boolean
) : MainDelegate<ITransaction>(
    viewBinding,
    dateEditBinding,
    methodRowBinding,
    isTemplate
) {

    override val operationType = TYPE_TRANSACTION

    override fun bind(
        transaction: ITransaction?,
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
        if (parentId != null) {
            hideRowsSpecificToMain()
        }

        addCurrencyToInput(
            viewBinding.EquivalentAmountLabel,
            viewBinding.EquivalentAmount,
            homeCurrency,
            R.string.menu_equivalent_amount
        )

        viewBinding.EquivalentAmount.setFractionDigits(homeCurrency.fractionDigits)
    }

    override fun buildMainTransaction(account: Account): ITransaction =
        (if (isTemplate) buildTemplate(account) else Transaction(account.id, parentId))

    override fun configureType() {
        super.configureType()
        setCategoryButton()
    }

    override fun populateFields(transaction: ITransaction, withAutoFill: Boolean) {
        super.populateFields(transaction, withAutoFill)
        if (withAutoFill && !isTemplate && !isSplitPart && shouldStartAutoFillWithFocus(prefHandler)) {
            viewBinding.Payee.requestFocus()
        }
    }

    fun autoFill(data: TransactionEditViewModel.AutoFillData) {
        var typeHasChanged = false
        if (catId == null && data.catId != null && data.label != null) {
            catId = data.catId
            label = data.label
            categoryIcon = data.icon
            setCategoryButton()
        }
        if (TextUtils.isEmpty(viewBinding.Comment.text.toString()) && data.comment != null) {
            viewBinding.Comment.setText(data.comment)
        }

        if (viewBinding.Amount.getAmount(
                showToUser = false
            ) == null && data.amount != null
        ) {
            val beforeType = isIncome
            fillAmount(data.amount.amountMajor)
            configureType()
            typeHasChanged = beforeType != isIncome
        }
        if (methodId == null && data.methodId != null) {
            methodId = data.methodId
            if (!typeHasChanged) { //if type has changed, we need to wait for methods to be reloaded, method is then selected in onLoadFinished
                setMethodSelection()
            }
        }
        if (data.accountId != null && data.accountId != accountId) {
            val oldAccount = mAccounts.firstOrNull { it.id == accountId }
            val newAccount = mAccounts.firstOrNull { it.id == data.accountId }
            if (oldAccount != null && newAccount != null) {
                accountSpinner.setSelection(accountAdapter.getPosition(newAccount.id))
                updateAccount(newAccount, newAccount.currency.code != oldAccount.currency.code)
            }
        }
        if (data.debtId != null) {
            debtId = data.debtId
            updateUiWithDebt()
        }
    }
}