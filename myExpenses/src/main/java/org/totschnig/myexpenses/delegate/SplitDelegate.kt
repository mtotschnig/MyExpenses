package org.totschnig.myexpenses.delegate

import android.os.Bundle
import android.text.Editable
import android.view.View
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.ISplit
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.ui.MyTextWatcher
import org.totschnig.myexpenses.viewmodel.data.Account

class SplitDelegate(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, methodRowBinding: MethodRowBinding, prefHandler: PrefHandler, isTemplate: Boolean) :
        MainDelegate<ISplit>(viewBinding, dateEditBinding, methodRowBinding, prefHandler, isTemplate) {
    override val operationType = TransactionsContract.Transactions.TYPE_SPLIT

    override val helpVariant: ExpenseEdit.HelpVariant
        get() = if  (isTemplate) ExpenseEdit.HelpVariant.templateSplit else ExpenseEdit.HelpVariant.split
    override val typeResId = R.string.split_transaction
    override val editResId = R.string.menu_edit_split
    override val shouldAutoFill = false
    private var missingRecurrenceFeature: ContribFeature? = null

    override fun bind(transaction: ISplit?, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, savedInstanceState: Bundle?, recurrence: Plan.Recurrence?, withAutoFill: Boolean) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance, savedInstanceState, recurrence, withAutoFill)
        viewBinding.Amount.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                host.updateSplitBalance()
            }
        })
        viewBinding.CategoryRow.visibility = View.GONE
        missingRecurrenceFeature = if (!newInstance || prefHandler.getBoolean(PrefKey.NEW_SPLIT_TEMPLATE_ENABLED, true)) super.missingRecurrenceFeature() else ContribFeature.SPLIT_TEMPLATE
    }

    override fun buildMainTransaction(accountId: Long): ISplit = if (isTemplate) buildTemplate(accountId) else SplitTransaction(accountId)

    override fun prepareForNew() {
        super.prepareForNew()
        rowId =  SplitTransaction.getNewInstance(accountId!!).id
        host.findSplitPartList()?.updateParent(rowId)
    }

    override fun configureType() {
        super.configureType()
        host.updateSplitBalance()
    }

    override fun setAccounts(data: List<Account>, currencyExtra: String?) {
        super.setAccounts(data, currencyExtra)
        host.addSplitPartList(rowId)
    }

    override fun updateAccount(account: Account) {
        super.updateAccount(account)
        host.updateSplitPartList(account, rowId)
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
            host.showSnackbar(host.getString(R.string.warning_cannot_move_split_transaction, account.label))
        }
    }

    override fun missingRecurrenceFeature() = missingRecurrenceFeature
}