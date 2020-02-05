package org.totschnig.myexpenses.delegate

import android.os.Bundle
import android.text.Editable
import android.view.View
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.ISplit
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.ui.MyTextWatcher
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.Account

class SplitDelegate(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, prefHandler: PrefHandler, isTemplate: Boolean) :
        MainDelegate<ISplit>(viewBinding, dateEditBinding, prefHandler, isTemplate) {
    override val operationType = TransactionsContract.Transactions.TYPE_SPLIT

    override val helpVariant: ExpenseEdit.HelpVariant
        get() = if  (isTemplate) ExpenseEdit.HelpVariant.templateSplit else ExpenseEdit.HelpVariant.split
    override val title = context.getString(R.string.menu_edit_split)
    override val typeResId = R.string.split_transaction
    override val shouldAutoFill = false

    override fun bind(transaction: ISplit?, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, savedInstanceState: Bundle?, recurrence: Plan.Recurrence?) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance, savedInstanceState, recurrence)
        viewBinding.Amount.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                host.updateSplitBalance()
            }
        })
        viewBinding.CategoryRow.visibility = View.GONE
    }

    override fun buildMainTransaction(accountId: Long): ISplit = if (isTemplate) buildTemplate(accountId) else SplitTransaction(accountId)

    override fun prepareForNew() {
        rowId =  SplitTransaction.getNewInstance(accountId!!).id
        val splitPartList = host.findSplitPartList()
        splitPartList?.updateParent(rowId!!)
        resetAmounts()
    }

    override fun configureType() {
        super.configureType()
        host.updateSplitBalance()
    }

    override fun setAccounts(data: List<Account>, currencyExtra: String?) {
        super.setAccounts(data, currencyExtra)
        host.addSplitPartList()
    }

    override fun updateAccount(account: Account) {
        host.updateSplitPartList(account)
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
            host.showSnackbar(host.getString(R.string.warning_cannot_move_split_transaction, account.label),
                    Snackbar.LENGTH_LONG)
        }
    }
}