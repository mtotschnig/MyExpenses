package org.totschnig.myexpenses.delegate

import android.content.Context
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
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.HELP_VARIANT_SPLIT_PART_CATEGORY
import org.totschnig.myexpenses.activity.HELP_VARIANT_TEMPLATE_CATEGORY
import org.totschnig.myexpenses.activity.HELP_VARIANT_TRANSACTION
import org.totschnig.myexpenses.adapter.AccountAdapter
import org.totschnig.myexpenses.adapter.CrStatusAdapter
import org.totschnig.myexpenses.adapter.GroupedSpinnerAdapter
import org.totschnig.myexpenses.adapter.NothingSelectedSpinnerAdapter
import org.totschnig.myexpenses.adapter.RecurrenceAdapter
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.asCategoryType
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.dialog.addAllAccounts
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod.Companion.translateIfPredefined
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.DateButton
import org.totschnig.myexpenses.ui.MyTextWatcher
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.TextUtils.appendCurrencyDescription
import org.totschnig.myexpenses.util.TextUtils.appendCurrencySymbol
import org.totschnig.myexpenses.util.config.Configurator
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.util.ui.UiUtils
import org.totschnig.myexpenses.util.ui.addChipsBulk
import org.totschnig.myexpenses.util.ui.getDateMode
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import javax.inject.Inject
import kotlin.math.absoluteValue

abstract class TransactionDelegate<T : ITransaction>(
    val viewBinding: OneExpenseBinding,
    private val dateEditBinding: DateEditBinding,
    private val methodRowBinding: MethodRowBinding,
    val isTemplate: Boolean,
) : AdapterView.OnItemSelectedListener {

    @State
    var label: String? = null

    @State
    var categoryIcon: String? = null

    @State
    var catId: Long? = null

    @State
    var catType: Byte = FLAG_NEUTRAL

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var configurator: Configurator

    val homeCurrency by lazy {
        currencyContext.homeCurrencyUnit
    }

    open val createNewOverride
        // when we edit a split part and the amount equals the remaining unsplit amount,
        // we finish activity even when createNew is set, and we want to reflect this on the FAB icon
        get() = !isSplitPart || lastFilledAmount?.compareTo(viewBinding.Amount.typedValue)?.absoluteValue == 1

    private val methodSpinner = SpinnerHelper(methodRowBinding.Method.MethodSpinner)
    val accountSpinner = SpinnerHelper(viewBinding.Account)
    private val statusSpinner = SpinnerHelper(viewBinding.Status)
    private val operationTypeSpinner = SpinnerHelper(viewBinding.toolbar.OperationType)
    val recurrenceSpinner = SpinnerHelper(viewBinding.Recurrence)
    private lateinit var methodsAdapter: ArrayAdapter<PaymentMethod>
    private lateinit var operationTypeAdapter: ArrayAdapter<OperationType>
    lateinit var accountAdapter: GroupedSpinnerAdapter<AccountFlag, Account>

    init {
        createMethodAdapter()
        viewBinding.advanceExecutionSeek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seekBar.requestFocusFromTouch() //prevent jump to first EditText https://stackoverflow.com/a/6177270/1199911
                viewBinding.advanceExecutionValue.text =
                    String.format(Locale.getDefault(), "%d", progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    open val helpVariant: String
        get() = when {
            isTemplate -> HELP_VARIANT_TEMPLATE_CATEGORY
            isSplitPart -> HELP_VARIANT_SPLIT_PART_CATEGORY
            else -> HELP_VARIANT_TRANSACTION
        }

    fun title(newInstance: Boolean) = with(context) {
        if (newInstance) {
            labelForNewInstance(operationType)
        } else {
            when {
                isTemplate -> getString(R.string.menu_edit_template) + " (" + getString(typeResId) + ")"
                isSplitPart -> getString(editPartResId)
                else -> getString(editResId)
            }
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

    @State
    var passedInAccountId: Long? = null

    @State
    var accountId: Long? = null

    @State
    var passedInAmount: Long? = null

    @State
    var methodId: Long? = null

    @State
    var methodsLoaded: Boolean = false

    @State
    var methodLabel: String? = null

    @State
    var crStatus: CrStatus = CrStatus.UNRECONCILED

    @State
    var parentId: Long? = null

    @State
    var rowId: Long = 0L

    @State
    var planId: Long? = null

    @State
    var originTemplateId: Long? = null

    @State
    var uuid: String? = null

    @State
    var debtId: Long? = null

    @State
    var lastFilledAmount: BigDecimal? = null

    protected var mAccounts = mutableListOf<Account>()

    val planButton: DateButton
        get() = viewBinding.PB
    private val planExecutionButton: CompoundButton
        get() = viewBinding.TB

    open fun bindUnsafe(
        transaction: ITransaction?,
        withTypeSpinner: Boolean,
        savedInstanceState: Bundle?,
        recurrence: Plan.Recurrence?,
        withAutoFill: Boolean,
        isCached: Boolean = false,
    ) {
        bind(
            transaction as T?,
            withTypeSpinner,
            savedInstanceState,
            recurrence,
            withAutoFill
        )
    }

    open fun bind(
        transaction: T?,
        withTypeSpinner: Boolean,
        savedInstanceState: Bundle?,
        recurrence: Plan.Recurrence?,
        withAutoFill: Boolean,
    ) {
        viewBinding.Category.setOnClickListener { host.startSelectCategory() }
        if (transaction != null) {
            label = transaction.categoryPath
            categoryIcon = transaction.categoryIcon
            catId = transaction.catId
            rowId = transaction.id
            parentId = transaction.parentId
            accountId = transaction.accountId
            passedInAccountId = transaction.accountId
            passedInAmount = transaction.amount.amountMinor
            methodId = transaction.methodId
            methodLabel = transaction.methodLabel
            planId = (transaction as? Template)?.plan?.id
            crStatus = transaction.crStatus
            originTemplateId = transaction.originTemplateId
            uuid = transaction.uuid
            debtId = transaction.debtId
            //Setting this early instead of waiting for call to setAccounts
            //works around a bug in some legacy virtual keyboards where configuring the
            //editText too late corrupt inputType
            viewBinding.Amount.setFractionDigits(transaction.amount.currencyUnit.fractionDigits)
        } else {
            try {
                StateSaver.restoreInstanceState(this, savedInstanceState)
            } catch (e: Exception) {
                //If user rotates device before delegate was initialized, restore crashes,
                //because AndroidState does not handle enum correctly
                CrashHandler.report(e)
            }
        }
        viewBinding.toolbar.OperationType.isVisible = withTypeSpinner
        originTemplateId?.let { host.loadOriginTemplate(it) }

        if (isMainTemplate) {
            viewBinding.TitleRow.visibility = View.VISIBLE
            viewBinding.DefaultActionRow.visibility = View.VISIBLE
            setPlannerRowVisibility(true)
            planButton.setOnClickListener {
                planId?.let {
                    host.launchPlanView(false, it)
                } ?: run {
                    planButton.onClick()
                }
            }
        }
        if (!isSplitPart) {
            //we set adapter even if spinner is not immediately visible, since it might become visible
            //after SAVE_AND_NEW action
            val recurrenceAdapter = RecurrenceAdapter(context)
            recurrenceSpinner.adapter = recurrenceAdapter
            recurrence?.let {
                recurrenceSpinner.setSelection(recurrenceAdapter.getPosition(it))
                if (isTemplate && it != Plan.Recurrence.NONE) {
                    configurePlanDependents(true)
                }
                configureLastDayButton()
            }
            recurrenceSpinner.setOnItemSelectedListener(this)
        }
        if (isSplitPart || isTemplate) {
            viewBinding.DateTimeRow.visibility = View.GONE
            viewBinding.AttachmentsRow.visibility = View.GONE
        }

        createAdapters(withTypeSpinner, withAutoFill)

        //when we have a savedInstance, fields have already been populated
        if (savedInstanceState == null) {
            isProcessingLinkedAmountInputs = true
            populateFields(transaction!!, withAutoFill)
            isProcessingLinkedAmountInputs = false
            if (!isSplitPart) {
                setLocalDateTime(transaction)
            }
        } else {
            populateStatusSpinner()
        }
        viewBinding.Amount.visibility = View.VISIBLE
        //}
        //after setLocalDateTime, so that the plan info can override the date
        configurePlan((transaction as? Template)?.plan, false)
        configureLastDayButton()

        viewBinding.ClearCategory.setOnClickListener {
            resetCategory()
        }
        setCategoryButton()
        viewBinding.Amount.addTextChangedListener(amountChangeWatcher)
    }

    val amountChangeWatcher = object : MyTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            onAmountChanged()
        }
    }

    open fun onAmountChanged() {
        if (isSplitPart) {
            host.configureFloatingActionButton()
        }
    }

    /**
     * set label on category button
     */
    fun setCategoryButton() {
        if (label.isNullOrEmpty()) {
            viewBinding.Category.setText(R.string.select)
            viewBinding.ClearCategory.visibility = View.GONE
        } else {
            viewBinding.Category.text = label
            viewBinding.ClearCategory.visibility = View.VISIBLE

        }
        val startDrawable = categoryIcon?.let {
            IIconInfo.resolveIcon(it)?.asDrawable(context, androidx.appcompat.R.attr.colorPrimary)
        }
        viewBinding.Category.setCompoundDrawablesRelativeWithIntrinsicBounds(
            startDrawable,
            null,
            null,
            null
        )
    }

    fun setCategory(
        label: String?,
        categoryIcon: String?,
        catId: Long?,
        catType: Byte = FLAG_NEUTRAL
    ) {
        this.label = label
        this.categoryIcon = categoryIcon
        this.catId = catId
        this.catType = catType
        setCategoryButton()
    }

    fun resetCategory() {
        setCategory(null, null, null)
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
        viewBinding.PlanRow.isVisible = visibility
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
        if (withAutoFill && isMainTemplate) {
            viewBinding.Title.requestFocus()
        }
    }

    private fun populateStatusSpinner() {
        if (crStatus != CrStatus.RECONCILED) {
            statusSpinner.setSelection(CrStatus.editableStatuses.indexOf(crStatus), false)
            updateStatusContentDescription()
        }
    }

    private fun updateStatusContentDescription() {
        statusSpinner.spinner.contentDescription =
            context.getString(R.string.status) + ": " + context.getString(crStatus.toStringRes())
    }

    fun fillAmount(amount: BigDecimal) {
        with(viewBinding.Amount) {
            if (amount.signum() != 0) {
                lastFilledAmount = amount
                setAmount(amount)
            }
            requestFocus()
            selectAll()
        }
    }

    protected fun addCurrencyToInput(
        label: TextView,
        amountInput: AmountInput,
        currencyUnit: CurrencyUnit,
        textResId: Int,
    ) {
        val text = appendCurrencySymbol(label.context, textResId, currencyUnit)
        label.text = text
        if (amountInput.contentDescription.isNullOrEmpty()) {
            amountInput.contentDescription =
                appendCurrencyDescription(label.context, textResId, currencyUnit)
        }
    }

    private fun setMethodSelection(methodId: Long?) {
        this.methodId = methodId
        setMethodSelection()
    }

    fun setMethodSelection() {
        if (!methodsLoaded) return
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
            if (found) {
                methodSpinner.spinner.isVisible = true
                methodRowBinding.Method.MethodOutlier.isVisible = false
            } else {
                methodSpinner.setSelection(0)
                if (methodLabel != null) {
                    methodSpinner.spinner.isVisible = false
                    with(methodRowBinding.Method.MethodOutlier) {
                        text = methodLabel!!.translateIfPredefined(context)
                        isVisible = true
                    }
                } else {
                    methodId = null
                }
            }
        } else {
            methodSpinner.spinner.isVisible = true
            methodRowBinding.Method.MethodOutlier.isVisible = false
            methodSpinner.setSelection(0)
        }
        methodRowBinding.ClearMethod.root.isVisible = methodId != null
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

    open fun createAdapters(withTypeSpinner: Boolean, withAutoFill: Boolean) {
        createStatusAdapter()
        if (withTypeSpinner) {
            createOperationTypeAdapter()
        }
        createAccountAdapter()
    }

    private fun labelForNewInstance(type: Int) = context.getString(
        when (type) {
            TYPE_SPLIT -> if (isTemplate) R.string.menu_create_template_for_split else R.string.menu_create_split
            TYPE_TRANSFER -> if (isSplitPart) R.string.menu_create_split_part_transfer else if (isTemplate) R.string.menu_create_template_for_transfer else R.string.menu_create_transfer
            TYPE_TRANSACTION -> if (isSplitPart) R.string.menu_create_split_part_category else if (isTemplate) R.string.menu_create_template_for_transaction else R.string.menu_create_transaction
            else -> throw IllegalStateException("Unknown operationType $type")
        }
    )

    protected fun createOperationTypeAdapter() {
        val allowedOperationTypes: MutableList<Int> = ArrayList()
        allowedOperationTypes.add(TYPE_TRANSACTION)
        allowedOperationTypes.add(TYPE_TRANSFER)
        if (parentId == null) {
            allowedOperationTypes.add(TYPE_SPLIT)
        }
        val objects = allowedOperationTypes.map {
            OperationType(it).apply {
                label = labelForNewInstance(it)
            }
        }
        operationTypeAdapter =
            ArrayAdapter<OperationType>(context, android.R.layout.simple_spinner_item, objects)
        operationTypeAdapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
        operationTypeSpinner.adapter = operationTypeAdapter
        resetOperationType()
        operationTypeSpinner.setOnItemSelectedListener(this)
    }

    protected fun createStatusAdapter() {
        if (crStatus != CrStatus.RECONCILED) {
            statusSpinner.adapter = CrStatusAdapter(context)
        }
    }

    protected fun createAccountAdapter() {
        accountAdapter = AccountAdapter(context)
        accountSpinner.adapter = accountAdapter
    }

    private fun createMethodAdapter() {
        methodsAdapter =
                //TODO Use IdAdapter
            object : ArrayAdapter<PaymentMethod>(context, android.R.layout.simple_spinner_item) {
                override fun getItemId(position: Int): Long {
                    return getItem(position)?.id() ?: 0L
                }
            }
        methodsAdapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
        methodSpinner.adapter = NothingSelectedSpinnerAdapter(
            methodsAdapter,
            android.R.layout.simple_spinner_item,  // R.layout.contact_spinner_nothing_selected_dropdown, // Optional
            context
        )
    }

    fun resetOperationType() {
        operationTypeSpinner.setSelection(
            operationTypeAdapter.getPosition(OperationType(operationType))
        )
    }

    fun setMethods(paymentMethods: List<PaymentMethod>?) {
        methodsLoaded = true
        if (paymentMethods.isNullOrEmpty()) {
            methodId = null
            methodRowBinding.MethodRow.visibility = View.GONE
        } else {
            methodRowBinding.MethodRow.visibility = View.VISIBLE
            methodRowBinding.ClearMethod.root.setOnClickListener {
                setMethodSelection(null)
            }
            methodsAdapter.clear()
            methodsAdapter.addAll(paymentMethods)
            setMethodSelection()
        }
    }

    open fun missingRecurrenceFeature() = if (prefHandler.getBoolean(
            PrefKey.NEW_PLAN_ENABLED,
            true
        )
    ) null else ContribFeature.PLANS_UNLIMITED

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        val host = context as ExpenseEdit
        if (parent.id != R.id.OperationType) {
            host.setDirty()
        }
        when (parent.id) {
            recurrenceSpinner.id -> {
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
                    }
                    host.checkPermissionsForPlaner()
                }
                if (isTemplate) {
                    configurePlanDependents(planVisibility)
                }
            }

            methodSpinner.id -> {
                val hasSelection = position > 0
                methodId = if (hasSelection) parent.selectedItemId.takeIf { it > 0 } else null
                methodRowBinding.ClearMethod.root.isVisible = hasSelection
                setReferenceNumberVisibility()
            }

            accountSpinner.id -> {
                mAccounts.firstOrNull() { it.id == id }?.let { newAccount ->
                    val oldAccount = mAccounts.first { it.id == accountId }
                    if (newAccount.color == oldAccount.color || !host.maybeApplyDynamicColor()) {
                        updateAccount(newAccount, oldAccount.currency.code != newAccount.currency.code)
                    } else {
                        accountId = newAccount.id
                        host.color = newAccount.color
                    }
                }
            }

            operationTypeSpinner.id -> {
                val newType =
                    (operationTypeSpinner.getItemAtPosition(position) as OperationType).type
                if (host.isValidType(newType)) {
                    if (newType == TYPE_TRANSFER) {
                        if (checkTransferEnabled()) {
                            host.restartWithType(newType)
                        } else {
                            host.showTransferAccountMissingMessage()
                            resetOperationType()
                        }
                    } else if (newType == TYPE_SPLIT) {
                        resetOperationType()
                        if (isTemplate) {
                            if (prefHandler.getBoolean(PrefKey.NEW_SPLIT_TEMPLATE_ENABLED, true)) {
                                host.restartWithType(newType)
                            } else {
                                host.contribFeatureRequested(ContribFeature.SPLIT_TEMPLATE)
                            }
                        } else {
                            host.contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION)
                        }
                    } else {
                        host.restartWithType(newType)
                    }
                }
            }

            statusSpinner.id -> {
                (parent.selectedItem as? CrStatus)?.let {
                    crStatus = it
                    updateStatusContentDescription()
                }
            }
        }
    }

    private fun checkTransferEnabled() = when {
        currentAccount() == null -> false
        mAccounts.size <= 1 -> false
        else -> true
    }

    private fun showCustomRecurrenceInfo() {
        if (recurrenceSpinner.selectedItem === Plan.Recurrence.CUSTOM) {
            (context as ExpenseEdit).showDismissibleSnackBar(R.string.plan_custom_recurrence_info)
        }
    }

    private val configuredDate: LocalDate
        get() = (if (isMainTemplate) planButton else dateEditBinding.DateButton).date

    fun configureLastDayButton() {
        val visible =
            recurrenceSpinner.selectedItem === Plan.Recurrence.MONTHLY && configuredDate.dayOfMonth > 28
        viewBinding.LastDay.isVisible = visible
        if (!visible) {
            viewBinding.LastDay.isChecked = false
        } else if (configuredDate.dayOfMonth == 31) {
            viewBinding.LastDay.isChecked = true
        }
    }

    open fun setupListeners(watcher: TextWatcher) {
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
    val isIncome: Boolean
        get() = viewBinding.Amount.type

    val shouldShowCategoryWarning: Byte?
        get() = catType.takeIf { it != FLAG_NEUTRAL && it != isIncome.asCategoryType }

    private fun readZonedDateTime(dateEdit: DateButton): ZonedDateTime {
        return ZonedDateTime.of(
            dateEdit.date,
            if (dateEditBinding.TimeButton.isVisible) dateEditBinding.TimeButton.time else LocalTime.now(),
            ZoneId.systemDefault()
        )
    }

    fun currentAccount() = getAccountFromSpinner(accountSpinner)

    protected fun getAccountFromSpinner(spinner: SpinnerHelper) =
        if (spinner.selectedItemPosition == AdapterView.INVALID_POSITION)
            null else mAccounts.find { it.id == spinner.selectedItemId }

    protected fun buildTemplate(account: Account) =
        Template.getTypedNewInstance(
            context.contentResolver,
            operationType,
            account.id,
            account.currency,
            false,
            parentId
        )!!

    abstract fun buildTransaction(
        forSave: Boolean,
        account: Account,
    ): T?

    abstract val operationType: Int

    open fun syncStateAndValidate(forSave: Boolean): T? {
        return currentAccount()?.let {
            buildTransaction(
                forSave && !isMainTemplate,
                it
            )
        }?.apply {
            catId = this@TransactionDelegate.catId
            categoryPath = this@TransactionDelegate.label
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
                setValueDate(
                    if (dateEditBinding.Date2Button.isVisible) readZonedDateTime(
                        dateEditBinding.Date2Button
                    ) else transactionDate
                )
            }
            if (isMainTemplate) {
                (this as Template).apply {
                    viewBinding.Title.text.toString().let {
                        if (it == "") {
                            if (forSave) {
                                viewBinding.Title.error = context.getString(R.string.required)
                                return null
                            }
                        }
                        this.title = it
                    }
                    this.isPlanExecutionAutomatic = planExecutionButton.isChecked
                    this.planExecutionAdvance = viewBinding.advanceExecutionSeek.progress
                    if (recurrenceSpinner.selectedItemPosition > 0 || this@TransactionDelegate.planId != null) {
                        plan = Plan(
                            this@TransactionDelegate.planId ?: 0L,
                            planButton.date,
                            selectedRecurrence,
                            title,
                            compileDescription(context.myApplication)
                        )
                    }
                    this.defaultAction =
                        Template.Action.entries[viewBinding.DefaultAction.selectedItemPosition]
                    if (this.amount.amountMinor == 0L &&
                        (this.transferAmount?.amountMinor ?: 0L) == 0L &&
                        forSave
                    ) {
                        if (plan == null && this.defaultAction == Template.Action.SAVE) {
                            host.showSnackBar(context.getString(R.string.template_default_action_without_amount_hint))
                            return null
                        }
                        if (plan != null && this.isPlanExecutionAutomatic) {
                            host.showSnackBar(context.getString(R.string.plan_automatic_without_amount_hint))
                            return null
                        }
                    }
                    prefHandler.putString(PrefKey.TEMPLATE_CLICK_DEFAULT, defaultAction.name)
                }
            } else {
                referenceNumber = methodRowBinding.Number.text.toString()
                if (forSave && !isSplitPart) {
                    if (host.createTemplate) {
                        setInitialPlan(
                            Triple(
                                viewBinding.Title.text.toString().takeIf { it.isNotEmpty() },
                                selectedRecurrence, dateEditBinding.DateButton.date
                            )
                        )
                    }
                }
            }
            crStatus = this@TransactionDelegate.crStatus
        }
    }

    private val selectedRecurrence
        get() = (recurrenceSpinner.selectedItem as? Plan.Recurrence)?.let {
            if (it == Plan.Recurrence.MONTHLY && configuredDate.dayOfMonth > 28 && viewBinding.LastDay.isChecked)
                Plan.Recurrence.LAST_DAY_OF_MONTH else it
        } ?: Plan.Recurrence.NONE

    protected fun validateAmountInput(): BigDecimal? =
        viewBinding.Amount.getAmount(showToUser = false)

    protected fun validateAmountInput(forSave: Boolean, currencyUnit: CurrencyUnit) =
        viewBinding.Amount.getAmount(
            currencyUnit,
            showToUser = forSave
        )

    open fun configureAccountDependent(account: Account, isInitialSetup: Boolean) {
        val currencyUnit = account.currency
        addCurrencyToInput(
            viewBinding.AmountLabel,
            viewBinding.Amount,
            currencyUnit,
            R.string.amount
        )
        configureDateInput(account)
        configureStatusSpinner()
        viewBinding.Amount.setFractionDigits(currencyUnit.fractionDigits)
        host.updateContentColor(account.color)
    }

    protected fun hasHomeCurrency(account: Account): Boolean {
        return account.currency == homeCurrency
    }

    private fun configureDateInput(account: Account) {
        val dateMode = getDateMode(account.type, prefHandler)
        dateEditBinding.TimeButton.isVisible = dateMode == UiUtils.DateMode.DATE_TIME
        dateEditBinding.Date2Button.isVisible = dateMode == UiUtils.DateMode.BOOKING_VALUE
        dateEditBinding.DateLink.isVisible = dateMode == UiUtils.DateMode.BOOKING_VALUE

        viewBinding.DateTimeLabel.text = when (dateMode) {
            UiUtils.DateMode.BOOKING_VALUE -> context.getString(R.string.booking_date) + "/" + context.getString(
                R.string.value_date
            )

            UiUtils.DateMode.DATE_TIME -> context.getString(R.string.date) + " / " + context.getString(
                R.string.time
            )

            UiUtils.DateMode.DATE -> context.getString(R.string.date)
        }
    }

    open fun setAccount(isInitialSetup: Boolean) {
        //if the accountId we have been passed does not exist, we select the first entry
        val selected = mAccounts.find { it.id == accountId } ?: mAccounts.first()
        accountSpinner.setSelection(accountAdapter.getPosition(selected.id))
        updateAccount(selected, isInitialSetup)
    }

    open fun setAccounts(data: List<Account>, firstLoad: Boolean, isInitialSetup: Boolean) {
        if (firstLoad) {
            mAccounts.clear()
            mAccounts.addAll(data)
            accountAdapter.addAllAccounts(data)
            viewBinding.Amount.setTypeEnabled(true)
            isProcessingLinkedAmountInputs = true
            configureType()
            isProcessingLinkedAmountInputs = false
            setAccount(isInitialSetup)
        } else {
            data.forEach { newData ->
                mAccounts.find { it.id == newData.id }?.currentBalance = newData.currentBalance
            }
        }
    }

    private fun configureStatusSpinner() {
        currentAccount()?.let {
            statusSpinner.spinner.isVisible =
                !isSplitPart && !isTemplate && it.type.supportsReconciliation && crStatus != CrStatus.RECONCILED
        }
    }

    open fun updateAccount(account: Account, isInitialSetup: Boolean) {
        accountId = account.id
        host.loadActiveTags(account.id)
        configureAccountDependent(account, isInitialSetup)
    }

    fun setType(type: Boolean) {
        isProcessingLinkedAmountInputs = true
        viewBinding.Amount.type = type
        isProcessingLinkedAmountInputs = false
    }

    open fun configureType() {
        viewBinding.PayeeLabel.setText(if (viewBinding.Amount.type) R.string.payer else R.string.payee)
    }

    private fun updatePlanButton(plan: Plan) {
        planButton.overrideText(Plan.prettyTimeInfo(context, plan.rRule, plan.dtStart))
    }

    fun configurePlan(plan: Plan?, fromObserver: Boolean) {
        plan?.let {
            updatePlanButton(it)
            if (viewBinding.Title.text.toString() == "") viewBinding.Title.setText(it.title)
            if (!fromObserver) {
                recurrenceSpinner.spinner.visibility = View.GONE
                configurePlanDependents(true)
            }
            host.observePlan(it.id)
        }
    }

    private fun configurePlanDependents(visibility: Boolean) {
        planButton.isVisible = visibility
        planExecutionButton.isVisible = visibility
        viewBinding.advanceExecutionRow.isVisible = visibility
        viewBinding.DefaultActionRow.isVisible = !visibility
    }

    open fun onSaveInstanceState(outState: Bundle) {
        StateSaver.saveInstanceState(this, outState)
    }

    protected fun disableAccountSpinner() {
        accountSpinner.isEnabled = false
    }

    open fun resetRecurrence() {
        recurrenceSpinner.spinner.visibility = View.VISIBLE
        recurrenceSpinner.setSelection(0)
        planButton.visibility = View.GONE
    }

    private fun resetAmounts(): Boolean {
        isProcessingLinkedAmountInputs = true
        lastFilledAmount?.takeIf { isSplitPart }?.also { lastFilled ->
            viewBinding.Amount.getAmount(false)?.let { currentSet ->
                (lastFilled - currentSet).takeIf { it.compareTo(BigDecimal.ZERO) != 0 }?.also {
                    fillAmount(it)
                } ?: run {
                    viewBinding.Amount.clear()
                    return false
                }
            }
        } ?: run {
            viewBinding.Amount.clear()
        }
        viewBinding.TransferAmount.clear()
        isProcessingLinkedAmountInputs = false
        return true
    }

    open fun prepareForNew() = if (resetAmounts()) {
        rowId = 0L
        uuid = null
        crStatus = CrStatus.UNRECONCILED
        resetRecurrence()
        populateStatusSpinner()
        true
    } else false

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
                        host.showPlanMonthFragment(template, it.color)
                    }
                }
            }
            viewBinding.EditPlan.isVisible = true
            viewBinding.EditPlan.setOnClickListener {
                host.launchPlanView(false, plan.id)
            }
            planId = plan.id
            host.observePlan(plan.id)
        }
    }

    fun showTags(tags: Iterable<Tag>?, closeFunction: (Tag) -> Unit) {
        with(viewBinding.TagRow.TagGroup) {
            removeAllViews()
            tags?.let { addChipsBulk(it, closeFunction) }
        }
    }

    fun setCreateTemplate(
        createTemplate: Boolean,
    ) {
        viewBinding.TitleRow.isVisible = createTemplate
        setPlannerRowVisibility(createTemplate)
    }

    data class OperationType(val type: Int) {
        var label: String = ""

        override fun toString(): String {
            return label
        }
    }

    companion object {
        fun create(
            transaction: ITransaction,
            viewBinding: OneExpenseBinding,
            dateEditBinding: DateEditBinding,
            methodRowBinding: MethodRowBinding,
            injector: AppComponent,
        ) =
            (transaction is Template).let { isTemplate ->
                with(transaction) {
                    when {
                        isTransfer -> TransferDelegate(
                            viewBinding,
                            dateEditBinding,
                            methodRowBinding,
                            isTemplate
                        ).also {
                            injector.inject(it)
                        }

                        isSplit -> SplitDelegate(
                            viewBinding,
                            dateEditBinding,
                            methodRowBinding,
                            isTemplate
                        ).also {
                            injector.inject(it)
                        }

                        else -> CategoryDelegate(
                            viewBinding,
                            dateEditBinding,
                            methodRowBinding,
                            isTemplate
                        ).also {
                            injector.inject(it)
                        }
                    }
                }
            }

        fun create(
            operationType: Int, isTemplate: Boolean, viewBinding: OneExpenseBinding,
            dateEditBinding: DateEditBinding, methodRowBinding: MethodRowBinding,
            injector: AppComponent,
        ) = when (operationType) {
            TYPE_TRANSFER -> TransferDelegate(
                viewBinding,
                dateEditBinding,
                methodRowBinding,
                isTemplate
            ).also {
                injector.inject(it)
            }

            TYPE_SPLIT -> SplitDelegate(
                viewBinding,
                dateEditBinding,
                methodRowBinding,
                isTemplate
            ).also {
                injector.inject(it)
            }

            else -> CategoryDelegate(
                viewBinding,
                dateEditBinding,
                methodRowBinding,
                isTemplate
            ).also {
                injector.inject(it)
            }
        }
    }
}