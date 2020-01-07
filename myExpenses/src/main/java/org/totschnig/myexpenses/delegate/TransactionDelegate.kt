package org.totschnig.myexpenses.delegate

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import icepick.Icepick
import org.threeten.bp.Instant
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.CrStatusAdapter
import org.totschnig.myexpenses.adapter.NothingSelectedSpinnerAdapter
import org.totschnig.myexpenses.adapter.OperationTypeAdapter
import org.totschnig.myexpenses.adapter.RecurrenceAdapter
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PreferenceUtils
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.DateButton
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import java.math.BigDecimal
import java.util.*

abstract class TransactionDelegate<T : Transaction>(val viewBinding: OneExpenseBinding, val dateEditBinding: DateEditBinding, val prefHandler: PrefHandler) : AdapterView.OnItemSelectedListener {
    private lateinit var methodSpinner: SpinnerHelper
    lateinit var accountSpinner: SpinnerHelper
    private lateinit var statusSpinner: SpinnerHelper
    private lateinit var operationTypeSpinner: SpinnerHelper
    lateinit var recurrenceSpinner: SpinnerHelper
    lateinit var accountsAdapter: SimpleCursorAdapter
    private lateinit var payeeAdapter: SimpleCursorAdapter
    private lateinit var methodsAdapter: ArrayAdapter<PaymentMethod>
    private lateinit var operationTypeAdapter: OperationTypeAdapter

    open val helpVariant: ExpenseEdit.HelpVariant
        get() = if (parentId == null) ExpenseEdit.HelpVariant.transaction else ExpenseEdit.HelpVariant.splitPartCategory
    open val title
        get() = if (parentId == null) R.string.menu_edit_transaction else R.string.menu_edit_split_part_category

    var isProcessingLinkedAmountInputs = false
    var originalAmountVisible = false
    var equivalentAmountVisible = false
    var originalCurrencyCode: String? = null
    var catId: Long? = null
    var accountId: Long? = null
    var methodId: Long? = null
    var parentId: Long? = null
    var rowId: Long? = null
    private var mPlan: Plan? = null

    protected var mAccounts = mutableListOf<Account>()

    private val planButton: DateButton
        get() = viewBinding.RR.PB.root as DateButton
    private val planExecutionButton: ToggleButton
        get() = viewBinding.RR.TB.root as ToggleButton

    open fun bind(transaction: T, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, recurrence: Plan.Recurrence?) {
        rowId = transaction.id
        parentId = transaction.parentId
        accountId = transaction.accountId
        methodSpinner = SpinnerHelper(viewBinding.Method)
        accountSpinner = SpinnerHelper(viewBinding.Account)
        statusSpinner = SpinnerHelper(viewBinding.Status)
        recurrenceSpinner = SpinnerHelper(viewBinding.RR.Recurrence.Recurrence)
        operationTypeSpinner = SpinnerHelper(viewBinding.toolbar.OperationType)
        viewBinding.toolbar.OperationType.visibility = View.VISIBLE
        viewBinding.Amount.setFractionDigits(transaction.amount.currencyUnit.fractionDigits())
/*        if (mOperationType == TransactionsContract.Transactions.TYPE_SPLIT) {
            amountInput.addTextChangedListener(object : MyTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    updateSplitBalance()
                }
            })
        }*/

/*        if (isSplitPart) {
            disableAccountSpinner()
        }*/
        val mIsMainTransactionOrTemplate = /*mOperationType != TransactionsContract.Transactions.TYPE_TRANSFER &&*/ !transaction.isSplitPart
        val mIsMainTransaction = mIsMainTransactionOrTemplate && transaction !is Template
        val mIsMainTemplate = transaction is Template && !transaction.isSplitPart()

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
                transaction.status = DatabaseConstants.STATUS_UNCOMMITTED
            } else { //Transfer
                transaction.status = DatabaseConstants.STATUS_UNCOMMITTED
            }*/
        } else { //Transfer or Transaction, we can suggest to create a plan
            if (!isCalendarPermissionPermanentlyDeclined) { //we set adapter even if spinner is not immediately visible, since it might become visible
//after SAVE_AND_NEW action
                val recurrenceAdapter = RecurrenceAdapter(context,
                        Plan.Recurrence.ONETIME, Plan.Recurrence.CUSTOM)
                recurrenceSpinner.adapter = recurrenceAdapter
                recurrence?.let {
                    recurrenceSpinner.setSelection(
                            recurrenceAdapter.getPosition(it))
                }
                recurrenceSpinner.setOnItemSelectedListener(this)
                setPlannerRowVisibility(View.VISIBLE)
                if (transaction.originTemplate != null && transaction.originTemplate.plan != null) {
                    recurrenceSpinner.spinner.visibility = View.GONE
                    planButton.visibility = View.VISIBLE
                    planButton.text = Plan.prettyTimeInfo(context,
                            transaction.originTemplate.plan.rrule, transaction.originTemplate.plan.dtstart)
                    planButton.setOnClickListener {
                        currentAccount()?.let {
                            (context as ExpenseEdit).showPlanMonthFragment(transaction.originTemplate, it.color)
                        }
                    }
                }
            }
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
/*        if (isNoMainTransaction) {
            rootBinding.DateTimeRow.visibility = View.GONE
        }*/
        //when we have a savedInstance, fields have already been populated
        //if (!mSavedInstance) {
        populateFields(transaction, prefHandler)
        if (true /*!isSplitPart*/) {
            setLocalDateTime(transaction)
        }
        //}
        //after setLocalDateTime, so that the plan info can override the date
        configurePlan()
        if (originalAmountVisible) {
            showOriginalAmount()
        }
        if (equivalentAmountVisible) {
            showEquivalentAmount()
        }
        createAdapters(newInstance, transaction)
    }

    protected fun hideRowsSpecificToMain() {
        viewBinding.PayeeRow.visibility = View.GONE
        viewBinding.MethodRow.visibility = View.GONE
    }

    private fun setLocalDateTime(transaction: Transaction) {
        val zonedDateTime = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(transaction.date), ZoneId.systemDefault())
        val localDate = zonedDateTime.toLocalDate()
        if (transaction is Template) {
            planButton.setDate(localDate)
        } else {
            dateEditBinding.DateButton.setDate(localDate)
            dateEditBinding.DateButton.setDate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(transaction.valueDate),
                    ZoneId.systemDefault()).toLocalDate())
            dateEditBinding.TimeButton.time = zonedDateTime.toLocalTime()
        }
    }

    fun setPlannerRowVisibility(visibility: Int) {
        viewBinding.PlanRow.visibility = visibility
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

    fun fillAmount(amount: BigDecimal) {
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

    protected fun setVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    protected fun addCurrencyToInput(label: TextView, amountInput: AmountInput, symbol: String, textResId: Int) {
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

    fun setMethodSelection(methodId: Long) {
        this.methodId = methodId
        setMethodSelection()
    }

    fun setMethodSelection() {
        if (methodId != 0L) {
            var found = false
            for (i in 0 until methodsAdapter.count) {
                val pm = methodsAdapter.getItem(i)
                if (pm != null) {
                    if (pm.id() == methodId) {
                        methodSpinner.setSelection(i + 1)
                        found = true
                        break
                    }
                }
            }
            if (!found) {
                methodSpinner.setSelection(0)
            }
        } else {
            methodSpinner.setSelection(0)
        }
        setVisibility(viewBinding.ClearMethod, methodId != 0L)
        setReferenceNumberVisibility()
    }

    private fun setReferenceNumberVisibility() {
/*        if (mTransaction is Template) {
            return
        }*/
        //ignore first row "select" merged in
        val position = methodSpinner.selectedItemPosition
        if (position > 0) {
            val pm = methodsAdapter.getItem(position - 1)
            viewBinding.Number.visibility = if (pm != null && pm.isNumbered) View.VISIBLE else View.INVISIBLE
        } else {
            viewBinding.Number.visibility = View.GONE
        }
    }

    val context: Context
        get() = viewBinding.root.context

    val host: ExpenseEdit
        get() = context as ExpenseEdit

    open fun createAdapters(newInstance: Boolean, transaction: Transaction) {
        createPayeeAdapter(newInstance)
        createMethodAdapter()
        createAccountAdapter()
        createStatusAdapter(transaction)
        createOperationTypeAdapter()
    }

    protected fun createOperationTypeAdapter() {
        val allowedOperationTypes: MutableList<Int> = ArrayList()
        allowedOperationTypes.add(TransactionsContract.Transactions.TYPE_TRANSACTION)
        allowedOperationTypes.add(TransactionsContract.Transactions.TYPE_TRANSFER)
        if (parentId == null) {
            allowedOperationTypes.add(TransactionsContract.Transactions.TYPE_SPLIT)
        }
        operationTypeAdapter = OperationTypeAdapter(context, allowedOperationTypes,
                false /*TODO isNewTemplate*/, parentId != null)
        operationTypeSpinner.adapter = operationTypeAdapter
        resetOperationType()
        operationTypeSpinner.setOnItemSelectedListener(this)
    }

    protected fun createStatusAdapter(transaction: Transaction) {
        val sAdapter: CrStatusAdapter = object : CrStatusAdapter(context) {
            override fun isEnabled(position: Int): Boolean { //if the transaction is reconciled, the status can not be changed
    //otherwise only unreconciled and cleared can be set
                return transaction.crStatus != Transaction.CrStatus.RECONCILED && position != Transaction.CrStatus.RECONCILED.ordinal
            }
        }
        statusSpinner.adapter = sAdapter
    }

    protected fun createAccountAdapter() {
        accountsAdapter = SimpleCursorAdapter(context, android.R.layout.simple_spinner_item, null, arrayOf(DatabaseConstants.KEY_LABEL), intArrayOf(android.R.id.text1), 0)
        accountsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        accountSpinner.adapter = accountsAdapter
    }

    private fun createMethodAdapter() {
        methodsAdapter = object : ArrayAdapter<PaymentMethod>(context, android.R.layout.simple_spinner_item) {
            override fun getItemId(position: Int): Long {
                return getItem(position)?.id() ?: 0L
            }
        }
        methodsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        methodSpinner.adapter = NothingSelectedSpinnerAdapter(
                methodsAdapter,
                android.R.layout.simple_spinner_item,  // R.layout.contact_spinner_nothing_selected_dropdown, // Optional
                context)
    }

    private fun createPayeeAdapter(newInstance: Boolean) {
        payeeAdapter = SimpleCursorAdapter(context, R.layout.support_simple_spinner_dropdown_item, null, arrayOf(DatabaseConstants.KEY_PAYEE_NAME), intArrayOf(android.R.id.text1),
                0)
        viewBinding.Payee.setAdapter(payeeAdapter)
        payeeAdapter.filterQueryProvider = FilterQueryProvider { constraint: CharSequence? ->
            var selection: String? = null
            var selectArgs = arrayOfNulls<String>(0)
            if (constraint != null) {
                val search = Utils.esacapeSqlLikeExpression(Utils.normalize(constraint.toString()))
                //we accept the string at the beginning of a word
                selection = DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED + " LIKE ? OR " +
                        DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED + " LIKE ? OR " +
                        DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED + " LIKE ?"
                selectArgs = arrayOf("$search%", "% $search%", "%.$search%")
            }
            context.contentResolver.query(
                    TransactionProvider.PAYEES_URI, arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PAYEE_NAME),
                    selection, selectArgs, null)
        }
        payeeAdapter.stringConversionColumn = 1
        viewBinding.Payee.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val c = payeeAdapter.getItem(position) as Cursor
            if (c.moveToPosition(position)) {
                val payeeId = c.getLong(0)
                payeeId.let {
                    if (newInstance /*&& mOperationType != TransactionsContract.Transactions.TYPE_SPLIT*/) { //moveToPosition should not be necessary,
    //but has been reported to not be positioned correctly on samsung GT-I8190N
                        if (prefHandler.getBoolean(PrefKey.AUTO_FILL_HINT_SHOWN, false)) {
                            if (PreferenceUtils.shouldStartAutoFill()) {
                                //TODO//startAutoFill(it, false)
                            }
                        } else {
                            val b = Bundle()
                            b.putLong(DatabaseConstants.KEY_ROWID, it)
                            b.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.dialog_title_information)
                            b.putString(ConfirmationDialogFragment.KEY_MESSAGE, context.getString(R.string.hint_auto_fill))
                            b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.AUTO_FILL_COMMAND)
                            b.putString(ConfirmationDialogFragment.KEY_PREFKEY,
                                    prefHandler.getKey(PrefKey.AUTO_FILL_HINT_SHOWN))
                            b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.yes)
                            b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.no)
                            ConfirmationDialogFragment.newInstance(b).show((context as FragmentActivity).supportFragmentManager,
                                    "AUTO_FILL_HINT")
                        }
                    }
                }

            }
        }
    }

    fun resetOperationType() {
        operationTypeSpinner.setSelection(operationTypeAdapter.getPosition(operationType))
    }

    fun setMethods(paymentMethods: List<PaymentMethod>?) {
        if (paymentMethods == null || paymentMethods.isEmpty()) {
            viewBinding.MethodRow.visibility = View.GONE
        } else {
            viewBinding.MethodRow.visibility = View.VISIBLE
            methodsAdapter.clear()
            methodsAdapter.addAll(paymentMethods)
            setMethodSelection()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val host = context as ExpenseEdit
        if (parent.id != R.id.OperationType) {
            host.setDirty()
        }
        when (parent.id) {
            R.id.Recurrence -> {
                var visibility = View.GONE
                if (id > 0) {
                    if (PermissionHelper.PermissionGroup.CALENDAR.hasPermission(context)) {
                        val newSplitTemplateEnabled = prefHandler.getBoolean(PrefKey.NEW_SPLIT_TEMPLATE_ENABLED, true)
                        val newPlanEnabled = prefHandler.getBoolean(PrefKey.NEW_PLAN_ENABLED, true)
                        if (newPlanEnabled && (newSplitTemplateEnabled /*|| mOperationType != TransactionsContract.Transactions.TYPE_SPLIT || mTransaction is Template*/)) {
                            visibility = View.VISIBLE
                            showCustomRecurrenceInfo()
                        } else {
                            recurrenceSpinner.setSelection(0)
                            val contribFeature = if (newSplitTemplateEnabled /*|| mOperationType != TransactionsContract.Transactions.TYPE_SPLIT*/) ContribFeature.PLANS_UNLIMITED else ContribFeature.SPLIT_TEMPLATE
                            host.showContribDialog(contribFeature, null)
                        }
                    } else {
                        host.requestPermission(PermissionHelper.PermissionGroup.CALENDAR)
                    }
                }
/*                if (mTransaction is Template) {
                    planButton.visibility = visibility
                    planExecutionButton.visibility = visibility
                }*/
            }
            R.id.Method -> {
                val hasSelection = position > 0
                if (hasSelection) {
                    methodId = parent.selectedItemId
                    if (methodId!! <= 0) {
                        methodId = null
                    }
                } else {
                    methodId = null
                }
                setVisibility(viewBinding.ClearMethod, hasSelection)
                setReferenceNumberVisibility()
            }
            R.id.Account -> {
                val account = mAccounts[position]
/*                if (mOperationType == TransactionsContract.Transactions.TYPE_SPLIT) {
                    val splitPartList = findSplitPartList()
                    if (splitPartList != null && splitPartList.splitCount > 0) { //call background task for moving parts to new account
                        startTaskExecution(
                                TaskExecutionFragment.TASK_MOVE_UNCOMMITED_SPLIT_PARTS, arrayOf(mRowId),
                                account.id,
                                R.string.progress_dialog_updating_split_parts)
                        return
                    }
                }*/
                updateAccount(account)
            }
            R.id.OperationType -> {
                host.discoveryHelper.markDiscovered(DiscoveryHelper.Feature.OPERATION_TYPE_SELECT)
                val newType = operationTypeSpinner.getItemAtPosition(position) as Int
                if (host.isValidType(newType)) {
                    if (newType == TransactionsContract.Transactions.TYPE_TRANSFER && !checkTransferEnabled()) { //reset to previous
                        resetOperationType()
                    } else if (newType == TransactionsContract.Transactions.TYPE_SPLIT) {
                        resetOperationType()
/*                        if (mTransaction is Template) {
                            if (PrefKey.NEW_SPLIT_TEMPLATE_ENABLED.getBoolean(true)) {
                                restartWithType(Transactions.TYPE_SPLIT)
                            } else {
                                showContribDialog(ContribFeature.SPLIT_TEMPLATE, null)
                            }
                        } else {*/
                        host.contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION, null)
                        //}
                    } else {
                        host.restartWithType(newType)
                    }
                }
            }
        }
    }

    private fun checkTransferEnabled(): Boolean {
        if (currentAccount() == null) return false
        if (mAccounts.size <= 1) {
            (context as ExpenseEdit).showMessage(R.string.dialog_command_disabled_insert_transfer)
            return false
        }
        return true
    }

    private fun showCustomRecurrenceInfo() {
        if (recurrenceSpinner.selectedItem === Plan.Recurrence.CUSTOM) {
            (context as ExpenseEdit).showSnackbar(R.string.plan_custom_recurrence_info, Snackbar.LENGTH_LONG)
        }
    }

    fun setupListeners(watcher: TextWatcher) {
        viewBinding.Comment.addTextChangedListener(watcher)
        viewBinding.Title.addTextChangedListener(watcher)
        viewBinding.Payee.addTextChangedListener(watcher)
        viewBinding.Number.addTextChangedListener(watcher)
        accountSpinner.setOnItemSelectedListener(this)
        methodSpinner.setOnItemSelectedListener(this)
        statusSpinner.setOnItemSelectedListener(this)
    }

    fun linkInputsWithLabels() {
        linkAccountLabels()
        with(host) {
            linkInputWithLabel(viewBinding.Title, viewBinding.TitleLabel)
            linkInputWithLabel(dateEditBinding.DateButton, viewBinding.DateTimeLabel)
            linkInputWithLabel(viewBinding.Payee, viewBinding.PayeeLabel)
            with(viewBinding.CommentLabel) {
                linkInputWithLabel(statusSpinner.spinner, this)
                linkInputWithLabel(viewBinding.AttachImage, this)
                linkInputWithLabel(viewBinding.PictureContainer.root, this)
                linkInputWithLabel(viewBinding.Comment, this)
            }
            linkInputWithLabel(viewBinding.Category, viewBinding.CategoryLabel)
            linkInputWithLabel(methodSpinner.spinner, viewBinding.MethodLabel)
            linkInputWithLabel(viewBinding.Number, viewBinding.MethodLabel)
            linkInputWithLabel(viewBinding.RR.PB.root, viewBinding.PlanLabel)
            linkInputWithLabel(recurrenceSpinner.spinner, viewBinding.PlanLabel)
            linkInputWithLabel(viewBinding.RR.TB.root, viewBinding.PlanLabel)
            linkInputWithLabel(viewBinding.TransferAmount, viewBinding.TransferAmountLabel)
            linkInputWithLabel(viewBinding.OriginalAmount, viewBinding.OriginalAmountLabel)
            linkInputWithLabel(viewBinding.EquivalentAmount, viewBinding.EquivalentAmountLabel)
        }
    }

    /**
     * @return true for income, false for expense
     */
    protected val isIncome: Boolean
        get() = viewBinding.Amount.type

    open fun linkAccountLabels() {
        with(host) {
            linkInputWithLabel(accountSpinner.spinner, viewBinding.AccountLabel)
        }
    }

    private fun readZonedDateTime(dateEdit: DateButton): ZonedDateTime {
        return ZonedDateTime.of(dateEdit.date,
                if (dateEditBinding.TimeButton.visibility == View.VISIBLE) dateEditBinding.TimeButton.time else LocalTime.now(),
                ZoneId.systemDefault())
    }

    fun currentAccount() = getAccountFromSpinner(accountSpinner)

    protected fun getAccountFromSpinner(spinner: SpinnerHelper): Account? {
        val selected = spinner.selectedItemPosition
        if (selected == AdapterView.INVALID_POSITION) {
            return null
        }
        val selectedID = spinner.selectedItemId
        for (account in mAccounts) {
            if (account.id == selectedID) {
                return account
            }
        }
        return null
    }

    abstract fun buildTransaction(forSave: Boolean, currencyContext: CurrencyContext): T?
    abstract val operationType: Int

    open fun syncStateAndValidate(forSave: Boolean, currencyContext: CurrencyContext, pictureUri: Uri?): Transaction? {
        val title: String

       return buildTransaction(forSave, currencyContext)?.apply {
            val currentAccount = currentAccount()!!
            id = rowId
            accountId = currentAccount.id
            comment = viewBinding.Comment.text.toString()
            //TODO
            if (true/*!isNoMainTransaction*/) {
                val transactionDate = readZonedDateTime(dateEditBinding.DateButton)
                setDate(transactionDate)
                if (dateEditBinding.Date2Button.visibility == View.VISIBLE) {
                    setValueDate(if (dateEditBinding.Date2Button.visibility == View.VISIBLE) readZonedDateTime(dateEditBinding.Date2Button) else transactionDate)
                }
            }
            if (false /*mIsMainTemplate*/) {
                /*title = rootBinding.Title.text.toString()
                if (title == "") {
                    if (forSave) {
                        rootBinding.Title.error = getString(R.string.no_title_given)
                    }
                    validP = false
                }
                (mTransaction as Template?)!!.title = title
                val description = mTransaction!!.compileDescription(this@TransactionEdit, currencyFormatter)
                if (mPlan == null) {
                    if (mRecurrenceSpinner.selectedItemPosition > 0) {
                        mPlan = Plan(
                                planButton.date,
                                mRecurrenceSpinner.selectedItem as Recurrence,
                                (mTransaction as Template?)!!.title,
                                description)
                        (mTransaction as Template?)!!.plan = mPlan
                    }
                } else {
                    mPlan!!.description = description
                    mPlan!!.title = title
                    (mTransaction as Template?)!!.plan = mPlan
                }*/
            } else {
                referenceNumber = viewBinding.Number.text.toString()
                if (forSave && !isSplitPart) {
                    if (recurrenceSpinner.selectedItemPosition > 0) {
                        setInitialPlan(Pair.create(recurrenceSpinner.selectedItem as Plan.Recurrence, dateEditBinding.DateButton.date))
                    }
                }
            }
            crStatus = (statusSpinner.selectedItem as Transaction.CrStatus)
            this.pictureUri = pictureUri
        }
    }

    protected fun validateAmountInput(showToUser: Boolean): BigDecimal? {
        return validateAmountInput(viewBinding.Amount, showToUser)
    }

    protected open fun validateAmountInput(input: AmountInput, showToUser: Boolean): BigDecimal? {
        return input.getTypedValue(true, showToUser)
    }

    private fun configureAccountDependent(account: Account?) {
        val currencyUnit = account!!.currencyUnit
        addCurrencyToInput(viewBinding.AmountLabel, viewBinding.Amount, currencyUnit.symbol(), R.string.amount)
        viewBinding.OriginalAmount.configureExchange(currencyUnit)
        if (hasHomeCurrency(account)) {
            viewBinding.EquivalentAmountRow.visibility = View.GONE
            equivalentAmountVisible = false
        } else {
            viewBinding.EquivalentAmount.configureExchange(currencyUnit, Utils.getHomeCurrency())
        }
        configureDateInput(account)
    }

    private fun hasHomeCurrency(account: Account): Boolean {
        return account.currencyUnit == Utils.getHomeCurrency()
    }

    private fun configureDateInput(account: Account?) {
        val dateMode = UiUtils.getDateMode(account, prefHandler)
        setVisibility(dateEditBinding.TimeButton, dateMode == UiUtils.DateMode.DATE_TIME)
        setVisibility(dateEditBinding.Date2Button, dateMode == UiUtils.DateMode.BOOKING_VALUE)
        setVisibility(dateEditBinding.DateLink, dateMode == UiUtils.DateMode.BOOKING_VALUE)
        var dateLabel: String
        if (dateMode == UiUtils.DateMode.BOOKING_VALUE) {
            dateLabel = context.getString(R.string.booking_date) + "/" + context.getString(R.string.value_date)
        } else {
            dateLabel = context.getString(R.string.date)
            if (dateMode == UiUtils.DateMode.DATE_TIME) {
                dateLabel += " / " + context.getString(R.string.time)
            }
        }
        viewBinding.DateTimeLabel.text = dateLabel
    }

    open fun setAccounts(data: Cursor, currencyExtra: String?) {
        accountsAdapter.swapCursor(data)
/*                if (didUserSetAccount) {
                    mTransaction!!.accountId = mAccountId
                    if (mOperationType == Transactions.TYPE_TRANSFER) {
                        mTransaction!!.transferAccountId = mTransferAccountId
                    }
                }*/
        data.moveToFirst()
        var selectionSet = false
        while (!data.isAfterLast) {
            val position = data.position
            val a = Account.fromCursor(data)
            mAccounts.add(a)
            if (!selectionSet &&
                    (a.currencyUnit.code() == currencyExtra ||
                            currencyExtra == null && a.id == accountId)) {
                accountSpinner.setSelection(position)
                configureAccountDependent(a)
                selectionSet = true
            }
            data.moveToNext()
        }
        //if the accountId we have been passed does not exist, we select the first entry
        if (accountSpinner.selectedItemPosition == AdapterView.INVALID_POSITION) {
            accountSpinner.setSelection(0)
            accountId = mAccounts[0].id
            configureAccountDependent(mAccounts[0])
        }
        viewBinding.Amount.setTypeEnabled(true)
        configureType()
        configureStatusSpinner()
    }

    private fun configureStatusSpinner() {
        val a = currentAccount()
        setVisibility(statusSpinner.spinner,
                /*TODO !isNoMainTransaction && */ a != null && a.type != AccountType.CASH)
    }

    open fun updateAccount(account: Account) {
        //didUserSetAccount = true
        accountId = account.id
        configureAccountDependent(account)
        configureStatusSpinner()
        viewBinding.Amount.setFractionDigits(account.currencyUnit.fractionDigits())
    }

    open fun configureType() {
        viewBinding.PayeeLabel.setText(if (viewBinding.Amount.type) R.string.payer else R.string.payee)
        /*  if (mOperationType == TransactionsContract.Transactions.TYPE_SPLIT) {
              updateSplitBalance()
          }*/
        //setCategoryButton()
    }

    private fun configurePlan() {
        mPlan?.let { plan ->
            planButton.text = Plan.prettyTimeInfo(context, plan.rrule, plan.dtstart)
            if (viewBinding.Title.text.toString() == "") viewBinding.Title.setText(plan.title)
            planExecutionButton.visibility = View.VISIBLE
            recurrenceSpinner.spinner.visibility = View.GONE
            planButton.visibility = View.VISIBLE
            /*pObserver = PlanObserver().also {
                contentResolver.registerContentObserver(
                        ContentUris.withAppendedId(CalendarContractCompat.Events.CONTENT_URI, plan.id),
                        false, it)*/
        }
    }

    open fun onSaveInstanceState(outState: Bundle) {
        val methodId = methodSpinner.selectedItemId
        if (methodId > 0) {
            this.methodId = methodId
        }
/*        if (didUserSetAccount) {
            val accountId = accountSpinner.selectedItemId
            if (accountId != AdapterView.INVALID_ROW_ID) {
                mAccountId = accountId
            }
        }*/
        val originalInputSelectedCurrency = viewBinding.OriginalAmount.selectedCurrency
        if (originalInputSelectedCurrency != null) {
            originalCurrencyCode = originalInputSelectedCurrency.code()
        }
        Icepick.saveInstanceState(this, outState)
    }

    private fun disableAccountSpinner() {
        accountSpinner.isEnabled = false
    }

    fun setPicture(pictureUri: Uri?) {
        if (pictureUri != null) {
            viewBinding.PictureContainer.root.visibility = View.VISIBLE
            Picasso.get().load(pictureUri).fit().into(viewBinding.PictureContainer.picture)
            viewBinding.AttachImage.visibility = View.GONE
        } else {
            viewBinding.AttachImage.visibility = View.VISIBLE
            viewBinding.PictureContainer.root.visibility = View.GONE
        }
    }

    companion object {
        fun <T : Transaction> createAndBind(transaction: T, viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, isCalendarPermissionPermanentlyDeclined: Boolean, prefHandler: PrefHandler, newInstance: Boolean, recurrence: Plan.Recurrence?) =
                when (transaction) {
                    is Transfer -> TransferDelegate(viewBinding, dateEditBinding, prefHandler)
                    is SplitTransaction -> SplitDelegate(viewBinding, dateEditBinding, prefHandler)
                    else -> CategoryDelegate(viewBinding, dateEditBinding, prefHandler)
                }.apply {
                    (this as TransactionDelegate<T>).bind(transaction, isCalendarPermissionPermanentlyDeclined, newInstance, recurrence)
                }
    }
}