package org.totschnig.myexpenses.delegate

import android.database.Cursor
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import icepick.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.shouldStartAutoFill
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils

class CategoryDelegate(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, methodRowBinding: MethodRowBinding, prefHandler: PrefHandler, isTemplate: Boolean)
    : MainDelegate<ITransaction>(viewBinding, dateEditBinding, methodRowBinding, prefHandler, isTemplate) {

    override val operationType = TYPE_TRANSACTION

    @JvmField
    @State
    var label: String? = null
    @JvmField
    @State
    var categoryIcon: String? = null
    @JvmField
    @State
    var catId: Long? = null

    override fun bind(transaction: ITransaction?, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, savedInstanceState: Bundle?, recurrence: Plan.Recurrence?, withAutoFill: Boolean) {
        super.bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance, savedInstanceState, recurrence, withAutoFill)
        viewBinding.Category.setOnClickListener { host.startSelectCategory() }
        if (transaction != null) {
            label = transaction.label
            categoryIcon = transaction.categoryIcon
            catId = transaction.catId
        }
        if (parentId != null) {
            hideRowsSpecificToMain()
        }
        setCategoryButton()
        val homeCurrency = Utils.getHomeCurrency()
        addCurrencyToInput(viewBinding.EquivalentAmountLabel, viewBinding.EquivalentAmount, homeCurrency, R.string.menu_equivalent_amount)
        viewBinding.EquivalentAmount.setFractionDigits(homeCurrency.fractionDigits)
    }

    override fun buildMainTransaction(accountId: Long): ITransaction =
            (if (isTemplate) buildTemplate(accountId) else Transaction(accountId, parentId)).apply {
                this.catId = this@CategoryDelegate.catId
                this.label = this@CategoryDelegate.label
            }

    override fun configureType() {
        super.configureType()
        setCategoryButton()
    }

    fun resetCategory() {
        setCategory(null, null, null)
    }

    /**
     * set label on category button
     */
    private fun setCategoryButton() {
        if (label.isNullOrEmpty()) {
            viewBinding.Category.setText(R.string.select)
            viewBinding.ClearCategory.visibility = View.GONE
        } else {
            viewBinding.Category.text = label
            viewBinding.ClearCategory.visibility = View.VISIBLE

        }
        UiUtils.setCompoundDrawablesCompatWithIntrinsicBounds(viewBinding.Category,
                if (categoryIcon != null) UiUtils.resolveIcon(viewBinding.root.context, categoryIcon) else 0, 0, 0, 0)
    }

    override fun populateFields(transaction: ITransaction, withAutoFill: Boolean) {
        super.populateFields(transaction, withAutoFill)
        if (withAutoFill && !isTemplate && !isSplitPart && shouldStartAutoFill(prefHandler)) {
            viewBinding.Payee.requestFocus()
        }
    }

    fun autoFill(data: Cursor, currencyContext: CurrencyContext) {
        if (data.moveToFirst()) {
            var typeHasChanged = false
            val columnIndexCatId = data.getColumnIndex(DatabaseConstants.KEY_CATID)
            val columnIndexLabel = data.getColumnIndex(DatabaseConstants.KEY_LABEL)
            if (catId == null && columnIndexCatId != -1 && columnIndexLabel != -1) {
                catId = DbUtils.getLongOrNull(data, columnIndexCatId)
                label = data.getString(columnIndexLabel)
                categoryIcon = data.getString(data.getColumnIndexOrThrow(KEY_ICON))
                setCategoryButton()
            }
            val columnIndexComment = data.getColumnIndex(DatabaseConstants.KEY_COMMENT)
            if (TextUtils.isEmpty(viewBinding.Comment.text.toString()) && columnIndexComment != -1) {
                viewBinding.Comment.setText(data.getString(columnIndexComment))
            }
            val columnIndexAmount = data.getColumnIndex(DatabaseConstants.KEY_AMOUNT)
            val columnIndexCurrency = data.getColumnIndex(DatabaseConstants.KEY_CURRENCY)
            if (validateAmountInput(viewBinding.Amount, showToUser = false, ifPresent = true) == null && columnIndexAmount != -1 && columnIndexCurrency != -1) {
                val beforeType = isIncome
                fillAmount(Money(currencyContext[data.getString(columnIndexCurrency)], data.getLong(columnIndexAmount)).amountMajor)
                configureType()
                typeHasChanged = beforeType != isIncome
            }
            val columnIndexMethodId = data.getColumnIndex(DatabaseConstants.KEY_METHODID)
            if (methodId == null && columnIndexMethodId != -1) {
                methodId = DbUtils.getLongOrNull(data, columnIndexMethodId)
                if (!typeHasChanged) { //if type has changed, we need to wait for methods to be reloaded, method is then selected in onLoadFinished
                    setMethodSelection()
                }
            }
            val columnIndexAccountId = data.getColumnIndex(DatabaseConstants.KEY_ACCOUNTID)
            if (columnIndexAccountId != -1) {
                val accountId = data.getLong(columnIndexAccountId)
                var i = 0
                while (i < accountsAdapter.count) {
                    if (mAccounts[i].id == accountId) {
                        accountSpinner.setSelection(i)
                        updateAccount(mAccounts[i])
                        break
                    }
                    i++
                }
            }
        }
    }

    fun setCategory(label: String?, categoryIcon: String?, catId: Long?) {
        this.label = label
        this.categoryIcon = categoryIcon
        this.catId = catId
        setCategoryButton()
    }
}