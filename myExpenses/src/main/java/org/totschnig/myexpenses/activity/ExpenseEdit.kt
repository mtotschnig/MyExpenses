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
import android.app.Activity
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
import android.view.*
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
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.delegate.CategoryDelegate
import org.totschnig.myexpenses.delegate.TransactionDelegate
import org.totschnig.myexpenses.delegate.TransferDelegate
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.fragment.PlanMonthFragment
import org.totschnig.myexpenses.fragment.SplitPartList
import org.totschnig.myexpenses.fragment.TemplatesList
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Plan.Recurrence
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PreferenceUtils
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.task.BuildTransactionTask
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.ui.*
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.*
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
    var mTemplateId = 0L
    @JvmField
    @State
    var mCatId: Long? = null
    @JvmField
    @State
    var parentId = 0L
    @JvmField
    @State
    var mPictureUri: Uri? = null
    @JvmField
    @State
    var mPictureUriTemp: Uri? = null

    val accountId: Long
        get() = currentAccount?.id ?: 0L

    private var mPlan: Plan? = null
    private var mPlanInstanceId: Long = 0
    private var mPlanInstanceDate: Long = 0
    /**
     * transaction, transfer or split
     */
    private var mOperationType = 0
    private lateinit var mManager: LoaderManager
    private var mClone = false
    private var mCreateNew = false
    private var mIsMainTransactionOrTemplate = false
    private var mIsMainTemplate = false
    private var mIsMainTransaction = false
    private var mSavedInstance = false
    private var mRecordTemplateWidget = false
    private var mIsResumed = false
    private var accountsLoaded = false
    var isProcessingLinkedAmountInputs = false
    private var pObserver: ContentObserver? = null
    private var mPlanUpdateNeeded = false
    private var didUserSetAccount = false
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

    public override fun getDiscardNewMessage(): Int {
        return /*if (mTransaction is Template) R.string.dialog_confirm_discard_new_template else*/ R.string.dialog_confirm_discard_new_transaction
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
        viewModel.getMethods().observe(this, Observer<List<PaymentMethod>> { paymentMethods ->
            delegate.setMethods(paymentMethods)
        })
        currencyViewModel = ViewModelProvider(this).get(CurrencyViewModel::class.java)
        currencyViewModel.getCurrencies().observe(this, Observer<List<Currency?>> { currencies ->
            delegate.setCurrencies(currencies, currencyContext)
        })
        //we enable it only after accountcursor has been loaded, preventing NPE when user clicks on it early
        amountInput.setTypeEnabled(false)

        val extras = intent.extras
        mRowId = Utils.getFromExtra(extras, DatabaseConstants.KEY_ROWID, 0)
        mTemplateId = intent.getLongExtra(DatabaseConstants.KEY_TEMPLATEID, 0)
        //upon orientation change stored in instance state, since new splitTransactions are immediately persisted to DB
        if (savedInstanceState != null) {
            mSavedInstance = true
            Icepick.restoreInstanceState(this, savedInstanceState)
            if (accountId != 0L) {
                didUserSetAccount = true
            }
        }
        //were we called from a notification
        val notificationId = intent.getIntExtra(MyApplication.KEY_NOTIFICATION_ID, 0)
        if (notificationId > 0) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        }
        //1. fetch the transaction or create a new instance
        if (mRowId != 0L) { //or template
            mNewInstance = false
            viewModel.transaction(mRowId).observe(this, Observer {
                populate(it)
            })
        } else if (mTemplateId != 0L) {
            mNewInstance = false
            viewModel.template(mTemplateId).observe(this, Observer {
                populate(it)
            })
        } else {
            mOperationType = intent.getIntExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
            if (!isValidType(mOperationType)) {
                mOperationType = Transactions.TYPE_TRANSACTION
            }
            val isNewTemplate = intent.getBooleanExtra(KEY_NEW_TEMPLATE, false)
            if (mOperationType == Transactions.TYPE_SPLIT) {
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
            if (!mSavedInstance && Intent.ACTION_INSERT == intent.action && extras != null) {
                val args = Bundle(1)
                args.putBundle(BuildTransactionTask.KEY_EXTRAS, extras)
                startTaskExecution(TaskExecutionFragment.TASK_BUILD_TRANSACTION_FROM_INTENT_EXTRAS, args,
                        R.string.progress_dialog_loading)
            } else {
                if (isNewTemplate) {
                    populate(Template.getTypedNewInstance(mOperationType, accountId, true, if (parentId != 0L) parentId else null).also { mTemplateId = it.id })
                } else {
                    when (mOperationType) {
                        Transactions.TYPE_TRANSACTION -> {
                            if (accountId == 0L) {
                                accountId = prefHandler.getLong(PrefKey.TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, 0L)
                            }
                            populate(Transaction.getNewInstance(accountId, if (parentId != 0L) parentId else null))
                        }
                        Transactions.TYPE_TRANSFER -> {
                            var transferAccountId = 0L
                            if (accountId == 0L) {
                                accountId = prefHandler.getLong(PrefKey.TRANSFER_LAST_ACCOUNT_FROM_WIDGET, 0L)
                                transferAccountId = prefHandler.getLong(PrefKey.TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, 0L)
                            }
                            populate(Transfer.getNewInstance(accountId,
                                    if (transferAccountId != 0L) transferAccountId else null,
                                    if (parentId != 0L) parentId else null))
                        }
                        Transactions.TYPE_SPLIT -> {
                            if (accountId == 0L) {
                                accountId = prefHandler.getLong(PrefKey.SPLIT_LAST_ACCOUNT_FROM_WIDGET, 0L)
                            }
                            populate(SplitTransaction.getNewInstance(accountId).also { mRowId = it.id })
                        }
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
        if (pObserver != null) {
            try {
                val cr = contentResolver
                cr.unregisterContentObserver(pObserver!!)
            } catch (ise: IllegalStateException) { // Do Nothing.  Observer has already been unregistered.
            }
        }
/*        val oldCursor = mPayeeAdapter.cursor
        if (oldCursor != null && !oldCursor.isClosed) {
            oldCursor.close()
        }*/
    }

    fun updateSplitBalance() {
        val splitPartList = findSplitPartList()
        splitPartList?.updateBalance()
    }

    private fun populate(transaction: Transaction?) {
        if (transaction == null) {
            val errMsg = getString(R.string.warning_no_account)
            abortWithMessage(errMsg)
            return
        }
        if (!mSavedInstance) { //processing data from user switching operation type
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
                    mPictureUri = it
                    transaction.pictureUri = it
                }
            }
        }
        delegate = TransactionDelegate.createAndBind(transaction, rootBinding, dateEditBinding,
                isCalendarPermissionPermanentlyDeclined(), prefHandler,
                mNewInstance,
                intent.getSerializableExtra(KEY_CACHED_RECURRENCE) as? Recurrence)
        currencyViewModel.loadCurrencies()
        linkInputsWithLabels()
        mManager.initLoader<Cursor>(ACCOUNTS_CURSOR, null, this)
        setHelpVariant(delegate.helpVariant)
        if (!mNewInstance) {
            setTitle(delegate.title)
        } else if (mClone) {
            setTitle(R.string.menu_clone_transaction)
        }
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

    private fun setPlannerRowVisibility(visibility: Int) {
        delegate.setPlannerRowVisibility(visibility)
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
        if (mIsMainTransactionOrTemplate) {
            loadMethods(currentAccount)
        }
        discoveryHelper.markDiscovered(DiscoveryHelper.Feature.EI_SWITCH)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
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
        return super.onPrepareOptionsMenu(menu)
    }

    private fun hasHomeCurrency(account: Account): Boolean {
        return account.currencyUnit == Utils.getHomeCurrency()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (!isNoMainTransaction && !(mOperationType == Transactions.TYPE_SPLIT &&
                        !MyApplication.getInstance().licenceHandler.isContribEnabled)) {
            menu.add(Menu.NONE, R.id.SAVE_AND_NEW_COMMAND, 0, R.string.menu_save_and_new)
                    .setIcon(R.drawable.ic_action_save_new)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        if (mOperationType == Transactions.TYPE_TRANSFER) {
            menu.add(Menu.NONE, R.id.INVERT_TRANSFER_COMMAND, 0, R.string.menu_invert_transfer)
                    .setIcon(R.drawable.ic_menu_move)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        } else if (mIsMainTransaction) {
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
        if (mOperationType == Transactions.TYPE_SPLIT &&
                !requireSplitPartList().splitComplete()) {
            showSnackbar(getString(R.string.unsplit_amount_greater_than_zero), Snackbar.LENGTH_SHORT)
        } else {
            if (andNew) {
                mCreateNew = true
            }
            super.doSave(andNew)
        }
    }

    override fun doHome() {
        cleanup()
        finish()
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
                amountInput.toggle()
                (delegate as? TransferDelegate)?.switchAccountViews()
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
        //i.putExtra(KEY_NEW_TEMPLATE, mTransaction is Template)
        startActivityForResult(i, ProtectedFragmentActivity.EDIT_SPLIT_REQUEST)
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
        i.putExtra(DatabaseConstants.KEY_ROWID, mCatId)
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
        delegate.syncStateAndValidate(true, currencyContext, mPictureUri)?.let {
            mIsSaving = true
            viewModel.save(it).observe(this, Observer {
                onSaved(it)
            })
            if (intent.getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false)) {
                when (mOperationType) {
                    Transactions.TYPE_TRANSACTION -> prefHandler.putLong(PrefKey.TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, accountId)
                    Transactions.TYPE_TRANSFER -> {
                        prefHandler.putLong(PrefKey.TRANSFER_LAST_ACCOUNT_FROM_WIDGET, accountId)
                        (delegate as? TransferDelegate)?.mTransferAccountId?.let {
                            prefHandler.putLong(PrefKey.TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, it)
                        }
                    }
                    Transactions.TYPE_SPLIT -> prefHandler.putLong(PrefKey.SPLIT_LAST_ACCOUNT_FROM_WIDGET, accountId)
                }
            }
        } ?: kotlin.run {
            //prevent this flag from being sticky if form was not valid
            mCreateNew = false
        }
    }

    private val isSplitPart: Boolean
        get() = parentId != 0L

    private val isNoMainTransaction: Boolean
        get() = isSplitPart /*|| mTransaction is Template*/

    /* (non-Javadoc)
   * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
   */
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == ProtectedFragmentActivity.SELECT_CATEGORY_REQUEST && intent != null) {
            mCatId = intent.getLongExtra(DatabaseConstants.KEY_CATID, 0)
            (delegate as? CategoryDelegate)?.setCategory(intent.getStringExtra(DatabaseConstants.KEY_LABEL), intent.getStringExtra(DatabaseConstants.KEY_ICON), mCatId)
            setDirty()
        }
        if (requestCode == ProtectedFragmentActivity.PICTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri: Uri?
            val errorMsg: String
            when {
                intent == null -> {
                    uri = mPictureUriTemp
                    Timber.d("got result for PICTURE request, intent null, relying on stored output uri %s", mPictureUriTemp)
                }
                intent.data != null -> {
                    uri = intent.data
                    Timber.d("got result for PICTURE request, found uri in intent data %s", uri.toString())
                }
                else -> {
                    Timber.d("got result for PICTURE request, intent != null, getData() null, relying on stored output uri %s", mPictureUriTemp)
                    uri = mPictureUriTemp
                }
            }
            if (uri != null) {
                mPictureUri = uri
                if (PermissionHelper.canReadUri(uri, this)) {
                    setPicture()
                    setDirty()
                } else {
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
    }

    override fun onBackPressed() {
        cleanup()
        super.onBackPressed()
    }

    private fun cleanup() {
        /*if (mTransaction != null) {
            mTransaction!!.cleanupCanceledEdit()
        }*/
    }

    /**
     * updates interface based on type (EXPENSE or INCOME)
     */
    override fun configureType() {
        delegate.configureType()
    }

    private inner class PlanObserver : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            if (mIsResumed) {
                refreshPlanData()
            } else {
                mPlanUpdateNeeded = true
            }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (mPlanUpdateNeeded) {
            refreshPlanData()
            mPlanUpdateNeeded = false
        }
    }

    private fun refreshPlanData() {
        if (mPlan != null) {
            startTaskExecution(TaskExecutionFragment.TASK_INSTANTIATE_PLAN, arrayOf(mPlan!!.id), null, 0)
        } else { //seen in report 96a04ce6a647555356751634fee9fc73, need to investigate how this can happen
            CrashHandler.report("Received onChange on ContentOberver for plan, but mPlan is null")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        delegate.onSaveInstanceState(outState)
    }

    val amount: Money?
        get() {
            val a = currentAccount ?: return null
            val amount = validateAmountInput(false)
            return if (amount == null) Money(a.currencyUnit, 0L) else Money(a.currencyUnit, amount)
        }
/*

    */
/*
   * callback of TaskExecutionFragment
   *//*

    override fun onPostExecute(taskId: Int, o: Any?) {
        super.onPostExecute(taskId, o)
        val success: Boolean
        when (taskId) {
            TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE, TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION, TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE, TaskExecutionFragment.TASK_BUILD_TRANSACTION_FROM_INTENT_EXTRAS -> {
                if (o == null) {
                    abortWithMessage(when(taskId) {
                        TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE -> getString(R.string.save_transaction_template_deleted)
                        TaskExecutionFragment.TASK_BUILD_TRANSACTION_FROM_INTENT_EXTRAS -> "Unable to build transaction from extras"
                        else -> "Object has been deleted from db"
                    })
                    return
                }
                mTransaction = o as Transaction
                if (mTransaction!!.isSealed) {
                    abortWithMessage("This transaction refers to a closed account and can no longer be edited")
                }
                if (taskId == TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE) {
                    if (mPlanInstanceId > 0L) {
                        mTransaction!!.originPlanInstanceId = mPlanInstanceId
                    }
                    if (mPlanInstanceDate != 0L) {
                        mTransaction!!.setDate(Date(mPlanInstanceDate))
                    }
                }
                if (mTransaction is Template) {
                    mPlan = (mTransaction as Template).plan
                }
                mOperationType = mTransaction!!.operationType()
                if (mPictureUri == null) {
                    mPictureUri = mTransaction!!.pictureUri
                    if (mPictureUri != null) {
                        if (PictureDirHelper.doesPictureExist(mTransaction!!.pictureUri)) {
                            setPicture()
                        } else {
                            unsetPicture()
                            showSnackbar(R.string.image_deleted, Snackbar.LENGTH_SHORT)
                        }
                    }
                }
                if (mCatId == null) {
                    mCatId = mTransaction!!.catId
                    mLabel = mTransaction!!.label
                    categoryIcon = mTransaction!!.categoryIcon
                }
                if (mMethodId == null) {
                    mMethodId = mTransaction!!.methodId
                }
                if (intent.getBooleanExtra(KEY_CLONE, false)) {
                    if (mTransaction is SplitTransaction) {
                        mRowId = mTransaction.getId()
                    } else {
                        mTransaction!!.id = 0L
                        mRowId = 0L
                    }
                    mTransaction!!.crStatus = CrStatus.UNRECONCILED
                    mTransaction!!.status = DatabaseConstants.STATUS_NONE
                    mTransaction!!.setDate(ZonedDateTime.now())
                    mTransaction!!.uuid = Model.generateUuid()
                    mClone = true
                }
                setup()
                invalidateOptionsMenu()
            }
            TaskExecutionFragment.TASK_MOVE_UNCOMMITED_SPLIT_PARTS -> {
                success = o as Boolean
                val account = mAccounts!![mAccountSpinner.selectedItemPosition]
                if (success) {
                    updateAccount(account)
                } else {
                    var i = 0
                    while (i < mAccounts!!.size) {
                        if (mAccounts!![i]!!.id == mTransaction!!.accountId) {
                            mAccountSpinner.setSelection(i)
                            break
                        }
                        i++
                    }
                    showSnackbar(getString(R.string.warning_cannot_move_split_transaction, account!!.label),
                            Snackbar.LENGTH_LONG)
                }
            }
            TaskExecutionFragment.TASK_INSTANTIATE_PLAN -> {
                mPlan = o as Plan
                configurePlan()
            }
        }
    }
*/

    private fun unsetPicture() {
        mPictureUri = null
        setPicture()
    }

    private fun setPicture() {
        delegate.setPicture(mPictureUri)
    }

    fun isValidType(type: Int): Boolean {
        return type == Transactions.TYPE_SPLIT || type == Transactions.TYPE_TRANSACTION || type == Transactions.TYPE_TRANSFER
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
        cleanup()
        val restartIntent = intent
        restartIntent.putExtra(Transactions.OPERATION_TYPE, newType)
        delegate.syncStateAndValidate(false, currencyContext, mPictureUri)?.let {
            restartIntent.putExtra(KEY_CACHED_DATA, it)
            if (it.pictureUri != null) {
                restartIntent.putExtra(KEY_CACHED_PICTURE_URI, it.pictureUri)
            }
        }
        restartIntent.putExtra(KEY_CACHED_RECURRENCE, delegate.recurrenceSpinner.selectedItem as? Recurrence)
        finish()
        startActivity(restartIntent)

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
                    mCatId = null
                    errorMsg = "Error while saving transaction"
                }
            }
            showSnackbar(errorMsg, Snackbar.LENGTH_LONG)
            mCreateNew = false
        } else {
            if (mRecordTemplateWidget) {
                recordUsage(ContribFeature.TEMPLATE_WIDGET)
                TemplateWidget.showContribMessage(this)
            }
            if (mOperationType == Transactions.TYPE_SPLIT) {
                recordUsage(ContribFeature.SPLIT_TRANSACTION)
            }
            if (mPictureUri != null) {
                recordUsage(ContribFeature.ATTACH_PICTURE)
            }
            if (mCreateNew) {
                mCreateNew = false
                delegate.prepareForNew()
                //while saving the picture might have been moved from temp to permanent
                //mPictureUri = mTransaction!!.pictureUri
                mNewInstance = true
                mClone = false
                showSnackbar(getString(R.string.save_transaction_and_new_success), Snackbar.LENGTH_SHORT)
            } else {
                if (delegate.recurrenceSpinner.selectedItem === Recurrence.CUSTOM) {
                    launchPlanView(true)
                } else { //make sure soft keyboard is closed
                    hideKeyboard()
                    val intent = Intent()
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                    //no need to call super after finish
                    return
                }
            }
        }
    }

    private fun hideKeyboard() {
        val im = this.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(window.decorView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        when (id) {
            ACCOUNTS_CURSOR -> return CursorLoader(this, TransactionProvider.ACCOUNTS_BASE_URI,
                    null, DatabaseConstants.KEY_SEALED + " = 0", null, null)
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
            ACCOUNTS_CURSOR -> {
                if (data.count == 0) {
                    abortWithMessage(getString(R.string.warning_no_account))
                    return
                }
                if (data.count == 1 && mOperationType == Transactions.TYPE_TRANSFER) {
                    abortWithMessage(getString(R.string.dialog_command_disabled_insert_transfer))
                    return
                }
                delegate.setAccounts(data, if (didUserSetAccount) null else intent.getStringExtra(DatabaseConstants.KEY_CURRENCY))
                if (mOperationType != Transactions.TYPE_TRANSFER) {//the methods cursor is based on the current account,
//hence it is loaded only after the accounts cursor is loaded
                    if (!isSplitPart) {
                        loadMethods(currentAccount)
                    }
                }
                accountsLoaded = true
                if (mIsResumed) setupListeners()
            }
            AUTOFILL_CURSOR ->
                (delegate as? CategoryDelegate)?.autoFill(data, currencyContext)
        }
    }

    fun launchPlanView(forResult: Boolean) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = ContentUris.withAppendedId(CalendarContractCompat.Events.CONTENT_URI, mPlan!!.id)
        //ACTION_VIEW expects to get a range http://code.google.com/p/android/issues/detail?id=23852
        intent.putExtra(CalendarContractCompat.EXTRA_EVENT_BEGIN_TIME, mPlan!!.dtstart)
        intent.putExtra(CalendarContractCompat.EXTRA_EVENT_END_TIME, mPlan!!.dtstart)
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
        when (loader.id) {
            ACCOUNTS_CURSOR -> delegate.accountsAdapter.swapCursor(null)
        }
    }

    fun onToggleClicked(view: View) {
        //(mTransaction as Template?)!!.isPlanExecutionAutomatic = (view as ToggleButton).isChecked
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
        if (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE) == R.id.AUTO_FILL_COMMAND) {
            PreferenceUtils.disableAutoFill()
        } else {
            super.onNegative(args)
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
            R.id.VIEW_COMMAND -> imageViewIntentProvider.startViewIntent(this, mPictureUri)
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
            if (mPictureUriTemp == null) {
                mPictureUriTemp = PictureDirHelper.getOutputMediaUri(true)
            }
            return mPictureUriTemp
        }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = PermissionHelper.allGranted(grantResults)
        when (requestCode) {
            PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR -> {
                run {
                    if (granted) {
                        /*if (mTransaction is Template) {
                            planButton.visibility = View.VISIBLE
                            planExecutionButton.visibility = View.VISIBLE
                            showCustomRecurrenceInfo()
                        }*/
                    } else {
                        //mRecurrenceSpinner.setSelection(0)
                        if (!PermissionGroup.CALENDAR.shouldShowRequestPermissionRationale(this)) {
                            setPlannerRowVisibility(View.GONE)
                        }
                    }
                }
                run {
                    if (granted) {
                        setPicture()
                    } else {
                        unsetPicture()
                    }
                }
            }
            PermissionHelper.PERMISSIONS_REQUEST_STORAGE -> {
                if (granted) {
                    setPicture()
                } else {
                    unsetPicture()
                }
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        isProcessingLinkedAmountInputs = true
        exchangeRateEdit.setBlockWatcher(true)
        super.onRestoreInstanceState(savedInstanceState)
        exchangeRateEdit.setBlockWatcher(false)
        isProcessingLinkedAmountInputs = false
        if (mRowId == 0L/* && mTemplateId == 0L*/) {
            (delegate as? TransferDelegate)?.configureTransferDirection()
        }
    }

    fun clearMethodSelection(view: View) {
        delegate.setMethodSelection(0L)
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

    fun addSplitPartList(transaction: Transaction) {
        val fm = supportFragmentManager
        if (findSplitPartList() == null && !fm.isStateSaved) {
            fm.beginTransaction()
                    .add(R.id.edit_container, SplitPartList.newInstance(transaction), SPLIT_PART_LIST)
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

    companion object {
        private const val SPLIT_PART_LIST = "SPLIT_PART_LIST"
        const val KEY_NEW_TEMPLATE = "newTemplate"
        const val KEY_CLONE = "clone"
        private const val KEY_CACHED_DATA = "cachedData"
        private const val KEY_CACHED_RECURRENCE = "cachedRecurrence"
        private const val KEY_CACHED_PICTURE_URI = "cachedPictureUri"
        const val KEY_AUTOFILL_MAY_SET_ACCOUNT = "autoFillMaySetAccount"
        private const val KEY_AUTOFILL_OVERRIDE_PREFERENCES = "autoFillOverridePreferences"
        const val ACCOUNTS_CURSOR = 3
        const val TRANSACTION_CURSOR = 5
        const val SUM_CURSOR = 6
        const val AUTOFILL_CURSOR = 8
    }
}