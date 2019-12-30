package org.totschnig.myexpenses.viewholder

import android.view.View
import android.widget.ArrayAdapter
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.OperationTypeAdapter
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import java.math.BigDecimal

open class TransactionViewHolder<T : Transaction>(val viewBinding: OneExpenseBinding)  {
    private lateinit var methodSpinner: SpinnerHelper
    private lateinit var accountSpinner: SpinnerHelper
    private lateinit var transferAccountSpinner: SpinnerHelper
    private lateinit var statusSpinner: SpinnerHelper
    private lateinit var operationTypeSpinner: SpinnerHelper
    private lateinit var recurrenceSpinner: SpinnerHelper

    var isProcessingLinkedAmountInputs = false
    var originalAmountVisible = false
    var equivalentAmountVisible = false
    var originalCurrencyCode: String? = null
    open fun bind(transaction: T, isCalendarPermissionPermanentlyDeclined: Boolean, prefHandler: PrefHandler) {
        methodSpinner = SpinnerHelper(viewBinding.Method)
        accountSpinner = SpinnerHelper(viewBinding.Account)
        transferAccountSpinner = SpinnerHelper(viewBinding.TransferAccount)
        statusSpinner = SpinnerHelper(viewBinding.Status)
        recurrenceSpinner = SpinnerHelper(viewBinding.RR.Recurrence.Recurrence)
        viewBinding.Amount.setFractionDigits(transaction.amount.currencyUnit.fractionDigits())
/*        if (mOperationType == TransactionsContract.Transactions.TYPE_SPLIT) {
            amountInput.addTextChangedListener(object : MyTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    updateSplitBalance()
                }
            })
        }*/
/*        if (mOperationType == TransactionsContract.Transactions.TYPE_TRANSFER) {
            amountInput.addTextChangedListener(LinkedTransferAmountTextWatcher(true))
            rootBinding.TransferAmount.addTextChangedListener(LinkedTransferAmountTextWatcher(false))
            exchangeRateEdit.setExchangeRateWatcher(LinkedExchangeRateTextWatcher())
        }*/

/*        if (isSplitPart) {
            disableAccountSpinner()
        }*/
        val mIsMainTransactionOrTemplate = /*mOperationType != TransactionsContract.Transactions.TYPE_TRANSFER &&*/ !transaction.isSplitPart
        val mIsMainTransaction = mIsMainTransactionOrTemplate && transaction !is Template
        val mIsMainTemplate = transaction is Template && !transaction.isSplitPart()
        if (!mIsMainTransactionOrTemplate) {
            viewBinding.PayeeRow.visibility = View.GONE
            viewBinding.MethodRow.visibility = View.GONE
        }
/*        if (mOperationType == TransactionsContract.Transactions.TYPE_TRANSFER) {
            amountInput.hideTypeButton()
            rootBinding.CategoryRow.visibility = View.GONE
            rootBinding.TransferAccountRow.visibility = View.VISIBLE
            rootBinding.AccountLabel.setText(R.string.transfer_from_account)
        }*/
/*        if (mIsMainTemplate) {
            rootBinding.TitleRow.visibility = View.VISIBLE
            if (!isCalendarPermissionPermanentlyDeclined) { //if user has denied access and checked that he does not want to be asked again, we do not
//bother him with a button that is not working
                setPlannerRowVisibility(View.VISIBLE)
                val recurrenceAdapter = RecurrenceAdapter(this,
                        if (DistribHelper.shouldUseAndroidPlatformCalendar()) null else Plan.Recurrence.CUSTOM)
                mRecurrenceSpinner.adapter = recurrenceAdapter
                mRecurrenceSpinner.setOnItemSelectedListener(this)
                planButton.setOnClickListener {
                    if (mPlan == null) {
                        planButton.showDialog()
                    } else if (DistribHelper.shouldUseAndroidPlatformCalendar()) {
                        launchPlanView(false)
                    }
                }
            }
            rootBinding.AttachImage.visibility = View.GONE
            if (transaction.id != 0L) {
                val typeResId = when (mOperationType) {
                    TransactionsContract.Transactions.TYPE_TRANSFER -> R.string.transfer
                    TransactionsContract.Transactions.TYPE_SPLIT -> R.string.split_transaction
                    else -> R.string.transaction
                }
                title = getString(R.string.menu_edit_template) + " (" + getString(typeResId) + ")"
            }
            when (mOperationType) {
                TransactionsContract.Transactions.TYPE_TRANSFER -> setHelpVariant(ExpenseEdit.HelpVariant.templateTransfer)
                TransactionsContract.Transactions.TYPE_SPLIT -> setHelpVariant(ExpenseEdit.HelpVariant.templateSplit)
                else -> setHelpVariant(ExpenseEdit.HelpVariant.templateCategory)
            }
        } else */if (transaction.isSplitPart) {
/*            if (mOperationType == TransactionsContract.Transactions.TYPE_TRANSACTION) {
                if (transaction.id != 0L) {
                    setTitle(R.string.menu_edit_split_part_category)
                }
                setHelpVariant(ExpenseEdit.HelpVariant.splitPartCategory)
                transaction.status = DatabaseConstants.STATUS_UNCOMMITTED
            } else { //Transfer
                if (transaction.id != 0L) {
                    setTitle(R.string.menu_edit_split_part_transfer)
                }
                setHelpVariant(ExpenseEdit.HelpVariant.splitPartTransfer)
                transaction.status = DatabaseConstants.STATUS_UNCOMMITTED
            }*/
        } else { //Transfer or Transaction, we can suggest to create a plan
            if (!isCalendarPermissionPermanentlyDeclined) { //we set adapter even if spinner is not immediately visible, since it might become visible
//after SAVE_AND_NEW action
/*                val recurrenceAdapter = RecurrenceAdapter(this,
                        Plan.Recurrence.ONETIME, Plan.Recurrence.CUSTOM)
                mRecurrenceSpinner.adapter = recurrenceAdapter
                val cachedRecurrence = intent.getSerializableExtra(ExpenseEdit.KEY_CACHED_RECURRENCE) as? Plan.Recurrence
                if (cachedRecurrence != null) {
                    mRecurrenceSpinner.setSelection(
                            (mRecurrenceSpinner.adapter as RecurrenceAdapter).getPosition(cachedRecurrence))
                }
                mRecurrenceSpinner.setOnItemSelectedListener(this)
                setPlannerRowVisibility(View.VISIBLE)
                if (transaction.originTemplate != null && transaction.originTemplate.plan != null) {
                    mRecurrenceSpinner.spinner.visibility = View.GONE
                    planButton.visibility = View.VISIBLE
                    planButton.text = Plan.prettyTimeInfo(this,
                            transaction.originTemplate.plan.rrule, transaction.originTemplate.plan.dtstart)
                    planButton.setOnClickListener {
                        val currentAccount = currentAccount
                        if (currentAccount != null) {
                            PlanMonthFragment.newInstance(
                                    transaction.originTemplate.title,
                                    transaction.originTemplate.id,
                                    transaction.originTemplate.planId,
                                    currentAccount.color, true, themeType).show(supportFragmentManager,
                                    TemplatesList.CALDROID_DIALOG_FRAGMENT_TAG)
                        }
                    }
                }*/
            }
/*            when (transaction) {
                is Transfer -> {
                    if (transaction.getId() != 0L) {
                        setTitle(R.string.menu_edit_transfer)
                    }
                    setHelpVariant(ExpenseEdit.HelpVariant.transfer)
                }
                is SplitTransaction -> {
                    if (!mNewInstance) {
                        setTitle(R.string.menu_edit_split)
                    }
                    setHelpVariant(ExpenseEdit.HelpVariant.split)
                }
                else -> {
                    if (transaction.id != 0L) {
                        setTitle(R.string.menu_edit_transaction)
                    }
                    setHelpVariant(ExpenseEdit.HelpVariant.transaction)
                }
            }*/
        }
/*        if (mOperationType == TransactionsContract.Transactions.TYPE_SPLIT) {
            rootBinding.CategoryRow.visibility = View.GONE
            //add split list
            val fm = supportFragmentManager
            if (findSplitPartList() == null && !fm.isStateSaved) {
                fm.beginTransaction()
                        .add(R.id.edit_container, SplitPartList.newInstance(transaction), ExpenseEdit.SPLIT_PART_LIST)
                        .commit()
                fm.executePendingTransactions()
            }
        }*/
/*        if (mClone) {
            setTitle(R.string.menu_clone_transaction)
        }*/
/*        if (isNoMainTransaction) {
            rootBinding.DateTimeRow.visibility = View.GONE
        }*/
        //when we have a savedInstance, fields have already been populated
        //if (!mSavedInstance) {
            populateFields(transaction, prefHandler)
            /*if (!isSplitPart) {
                setLocalDateTime(transaction)
            }*/
        //}
        if (transaction.id != 0L) {
            //configureTransferDirection()
        }
        //after setLocalDateTime, so that the plan info can override the date
        //configurePlan()
        setCategoryButton(transaction.label, transaction.categoryIcon)
        //if (mOperationType != TransactionsContract.Transactions.TYPE_TRANSFER) {
            viewBinding.Category.setOnClickListener { /*startSelectCategory()*/ }
        //}
        if (originalAmountVisible) {
            showOriginalAmount()
        }
        if (equivalentAmountVisible) {
            showEquivalentAmount()
        }
        if (mIsMainTransaction) {
            val homeCurrency = Utils.getHomeCurrency()
            addCurrencyToInput(viewBinding.EquivalentAmountLabel, viewBinding.EquivalentAmount, homeCurrency.symbol(), R.string.menu_equivalent_amount)
            viewBinding.EquivalentAmount.setFractionDigits(homeCurrency.fractionDigits())
        }
    }

    /**
     * populates the input fields with a transaction from the database or a new one
     */
    private fun populateFields(transaction: Transaction, prefHandler: PrefHandler) {
        //val cached = intent.getSerializableExtra(ExpenseEdit.KEY_CACHED_DATA) as? Transaction
        val cachedOrSelf =/* cached ?:*/ transaction
        isProcessingLinkedAmountInputs = true
        //mStatusSpinner.setSelection(cachedOrSelf.crStatus.ordinal, false)
        viewBinding.Comment.setText(cachedOrSelf.comment)
        //if (mIsMainTransactionOrTemplate) {
            viewBinding.Payee.setText(cachedOrSelf.payee)
        //}
/*        if (mIsMainTemplate) {
            rootBinding.Title.setText((cachedOrSelf as Template).title)
            planExecutionButton.isChecked = (transaction as Template).isPlanExecutionAutomatic
        } else {*/
        viewBinding.Number.setText(cachedOrSelf.referenceNumber)
        //}
        fillAmount(cachedOrSelf.amount.amountMajor)
        if (cachedOrSelf.originalAmount != null) {
            originalAmountVisible = true
            showOriginalAmount()
            viewBinding.OriginalAmount.setAmount(cachedOrSelf.originalAmount.amountMajor)
            originalCurrencyCode = cachedOrSelf.originalAmount.currencyUnit.code()
        } else {
            originalCurrencyCode = prefHandler.getString(PrefKey.LAST_ORIGINAL_CURRENCY, null)
        }
        populateOriginalCurrency()
        if (cachedOrSelf.equivalentAmount != null) {
            equivalentAmountVisible = true
            viewBinding.EquivalentAmount.setAmount(cachedOrSelf.equivalentAmount.amountMajor.abs())
        }
/*        if (mNewInstance) {
            if (mIsMainTemplate) {
                viewBinding.Title.requestFocus()
            } else if (mIsMainTransactionOrTemplate && PreferenceUtils.shouldStartAutoFill()) {
                viewBinding.Payee.requestFocus()
            }
        }*/
        isProcessingLinkedAmountInputs = false
    }

    private fun fillAmount(amount: BigDecimal) {
        with(viewBinding.Amount) {
            if (amount.signum() != 0) {
               setAmount(amount)
            }
            requestFocus()
            selectAll()
        }
    }

    private fun showEquivalentAmount() {
        setVisibility(viewBinding.EquivalentAmountRow, equivalentAmountVisible)
        viewBinding.EquivalentAmount.setCompoundResultInput(if (equivalentAmountVisible) viewBinding.Amount.validate(false) else null)
    }

    private fun showOriginalAmount() {
        setVisibility(viewBinding.OriginalAmountRow, originalAmountVisible)
    }

    fun populateOriginalCurrency() {
        if (originalCurrencyCode != null) {
            viewBinding.OriginalAmount.setSelectedCurrency(originalCurrencyCode)
        }
    }

    private fun setVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * set label on category button
     */
    fun setCategoryButton(label: String?, categoryIcon: String?) {
        if (!label.isNullOrEmpty()) {
            viewBinding.Category.text = label
            viewBinding.ClearCategory.visibility = View.VISIBLE
            UiUtils.setCompoundDrawablesCompatWithIntrinsicBounds(viewBinding.Category,
                    if (categoryIcon != null) UiUtils.resolveIcon(viewBinding.root.context, categoryIcon) else 0, 0, 0, 0)
        } else {
            viewBinding.Category.setText(R.string.select)
            viewBinding.ClearCategory.visibility = View.GONE
        }
    }

/*    private fun configureTransferInput() {
        val transferAccount = transferAccount
        val currentAccount = currentAccount
        if (transferAccount == null || currentAccount == null) {
            return
        }
        val currency = currentAccount.currencyUnit
        val transferAccountCurrencyUnit = transferAccount.currencyUnit
        val isSame = currency == transferAccountCurrencyUnit
        setVisibility(viewBinding.TransferAmountRow, !isSame)
        setVisibility(viewBinding.ERR.root as ViewGroup, !isSame *//*&& mTransaction !is Template*//*)
        addCurrencyToInput(viewBinding.TransferAmountLabel, viewBinding.TransferAmount, transferAccountCurrencyUnit.symbol(), R.string.amount)
        viewBinding.TransferAmount.setFractionDigits(transferAccountCurrencyUnit.fractionDigits())
        viewBinding.ERR.ExchangeRate.setCurrencies(currency, transferAccountCurrencyUnit)
        val bundle = Bundle(2)
        bundle.putStringArray(DatabaseConstants.KEY_CURRENCY, arrayOf(currency.code(), transferAccountCurrencyUnit.code()))
    }*/

    private fun addCurrencyToInput(label: TextView, amountInput: AmountInput, symbol: String, textResId: Int) {
        val text = org.totschnig.myexpenses.util.TextUtils.appendCurrencySymbol(label.context, textResId, symbol)
        label.text = text
        amountInput.contentDescription = text
    }

    fun setCurrencies(currencies: List<Currency?>?, currencyContext: CurrencyContext?) {
        viewBinding.OriginalAmount.setCurrencies(currencies, currencyContext)
        populateOriginalCurrency()
    }

    fun toggleOriginalAmount() {
        originalAmountVisible = !originalAmountVisible
        showOriginalAmount()
        if (originalAmountVisible) {
            viewBinding.OriginalAmount.requestFocus()
        } else {
            viewBinding.OriginalAmount.clear()
        }
    }

    fun toggleEquivalentAmount(currentAccount: Account?) {
        equivalentAmountVisible = !equivalentAmountVisible
        showEquivalentAmount()
        if (equivalentAmountVisible) {
            if (validateAmountInput(viewBinding.EquivalentAmount, false) == null && currentAccount != null) {
                val rate = BigDecimal(currentAccount.exchangeRate)
                viewBinding.EquivalentAmount.setExchangeRate(rate)
            }
            viewBinding.EquivalentAmount.requestFocus()
        } else {
            viewBinding.EquivalentAmount.clear()
        }
    }

    fun setAdapters(accountsAdapter: SimpleCursorAdapter, methodsAdapter: ArrayAdapter<PaymentMethod>, payeeAdapter: SimpleCursorAdapter, operationTypeAdapter: OperationTypeAdapter, transferAccountsAdapter: SimpleCursorAdapter) {

    }

    companion object {
        fun <T: Transaction> createAndBind(transaction: T, viewBinding: OneExpenseBinding, isCalendarPermissionPermanentlyDeclined: Boolean, prefHandler: PrefHandler) =
                when (transaction) {
                    is Transfer -> TransferViewHolder(viewBinding)
                    else -> TransactionViewHolder<Transaction>(viewBinding)
                }.apply {
                    (this as TransactionViewHolder<T>).bind(transaction, isCalendarPermissionPermanentlyDeclined, prefHandler)
                }
    }
}