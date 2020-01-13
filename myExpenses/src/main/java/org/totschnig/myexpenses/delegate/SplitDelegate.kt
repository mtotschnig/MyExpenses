package org.totschnig.myexpenses.delegate

import android.text.Editable
import android.view.View
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.ui.MyTextWatcher

class SplitDelegate(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, prefHandler: PrefHandler, isTemplate: Boolean) :
        MainDelegate<SplitTransaction>(viewBinding, dateEditBinding, prefHandler, isTemplate) {
    override val operationType = TransactionsContract.Transactions.TYPE_SPLIT

    override val helpVariant: ExpenseEdit.HelpVariant
        get() = if  (isTemplate) ExpenseEdit.HelpVariant.templateSplit else ExpenseEdit.HelpVariant.split
    override val title = context.getString(R.string.menu_edit_split)
    override val typeResId = R.string.split_transaction
    override val shouldAutoFill = false

    override fun bind(transaction: SplitTransaction, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, recurrence: Plan.Recurrence?, plan: Plan?) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance, recurrence, plan)
        viewBinding.Amount.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                host.updateSplitBalance()
            }
        })
        viewBinding.CategoryRow.visibility = View.GONE
        host.addSplitPartList(transaction)
    }

    override fun buildMainTransaction(accountId: Long) = if (isTemplate) buildTemplate(accountId) else SplitTransaction(accountId)

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

    override fun updateAccount(account: Account) {
        super.updateAccount(account)
        host.updateSplitPartList(account)
    }
}