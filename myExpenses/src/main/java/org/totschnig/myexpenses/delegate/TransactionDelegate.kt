package org.totschnig.myexpenses.delegate

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import icepick.Icepick
import icepick.State
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.AccountAdapter
import org.totschnig.myexpenses.adapter.CrStatusAdapter
import org.totschnig.myexpenses.adapter.NothingSelectedSpinnerAdapter
import org.totschnig.myexpenses.adapter.RecurrenceAdapter
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.DateButton
import org.totschnig.myexpenses.ui.MyTextWatcher
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.TextUtils.appendCurrencyDescription
import org.totschnig.myexpenses.util.TextUtils.appendCurrencySymbol
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.addChipsBulk
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.math.BigDecimal
import java.util.*

abstract class TransactionDelegate<T : ITransaction>(
        val viewBinding: OneExpenseBinding, private val dateEditBinding: DateEditBinding, private val methodRowBinding: MethodRowBinding,
        val prefHandler: PrefHandler, val isTemplate: Boolean) : AdapterView.OnItemSelectedListener {

    private val methodSpinner = SpinnerHelper(methodRowBinding.Method.root)
    val accountSpinner = SpinnerHelper(viewBinding.Account)
    private val statusSpinner = SpinnerHelper(viewBinding.Status)
    private val operationTypeSpinner = SpinnerHelper(viewBinding.toolbar.OperationType)
    val recurrenceSpinner = SpinnerHelper(viewBinding.Recurrence)
    lateinit var accountsAdapter: AccountAdapter
    private lateinit var methodsAdapter: ArrayAdapter<PaymentMethod>
    private lateinit var operationTypeAdapter: ArrayAdapter<OperationType>

    init {
        createAccountAdapter()
        createMethodAdapter()
        viewBinding.advanceExecutionSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seekBar.requestFocusFromTouch() //prevent jump to first EditText https://stackoverflow.com/a/6177270/1199911
                viewBinding.advanceExecutionValue.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    open val helpVariant: ExpenseEdit.HelpVariant
        get() = when {
            isTemplate -> ExpenseEdit.HelpVariant.templateCategory
            isSplitPart -> ExpenseEdit.HelpVariant.splitPartCategory
            else -> ExpenseEdit.HelpVariant.transaction
        }
    open val title
        get() = with(context) {
            when {
                isTemplate -> getString(R.string.menu_edit_template) + " (" + getString(typeResId) + ")"
                isSplitPart -> getString(editPartResId)
                else -> getString(editResId)
            }
        }
    open val typeResId = R.string.transaction
    open val editResId = R.string.menu_edit_transaction
    open val editPartResId = R.string.menu_edit_split_part_category

    private val isMainTransaction: Boolean
        get() = !isSplitPart && !isTemplate
    open val shouldAutoFill
        get() = !isTemplate

    val isSplitPart
        get() = parentId != null
    private val isMainTemplate
        get() = isTemplate && !isSplitPart

    var isProcessingLinkedAmountInputs = false

    @JvmField
    @State
    var originalAmountVisible = false

    @JvmField
    @State
    var equivalentAmountVisible = false

    @JvmField
    @State
    var originalCurrencyCode: String? = null

    @JvmField
    @State
    var accountId: Long? = null

    @JvmField
    @State
    var methodId: Long? = null

    @JvmField
    @State
    var pictureUri: Uri? = null

    @JvmField
    @State
    var _crStatus: CrStatus? = CrStatus.UNRECONCILED

    @JvmField
    @State
    var parentId: Long? = null

    @JvmField
    @State
    var rowId: Long = 0L

    @JvmField
    @State
    var planId: Long? = null

    @JvmField
    @State
    var originTemplateId: Long? = null

    @JvmField
    @State
    var uuid: String? = null

    val crStatus
        get() = _crStatus ?: CrStatus.UNRECONCILED

    protected var mAccounts = mutableListOf<Account>()

    private val planButton: DateButton
        get() = viewBinding.PB
    private val planExecutionButton: CompoundButton
        get() = viewBinding.TB

    fun bindUnsafe(transaction: ITransaction?, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, savedInstanceState: Bundle?, recurrence: Plan.Recurrence?, withAutoFill: Boolean) {
        bind(transaction as T?, isCalendarPermissionPermanentlyDeclined, newInstance, savedInstanceState, recurrence, withAutoFill)
    }

    open fun bind(transaction: T?, isCalendarPermissionPermanentlyDeclined: Boolean, newInstance: Boolean, savedInstanceState: Bundle?, recurrence: Plan.Recurrence?, withAutoFill: Boolean) {
        if (transaction != null) {
            rowId = transaction.id
            parentId = transaction.parentId
            accountId = transaction.accountId
            methodId = transaction.methodId
            setPicture(transaction.pictureUri)
            planId = (transaction as? Template)?.plan?.id
            _crStatus = transaction.crStatus
            originTemplateId = transaction.originTemplateId
            uuid = transaction.uuid
            //Setting this early instead of waiting for call to setAccounts
            //works around a bug in some legacy virtual keyboards where configuring the
            //editText too late corrupt inputType
            viewBinding.Amount.setFractionDigits(transaction.amount.currencyUnit.fractionDigits)
        } else {
            Icepick.restoreInstanceState(this, savedInstanceState)
        }
        setVisibility(viewBinding.toolbar.OperationType, newInstance)
        originTemplateId?.let { host.loadOriginTemplate(it) }
        if (isSplitPart) {
            disableAccountSpinner()
            viewBinding.TagRow.visibility = View.GONE
        }

        if (isMainTemplate) {
            viewBinding.TitleRow.visibility = View.VISIBLE
            viewBinding.DefaultActionRow.visibility = View.VISIBLE
            if (!isCalendarPermissionPermanentlyDeclined) { //if user has denied access and checked that he does not want to be asked again, we do not
//bother him with a button that is not working
                setPlannerRowVisibility(true)
                val recurrenceAdapter = RecurrenceAdapter(context)
                recurrenceSpinner.adapter = recurrenceAdapter
                recurrenceSpinner.setOnItemSelectedListener(this)
                planButton.setOnClickListener {
                    planId?.let {
                        host.launchPlanView(false, it)
                    } ?: run {
                        planButton.onClick()
                    }
                }
            }
            viewBinding.AttachImage.visibility = View.GONE
        } else if (!isSplitPart) {
            if (!isCalendarPermissionPermanentlyDeclined) { //we set adapter even if spinner is not immediately visible, since it might become visible
//after SAVE_AND_NEW action
                val recurrenceAdapter = RecurrenceAdapter(context)
                recurrenceSpinner.adapter = recurrenceAdapter
                if (missingRecurrenceFeature() == null) {
                    recurrence?.let {
                        recurrenceSpinner.setSelection(
                                recurrenceAdapter.getPosition(it))
                    }
                }
                recurrenceSpinner.setOnItemSelectedListener(this)
            }
        }
        if (isSplitPart || isTemplate) {
            viewBinding.DateTimeRow.visibility = View.GONE
        }

        createAdapters(newInstance, withAutoFill)

        //when we have a savedInstance, fields have already been populated
        if (savedInstanceState == null) {
            isProcessingLinkedAmountInputs = true
            populateFields(transaction!!, withAutoFill)
            isProcessingLinkedAmountInputs = false
            if (!isSplitPart) {
                setLocalDateTime(transaction)
            }
        } else {
            configurePicture()

            populateStatusSpinner()
        }
        viewBinding.Amount.visibility = View.VISIBLE
        //}
        //after setLocalDateTime, so that the plan info can override the date
        configurePlan((transaction as? Template)?.plan)
        configureLastDayButton()

        viewBinding.Amount.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                viewBinding.EquivalentAmount.setCompoundResultInput(viewBinding.Amount.validate(false))
            }
        })
        viewBinding.OriginalAmount.setCompoundResultOutListener { amount: BigDecimal -> viewBinding.Amount.setAmount(amount, false) }

        if (originalAmountVisible) {
            showOriginalAmount()
        }
        if (equivalentAmountVisible) {
            showEquivalentAmount()
        }
    }

    protected fun hideRowsSpecificToMain() {
        viewBinding.PayeeRow.visibility = View.GONE
        methodRowBinding.MethodRow.visibility = View.GONE
    }

    private fun setLocalDateTime(transaction: ITransaction) {
        val zonedDateTime = epoch2ZonedDateTime(transaction.date)
        val localDate = zonedDateTime.toLocalDate()
        if (transaction is Template) {
            planButton.setDate(localDate)
        } else {
            dateEditBinding.DateButton.setDate(localDate)
            dateEditBinding.Date2Button.setDate(epoch2ZonedDateTime(transaction.valueDate).toLocalDate())
            dateEditBinding.TimeButton.setTime(zonedDateTime.toLocalTime())
        }
    }

    private fun setPlannerRowVisibility(visibility: Boolean) {
        setVisibility(viewBinding.PlanRow, visibility)
    }

    /**
     * populates the input fields with a transaction from the database or a new one
     */
    open fun populateFields(transaction: T, withAutoFill: Boolean) {
        populateStatusSpinner()
        viewBinding.Comment.setText(transaction.comment)
        if (isMainTemplate) {
            (transaction as Template).let { template ->
                viewBinding.Title.setText(template.title)
                planExecutionButton.isChecked = template.isPlanExecutionAutomatic
                viewBinding.advanceExecutionSeek.progress = template.planExecutionAdvance
                viewBinding.DefaultAction.setSelection(template.defaultAction.ordinal)
            }
        } else {
            methodRowBinding.Number.setText(transaction.referenceNumber)
        }
        fillAmount(transaction.amount.amountMajor)
        transaction.originalAmount?.let {
            originalAmountVisible = true
            showOriginalAmount()
            viewBinding.OriginalAmount.setAmount(it.amountMajor)
            originalCurrencyCode = it.currencyUnit.code
        }
                ?: kotlin.run { originalCurrencyCode = prefHandler.getString(PrefKey.LAST_ORIGINAL_CURRENCY, null) }

        populateOriginalCurrency()
        transaction.equivalentAmount?.let {
            if (transaction.equivalentAmount != null) {
                equivalentAmountVisible = true
                viewBinding.EquivalentAmount.setAmount(it.amountMajor.abs())
            }
        }
        if (withAutoFill && isMainTemplate) {
            viewBinding.Title.requestFocus()
        }
    }

    private fun populateStatusSpinner() {
        statusSpinner.setSelection(crStatus.ordinal, false)
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

    private fun populateOriginalCurrency() {
        if (originalCurrencyCode != null) {
            viewBinding.OriginalAmount.setSelectedCurrency(originalCurrencyCode)
        }
    }

    protected fun setVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    protected fun addCurrencyToInput(label: TextView, amountInput: AmountInput, currencyUnit: CurrencyUnit, textResId: Int) {
        val text = appendCurrencySymbol(label.context, textResId, currencyUnit)
        label.text = text
        amountInput.contentDescription = appendCurrencyDescription(label.context, textResId, currencyUnit)
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
            if (validateAmountInput(viewBinding.EquivalentAmount, showToUser = false, ifPresent = true) == null && currentAccount != null) {
                val rate = BigDecimal(currentAccount.exchangeRate)
                viewBinding.EquivalentAmount.setExchangeRate(rate)
            }
            viewBinding.EquivalentAmount.requestFocus()
        } else {
            viewBinding.EquivalentAmount.clear()
        }
    }

    fun setMethodSelection(methodId: Long?) {
        this.methodId = methodId
        setMethodSelection()
    }

    fun setMethodSelection() {
        if (methodId != null) {
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
                methodId = null
                methodSpinner.setSelection(0)
            }
        } else {
            methodSpinner.setSelection(0)
        }
        setVisibility(methodRowBinding.ClearMethod.root, methodId != null)
        setReferenceNumberVisibility()
    }

    private fun setReferenceNumberVisibility() {
        if (isTemplate) return
        //ignore first row "select" merged in
        val position = methodSpinner.selectedItemPosition
        val visibility = if (position > 0) {
            val pm = methodsAdapter.getItem(position - 1)
            if (pm != null && pm.isNumbered) View.VISIBLE else View.GONE
        } else {
            View.GONE
        }
        (methodRowBinding.ReferenceNumberRow ?: methodRowBinding.Number).visibility = visibility
    }

    val context: Context
        get() = viewBinding.root.context

    val host: ExpenseEdit
        get() = context as ExpenseEdit

    abstract fun createAdapters(newInstance: Boolean, withAutoFill: Boolean)

    protected fun createOperationTypeAdapter() {
        val allowedOperationTypes: MutableList<Int> = ArrayList()
        allowedOperationTypes.add(TYPE_TRANSACTION)
        allowedOperationTypes.add(TYPE_TRANSFER)
        if (parentId == null) {
            allowedOperationTypes.add(TYPE_SPLIT)
        }
        val objects = allowedOperationTypes.map {
            OperationType(it).apply {
                label = context.getString(when (it) {
                TYPE_SPLIT -> if (isTemplate) R.string.menu_create_template_for_split else R.string.menu_create_split
                TYPE_TRANSFER -> if (isSplitPart) R.string.menu_create_split_part_transfer else if (isTemplate) R.string.menu_create_template_for_transfer else R.string.menu_create_transfer
                TYPE_TRANSACTION -> if (isSplitPart) R.string.menu_create_split_part_category else if (isTemplate) R.string.menu_create_template_for_transaction else R.string.menu_create_transaction
                else -> throw IllegalStateException("Unknown operationType $it")
            })
            }
        }
        operationTypeAdapter = ArrayAdapter<OperationType>(context, android.R.layout.simple_spinner_item, objects)
        operationTypeAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        operationTypeSpinner.adapter = operationTypeAdapter
        resetOperationType()
        operationTypeSpinner.setOnItemSelectedListener(this)
    }

    protected fun createStatusAdapter() {
        val sAdapter: CrStatusAdapter = object : CrStatusAdapter(context) {
            override fun isEnabled(position: Int): Boolean { //if the transaction is reconciled, the status can not be changed
                //otherwise only unreconciled and cleared can be set
                return _crStatus != CrStatus.RECONCILED && position != CrStatus.RECONCILED.ordinal
            }
        }
        statusSpinner.adapter = sAdapter
    }

    private fun createAccountAdapter() {
        accountsAdapter = AccountAdapter(context)
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

    fun resetOperationType() {
        operationTypeSpinner.setSelection(operationTypeAdapter.getPosition(OperationType(operationType)))
    }

    fun setMethods(paymentMethods: List<PaymentMethod>?) {
        if (paymentMethods == null || paymentMethods.isEmpty()) {
            methodId = null
            methodRowBinding.MethodRow.visibility = View.GONE
        } else {
            methodRowBinding.MethodRow.visibility = View.VISIBLE
            methodsAdapter.clear()
            methodsAdapter.addAll(paymentMethods)
            setMethodSelection()
        }
    }

    open fun missingRecurrenceFeature() = if (prefHandler.getBoolean(PrefKey.NEW_PLAN_ENABLED, true)) null else ContribFeature.PLANS_UNLIMITED

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        val host = context as ExpenseEdit
        if (parent.id != R.id.OperationType) {
            host.setDirty()
        }
        when (parent.id) {
            R.id.Recurrence -> {
                var planVisibility = false
                if (id > 0) {
                    if (PermissionHelper.PermissionGroup.CALENDAR.hasPermission(context)) {
                        missingRecurrenceFeature()?.let {
                            recurrenceSpinner.setSelection(0)
                            host.showContribDialog(it, null)
                        } ?: run {
                            planVisibility = true
                            showCustomRecurrenceInfo()
                            configureLastDayButton()
                        }
                    } else {
                        host.requestPermission(PermissionHelper.PermissionGroup.CALENDAR)
                    }
                }
                if (isTemplate) {
                    configurePlanDependents(planVisibility)
                }
            }
            R.id.Method -> {
                val hasSelection = position > 0
                methodId = if (hasSelection) parent.selectedItemId.takeIf { it > 0 } else null
                setVisibility(methodRowBinding.ClearMethod.root, hasSelection)
                setReferenceNumberVisibility()
            }
            R.id.Account -> {
                val account = mAccounts[position]
                updateAccount(account)
            }
            R.id.OperationType -> {
                val newType = (operationTypeSpinner.getItemAtPosition(position) as OperationType).type
                if (host.isValidType(newType)) {
                    if (newType == TYPE_TRANSFER && !checkTransferEnabled()) { //reset to previous
                        resetOperationType()
                    } else if (newType == TYPE_SPLIT) {
                        resetOperationType()
                        if (isTemplate) {
                            if (prefHandler.getBoolean(PrefKey.NEW_SPLIT_TEMPLATE_ENABLED, true)) {
                                host.restartWithType(newType)
                            } else {
                                host.contribFeatureRequested(ContribFeature.SPLIT_TEMPLATE, null)
                            }
                        } else {
                            host.contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION, null)
                        }
                    } else {
                        host.restartWithType(newType)
                    }
                }
            }
            R.id.Status -> {
                (parent.selectedItem as? CrStatus)?.let {
                    _crStatus = it
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
            (context as ExpenseEdit).showDismissibleSnackbar(R.string.plan_custom_recurrence_info)
        }
    }

    private val configuredDate: LocalDate
        get() = (if (isMainTemplate) planButton else dateEditBinding.DateButton).date

    fun configureLastDayButton() {
        val visible = recurrenceSpinner.selectedItem === Plan.Recurrence.MONTHLY && configuredDate.dayOfMonth > 28
        viewBinding.LastDay.isVisible = visible
        if (!visible) {
            viewBinding.LastDay.isChecked = false
        } else if (configuredDate.dayOfMonth == 31) {
            viewBinding.LastDay.isChecked = true
        }
    }

    fun setupListeners(watcher: TextWatcher) {
        viewBinding.Comment.addTextChangedListener(watcher)
        viewBinding.Title.addTextChangedListener(watcher)
        viewBinding.Payee.addTextChangedListener(watcher)
        methodRowBinding.Number.addTextChangedListener(watcher)
        accountSpinner.setOnItemSelectedListener(this)
        methodSpinner.setOnItemSelectedListener(this)
        statusSpinner.setOnItemSelectedListener(this)
    }

    /**
     * @return true for income, false for expense
     */
    protected val isIncome: Boolean
        get() = viewBinding.Amount.type

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

    //TODO make getTypedNewInstance return non-null
    protected fun buildTemplate(accountId: Long) = Template.getTypedNewInstance(operationType, accountId, false, parentId)!!

    abstract fun buildTransaction(forSave: Boolean, currencyContext: CurrencyContext, accountId: Long): T?
    abstract val operationType: Int

    open fun syncStateAndValidate(forSave: Boolean, currencyContext: CurrencyContext): T? {
        return currentAccount()?.let { buildTransaction(forSave && !isMainTemplate, currencyContext, it.id) }?.apply {
            originTemplateId = this@TransactionDelegate.originTemplateId
            uuid = this@TransactionDelegate.uuid
            id = rowId
            if (isSplitPart) {
                status = DatabaseConstants.STATUS_UNCOMMITTED
            }
            comment = viewBinding.Comment.text.toString()
            if (isMainTransaction) {
                val transactionDate = readZonedDateTime(dateEditBinding.DateButton)
                setDate(transactionDate)
                setValueDate(if (dateEditBinding.Date2Button.visibility == View.VISIBLE) readZonedDateTime(dateEditBinding.Date2Button) else transactionDate)
            }
            if (isMainTemplate) {
                (this as Template).apply {
                    viewBinding.Title.text.toString().let {
                        if (it == "") {
                            if (forSave) {
                                viewBinding.Title.error = context.getString(R.string.no_title_given)
                                return null
                            }
                        }
                        this.title = it
                    }
                    this.isPlanExecutionAutomatic = planExecutionButton.isChecked
                    this.planExecutionAdvance = viewBinding.advanceExecutionSeek.progress
                    val description = compileDescription(context.applicationContext as MyApplication)
                    if (recurrenceSpinner.selectedItemPosition > 0 || this@TransactionDelegate.planId != null) {
                        plan = Plan(
                                this@TransactionDelegate.planId ?: 0L,
                                planButton.date,
                                selectedRecurrence,
                                title,
                                description)
                    }
                    this.defaultAction = Template.Action.values()[viewBinding.DefaultAction.selectedItemPosition]
                    if (this.amount.amountMinor == 0L) {
                        if (plan == null && this.defaultAction == Template.Action.SAVE) {
                            host.showSnackbar(context.getString(R.string.template_default_action_without_amount_hint))
                            return null
                        }
                        if (plan != null && this.isPlanExecutionAutomatic) {
                            host.showSnackbar(context.getString(R.string.plan_automatic_without_amount_hint))
                            return null
                        }
                    }
                    prefHandler.putString(PrefKey.TEMPLATE_CLICK_DEFAULT, defaultAction.name)
                }
            } else {
                referenceNumber = methodRowBinding.Number.text.toString()
                if (forSave && !isSplitPart) {
                    if (host.createTemplate) {
                        setInitialPlan(Triple(viewBinding.Title.text.toString(),
                                selectedRecurrence, dateEditBinding.DateButton.date))
                    }
                }
            }
            crStatus = (statusSpinner.selectedItem as CrStatus)
            this.pictureUri = this@TransactionDelegate.pictureUri
        }
    }

    private val selectedRecurrence
        get() = (recurrenceSpinner.selectedItem as? Plan.Recurrence)?.let {
            if (it == Plan.Recurrence.MONTHLY && configuredDate.dayOfMonth > 28 && viewBinding.LastDay.isChecked)
                Plan.Recurrence.LAST_DAY_OF_MONTH else it
        } ?: Plan.Recurrence.NONE

    protected fun validateAmountInput(forSave: Boolean): BigDecimal? {
        return validateAmountInput(viewBinding.Amount, forSave, forSave)
    }

    protected open fun validateAmountInput(input: AmountInput, showToUser: Boolean, ifPresent: Boolean): BigDecimal? {
        return input.getTypedValue(ifPresent, showToUser)
    }

    private fun configureAccountDependent(account: Account) {
        val currencyUnit = account.currency
        addCurrencyToInput(viewBinding.AmountLabel, viewBinding.Amount, currencyUnit, R.string.amount)
        viewBinding.OriginalAmount.configureExchange(currencyUnit)
        if (hasHomeCurrency(account)) {
            viewBinding.EquivalentAmountRow.visibility = View.GONE
            equivalentAmountVisible = false
        } else {
            viewBinding.EquivalentAmount.configureExchange(currencyUnit, Utils.getHomeCurrency())
        }
        configureDateInput(account)
        configureStatusSpinner()
        viewBinding.Amount.setFractionDigits(account.currency.fractionDigits)
        host.tintSystemUiAndFab(account.color)
    }

    private fun hasHomeCurrency(account: Account): Boolean {
        return account.currency == Utils.getHomeCurrency()
    }

    private fun configureDateInput(account: Account) {
        val dateMode = UiUtils.getDateMode(account.type, prefHandler)
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

    open fun setAccount(currencyExtra: String?) {
        var selected = 0 //if the accountId we have been passed does not exist, we select the first entry
        for (item in mAccounts.indices) {
            val account = mAccounts[item]
            if (account.currency.code == currencyExtra ||
                    currencyExtra == null && account.id == accountId) {
                selected = item
                break
            }
        }
        accountSpinner.setSelection(selected)
        updateAccount(mAccounts[selected])
    }

    open fun setAccounts(data: List<Account>, currencyExtra: String?) {
        mAccounts.clear()
        mAccounts.addAll(data)
        accountsAdapter.clear()
        accountsAdapter.addAll(data)

        viewBinding.Amount.setTypeEnabled(true)
        configureType()
        setAccount(currencyExtra)
    }

    private fun configureStatusSpinner() {
        currentAccount()?.let {
            setVisibility(statusSpinner.spinner, !isSplitPart && !isTemplate && it.type != AccountType.CASH)
        }
    }

    open fun updateAccount(account: Account) {
        accountId = account.id
        configureAccountDependent(account)
    }

    open fun configureType() {
        viewBinding.PayeeLabel.setText(if (viewBinding.Amount.type) R.string.payer else R.string.payee)
    }

    private fun updatePlanButton(plan: Plan) {
        planButton.overrideText(Plan.prettyTimeInfo(context, plan.rRule, plan.dtStart))
    }

    fun configurePlan(plan: Plan?) {
        plan?.let {
            updatePlanButton(it)
            if (viewBinding.Title.text.toString() == "") viewBinding.Title.setText(it.title)
            recurrenceSpinner.spinner.visibility = View.GONE
            configurePlanDependents(true)
            host.observePlan(it.id)
        }
    }

    private fun configurePlanDependents(visibility: Boolean) {
        setVisibility(planButton, visibility)
        setVisibility(planExecutionButton, visibility)
        setVisibility(viewBinding.advanceExecutionRow, visibility)
        setVisibility(viewBinding.DefaultActionRow, !visibility)
    }

    open fun onSaveInstanceState(outState: Bundle) {
        val originalInputSelectedCurrency = viewBinding.OriginalAmount.selectedCurrency
        if (originalInputSelectedCurrency != null) {
            originalCurrencyCode = originalInputSelectedCurrency.code
        }
        Icepick.saveInstanceState(this, outState)
    }

    private fun disableAccountSpinner() {
        accountSpinner.isEnabled = false
    }

    fun setPicture(pictureUri: Uri?) {
        this.pictureUri = pictureUri
        configurePicture()
    }

    private fun configurePicture() {
        if (pictureUri != null) {
            viewBinding.PictureContainer.root.visibility = View.VISIBLE
            Picasso.get().load(pictureUri).fit().into(viewBinding.PictureContainer.picture)
            viewBinding.AttachImage.visibility = View.GONE
        } else {
            viewBinding.AttachImage.visibility = View.VISIBLE
            viewBinding.PictureContainer.root.visibility = View.GONE
        }
    }

    open fun resetRecurrence() {
        recurrenceSpinner.spinner.visibility = View.VISIBLE
        recurrenceSpinner.setSelection(0)
        planButton.visibility = View.GONE
    }

    private fun resetAmounts() {
        isProcessingLinkedAmountInputs = true
        viewBinding.Amount.clear()
        viewBinding.TransferAmount.clear()
        isProcessingLinkedAmountInputs = false
    }

    open fun prepareForNew() {
        rowId = 0L
        uuid = null
        resetRecurrence()
        resetAmounts()
    }

    open fun onDestroy() {
    }

    fun onCalendarPermissionsResult(granted: Boolean) {
        if (granted) {
            if (isTemplate) {
                configurePlanDependents(true)
            }
            showCustomRecurrenceInfo()
            configureLastDayButton()
        } else {
            recurrenceSpinner.setSelection(0)
            if (!PermissionHelper.PermissionGroup.CALENDAR.shouldShowRequestPermissionRationale(host)) {
                setPlannerRowVisibility(false)
            }
        }
    }

    fun originTemplateLoaded(template: Template) {
        template.plan?.let { plan ->
            setPlannerRowVisibility(true)
            recurrenceSpinner.spinner.visibility = View.GONE
            updatePlanButton(plan)
            with(planButton) {
                visibility = View.VISIBLE
                setOnClickListener {
                    currentAccount()?.let {
                        (context as ExpenseEdit).showPlanMonthFragment(template, it.color)
                    }
                }
            }
            setVisibility(viewBinding.EditPlan, true)
            planId = plan.id
            host.observePlan(plan.id)
        }
    }

    fun showTags(tags: Iterable<Tag>?, closeFunction: (Tag) -> Unit) {
        with(viewBinding.TagGroup) {
            removeAllViews()
            tags?.let { addChipsBulk(it, closeFunction) }
        }
    }

    fun setCreateTemplate(createTemplate: Boolean, isCalendarPermissionPermanentlyDeclined: Boolean) {
        setVisibility(viewBinding.TitleRow, createTemplate)
        setPlannerRowVisibility(createTemplate && !isCalendarPermissionPermanentlyDeclined)
    }

    data class OperationType(val type: Int) {
        var label: String = ""

        override fun toString(): String {
            return label
        }
    }

    companion object {
        fun create(transaction: ITransaction, viewBinding: OneExpenseBinding,
                   dateEditBinding: DateEditBinding, methodRowBinding: MethodRowBinding,
                   prefHandler: PrefHandler) =
                (transaction is Template).let { isTemplate ->
                    with(transaction) {
                        when {
                            isTransfer -> TransferDelegate(viewBinding, dateEditBinding, methodRowBinding, prefHandler, isTemplate)
                            isSplit -> SplitDelegate(viewBinding, dateEditBinding, methodRowBinding, prefHandler, isTemplate)
                            else -> CategoryDelegate(viewBinding, dateEditBinding, methodRowBinding, prefHandler, isTemplate)
                        }
                    }
                }

        fun create(operationType: Int, isTemplate: Boolean, viewBinding: OneExpenseBinding,
                   dateEditBinding: DateEditBinding, methodRowBinding: MethodRowBinding,
                   prefHandler: PrefHandler) = when (operationType) {
            TYPE_TRANSFER -> TransferDelegate(viewBinding, dateEditBinding, methodRowBinding, prefHandler, isTemplate)
            TYPE_SPLIT -> SplitDelegate(viewBinding, dateEditBinding, methodRowBinding, prefHandler, isTemplate)
            else -> CategoryDelegate(viewBinding, dateEditBinding, methodRowBinding, prefHandler, isTemplate)
        }
    }
}