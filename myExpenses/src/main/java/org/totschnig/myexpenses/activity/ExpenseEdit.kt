/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses.activity

import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.provider.CalendarContract
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.loader.app.LoaderManager
import com.evernote.android.state.State
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageCategories.Companion.KEY_PROTECTION_INFO
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.delegate.*
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.exception.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.exception.UnknownPictureSaveException
import org.totschnig.myexpenses.feature.OcrResultFlat
import org.totschnig.myexpenses.fragment.PlanMonthFragment
import org.totschnig.myexpenses.fragment.TemplatesList
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Plan.Recurrence
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.disableAutoFill
import org.totschnig.myexpenses.preference.enableAutoFill
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.ui.*
import org.totschnig.myexpenses.util.*
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.*
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel.DeleteState.DeleteComplete
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.InstantiationTask
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.InstantiationTask.*
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Tag
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlin.Result
import kotlin.collections.set
import org.totschnig.myexpenses.viewmodel.data.Template as DataTemplate

/**
 * Activity for editing a transaction
 *
 * @author Michael Totschnig
 */
open class ExpenseEdit : AmountActivity<TransactionEditViewModel>(), ContribIFace,
    ConfirmationDialogListener, ExchangeRateEdit.Host {
    private lateinit var rootBinding: OneExpenseBinding
    private lateinit var dateEditBinding: DateEditBinding
    private lateinit var methodRowBinding: MethodRowBinding
    override val amountLabel: TextView
        get() = rootBinding.AmountLabel
    override val amountRow: ViewGroup
        get() = rootBinding.AmountRow
    override val exchangeRateRow: ViewGroup
        get() = rootBinding.ERR.root
    override val amountInput: AmountInput
        get() = rootBinding.Amount
    override val exchangeRateEdit: ExchangeRateEdit
        get() = rootBinding.ERR.ExchangeRate

    @JvmField
    @State
    var color = 0

    @JvmField
    @State
    var parentId = 0L

    val accountId: Long
        get() = currentAccount?.id ?: 0L

    /**
     * transaction, transfer or split
     */
    @JvmField
    @State
    var operationType = 0
    private lateinit var mManager: LoaderManager

    @JvmField
    @State
    var createNew = false

    @JvmField
    @State
    var createTemplate = false

    @JvmField
    @State
    var isTemplate = false

    @JvmField
    @State
    var shouldShowCreateTemplate = false

    @JvmField
    @State
    var areDatesLinked = false

    @JvmField
    @State
    var withTypeSpinner = false

    private var mIsResumed = false
    private var accountsLoaded = false
    private var pObserver: ContentObserver? = null
    private lateinit var currencyViewModel: CurrencyViewModel
    override fun getDate(): LocalDate {
        return dateEditBinding.DateButton.date
    }

    enum class HelpVariant {
        transaction, transfer, split, templateCategory, templateTransfer, templateSplit, splitPartCategory, splitPartTransfer
    }

    @Inject
    lateinit var imageViewIntentProvider: ImageViewIntentProvider

    @Inject
    lateinit var discoveryHelper: IDiscoveryHelper

    lateinit var delegate: TransactionDelegate<*>

    private var menuItem2TemplateMap: MutableMap<Int, DataTemplate> = mutableMapOf()

    private val isSplitPart: Boolean
        get() = parentId != 0L

    private val isSplitPartOrTemplate: Boolean
        get() = isSplitPart || isTemplate

    private val isMainTemplate: Boolean
        get() = isTemplate && !isSplitPart

    private val shouldLoadMethods: Boolean
        get() = operationType != TYPE_TRANSFER && !isSplitPart

    private val shouldLoadDebts: Boolean
        get() = operationType != TYPE_TRANSFER && !parentHasDebt

    private val parentHasDebt: Boolean
        get() = intent.getBooleanExtra(KEY_PARENT_HAS_DEBT, false)

    val parentPayeeId: Long
        get() = intent.getLongExtra(KEY_PAYEEID, 0)

    @Suppress("UNCHECKED_CAST")
    val parentOriginalAmountExchangeRate: Pair<BigDecimal, Currency>?
        get() = (intent.getSerializableExtra(KEY_PARENT_ORIGINAL_AMOUNT_EXCHANGE_RATE) as? Pair<BigDecimal, Currency>)

    private val isMainTransaction: Boolean
        get() = operationType != TYPE_TRANSFER && !isSplitPartOrTemplate

    private val isClone: Boolean
        get() = intent.getBooleanExtra(KEY_CLONE, false)

    private val withAutoFill: Boolean
        get() = newInstance && !isClone && !hasCreateFromTemplateAction

    private val hasCreateFromTemplateAction
        get() = intent.action == ACTION_CREATE_FROM_TEMPLATE || planInstanceId > 0L

    private val planInstanceId: Long
        get() = intent.getLongExtra(KEY_INSTANCEID, 0L)

    override fun injectDependencies() {
        injector.inject(this)
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        floatingActionButton.show()
    }

    fun updateContentColor(color: Int) {
        this.color = color
        //tintSystemUi(color) //dynamic colors
        tintSystemUiAndFab(color)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //https://github.com/material-components/material-components-android/issues/3422
/*        (color.takeIf { it != 0 } ?: intent.getIntExtra(KEY_COLOR, 0).takeIf { it != 0 })?.let {
            DynamicColors.applyToActivityIfAvailable(
                this,
                DynamicColorsOptions.Builder()
                    .setContentBasedSource(it)
                    .build()
            )
        }*/
        maybeRepairRequerySchema()
        setHelpVariant(HelpVariant.transaction, false)
        rootBinding = OneExpenseBinding.inflate(LayoutInflater.from(this))
        rootBinding.TagRow.TagLabel.setText(R.string.tags)
        dateEditBinding = DateEditBinding.bind(rootBinding.root)
        methodRowBinding = MethodRowBinding.bind(rootBinding.root)
        setContentView(rootBinding.root)
        setupToolbarWithClose()
        mManager = LoaderManager.getInstance(this)
        val viewModelProvider = ViewModelProvider(this)
        viewModel = viewModelProvider[TransactionEditViewModel::class.java]
        currencyViewModel = viewModelProvider[CurrencyViewModel::class.java]
        with(injector) {
            inject(viewModel)
            inject(currencyViewModel)
        }
        //we enable it only after accountCursor has been loaded, preventing NPE when user clicks on it early
        amountInput.setTypeEnabled(false)

        if (savedInstanceState != null) {
            delegate = TransactionDelegate.create(
                operationType,
                isTemplate,
                rootBinding,
                dateEditBinding,
                methodRowBinding,
                injector
            )
            setupObservers(true)
            delegate.bind(
                null,
                withTypeSpinner,
                savedInstanceState,
                null,
                withAutoFill
            )
            setHelpVariant(delegate.helpVariant)
            setTitle()
            if (isTemplate) {
                refreshPlanData(false)
            }
            floatingActionButton.show()
        } else {
            areDatesLinked = prefHandler.getBoolean(PrefKey.DATES_ARE_LINKED, false)
            updateDateLink()
            val extras = intent.extras
            var mRowId = Utils.getFromExtra(extras, KEY_ROWID, 0L)
            var task: InstantiationTask? = null
            if (mRowId == 0L) {
                mRowId = intent.getLongExtra(KEY_TEMPLATEID, 0L)
                if (mRowId != 0L) {
                    task = if (hasCreateFromTemplateAction) {
                        TRANSACTION_FROM_TEMPLATE
                    } else {
                        isTemplate = true
                        TEMPLATE
                    }
                }
            } else {
                task = if (intent.getBooleanExtra(KEY_TEMPLATE_FROM_TRANSACTION, false)) {
                    isTemplate = true
                    TEMPLATE_FROM_TRANSACTION
                } else {
                    TRANSACTION
                }
            }
            newInstance =
                mRowId == 0L || task == TRANSACTION_FROM_TEMPLATE || task == TEMPLATE_FROM_TRANSACTION
            withTypeSpinner = mRowId == 0L
            //were we called from a notification
            val notificationId = intent.getIntExtra(MyApplication.KEY_NOTIFICATION_ID, 0)
            if (notificationId > 0) {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
                    notificationId
                )
            }
            if (Intent.ACTION_INSERT == intent.action && extras != null) {
                task = FROM_INTENT_EXTRAS
            }
            // fetch the transaction or create a new instance
            if (task != null) {
                viewModel.transaction(mRowId, task, isClone, true, extras).observe(this) {
                    populateFromTask(it, task)
                }
            } else {
                operationType =
                    intent.getIntExtra(Transactions.OPERATION_TYPE, TYPE_TRANSACTION)
                if (!isValidType(operationType)) {
                    operationType = TYPE_TRANSACTION
                }
                val isNewTemplate = intent.getBooleanExtra(KEY_NEW_TEMPLATE, false)
                if (operationType == TYPE_SPLIT) {
                    val allowed: Boolean
                    val contribFeature: ContribFeature
                    if (isNewTemplate) {
                        contribFeature = ContribFeature.SPLIT_TEMPLATE
                        allowed = prefHandler.getBoolean(PrefKey.NEW_SPLIT_TEMPLATE_ENABLED, true)
                    } else {
                        contribFeature = ContribFeature.SPLIT_TRANSACTION
                        allowed = licenceHandler.hasTrialAccessTo(contribFeature)
                    }
                    if (!allowed) {
                        abortWithMessage(contribFeature.buildRequiresString(this))
                        return
                    }
                }
                parentId = intent.getLongExtra(KEY_PARENTID, 0)
                val currencyUnit = intent.getStringExtra(KEY_CURRENCY)
                    ?.let { currencyContext.get(it) }

                lifecycleScope.launch {
                    populateWithNewInstance(
                        if (isNewTemplate) {
                            isTemplate = true
                            viewModel.newTemplate(
                                operationType,
                                if (parentId != 0L) parentId else null
                            )?.also {
                                mRowId = it.id
                                it.defaultAction = prefHandler.enumValueOrDefault(
                                    PrefKey.TEMPLATE_CLICK_DEFAULT,
                                    Template.Action.SAVE
                                )
                            }
                        } else {
                            var accountId = intent.getLongExtra(KEY_ACCOUNTID, 0)
                            when (operationType) {
                                TYPE_TRANSACTION -> {
                                    if (accountId == 0L) {
                                        accountId = prefHandler.getLong(
                                            PrefKey.TRANSACTION_LAST_ACCOUNT_FROM_WIDGET,
                                            0L
                                        )
                                    }
                                    viewModel.newTransaction(
                                        accountId,
                                        currencyUnit,
                                        if (parentId != 0L) parentId else null
                                    )
                                }

                                TYPE_TRANSFER -> {
                                    var transferAccountId = 0L
                                    if (accountId == 0L) {
                                        accountId = prefHandler.getLong(
                                            PrefKey.TRANSFER_LAST_ACCOUNT_FROM_WIDGET,
                                            0L
                                        )
                                        transferAccountId = prefHandler.getLong(
                                            PrefKey.TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET,
                                            0L
                                        )
                                    }
                                    viewModel.newTransfer(
                                        accountId,
                                        currencyUnit,
                                        if (transferAccountId != 0L) transferAccountId else null,
                                        if (parentId != 0L) parentId else null
                                    )
                                }

                                TYPE_SPLIT -> {
                                    if (accountId == 0L) {
                                        accountId =
                                            prefHandler.getLong(
                                                PrefKey.SPLIT_LAST_ACCOUNT_FROM_WIDGET,
                                                0L
                                            )
                                    }
                                    viewModel.newSplit(accountId, currencyUnit)?.also {
                                        mRowId = it.id
                                    }
                                }

                                else -> throw IllegalStateException()
                            }
                        }
                    )
                }
            }
            if (newInstance) {
                if (operationType != TYPE_TRANSFER) {
                    discoveryHelper.discover(
                        this, amountInput.typeButton(), 1,
                        DiscoveryHelper.Feature.expense_income_switch
                    )
                }
            }
        }
        viewModel.getMethods().observe(this) { paymentMethods ->
            if (::delegate.isInitialized) {
                delegate.setMethods(paymentMethods)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bulkDeleteState.filterNotNull().collect {
                    onDeleteResult(it)
                }
            }
        }
        dateEditBinding.DateLink.setOnClickListener {
            areDatesLinked = !areDatesLinked
            prefHandler.putBoolean(PrefKey.DATES_ARE_LINKED, areDatesLinked)
            updateDateLink()
        }
        rootBinding.AttachImage.setOnClickListener {
            startMediaChooserDo()
        }
        rootBinding.PictureContainer.root.setOnClickListener {
            showPicturePopupMenu(it)
        }
        rootBinding.TagRow.bindListener()
    }

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.add(0, R.id.EDIT_COMMAND, 0, R.string.menu_edit)
        menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete)
    }

    private fun onDeleteResult(result: ContentResolvingAndroidViewModel.DeleteState) {
        if (result is DeleteComplete) {
            if (result.success == 1) {
                showSnackBar(
                    resources.getQuantityString(
                        R.plurals.delete_success,
                        result.success,
                        result.success
                    )
                )
                setDirty()
            } else {
                showDeleteFailureFeedback(null)
            }
            viewModel.bulkDeleteCompleteShown()
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as ContextAwareRecyclerView.RecyclerContextMenuInfo
        return when (item.itemId) {
            R.id.EDIT_COMMAND -> {
                startActivityForResult(Intent(this, ExpenseEdit::class.java).apply {
                    putExtra(if (isTemplate) KEY_TEMPLATEID else KEY_ROWID, info.id)
                }, EDIT_REQUEST)
                true
            }

            R.id.DELETE_COMMAND -> {
                if (isTemplate) {
                    viewModel.deleteTemplates(longArrayOf(info.id), false)
                        .observe(this) {
                            onDeleteResult(it)
                        }
                } else {
                    viewModel.deleteTransactions(longArrayOf(info.id))
                }
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    private fun setupObservers(fromSavedState: Boolean) {
        loadAccounts(fromSavedState)
        loadTemplates()
        linkInputsWithLabels()
        loadTags()
        loadCurrencies()
        observeMoveResult()
        observeAutoFillData()
    }

    private fun collectSplitParts() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.splitParts.collect { transactions ->
                    (delegate as? SplitDelegate)?.also {
                        it.showSplits(transactions)
                    }
                        ?: run { CrashHandler.report(java.lang.IllegalStateException("expected SplitDelegate, found ${delegate::class.java.name}")) }
                }
            }
        }
    }

    private fun loadDebts() {
        if (shouldLoadDebts) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.loadDebts(delegate.rowId).collect { debts ->
                        (delegate as? MainDelegate)?.let {
                            it.setDebts(debts)
                            it.setupDebtChangedListener()
                        }
                    }
                }
            }
        }
    }

    private fun loadTags() {
        viewModel.tagsLiveData.observe(this) { tags ->
            if (::delegate.isInitialized) {
                delegate.showTags(tags) { tag ->
                    viewModel.removeTag(tag)
                    setDirty()
                }
            }
        }
    }

    private fun loadTemplates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.templates.collect { templates ->
                    menuItem2TemplateMap.clear()
                    for (template in templates) {
                        val menuId = ViewCompat.generateViewId()
                        menuItem2TemplateMap[menuId] = template
                        invalidateOptionsMenu()
                    }
                }
            }
        }
    }

    @VisibleForTesting
    open fun setAccounts(accounts: List<Account>) {
        if (accounts.isEmpty()) {
            abortWithMessage(getString(R.string.warning_no_account))
        } else if (accounts.size == 1 && operationType == TYPE_TRANSFER) {
            abortWithMessage(getString(R.string.dialog_command_disabled_insert_transfer))
        } else {
            if (::delegate.isInitialized) {
                delegate.setAccounts(accounts)
                loadDebts()
                accountsLoaded = true
                if (mIsResumed) setupListeners()
            }
        }
    }

    private fun loadAccounts(fromSavedState: Boolean) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accounts.collect {
                    setAccounts(it)
                    collectSplitParts()
                    if (operationType == TYPE_SPLIT) {
                        viewModel.loadSplitParts(delegate.rowId, isTemplate)
                    }
                }
            }
        }
    }

    private fun loadCurrencies() {
        lifecycleScope.launchWhenStarted {
            currencyViewModel.currencies.collect { currencies ->
                if (::delegate.isInitialized) {
                    delegate.setCurrencies(currencies)
                }
            }
        }
    }

    private fun abortWithMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onResume() {
        super.onResume()
        mIsResumed = true
        if (accountsLoaded) setupListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        pObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (ise: IllegalStateException) { // Do Nothing.  Observer has already been unregistered.
            }
        }
        if (::delegate.isInitialized) delegate.onDestroy()
    }

    private fun populateFromTask(
        transaction: Transaction?,
        task: InstantiationTask
    ) {
        transaction?.let {
            if (transaction.isSealed) {
                abortWithMessage("This transaction refers to a closed account or debt and can no longer be edited")
            } else {
                populate(it, withAutoFill && task != TRANSACTION_FROM_TEMPLATE)

            }
        } ?: run {
            abortWithMessage(
                when (task) {
                    TRANSACTION, TEMPLATE -> "Object has been deleted from db"
                    TRANSACTION_FROM_TEMPLATE -> getString(R.string.save_transaction_template_deleted)
                    FROM_INTENT_EXTRAS -> "Unable to build transaction from extras"
                    TEMPLATE_FROM_TRANSACTION -> "Unable to build template from transaction"
                }
            )
        }
    }

    private fun populateWithNewInstance(transaction: Transaction?) {
        transaction?.let { populate(it, withAutoFill) } ?: run {
            val errMsg = getString(R.string.warning_no_account)
            abortWithMessage(errMsg)
        }
        intent.getParcelableExtra<OcrResultFlat>(KEY_OCR_RESULT)?.let { ocrResultFlat ->
            ocrResultFlat.amount?.let { amountInput.setRaw(it) }
            ocrResultFlat.date?.let { pair ->
                dateEditBinding.DateButton.setDate(pair.first)
                pair.second?.let { dateEditBinding.TimeButton.setTime(it) }
            }
            ocrResultFlat.payee?.let {
                rootBinding.Payee.setText(it.name)
                startAutoFill(it.id, true)
            }
        }
        intent.getParcelableExtra<Uri>(KEY_PICTURE_URI)?.let {
            delegate.setPicture(it)
        }
        if (!intent.hasExtra(KEY_CACHED_DATA)) {
            amountInput.type = intent.getBooleanExtra(KEY_INCOME, false)
        }
        (intent.getSerializableExtra(KEY_AMOUNT) as? BigDecimal)?.let {
            amountInput.setAmount(it)
            (delegate as? TransferDelegate)?.configureTransferDirection()
        }
    }

    private fun populate(transaction: Transaction, withAutoFill: Boolean) {
        parentId = transaction.parentId ?: 0L
        if (isClone) {
            transaction.crStatus = CrStatus.UNRECONCILED
            transaction.status = STATUS_NONE
            transaction.uuid = Model.generateUuid()
            newInstance = true
        }
        //processing data from user switching operation type
        val cached = intent.getParcelableExtra(KEY_CACHED_DATA) as? CachedTransaction
        if (cached != null) {
            transaction.accountId = cached.accountId
            transaction.methodId = cached.methodId
            transaction.date = cached.date
            transaction.valueDate = cached.valueDate
            transaction.crStatus = cached.crStatus
            transaction.comment = cached.comment
            transaction.payee = cached.payee
            (transaction as? Template)?.let { template ->
                with(cached.cachedTemplate!!) {
                    template.title = title
                    template.isPlanExecutionAutomatic = isPlanExecutionAutomatic
                    template.planExecutionAdvance = planExecutionAdvance
                }
            }
            transaction.referenceNumber = cached.referenceNumber
            transaction.amount = cached.amount
            transaction.originalAmount = cached.originalAmount
            transaction.equivalentAmount = cached.equivalentAmount
            transaction.pictureUri = cached.pictureUri
            setDirty()
        } else {
            intent.getLongExtra(KEY_DATE, 0).takeIf { it != 0L }?.let {
                transaction.date = it
                transaction.valueDate = it
            }
        }
        operationType = transaction.operationType()
        delegate = TransactionDelegate.create(
            transaction,
            rootBinding,
            dateEditBinding,
            methodRowBinding,
            injector
        )
        setupObservers(false)
        if (intent.getBooleanExtra(KEY_CREATE_TEMPLATE, false)) {
            createTemplate = true
            delegate.setCreateTemplate(true)
        }
        delegate.bindUnsafe(
            transaction,
            withTypeSpinner,
            null,
            cached?.recurrence,
            withAutoFill,
            cached != null
        )
        cached?.cachedTemplate?.date?.let {
            delegate.planButton.setDate(it)
        }
        if (accountsLoaded) {
            delegate.setAccount()
        }
        setHelpVariant(delegate.helpVariant)
        setTitle()
        shouldShowCreateTemplate = transaction.originTemplateId == null
        if (!isTemplate) {
            createNew = newInstance && prefHandler.getBoolean(saveAndNewPrefKey, false)
            updateFab()
        }
        invalidateOptionsMenu()
        cached?.tags?.let {
            viewModel.updateTags(it, true)
        }
        rootBinding.CREATEPARTCOMMAND.setOnClickListener {
            createRow()
        }
    }

    private val saveAndNewPrefKey: PrefKey
        get() = if (isSplitPart) PrefKey.EXPENSE_EDIT_SAVE_AND_NEW_SPLIT_PART else PrefKey.EXPENSE_EDIT_SAVE_AND_NEW

    private fun setTitle() {
        if (withTypeSpinner) {
            supportActionBar!!.setDisplayShowTitleEnabled(false)
        } else {
            title = delegate.title(newInstance)
        }
    }

    override fun onValueSet(view: View) {
        super.onValueSet(view)
        if (view is DateButton) {
            val date = view.date
            if (areDatesLinked) {
                when (view.id) {
                    R.id.Date2Button -> dateEditBinding.DateButton
                    R.id.DateButton -> dateEditBinding.Date2Button
                    else -> null
                }?.setDate(date)
            }
            if (::delegate.isInitialized) {
                delegate.configureLastDayButton()
            }
        }
    }

    private fun updateDateLink() {
        dateEditBinding.DateLink.setImageResource(if (areDatesLinked) R.drawable.ic_hchain else R.drawable.ic_hchain_broken)
        dateEditBinding.DateLink.contentDescription =
            getString(if (areDatesLinked) R.string.content_description_dates_are_linked else R.string.content_description_dates_are_not_linked)
    }

    override fun setupListeners() {
        super.setupListeners()
        delegate.setupListeners(this)
    }

    @VisibleForTesting
    val currentAccount: Account?
        get() = if (::delegate.isInitialized) delegate.currentAccount() else null

    override fun onTypeChanged(isChecked: Boolean) {
        super.onTypeChanged(isChecked)
        if (shouldLoadMethods) {
            loadMethods(currentAccount)
        }
        (delegate as? MainDelegate)?.onAmountChanged()
        discoveryHelper.markDiscovered(DiscoveryHelper.Feature.expense_income_switch)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (::delegate.isInitialized) {
            menu.findItem(R.id.SAVE_AND_NEW_COMMAND)?.let {
                it.isChecked = createNew
                checkMenuIcon(it)
            }
            menu.findItem(R.id.CREATE_TEMPLATE_COMMAND)?.let {
                it.isChecked = createTemplate
                checkMenuIcon(it)
            }
            menu.findItem(R.id.ORIGINAL_AMOUNT_COMMAND)?.let {
                it.isChecked = delegate.originalAmountVisible
            }
            val currentAccount = currentAccount
            menu.findItem(R.id.EQUIVALENT_AMOUNT_COMMAND)?.let {
                it.setEnabledAndVisible(
                    !(currentAccount == null || hasHomeCurrency(currentAccount))
                )
                it.isChecked = delegate.equivalentAmountVisible
            }
            menu.findItem(R.id.MANAGE_TEMPLATES_COMMAND)?.let {
                it.subMenu?.let { subMenu ->
                    subMenu.clear()
                    menuItem2TemplateMap.forEach { entry ->
                        subMenu.add(Menu.NONE, entry.key, Menu.NONE, entry.value.title)
                    }
                }
                it.setEnabledAndVisible(menuItem2TemplateMap.isNotEmpty())
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun hasHomeCurrency(account: Account): Boolean {
        return account.currency == homeCurrency
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (!isTemplate) {
            menu.add(Menu.NONE, R.id.SAVE_AND_NEW_COMMAND, 0, R.string.menu_save_and_new)
                .setCheckable(true)
                .setIcon(R.drawable.ic_action_save_new)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        if (!isSplitPartOrTemplate) {
            menu.addSubMenu(
                Menu.NONE,
                R.id.MANAGE_TEMPLATES_COMMAND,
                0,
                R.string.widget_title_templates
            ).apply {
                item.setIcon(R.drawable.ic_menu_template)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
            if (shouldShowCreateTemplate) {
                menu.add(
                    Menu.NONE,
                    R.id.CREATE_TEMPLATE_COMMAND,
                    0,
                    R.string.menu_create_template_from_transaction
                )
                    .setCheckable(true)
                    .setIcon(R.drawable.ic_action_template_add)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
        }
        if (operationType == TYPE_TRANSFER) {
            menu.add(Menu.NONE, R.id.INVERT_TRANSFER_COMMAND, 0, R.string.menu_invert_transfer)
                .setIcon(R.drawable.ic_menu_move)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        } else if (isMainTransaction) {
            menu.add(
                Menu.NONE,
                R.id.ORIGINAL_AMOUNT_COMMAND,
                0,
                R.string.menu_original_amount
            ).isCheckable =
                true
            menu.add(
                Menu.NONE,
                R.id.EQUIVALENT_AMOUNT_COMMAND,
                0,
                R.string.menu_equivalent_amount
            ).isCheckable =
                true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        handleTemplateMenuItem(item) || super.onOptionsItemSelected(item)

    private fun handleTemplateMenuItem(item: MenuItem): Boolean {
        return menuItem2TemplateMap[item.itemId]?.let {
            if (isDirty) {
                Bundle().apply {
                    putString(
                        ConfirmationDialogFragment.KEY_MESSAGE,
                        getString(R.string.confirmation_load_template_discard_data)
                    )
                    putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.LOAD_TEMPLATE_DO)
                    putLong(KEY_ROWID, it.id)
                    ConfirmationDialogFragment.newInstance(this)
                        .show(supportFragmentManager, "CONFIRM_LOAD")
                }
            } else {
                loadTemplate(it.id)
            }
            true
        } ?: false
    }

    private fun loadTemplate(id: Long) {
        cleanup {
            val restartIntent = Intent(this, ExpenseEdit::class.java).apply {
                action = ACTION_CREATE_FROM_TEMPLATE
                putExtra(KEY_TEMPLATEID, id)
                putExtra(KEY_INSTANCEID, -1L)
            }
            finish()
            startActivity(restartIntent)
        }
    }

    override fun doSave(andNew: Boolean) {
        if (::delegate.isInitialized) {
            (delegate as? SplitDelegate)?.let {
                if (!it.splitComplete) {
                    showSnackBar(
                        getString(R.string.unsplit_amount_greater_than_zero),
                        Snackbar.LENGTH_SHORT
                    )
                    return
                }
            }
            super.doSave(andNew)
        }
    }

    override fun doHome() {
        cleanup {
            backwardCanceledTagsIntent()?.let {
                setResult(RESULT_CANCELED, it)
            }
            finish()
        }
    }

    private fun backwardCanceledTagsIntent() =
        viewModel.deletedTagIds.takeIf { it.isNotEmpty() }?.let {
            Intent().apply {
                putExtra(TagBaseViewModel.KEY_DELETED_IDS, it)
            }
        }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        when (command) {
            R.id.CREATE_TEMPLATE_COMMAND -> {
                if (::delegate.isInitialized) {
                    createTemplate = !createTemplate
                    delegate.setCreateTemplate(
                        createTemplate
                    )
                    invalidateOptionsMenu()
                }
            }

            R.id.SAVE_AND_NEW_COMMAND -> {
                createNew = !createNew
                prefHandler.putBoolean(saveAndNewPrefKey, createNew)
                updateFab()
                invalidateOptionsMenu()
                return true
            }

            R.id.INVERT_TRANSFER_COMMAND -> {
                if (::delegate.isInitialized) {
                    (delegate as? TransferDelegate)?.invert()
                    return true
                }
            }

            R.id.ORIGINAL_AMOUNT_COMMAND -> {
                if (::delegate.isInitialized) {
                    delegate.toggleOriginalAmount()
                    invalidateOptionsMenu()
                    return true
                }
            }

            R.id.EQUIVALENT_AMOUNT_COMMAND -> {
                if (::delegate.isInitialized) {
                    delegate.toggleEquivalentAmount()
                    invalidateOptionsMenu()
                    return true
                }
            }
        }
        return false
    }

    private fun createRow() {
        val account = currentAccount
        if (account == null) {
            showSnackBar(R.string.account_list_not_yet_loaded)
            return
        }
        startActivityForResult(Intent(this, ExpenseEdit::class.java).apply {
            forwardDataEntryFromWidget(this)
            putExtra(Transactions.OPERATION_TYPE, TYPE_TRANSACTION)
            putExtra(KEY_ACCOUNTID, account.id)
            putExtra(KEY_PARENTID, delegate.rowId)
            putExtra(KEY_PARENT_HAS_DEBT, (delegate as? MainDelegate)?.debtId != null)
            putExtra(KEY_PARENT_ORIGINAL_AMOUNT_EXCHANGE_RATE, delegate.originalAmountExchangeRate)
            putExtra(KEY_PAYEEID, (delegate as? MainDelegate)?.payeeId)
            putExtra(KEY_NEW_TEMPLATE, isMainTemplate)
            putExtra(KEY_INCOME, delegate.isIncome)
        }, EDIT_REQUEST)
    }

    /**
     * calls the activity for selecting (and managing) categories
     */
    fun startSelectCategory() {
        val i = Intent(this, ManageCategories::class.java)
        forwardDataEntryFromWidget(i)
        //we pass the currently selected category in to prevent
        //it from being deleted, which can theoretically lead
        //to crash upon saving https://github.com/mtotschnig/MyExpenses/issues/71
        (delegate as? CategoryDelegate)?.catId?.let {
            i.putExtra(KEY_PROTECTION_INFO, ManageCategories.ProtectionInfo(it, isTemplate))
        }
        startActivityForResult(i, SELECT_CATEGORY_REQUEST)
    }

    override fun saveState() {
        if (::delegate.isInitialized) {
            delegate.syncStateAndValidate(true)?.let { transaction ->
                isSaving = true
                if (planInstanceId > 0L) {
                    transaction.originPlanInstanceId = planInstanceId
                }
                viewModel.save(transaction).observe(this) {
                    onSaved(it, transaction)
                }
                if (intent.getBooleanExtra(EXTRA_START_FROM_WIDGET, false)) {
                    when (operationType) {
                        TYPE_TRANSACTION -> prefHandler.putLong(
                            PrefKey.TRANSACTION_LAST_ACCOUNT_FROM_WIDGET,
                            accountId
                        )

                        TYPE_TRANSFER -> {
                            prefHandler.putLong(
                                PrefKey.TRANSFER_LAST_ACCOUNT_FROM_WIDGET,
                                accountId
                            )
                            (delegate as? TransferDelegate)?.mTransferAccountId?.let {
                                prefHandler.putLong(
                                    PrefKey.TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET,
                                    it
                                )
                            }
                        }

                        TYPE_SPLIT -> prefHandler.putLong(
                            PrefKey.SPLIT_LAST_ACCOUNT_FROM_WIDGET,
                            accountId
                        )
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (requestCode) {
            SELECT_CATEGORY_REQUEST -> if (intent != null) {
                (delegate as? CategoryDelegate)?.setCategory(
                    intent.getStringExtra(KEY_LABEL),
                    intent.getStringExtra(KEY_ICON),
                    intent.getLongExtra(KEY_ROWID, 0)
                )
                setDirty()
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(intent)
                if (resultCode == RESULT_OK) {
                    setPicture(result.uri)
                    viewModel.cleanupOrigFile(result)
                } else {
                    processImageCaptureError(resultCode, result)
                }
            }

            PLAN_REQUEST -> finish()
            EDIT_REQUEST -> if (resultCode == RESULT_OK) {
                setDirty()
            }
        }
    }

    override fun dispatchOnBackPressed() {
        hideKeyboard()
        cleanup {
            backwardCanceledTagsIntent()?.let {
                setResult(RESULT_CANCELED, it)
            }
            super.dispatchOnBackPressed()
        }
    }

    private fun cleanup(onComplete: () -> Unit) {
        if (operationType == TYPE_SPLIT && ::delegate.isInitialized) {
            delegate.rowId.let {
                viewModel.cleanupSplit(it, isTemplate).observe(this) {
                    onComplete()
                }
            }
        } else {
            onComplete()
        }
    }

    /**
     * updates interface based on type (EXPENSE or INCOME)
     */
    override fun configureType() {
        delegate.configureType()
    }

    private inner class PlanObserver : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            refreshPlanData(true)
        }
    }

    private fun refreshPlanData(fromObserver: Boolean) {
        delegate.planId?.let { planId ->
            viewModel.plan(planId).observe(this) { plan ->
                plan?.let { delegate.configurePlan(it, fromObserver) }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::delegate.isInitialized) {
            delegate.onSaveInstanceState(outState)
        }
    }

    val amount: Money?
        get() = currentAccount?.let {
            Money(it.currency, validateAmountInput(showToUser = false, ifPresent = false)!!)
        }

    private fun unsetPicture() {
        setPicture(null)
    }

    private fun setPicture(pictureUri: Uri?) {
        delegate.setPicture(pictureUri)
    }

    fun isValidType(type: Int): Boolean {
        return type == TYPE_SPLIT || type == TYPE_TRANSACTION || type == TYPE_TRANSFER
    }

    fun loadMethods(account: Account?) {
        if (account != null) {
            viewModel.loadMethods(isIncome, account.type)
        }
    }

    fun restartWithType(newType: Int) {
        val bundle = Bundle()
        bundle.putInt(Tracker.EVENT_PARAM_OPERATION_TYPE, newType)
        logEvent(Tracker.EVENT_SELECT_OPERATION_TYPE, bundle)
        cleanup {
            val restartIntent = intent.apply {
                putExtra(Transactions.OPERATION_TYPE, newType)
                if (isDirty) {
                    delegate.syncStateAndValidate(false)?.let {
                        putExtra(
                            KEY_CACHED_DATA, it.toCached(
                                delegate.recurrenceSpinner.selectedItem as? Recurrence,
                                delegate.planButton.date
                            )
                        )
                    }
                }
                putExtra(KEY_CREATE_TEMPLATE, createTemplate)
            }
            finish()
            startActivity(restartIntent)
        }
    }

    private fun onSaved(result: Result<Long>, transaction: ITransaction) {
        result.onSuccess {
            if (operationType == TYPE_SPLIT) {
                recordUsage(ContribFeature.SPLIT_TRANSACTION)
            }
            if (createNew) {
                delegate.prepareForNew()
                newInstance = true
                clearDirty()
                showSnackBar(
                    getString(R.string.save_transaction_and_new_success),
                    Snackbar.LENGTH_SHORT
                )
            } else {
                if (delegate.recurrenceSpinner.selectedItem === Recurrence.CUSTOM) {
                    if (isTemplate) {
                        (transaction as? Template)?.planId
                    } else {
                        transaction.originPlanId
                    }?.let { launchPlanView(true, it) }
                } else { //make sure soft keyboard is closed
                    hideKeyboard()
                    setResult(RESULT_OK, backwardCanceledTagsIntent())
                    finish()
                    //no need to call super after finish
                    return
                }
            }
        }.onFailure {
            showSnackBar(
                when (it) {
                    is ExternalStorageNotAvailableException -> "External storage (sdcard) not available"
                    is UnknownPictureSaveException -> {
                        val customData = buildMap {
                            put("pictureUri", it.pictureUri.toString())
                            put("homeUri", it.homeUri.toString())
                        }
                        CrashHandler.report(it, customData)
                        "Error while saving picture"
                    }

                    is Plan.CalendarIntegrationNotAvailableException -> {
                        delegate.recurrenceSpinner.setSelection(0)
                        "Recurring transactions are not available, because calendar integration is not functional on this device."
                    }

                    else -> {
                        CrashHandler.report(it)
                        (delegate as? CategoryDelegate)?.resetCategory()
                        "Error while saving transaction: ${it.safeMessage}"
                    }
                }
            )
        }
        isSaving = false
    }

    fun launchPlanView(forResult: Boolean, planId: Long) {
        val intent = Intent(Intent.ACTION_VIEW)
        //ACTION_VIEW expects to get a range http://code.google.com/p/android/issues/detail?id=23852
        //intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, mPlan!!.dtstart)
        //intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, mPlan!!.dtstart)
        intent.data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, planId)
        startActivity(
            intent,
            R.string.no_calendar_app_installed,
            if (forResult) PLAN_REQUEST else null
        )
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (feature === ContribFeature.SPLIT_TRANSACTION) {
            restartWithType(TYPE_SPLIT)
        }
    }

    override fun contribFeatureNotCalled(feature: ContribFeature) {
        if (feature === ContribFeature.SPLIT_TRANSACTION) {
            delegate.resetOperationType()
        }
    }

    override fun onPositive(args: Bundle, checked: Boolean) {
        super.onPositive(args, checked)
        when (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
            R.id.AUTO_FILL_COMMAND -> {
                startAutoFill(args.getLong(KEY_ROWID), true)
                enableAutoFill(prefHandler)
            }

            R.id.LOAD_TEMPLATE_DO -> {
                loadTemplate(args.getLong(KEY_ROWID))
            }
        }
    }

    /**
     * @param id                  id of Payee/Payer for whom data should be loaded
     * @param overridePreferences if true data is loaded irrespective of what is set in preferences
     */
    fun startAutoFill(id: Long, overridePreferences: Boolean) {
        viewModel.startAutoFill(
            id,
            overridePreferences,
            intent.getBooleanExtra(KEY_AUTOFILL_MAY_SET_ACCOUNT, false)
        )
    }

    override fun onNegative(args: Bundle) {
        if (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE) == R.id.AUTO_FILL_COMMAND) {
            disableAutoFill(prefHandler)
        }
    }

    override fun onPause() {
        mIsResumed = false
        super.onPause()
    }

    fun showPicturePopupMenu(v: View) {
        val popup = PopupMenu(this, v)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            handlePicturePopupMenuClick(item.itemId)
            true
        }
        popup.inflate(R.menu.picture_popup)
        popup.show()
    }

    private fun handlePicturePopupMenuClick(command: Int) {
        when (command) {
            R.id.DELETE_COMMAND -> unsetPicture()
            R.id.VIEW_COMMAND -> delegate.pictureUri?.let {
                imageViewIntentProvider.startViewIntent(
                    this,
                    it
                )
            }

            R.id.CHANGE_COMMAND -> startMediaChooserDo()
        }
    }

    override fun contribFeatureRequested(feature: ContribFeature, tag: Serializable?) {
        hideKeyboard()
        super.contribFeatureRequested(feature, tag)
    }

    private fun startMediaChooserDo() {
        startMediaChooserDo(PictureDirHelper.defaultFileName, false)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        super.onPermissionsGranted(requestCode, perms)
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR &&
            PermissionHelper.PermissionGroup.CALENDAR.androidPermissions.all { perms.contains(it) }
        ) {
            delegate.onCalendarPermissionsResult(true)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        super.onPermissionsDenied(requestCode, perms)
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR &&
            PermissionHelper.PermissionGroup.CALENDAR.androidPermissions.any { perms.contains(it) }
        ) {
            delegate.onCalendarPermissionsResult(false)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        delegate.isProcessingLinkedAmountInputs = true
        exchangeRateEdit.setBlockWatcher(true)
        super.onRestoreInstanceState(savedInstanceState)
        exchangeRateEdit.setBlockWatcher(false)
        delegate.isProcessingLinkedAmountInputs = false
        if (delegate.rowId == 0L) {
            (delegate as? TransferDelegate)?.configureTransferDirection()
        }
        updateFab()
        updateDateLink()
        if (!isSplitPartOrTemplate) {
            delegate.setCreateTemplate(createTemplate)
        }
    }

    private fun updateFab() {
        floatingActionButton.let {
            it.setImageResource(if (createNew) R.drawable.ic_action_save_new else R.drawable.ic_menu_done)
            it.contentDescription =
                getString(if (createNew) R.string.menu_save_and_new_content_description else R.string.menu_save_help_text)
        }
    }

    fun showPlanMonthFragment(originTemplate: Template, color: Int) {
        PlanMonthFragment.newInstance(
            originTemplate.title,
            originTemplate.id,
            originTemplate.planId,
            color, true, prefHandler
        ).show(
            supportFragmentManager,
            TemplatesList.CALDROID_DIALOG_FRAGMENT_TAG
        )
    }

    fun observePlan(planId: Long) {
        if (pObserver == null) {
            pObserver = PlanObserver().also {
                contentResolver.registerContentObserver(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, planId),
                    false, it
                )
            }
        }
    }

    fun loadOriginTemplate(templateId: Long) {
        viewModel.transaction(templateId, TEMPLATE, clone = false, forEdit = false, extras = null)
            .observe(this) { transaction ->
                (transaction as? Template)?.let { delegate.originTemplateLoaded(it) }
            }
    }

    @Parcelize
    data class CachedTransaction(
        val accountId: Long,
        val methodId: Long?,
        val date: Long,
        val valueDate: Long,
        val crStatus: CrStatus,
        val comment: String?,
        val payee: String?,
        val cachedTemplate: CachedTemplate?,
        val referenceNumber: String?,
        val amount: Money,
        val originalAmount: Money?,
        val equivalentAmount: Money?,
        val pictureUri: Uri?,
        val recurrence: Recurrence?,
        val tags: List<Tag>?
    ) : Parcelable

    @Parcelize
    data class CachedTemplate(
        val title: String?,
        val isPlanExecutionAutomatic: Boolean,
        val planExecutionAdvance: Int,
        val date: LocalDate
    ) : Parcelable

    private fun ITransaction.toCached(withRecurrence: Recurrence?, withPlanDate: LocalDate) =
        CachedTransaction(
            accountId,
            methodId,
            if (this is Template) plan?.dtStart ?: 0L else date,
            valueDate,
            crStatus,
            comment,
            payee,
            (this as? Template)?.run {
                CachedTemplate(
                    title,
                    isPlanExecutionAutomatic,
                    planExecutionAdvance,
                    withPlanDate
                )
            },
            referenceNumber,
            amount,
            originalAmount,
            equivalentAmount,
            pictureUri,
            withRecurrence,
            viewModel.tagsLiveData.value
        )

    companion object {
        const val KEY_NEW_TEMPLATE = "newTemplate"
        const val KEY_CLONE = "clone"
        const val KEY_TEMPLATE_FROM_TRANSACTION = "templateFromTransaction"
        private const val KEY_CACHED_DATA = "cachedData"
        const val KEY_CREATE_TEMPLATE = "createTemplate"
        const val KEY_AUTOFILL_MAY_SET_ACCOUNT = "autoFillMaySetAccount"
        const val KEY_OCR_RESULT = "ocrResult"
        const val KEY_INCOME = "income"
        const val KEY_PARENT_HAS_DEBT = "parentHasSplit"

        /**
         * holds pair of rate and currency
         */
        const val KEY_PARENT_ORIGINAL_AMOUNT_EXCHANGE_RATE = "parentOriginalAmountExchangeRate"

        const val ACTION_CREATE_FROM_TEMPLATE = "CREATE_FROM_TEMPLATE"
    }

    fun loadActiveTags(id: Long) {
        if (withAutoFill) {
            viewModel.loadActiveTags(id)
        }
    }

    fun startMoveSplitParts(rowId: Long, accountId: Long) {
        showSnackBarIndefinite(R.string.progress_dialog_updating_split_parts)
        viewModel.moveUnCommittedSplitParts(rowId, accountId, isTemplate)
    }

    private fun observeMoveResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.moveResult.collect { result ->
                    result?.let {
                        dismissSnackBar()
                        (delegate as? SplitDelegate)?.onUncommittedSplitPartsMoved(it)
                        viewModel.moveResultProcessed()
                    }
                }
            }
        }
    }

    private fun observeAutoFillData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.autoFillData.collect { data ->
                    data?.let {
                        (delegate as? CategoryDelegate)?.autoFill(it)
                        viewModel.autoFillDone()
                    }
                }
            }
        }
    }

    override fun handleDeletedTagIds(ids: LongArray) {
        super.handleDeletedTagIds(ids)
        if (isSplitPart) {
            viewModel.deletedTagIds = ids
        }
    }
}