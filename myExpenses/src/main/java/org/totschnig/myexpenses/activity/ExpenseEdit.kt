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

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.android.calendar.CalendarContractCompat
import com.google.android.material.snackbar.Snackbar
import icepick.Icepick
import icepick.State
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.delegate.CategoryDelegate
import org.totschnig.myexpenses.delegate.SplitDelegate
import org.totschnig.myexpenses.delegate.TransactionDelegate
import org.totschnig.myexpenses.delegate.TransferDelegate
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.fragment.PlanMonthFragment
import org.totschnig.myexpenses.fragment.SplitPartList
import org.totschnig.myexpenses.fragment.TemplatesList
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan.Recurrence
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PreferenceUtils
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ButtonWithDialog
import org.totschnig.myexpenses.ui.DateButton
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE
import org.totschnig.myexpenses.viewmodel.ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE
import org.totschnig.myexpenses.viewmodel.ERROR_PICTURE_SAVE_UNKNOWN
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.Account
import org.totschnig.myexpenses.viewmodel.TransactionViewModel
import org.totschnig.myexpenses.viewmodel.TransactionViewModel.InstantiationTask.FROM_INTENT_EXTRAS
import org.totschnig.myexpenses.viewmodel.TransactionViewModel.InstantiationTask.TEMPLATE
import org.totschnig.myexpenses.viewmodel.TransactionViewModel.InstantiationTask.TRANSACTION
import org.totschnig.myexpenses.viewmodel.TransactionViewModel.InstantiationTask.TRANSACTION_FROM_TEMPLATE
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import org.totschnig.myexpenses.widget.AbstractWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import timber.log.Timber
import java.io.Serializable
import java.util.*
import javax.inject.Inject

/**
 * Activity for editing a transaction
 *
 * @author Michael Totschnig
 */
class ExpenseEdit : AmountActivity(), LoaderManager.LoaderCallbacks<Cursor?>, ContribIFace, ConfirmationDialogListener, ButtonWithDialog.Host, ExchangeRateEdit.Host {
    private lateinit var rootBinding: OneExpenseBinding
    private lateinit var dateEditBinding: DateEditBinding
    override val amountLabel: TextView
        get() = rootBinding.AmountLabel
    override val amountRow: ViewGroup
        get() = rootBinding.AmountRow
    override val exchangeRateRow: ViewGroup
        get() = rootBinding.ERR.root as ViewGroup
    override val amountInput: AmountInput
        get() = rootBinding.Amount
    override val exchangeRateEdit: ExchangeRateEdit
        get() = rootBinding.ERR.ExchangeRate

    @JvmField
    @State
    var mRowId = 0L
    @JvmField
    @State
    var parentId = 0L
    @JvmField
    @State
    var pictureUriTemp: Uri? = null

    val accountId: Long
        get() = currentAccount?.id ?: 0L

    private var planInstanceId: Long = 0
    /**
     * transaction, transfer or split
     */
    @JvmField
    @State
    var operationType = 0
    private lateinit var mManager: LoaderManager
    private var createNew = false
    @JvmField
    @State
    var isTemplate = false
    private val recordTemplateWidget
        get() = intent.getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false) &&
                !ContribFeature.TEMPLATE_WIDGET.hasAccess()
    private var mIsResumed = false
    private var accountsLoaded = false
    private var shouldRecordAttachPictureFeature = false
    private var pObserver: ContentObserver? = null
    private lateinit var viewModel: TransactionEditViewModel
    private lateinit var currencyViewModel: CurrencyViewModel
    override fun getDate(): LocalDate {
        return dateEditBinding.Date2Button.date
    }

    enum class HelpVariant {
        transaction, transfer, split, templateCategory, templateTransfer, templateSplit, splitPartCategory, splitPartTransfer
    }

    @Inject
    lateinit var imageViewIntentProvider: ImageViewIntentProvider
    @Inject
    lateinit var currencyFormatter: CurrencyFormatter
    @Inject
    lateinit var discoveryHelper: DiscoveryHelper

    lateinit var delegate: TransactionDelegate<*>

    private val isSplitPart: Boolean
        get() = parentId != 0L

    private val isNoMainTransaction: Boolean
        get() = isSplitPart || isTemplate

    private val isMainTemplate: Boolean
        get() = isTemplate && !isSplitPart

    private val shouldLoadMethods: Boolean
        get() = operationType != TYPE_TRANSFER && !isSplitPart

    private val isMainTransactione: Boolean
        get() = operationType != TYPE_TRANSFER && !isSplitPart && !isTemplate

    public override fun getDiscardNewMessage(): Int {
        return if (isTemplate) R.string.dialog_confirm_discard_new_template else R.string.dialog_confirm_discard_new_transaction
    }

    override fun injectDependencies() {
        MyApplication.getInstance().appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootBinding = OneExpenseBinding.inflate(LayoutInflater.from(this))
        dateEditBinding = DateEditBinding.bind(rootBinding.root)
        setContentView(rootBinding.root)
        setupToolbar()
        mManager = LoaderManager.getInstance(this)
        viewModel = ViewModelProvider(this).get(TransactionEditViewModel::class.java)
        currencyViewModel = ViewModelProvider(this).get(CurrencyViewModel::class.java)
        //we enable it only after accountcursor has been loaded, preventing NPE when user clicks on it early
        amountInput.setTypeEnabled(false)

        if (savedInstanceState != null) {
            Icepick.restoreInstanceState(this, savedInstanceState)
            delegate = TransactionDelegate.create(operationType, isTemplate, rootBinding, dateEditBinding, prefHandler);
            loadData()
            delegate.bind(null, isCalendarPermissionPermanentlyDeclined, mNewInstance, savedInstanceState, null);
        } else {
            val extras = intent.extras
            var task: TransactionViewModel.InstantiationTask? = null
            mRowId = Utils.getFromExtra(extras, DatabaseConstants.KEY_ROWID, 0L)
            if (mRowId == 0L) {
                mRowId = intent.getLongExtra(DatabaseConstants.KEY_TEMPLATEID, 0L)
                if (mRowId != 0L) {
                    planInstanceId = getIntent().getLongExtra(KEY_INSTANCEID, 0)
                    if (planInstanceId != 0L) {
                        task = TRANSACTION_FROM_TEMPLATE
                    } else {
                        isTemplate = true
                        task = TEMPLATE
                    }
                }
            } else {
                task = TRANSACTION
            }

            //were we called from a notification
            val notificationId = intent.getIntExtra(MyApplication.KEY_NOTIFICATION_ID, 0)
            if (notificationId > 0) {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
            }
            if (Intent.ACTION_INSERT == intent.action && extras != null) {
                task = FROM_INTENT_EXTRAS
            }
            // fetch the transaction or create a new instance
            if (task != null) {
                mNewInstance = task == FROM_INTENT_EXTRAS
                //if called with extra KEY_CLONE, we ask the task to clone, but no longer after orientation change
                viewModel.transaction(mRowId, task, intent.getBooleanExtra(KEY_CLONE, false), true, extras).observe(this, Observer {
                    populateFromTask(it, task)
                })
            } else {
                operationType = intent.getIntExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
                if (!isValidType(operationType)) {
                    operationType = Transactions.TYPE_TRANSACTION
                }
                val isNewTemplate = intent.getBooleanExtra(KEY_NEW_TEMPLATE, false)
                if (operationType == Transactions.TYPE_SPLIT) {
                    val allowed: Boolean
                    val contribFeature: ContribFeature
                    if (isNewTemplate) {
                        contribFeature = ContribFeature.SPLIT_TEMPLATE
                        allowed = prefHandler.getBoolean(PrefKey.NEW_SPLIT_TEMPLATE_ENABLED, true)
                    } else {
                        contribFeature = ContribFeature.SPLIT_TRANSACTION
                        allowed = contribFeature.hasAccess() || contribFeature.usagesLeft(prefHandler) > 0
                    }
                    if (!allowed) {
                        abortWithMessage(contribFeature.buildRequiresString(this))
                        return
                    }
                }
                parentId = intent.getLongExtra(DatabaseConstants.KEY_PARENTID, 0)
                supportActionBar!!.setDisplayShowTitleEnabled(false)
                var accountId = intent.getLongExtra(DatabaseConstants.KEY_ACCOUNTID, 0)
                if (isNewTemplate) {
                    populateWithNewInstance(Template.getTypedNewInstance(operationType, accountId, true, if (parentId != 0L) parentId else null).also { mRowId = it.id })
                    isTemplate = true
                } else {
                    when (operationType) {
                        Transactions.TYPE_TRANSACTION -> {
                            if (accountId == 0L) {
                                accountId = prefHandler.getLong(PrefKey.TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, 0L)
                            }
                            populateWithNewInstance(Transaction.getNewInstance(accountId, if (parentId != 0L) parentId else null))
                        }
                        TYPE_TRANSFER -> {
                            var transferAccountId = 0L
                            if (accountId == 0L) {
                                accountId = prefHandler.getLong(PrefKey.TRANSFER_LAST_ACCOUNT_FROM_WIDGET, 0L)
                                transferAccountId = prefHandler.getLong(PrefKey.TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, 0L)
                            }
                            populateWithNewInstance(Transfer.getNewInstance(accountId,
                                    if (transferAccountId != 0L) transferAccountId else null,
                                    if (parentId != 0L) parentId else null))
                        }
                        Transactions.TYPE_SPLIT -> {
                            if (accountId == 0L) {
                                accountId = prefHandler.getLong(PrefKey.SPLIT_LAST_ACCOUNT_FROM_WIDGET, 0L)
                            }
                            populateWithNewInstance(SplitTransaction.getNewInstance(accountId)?.also { mRowId = it.id })
                        }
                    }
                }
            }
            if (mNewInstance) {
                if (!discoveryHelper.discover(this, amountInput.typeButton, String.format("%s / %s", getString(R.string.expense), getString(R.string.income)),
                                getString(R.string.discover_feature_expense_income_switch),
                                1, DiscoveryHelper.Feature.EI_SWITCH, false)) {
                    discoveryHelper.discover(this, rootBinding.toolbar.OperationType, String.format("%s / %s / %s", getString(R.string.transaction), getString(R.string.transfer), getString(R.string.split_transaction)),
                            getString(R.string.discover_feature_operation_type_select),
                            2, DiscoveryHelper.Feature.OPERATION_TYPE_SELECT, true)
                }
            }
        }
        viewModel.getMethods().observe(this, Observer<List<PaymentMethod>> { paymentMethods ->
            delegate.setMethods(paymentMethods)
        })
        currencyViewModel.getCurrencies().observe(this, Observer<List<Currency?>> { currencies ->
            delegate.setCurrencies(currencies, currencyContext)
        })
        viewModel.getAccounts().observe(this, Observer<List<Account>> { accounts ->
            if (accounts.size == 0) {
                abortWithMessage(getString(R.string.warning_no_account))
            } else if (accounts.size == 1 && operationType == TYPE_TRANSFER) {
                abortWithMessage(getString(R.string.dialog_command_disabled_insert_transfer))
            } else {
                delegate.setAccounts(accounts, if (savedInstanceState != null) null else intent.getStringExtra(DatabaseConstants.KEY_CURRENCY))

                linkInputsWithLabels()
                if (operationType != TYPE_TRANSFER) {//the methods cursor is based on the current account,
                    //hence it is loaded only after the accounts cursor is loaded
                    if (!isSplitPart) {
                        loadMethods(currentAccount)
                    }
                }
                accountsLoaded = true
                if (mIsResumed) setupListeners()
            }
        })
    }

    private fun loadData() {
        loadCurrencies()
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModel.loadAccounts(currencyContext)
    }

    private fun loadCurrencies() {
        currencyViewModel.loadCurrencies()
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
                val cr = contentResolver
                cr.unregisterContentObserver(it)
            } catch (ise: IllegalStateException) { // Do Nothing.  Observer has already been unregistered.
            }
        }
        if (::delegate.isInitialized) delegate.onDestroy()
    }

    fun updateSplitBalance() {
        val splitPartList = findSplitPartList()
        splitPartList?.updateBalance()
    }

    private fun populateFromTask(transaction: Transaction?, task: TransactionViewModel.InstantiationTask) {
        transaction?.let {
            if (transaction.isSealed) {
                abortWithMessage("This transaction refers to a closed account and can no longer be edited")
            } else {
                populate(it)
            }
        } ?: run {
            abortWithMessage(when(task) {
                TRANSACTION, TEMPLATE -> "Object has been deleted from db"
                TRANSACTION_FROM_TEMPLATE -> getString(R.string.save_transaction_template_deleted)
                FROM_INTENT_EXTRAS -> "Unable to build transaction from extras"
            })
        }
    }

    private fun populateWithNewInstance(transaction: Transaction?) {
        transaction?.let { populate(it) } ?: run {
            val errMsg = getString(R.string.warning_no_account)
            abortWithMessage(errMsg)
        }
    }

    private fun populate(transaction: Transaction) {
        if (intent.getBooleanExtra(KEY_CLONE, false)) {
            if (transaction is SplitTransaction) {
                mRowId = transaction.id
            } else {
                transaction.id = 0L
                mRowId = 0L
            }
            transaction.crStatus = Transaction.CrStatus.UNRECONCILED
            transaction.status = DatabaseConstants.STATUS_NONE
            ZonedDateTime.now().let {
                transaction.setDate(it)
                transaction.setValueDate(it)
            }
            transaction.uuid = Model.generateUuid()
            mNewInstance = true
        }
        //processing data from user switching operation type
        val cached = intent.getSerializableExtra(KEY_CACHED_DATA) as? Transaction
        if (cached != null) {
            transaction.accountId = cached.accountId
            transaction.methodId = cached.methodId
            transaction.date = cached.date
            transaction.valueDate = cached.valueDate
            transaction.crStatus = cached.crStatus
            transaction.comment = cached.comment
            transaction.payee = cached.payee
            (transaction as? Template)?.let {
                it.title = (cached as? Template)?.title
                it.isPlanExecutionAutomatic = (cached as? Template)?.isPlanExecutionAutomatic == true
            }
            transaction.referenceNumber = cached.referenceNumber
            transaction.amount = cached.amount
            transaction.originalAmount = cached.originalAmount
            transaction.equivalentAmount = cached.equivalentAmount
            intent.getParcelableExtra<Uri>(KEY_CACHED_PICTURE_URI).let {
                transaction.pictureUri = it
            }
            setDirty()
        } else {
            getIntent().getLongExtra(KEY_DATE, 0).takeIf { it != 0L }?.let {
                transaction.date = it / 1000
            }
        }
        delegate = TransactionDelegate.create(transaction, rootBinding, dateEditBinding, prefHandler)
        loadData()
        delegate.bindUnsafe(transaction, isCalendarPermissionPermanentlyDeclined, mNewInstance, null, intent.getSerializableExtra(KEY_CACHED_RECURRENCE) as? Recurrence)
        setHelpVariant(delegate.helpVariant)
        if (!mNewInstance) {
            setTitle(delegate.title)
        }
        operationType = transaction.operationType()
        invalidateOptionsMenu()
    }

    override fun hideKeyBoardAndShowDialog(id: Int) {
        hideKeyboard()
        showDialog(id)
    }

    override fun onValueSet(view: View) {
        setDirty()
        if (view is DateButton) {
            val date = view.date
            if (areDatesLinked()) {
                val other = if (view.getId() == R.id.Date2Button) dateEditBinding.DateButton else dateEditBinding.Date2Button
                other.setDate(date)
            }
        }
    }

    fun toggleDateLink(view: View) {
        val isLinked = !areDatesLinked()
        (view as ImageView).setImageResource(if (isLinked) R.drawable.ic_hchain else R.drawable.ic_hchain_broken)
        view.setTag(isLinked.toString())
        view.setContentDescription(getString(if (isLinked) R.string.content_description_dates_are_linked else R.string.content_description_dates_are_not_linked))
    }

    private fun areDatesLinked(): Boolean {
        return java.lang.Boolean.parseBoolean(dateEditBinding.DateLink.tag as String)
    }

    override fun setupListeners() {
        super.setupListeners()
        delegate.setupListeners(this)
    }

    override fun linkInputsWithLabels() {
        super.linkInputsWithLabels()
        delegate.linkInputsWithLabels()
    }

    val currentAccount: Account?
        get() = if (::delegate.isInitialized) delegate.currentAccount() else null

    override fun onTypeChanged(isChecked: Boolean) {
        super.onTypeChanged(isChecked)
        if (shouldLoadMethods) {
            loadMethods(currentAccount)
        }
        discoveryHelper.markDiscovered(DiscoveryHelper.Feature.EI_SWITCH)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (::delegate.isInitialized) {
            val oaMenuItem = menu.findItem(R.id.ORIGINAL_AMOUNT_COMMAND)
            if (oaMenuItem != null) {
                oaMenuItem.isChecked = delegate.originalAmountVisible
            }
            val currentAccount = currentAccount
            val eaMenuItem = menu.findItem(R.id.EQUIVALENT_AMOUNT_COMMAND)
            if (eaMenuItem != null) {
                Utils.menuItemSetEnabledAndVisible(eaMenuItem, !(currentAccount == null || hasHomeCurrency(currentAccount)))
                eaMenuItem.isChecked = delegate.equivalentAmountVisible
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun hasHomeCurrency(account: Account): Boolean {
        return account.currency == Utils.getHomeCurrency()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (!isNoMainTransaction && !(operationType == Transactions.TYPE_SPLIT &&
                        !MyApplication.getInstance().licenceHandler.isContribEnabled)) {
            menu.add(Menu.NONE, R.id.SAVE_AND_NEW_COMMAND, 0, R.string.menu_save_and_new)
                    .setIcon(R.drawable.ic_action_save_new)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        if (operationType == TYPE_TRANSFER) {
            menu.add(Menu.NONE, R.id.INVERT_TRANSFER_COMMAND, 0, R.string.menu_invert_transfer)
                    .setIcon(R.drawable.ic_menu_move)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        } else if (isMainTransactione) {
            menu.add(Menu.NONE, R.id.ORIGINAL_AMOUNT_COMMAND, 0, R.string.menu_original_amount)
                    .setCheckable(true)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.add(Menu.NONE, R.id.EQUIVALENT_AMOUNT_COMMAND, 0, R.string.menu_equivalent_amount)
                    .setCheckable(true)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return true
    }

    override fun doSave(andNew: Boolean) {
        if (operationType == Transactions.TYPE_SPLIT &&
                !requireSplitPartList().splitComplete()) {
            showSnackbar(getString(R.string.unsplit_amount_greater_than_zero), Snackbar.LENGTH_SHORT)
        } else {
            if (andNew) {
                createNew = true
            }
            super.doSave(andNew)
        }
    }

    override fun doHome() {
        cleanup({ finish() })
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        when (command) {
            R.id.CREATE_COMMAND -> {
                createRow()
                return true
            }
            R.id.INVERT_TRANSFER_COMMAND -> {
                (delegate as? TransferDelegate)?.invert()
                return true
            }
            R.id.ORIGINAL_AMOUNT_COMMAND -> {
                delegate.toggleOriginalAmount()
                invalidateOptionsMenu()
                return true
            }
            R.id.EQUIVALENT_AMOUNT_COMMAND -> {
                delegate.toggleEquivalentAmount(currentAccount)
                invalidateOptionsMenu()
                return true
            }
        }
        return false
    }

    private fun createRow() {
        val account = currentAccount
        if (account == null) {
            showSnackbar(R.string.account_list_not_yet_loaded, Snackbar.LENGTH_LONG)
            return
        }
        val i = Intent(this, ExpenseEdit::class.java)
        forwardDataEntryFromWidget(i)
        i.putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
        i.putExtra(DatabaseConstants.KEY_ACCOUNTID, account.id)
        i.putExtra(DatabaseConstants.KEY_PARENTID, mRowId)
        i.putExtra(KEY_NEW_TEMPLATE, isMainTemplate)
        startActivityForResult(i, ProtectedFragmentActivity.EDIT_REQUEST)
    }

    /**
     * calls the activity for selecting (and managing) categories
     */
    fun startSelectCategory() {
        val i = Intent(this, ManageCategories::class.java)
        i.action = ManageCategories.ACTION_SELECT_MAPPING
        forwardDataEntryFromWidget(i)
        //we pass the currently selected category in to prevent
        //it from being deleted, which can theoretically lead
        //to crash upon saving https://github.com/mtotschnig/MyExpenses/issues/71
        i.putExtra(DatabaseConstants.KEY_ROWID, (delegate as? CategoryDelegate)?.catId)
        startActivityForResult(i, ProtectedFragmentActivity.SELECT_CATEGORY_REQUEST)
    }

    override fun onCreateDialog(id: Int): Dialog? {
        hideKeyboard()
        return try {
            (findViewById<View>(id) as ButtonWithDialog).onCreateDialog()
        } catch (e: ClassCastException) {
            Timber.e(e)
            null
        }
    }

    override fun saveState() {
        delegate.syncStateAndValidate(true, currencyContext)?.let {
            mIsSaving = true
            if (planInstanceId > 0L) {
                it.originPlanInstanceId = planInstanceId
            }
            viewModel.save(it).observe(this, Observer {
                onSaved(it)
            })
            if (intent.getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false)) {
                when (operationType) {
                    Transactions.TYPE_TRANSACTION -> prefHandler.putLong(PrefKey.TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, accountId)
                    TYPE_TRANSFER -> {
                        prefHandler.putLong(PrefKey.TRANSFER_LAST_ACCOUNT_FROM_WIDGET, accountId)
                        (delegate as? TransferDelegate)?.mTransferAccountId?.let {
                            prefHandler.putLong(PrefKey.TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, it)
                        }
                    }
                    Transactions.TYPE_SPLIT -> prefHandler.putLong(PrefKey.SPLIT_LAST_ACCOUNT_FROM_WIDGET, accountId)
                }
            }
        } ?: run {
            //prevent this flag from being sticky if form was not valid
            createNew = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == ProtectedFragmentActivity.SELECT_CATEGORY_REQUEST && intent != null) {
            (delegate as? CategoryDelegate)?.setCategory(intent.getStringExtra(DatabaseConstants.KEY_LABEL),
                    intent.getStringExtra(DatabaseConstants.KEY_ICON),
                    intent.getLongExtra(DatabaseConstants.KEY_CATID, 0))
            setDirty()
        }
        if (requestCode == ProtectedFragmentActivity.PICTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri: Uri?
            val errorMsg: String
            when {
                intent == null -> {
                    uri = pictureUriTemp
                    Timber.d("got result for PICTURE request, intent null, relying on stored output uri %s", pictureUriTemp)
                }
                intent.data != null -> {
                    uri = intent.data
                    Timber.d("got result for PICTURE request, found uri in intent data %s", uri.toString())
                }
                else -> {
                    Timber.d("got result for PICTURE request, intent != null, getData() null, relying on stored output uri %s", pictureUriTemp)
                    uri = pictureUriTemp
                }
            }
            if (uri != null) {
                if (PermissionHelper.canReadUri(uri, this)) {
                    setPicture(uri)
                    setDirty()
                } else {
                    pictureUriTemp = uri
                    requestStoragePermission()
                }
                return
            } else {
                errorMsg = "Error while retrieving image: No data found."
            }
            CrashHandler.report(errorMsg)
            showSnackbar(errorMsg, Snackbar.LENGTH_LONG)
        }
        if (requestCode == ProtectedFragmentActivity.PLAN_REQUEST) {
            finish()
        }
        if (requestCode == ProtectedFragmentActivity.EDIT_REQUEST && resultCode == RESULT_OK) {
            setDirty()
        }
    }

    override fun dispatchOnBackPressed() {
        cleanup({ super.dispatchOnBackPressed() })
    }

    private fun cleanup(onComplete: () -> Unit) {
        if (operationType == Transactions.TYPE_SPLIT) {
            if (mRowId != 0L) {
                viewModel.cleanupSplit(mRowId, isTemplate).observe(this, Observer {
                    onComplete()
                })
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
            refreshPlanData()
        }
    }

    private fun refreshPlanData() {
        delegate.planId?.let {
            viewModel.plan(it).observe(this, Observer {
                delegate.configurePlan(it)
            })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
        delegate.onSaveInstanceState(outState)
    }

    val amount: Money?
        get() {
            val a = currentAccount ?: return null
            val amount = validateAmountInput(false)
            return if (amount == null) Money(a.currency, 0L) else Money(a.currency, amount)
        }
/*

    */
/*
   * callback of TaskExecutionFragment
   */

    override fun onPostExecute(taskId: Int, o: Any?) {
        super.onPostExecute(taskId, o)
        when (taskId) {
            TaskExecutionFragment.TASK_MOVE_UNCOMMITED_SPLIT_PARTS -> {
                (delegate as? SplitDelegate)?.onUncommitedSplitPartsMoved(o as Boolean)
            }
        }
    }

    private fun unsetPicture() {
        setPicture(null)
    }

    private fun setPicture(pictureUri: Uri?) {
        shouldRecordAttachPictureFeature = true
        delegate.setPicture(pictureUri)
    }

    fun isValidType(type: Int): Boolean {
        return type == Transactions.TYPE_SPLIT || type == Transactions.TYPE_TRANSACTION || type == TYPE_TRANSFER
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
        cleanup({
            val restartIntent = intent
            restartIntent.putExtra(Transactions.OPERATION_TYPE, newType)
            if (isDirty) {
                delegate.syncStateAndValidate(false, currencyContext)?.let {
                    restartIntent.putExtra(KEY_CACHED_DATA, it)
                    if (it.pictureUri != null) {
                        restartIntent.putExtra(KEY_CACHED_PICTURE_URI, it.pictureUri)
                    }
                }
                restartIntent.putExtra(KEY_CACHED_RECURRENCE, delegate.recurrenceSpinner.selectedItem as? Recurrence)
            }
            finish()
            startActivity(restartIntent)
        })
    }

    fun onSaved(result: Long) {
        if (result < 0L) {
            val errorMsg: String
            when (result) {
                ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE -> errorMsg = getString(R.string.external_storage_unavailable)
                ERROR_PICTURE_SAVE_UNKNOWN -> errorMsg = "Error while saving picture"
                ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE -> {
                    delegate.recurrenceSpinner.setSelection(0)
                    //mTransaction!!.originTemplate = null
                    errorMsg = "Recurring transactions are not available, because calendar integration is not functional on this device."
                }
                else -> {
                    (delegate as? CategoryDelegate)?.resetCategory()
                    errorMsg = "Error while saving transaction"
                }
            }
            showSnackbar(errorMsg, Snackbar.LENGTH_LONG)
            createNew = false
        } else {
            if (recordTemplateWidget) {
                recordUsage(ContribFeature.TEMPLATE_WIDGET)
                TemplateWidget.showContribMessage(this)
            }
            if (operationType == Transactions.TYPE_SPLIT) {
                recordUsage(ContribFeature.SPLIT_TRANSACTION)
            }
            if (shouldRecordAttachPictureFeature) {
                recordUsage(ContribFeature.ATTACH_PICTURE)
            }
            if (createNew) {
                createNew = false
                delegate.prepareForNew()
                //while saving the picture might have been moved from temp to permanent
                //mPictureUri = mTransaction!!.pictureUri
                mNewInstance = true
                showSnackbar(getString(R.string.save_transaction_and_new_success), Snackbar.LENGTH_SHORT)
            } else {
                if (delegate.recurrenceSpinner.selectedItem === Recurrence.CUSTOM) {
                    viewModel.transaction(result, TEMPLATE, false, false, null).observe(this, Observer {
                        it?.let { launchPlanView(true, (it as Template).planId) }
                    })
                } else { //make sure soft keyboard is closed
                    hideKeyboard()
                    val intent = Intent()
                    setResult(RESULT_OK, intent)
                    finish()
                    //no need to call super after finish
                    return
                }
            }
        }
        mIsSaving = false
    }

    private fun hideKeyboard() {
        val im = this.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(window.decorView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        when (id) {
            AUTOFILL_CURSOR -> {
                val dataToLoad: MutableList<String> = ArrayList()
                val autoFillAccountFromPreference = prefHandler.getString(PrefKey.AUTO_FILL_ACCOUNT, "never")
                val autoFillAccountFromExtra = intent.getBooleanExtra(KEY_AUTOFILL_MAY_SET_ACCOUNT, false)
                val overridePreferences = args!!.getBoolean(KEY_AUTOFILL_OVERRIDE_PREFERENCES)
                val mayLoadAccount = overridePreferences && autoFillAccountFromExtra || autoFillAccountFromPreference == "always" ||
                        autoFillAccountFromPreference == "aggregate" && autoFillAccountFromExtra
                if (overridePreferences || prefHandler.getBoolean(PrefKey.AUTO_FILL_AMOUNT, false)) {
                    dataToLoad.add(DatabaseConstants.KEY_CURRENCY)
                    dataToLoad.add(DatabaseConstants.KEY_AMOUNT)
                }
                if (overridePreferences || prefHandler.getBoolean(PrefKey.AUTO_FILL_CATEGORY, false)) {
                    dataToLoad.add(DatabaseConstants.KEY_CATID)
                    dataToLoad.add(DatabaseConstants.CAT_AS_LABEL)
                    dataToLoad.add(DatabaseConstants.CATEGORY_ICON)
                }
                if (overridePreferences || prefHandler.getBoolean(PrefKey.AUTO_FILL_COMMENT, false)) {
                    dataToLoad.add(DatabaseConstants.KEY_COMMENT)
                }
                if (overridePreferences || prefHandler.getBoolean(PrefKey.AUTO_FILL_METHOD, false)) {
                    dataToLoad.add(DatabaseConstants.KEY_METHODID)
                }
                if (mayLoadAccount) {
                    dataToLoad.add(DatabaseConstants.KEY_ACCOUNTID)
                }
                return CursorLoader(this,
                        ContentUris.withAppendedId(TransactionProvider.AUTOFILL_URI, args.getLong(DatabaseConstants.KEY_ROWID)),
                        dataToLoad.toTypedArray(), null, null, null)
            }
        }
        throw IllegalStateException()
    }

    override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
        if (data == null || isFinishing) {
            return
        }
        when (loader.id) {
            AUTOFILL_CURSOR ->
                (delegate as? CategoryDelegate)?.autoFill(data, currencyContext)
        }
    }

    fun launchPlanView(forResult: Boolean, planId: Long) {
        val intent = Intent(Intent.ACTION_VIEW)
        //ACTION_VIEW expects to get a range http://code.google.com/p/android/issues/detail?id=23852
        //intent.putExtra(CalendarContractCompat.EXTRA_EVENT_BEGIN_TIME, mPlan!!.dtstart)
        //intent.putExtra(CalendarContractCompat.EXTRA_EVENT_END_TIME, mPlan!!.dtstart)
        intent.data = ContentUris.withAppendedId(CalendarContractCompat.Events.CONTENT_URI, planId)
        if (Utils.isIntentAvailable(this, intent)) {
            if (forResult) {
                startActivityForResult(intent, ProtectedFragmentActivity.PLAN_REQUEST)
            } else {
                startActivity(intent)
            }
        } else {
            showSnackbar(R.string.no_calendar_app_installed, Snackbar.LENGTH_SHORT)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor?>) { //should not be necessary to empty the autocompletetextview
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (feature === ContribFeature.ATTACH_PICTURE) {
            startMediaChooserDo()
        } else if (feature === ContribFeature.SPLIT_TRANSACTION) {
            restartWithType(Transactions.TYPE_SPLIT)
        }
    }

    override fun contribFeatureNotCalled(feature: ContribFeature) {
        if (feature === ContribFeature.SPLIT_TRANSACTION) {
            delegate.resetOperationType()
        }
    }

    override fun onPositive(args: Bundle) {
        when (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
            R.id.AUTO_FILL_COMMAND -> {
                startAutoFill(args.getLong(DatabaseConstants.KEY_ROWID), true)
                PreferenceUtils.enableAutoFill()
            }
            else -> super.onPositive(args)
        }
    }

    /**
     * @param id                  id of Payee/Payer for whom data should be loaded
     * @param overridePreferences if true data is loaded irrespective of what is set in preferences
     */
    fun startAutoFill(id: Long, overridePreferences: Boolean) {
        val extras = Bundle(2)
        extras.putLong(DatabaseConstants.KEY_ROWID, id)
        extras.putBoolean(KEY_AUTOFILL_OVERRIDE_PREFERENCES, overridePreferences)
        Utils.requireLoader(mManager, AUTOFILL_CURSOR, extras, this)
    }

    override fun onNegative(args: Bundle) {
        if (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE) == R.id.AUTO_FILL_COMMAND) {
            PreferenceUtils.disableAutoFill()
        }
    }

    override fun onPause() {
        mIsResumed = false
        super.onPause()
    }

    fun findSplitPartList(): SplitPartList? {
        return supportFragmentManager.findFragmentByTag(SPLIT_PART_LIST) as SplitPartList?
    }

    private fun requireSplitPartList(): SplitPartList {
        return findSplitPartList()
                ?: throw IllegalStateException("Split part list not found")
    }

    @SuppressLint("NewApi")
    fun showPicturePopupMenu(v: View?) {
        val popup = PopupMenu(this, v!!)
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
            R.id.VIEW_COMMAND -> imageViewIntentProvider.startViewIntent(this, delegate.pictureUri)
            R.id.CHANGE_COMMAND -> startMediaChooserDo()
        }
    }

    fun startMediaChooser(v: View?) {
        contribFeatureRequested(ContribFeature.ATTACH_PICTURE, null)
    }

    override fun contribFeatureRequested(feature: ContribFeature, @Nullable tag: Serializable?) {
        hideKeyboard()
        super.contribFeatureRequested(feature, tag)
    }

    private fun startMediaChooserDo() {
        val outputMediaUri = cameraUri
        val gallIntent = Intent(PictureDirHelper.getContentIntentAction())
        gallIntent.type = "image/*"
        val chooserIntent = Intent.createChooser(gallIntent, null)
        //if external storage is not available, camera capture won't work
        if (outputMediaUri != null) {
            val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            camIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputMediaUri)
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camIntent))
        }
        Timber.d("starting chooser for PICTURE_REQUEST with EXTRA_OUTPUT %s ", outputMediaUri)
        startActivityForResult(chooserIntent, ProtectedFragmentActivity.PICTURE_REQUEST_CODE)
    }

    private val cameraUri: Uri?
        get() {
            if (pictureUriTemp == null) {
                pictureUriTemp = PictureDirHelper.getOutputMediaUri(true)
            }
            return pictureUriTemp
        }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = PermissionHelper.allGranted(grantResults)
        when (requestCode) {
            PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR -> {
                delegate.onCalendarPermissionsResult(granted)
            }
            PermissionHelper.PERMISSIONS_REQUEST_STORAGE -> {
                if (granted) {
                    setPicture(pictureUriTemp)
                } else {
                    unsetPicture()
                }
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        delegate.isProcessingLinkedAmountInputs = true
        exchangeRateEdit.setBlockWatcher(true)
        super.onRestoreInstanceState(savedInstanceState)
        exchangeRateEdit.setBlockWatcher(false)
        delegate.isProcessingLinkedAmountInputs = false
        if (mRowId == 0L) {
            (delegate as? TransferDelegate)?.configureTransferDirection()
        }
    }

    fun clearMethodSelection(view: View) {
        delegate.setMethodSelection(null)
    }

    fun clearCategorySelection(view: View) {
        (delegate as? CategoryDelegate)?.setCategory(null, null, null)
    }

    fun showPlanMonthFragment(originTemplate: Template, color: Int) {
        PlanMonthFragment.newInstance(
                originTemplate.title,
                originTemplate.id,
                originTemplate.planId,
                color, true, themeType).show(supportFragmentManager,
                TemplatesList.CALDROID_DIALOG_FRAGMENT_TAG)
    }

    fun addSplitPartList() {
        val fm = supportFragmentManager
        if (findSplitPartList() == null && !fm.isStateSaved) {
            fm.beginTransaction()
                    .add(R.id.edit_container, SplitPartList.newInstance(mRowId, isTemplate, currentAccount!!), SPLIT_PART_LIST)
                    .commit()
            fm.executePendingTransactions()
        }
    }

    fun updateSplitPartList(account: Account) {
        findSplitPartList()?.let {
            it.updateAccount(account)
            if (it.splitCount > 0) { //call background task for moving parts to new account
                startTaskExecution(
                        TaskExecutionFragment.TASK_MOVE_UNCOMMITED_SPLIT_PARTS, arrayOf(mRowId),
                        account.id,
                        R.string.progress_dialog_updating_split_parts)
                return
            }
        }
    }

    fun observePlan(planId: Long) {
        pObserver = PlanObserver().also {
            contentResolver.registerContentObserver(
                    ContentUris.withAppendedId(CalendarContractCompat.Events.CONTENT_URI, planId),
                    false, it)
        }
    }

    fun loadOriginTemplate(templateId: Long) {
        viewModel.transaction(templateId, TEMPLATE, false, false, null).observe(this, Observer {
            (it as? Template)?.let { delegate.originTemplateLoaded(it) }
        })
    }

    companion object {
        private const val SPLIT_PART_LIST = "SPLIT_PART_LIST"
        const val KEY_NEW_TEMPLATE = "newTemplate"
        const val KEY_CLONE = "clone"
        private const val KEY_CACHED_DATA = "cachedData"
        private const val KEY_CACHED_RECURRENCE = "cachedRecurrence"
        private const val KEY_CACHED_PICTURE_URI = "cachedPictureUri"
        const val KEY_AUTOFILL_MAY_SET_ACCOUNT = "autoFillMaySetAccount"
        private const val KEY_AUTOFILL_OVERRIDE_PREFERENCES = "autoFillOverridePreferences"
        const val AUTOFILL_CURSOR = 8
    }
}