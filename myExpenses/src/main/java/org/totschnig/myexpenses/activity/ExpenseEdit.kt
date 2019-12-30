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
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.PopupMenu
import androidx.core.util.Pair
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.android.calendar.CalendarContractCompat
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import icepick.Icepick
import icepick.State
import org.threeten.bp.*
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.CrStatusAdapter
import org.totschnig.myexpenses.adapter.NothingSelectedSpinnerAdapter
import org.totschnig.myexpenses.adapter.OperationTypeAdapter
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.fragment.SplitPartList
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Plan.Recurrence
import org.totschnig.myexpenses.model.Transaction.CrStatus
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PreferenceUtils
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.task.BuildTransactionTask
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.ui.*
import org.totschnig.myexpenses.ui.ExchangeRateEdit.ExchangeRateWatcher
import org.totschnig.myexpenses.util.*
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.UiUtils.DateMode
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewholder.TransactionViewHolder
import org.totschnig.myexpenses.viewmodel.*
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import org.totschnig.myexpenses.widget.AbstractWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import timber.log.Timber
import java.io.Serializable
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

/**
 * Activity for editing a transaction
 *
 * @author Michael Totschnig
 */
class ExpenseEdit : AmountActivity(), AdapterView.OnItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor?>, ContribIFace, ConfirmationDialogListener, ButtonWithDialog.Host, ExchangeRateEdit.Host {
    private val lastExchangeRateRelevantInputs = intArrayOf(INPUT_EXCHANGE_RATE, INPUT_AMOUNT)
    private lateinit var rootBinding: OneExpenseBinding
    private lateinit var dateEditBinding: DateEditBinding
    private val planButton: DateButton
        get() = rootBinding.RR.PB.root as DateButton
    private val planExecutionButton: ToggleButton
        get() = rootBinding.RR.TB.root as ToggleButton
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

    private lateinit var mAccountsAdapter: SimpleCursorAdapter
    private lateinit var mTransferAccountsAdapter: SimpleCursorAdapter
    private lateinit var mPayeeAdapter: SimpleCursorAdapter
    private lateinit var mMethodsAdapter: ArrayAdapter<PaymentMethod>
    private lateinit var mOperationTypeAdapter: OperationTypeAdapter
    private lateinit var mTransferAccountCursor: FilterCursorWrapper
    @JvmField
    @State
    var mRowId = 0L
    @JvmField
    @State
    var mCatId: Long? = null
    @JvmField
    @State
    var mMethodId: Long? = null
    @JvmField
    @State
    var payeeId: Long? = null
    @JvmField
    @State
    var mAccountId = 0L
    @JvmField
    @State
    var parentId = 0L
    @JvmField
    @State
    var mTransferAccountId = 0L
    @JvmField
    @State
    var mLabel: String? = null
    @JvmField
    @State
    var categoryIcon: String? = null
    @JvmField
    @State
    var mPictureUri: Uri? = null
    @JvmField
    @State
    var mPictureUriTemp: Uri? = null
    @JvmField
    @State
    var crStatus: CrStatus? = null

    private var mAccounts = mutableListOf<Account>()
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

    lateinit var viewholder: TransactionViewHolder<*>

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
        viewModel = ViewModelProviders.of(this).get(TransactionEditViewModel::class.java)
        viewModel.getMethods().observe(this, Observer<List<PaymentMethod?>> { paymentMethods ->
            if (paymentMethods == null || paymentMethods.isEmpty()) {
                rootBinding.MethodRow.visibility = View.GONE
                mMethodId = null
            } else {
                rootBinding.MethodRow.visibility = View.VISIBLE
                mMethodsAdapter.clear()
                mMethodsAdapter.addAll(paymentMethods)
                setMethodSelection()
            }
        })
        currencyViewModel = ViewModelProviders.of(this).get(CurrencyViewModel::class.java)
        currencyViewModel.getCurrencies().observe(this, Observer<List<Currency?>> { currencies ->
            viewholder.setCurrencies(currencies, currencyContext)
        })
        //we enable it only after accountcursor has been loaded, preventing NPE when user clicks on it early
        amountInput.setTypeEnabled(false)
        amountInput.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                rootBinding.EquivalentAmount.setCompoundResultInput(amountInput.validate(false))
            }
        })
        rootBinding.OriginalAmount.setCompoundResultOutListener { amount: BigDecimal? -> amountInput.setAmount(amount!!, false) }
        mPayeeAdapter = SimpleCursorAdapter(this, R.layout.support_simple_spinner_dropdown_item, null, arrayOf(DatabaseConstants.KEY_PAYEE_NAME), intArrayOf(android.R.id.text1),
                0)
        rootBinding.Payee.setAdapter(mPayeeAdapter)
        mPayeeAdapter.filterQueryProvider = FilterQueryProvider { constraint: CharSequence? ->
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
            contentResolver.query(
                    TransactionProvider.PAYEES_URI, arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PAYEE_NAME),
                    selection, selectArgs, null)
        }
        mPayeeAdapter.stringConversionColumn = 1
        val supportFragmentManager = supportFragmentManager
        rootBinding.Payee.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val c = mPayeeAdapter.getItem(position) as Cursor
            if (c.moveToPosition(position)) {
                payeeId = c.getLong(0)
                payeeId?.let {
                    if (mNewInstance && mOperationType != Transactions.TYPE_SPLIT) { //moveToPosition should not be necessary,
//but has been reported to not be positioned correctly on samsung GT-I8190N
                        if (prefHandler.getBoolean(PrefKey.AUTO_FILL_HINT_SHOWN, false)) {
                            if (PreferenceUtils.shouldStartAutoFill()) {
                                startAutoFill(it, false)
                            }
                        } else {
                            val b = Bundle()
                            b.putLong(DatabaseConstants.KEY_ROWID, it)
                            b.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.dialog_title_information)
                            b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(R.string.hint_auto_fill))
                            b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.AUTO_FILL_COMMAND)
                            b.putString(ConfirmationDialogFragment.KEY_PREFKEY,
                                    prefHandler.getKey(PrefKey.AUTO_FILL_HINT_SHOWN))
                            b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.yes)
                            b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.no)
                            ConfirmationDialogFragment.newInstance(b).show(supportFragmentManager,
                                    "AUTO_FILL_HINT")
                        }
                    }
                }

            }
        }
        mMethodsAdapter = object : ArrayAdapter<PaymentMethod>(this, android.R.layout.simple_spinner_item) {
            override fun getItemId(position: Int): Long {
                return getItem(position)?.id() ?: 0L
            }
        }
        mMethodsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        mMethodSpinner.adapter = NothingSelectedSpinnerAdapter(
                mMethodsAdapter,
                android.R.layout.simple_spinner_item,  // R.layout.contact_spinner_nothing_selected_dropdown, // Optional
                this)
        mAccountsAdapter = SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null, arrayOf(DatabaseConstants.KEY_LABEL), intArrayOf(android.R.id.text1), 0)
        mAccountsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        mAccountSpinner.adapter = mAccountsAdapter
        mTransferAccountsAdapter = SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null, arrayOf(DatabaseConstants.KEY_LABEL), intArrayOf(android.R.id.text1), 0)
        mTransferAccountsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        mTransferAccountSpinner.adapter = mTransferAccountsAdapter
        mTransferAccountSpinner.setOnItemSelectedListener(this)
        currencyViewModel.loadCurrencies()
        val paint = planExecutionButton.paint
        val automatic = paint.measureText(getString(R.string.plan_automatic)).toInt()
        val manual = paint.measureText(getString(R.string.plan_manual)).toInt()
        with(planExecutionButton) {
            width = ((if (automatic > manual) automatic else manual) +
                    +paddingLeft + paddingRight)
        }
        val extras = intent.extras
        mRowId = Utils.getFromExtra(extras, DatabaseConstants.KEY_ROWID, 0)
        //mTemplateId = intent.getLongExtra(DatabaseConstants.KEY_TEMPLATEID, 0)
        //upon orientation change stored in instance state, since new splitTransactions are immediately persisted to DB
        if (savedInstanceState != null) {
            mSavedInstance = true
            Icepick.restoreInstanceState(this, savedInstanceState)
            setPicture()
            if (mAccountId != null) {
                didUserSetAccount = true
            }
        }
        //were we called from a notification
        val notificationId = intent.getIntExtra(MyApplication.KEY_NOTIFICATION_ID, 0)
        if (notificationId > 0) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        }
        val sAdapter: CrStatusAdapter = object : CrStatusAdapter(this) {
            override fun isEnabled(position: Int): Boolean { //if the transaction is reconciled, the status can not be changed
//otherwise only unreconciled and cleared can be set
                return crStatus != CrStatus.RECONCILED && position != CrStatus.RECONCILED.ordinal
            }
        }
        mStatusSpinner.adapter = sAdapter
        //1. fetch the transaction or create a new instance
        if (mRowId != 0L) {
            mNewInstance = false
            if (mRowId != 0L) {
                viewModel.transaction(mRowId).observe(this, Observer {
                    populate(it)
                })
                //if called with extra KEY_CLONE, we ask the task to clone, but no longer after orientation change
                //extra = intent.getBooleanExtra(KEY_CLONE, false) && savedInstanceState == null
                //objectId = mRowId
            } else {
//                objectId = mTemplateId!!
//                //are we editing the template or instantiating a new transaction from the template
//                if (intent.getLongExtra(DatabaseConstants.KEY_INSTANCEID, 0).also { mPlanInstanceId = it } != 0L) {
//                    taskId = TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE
//                    mPlanInstanceDate = intent.getLongExtra(DatabaseConstants.KEY_DATE, 0)
//                    mRecordTemplateWidget = intent.getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false) &&
//                            !ContribFeature.TEMPLATE_WIDGET.hasAccess()
//                } else {
//                    taskId = TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE
//                }
            }
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
            mOperationTypeSpinner = SpinnerHelper(rootBinding.toolbar.OperationType)
            rootBinding.toolbar.OperationType.visibility = View.VISIBLE
            val allowedOperationTypes: MutableList<Int> = ArrayList()
            allowedOperationTypes.add(Transactions.TYPE_TRANSACTION)
            allowedOperationTypes.add(Transactions.TYPE_TRANSFER)
            if (parentId == 0L) {
                allowedOperationTypes.add(Transactions.TYPE_SPLIT)
            }
            mOperationTypeAdapter = OperationTypeAdapter(this, allowedOperationTypes,
                    isNewTemplate, parentId != 0L)
            mOperationTypeSpinner.adapter = mOperationTypeAdapter
            resetOperationType()
            mOperationTypeSpinner.setOnItemSelectedListener(this)
            var accountId = intent.getLongExtra(DatabaseConstants.KEY_ACCOUNTID, 0)
            if (!mSavedInstance && Intent.ACTION_INSERT == intent.action && extras != null) {
                val args = Bundle(1)
                args.putBundle(BuildTransactionTask.KEY_EXTRAS, extras)
                startTaskExecution(TaskExecutionFragment.TASK_BUILD_TRANSACTION_FROM_INTENT_EXTRAS, args,
                        R.string.progress_dialog_loading)
            } else {
                if (false) {
//                    mTransaction = Template.getTypedNewInstance(mOperationType, accountId, true, if (parentId != 0L) parentId else null)
////                    if (mOperationType == Transactions.TYPE_SPLIT && mTransaction != null) {
////                        mTemplateId = mTransaction.getId()
////                    }
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
                        this@ExpenseEdit.getString(R.string.discover_feature_operation_type_select),
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
        if (mAccounts != null) setupListeners()
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
        val oldCursor = mPayeeAdapter.cursor
        if (oldCursor != null && !oldCursor.isClosed) {
            oldCursor.close()
        }
    }

    private fun updateSplitBalance() {
        val splitPartList = findSplitPartList()
        splitPartList?.updateBalance()
    }

    private fun populate(transaction: Transaction?) {
        if (transaction == null) {
            val errMsg = getString(R.string.warning_no_account)
            abortWithMessage(errMsg)
            return
        }
        viewholder = TransactionViewHolder.createAndBind(transaction, rootBinding, isCalendarPermissionPermanentlyDeclined(), prefHandler)
        viewholder.setAdapters(mAccountsAdapter, mMethodsAdapter, mPayeeAdapter, mOperationTypeAdapter, mTransferAccountsAdapter)
        linkInputsWithLabels()
        mManager.initLoader<Cursor>(ACCOUNTS_CURSOR, null, this)
        //setHelpVariant()
        //setTitle()
/*        if (!mSavedInstance) { //processing data from user switching operation type
            val cached = intent.getSerializableExtra(KEY_CACHED_DATA) as? Transaction
            if (cached != null) {
                transaction.accountId = cached.accountId
                setLocalDateTime(cached)
                mPictureUri = intent.getParcelableExtra(KEY_CACHED_PICTURE_URI)
                setPicture()
                mMethodId = cached.methodId
            }
        }*/
        // Spinner for methods
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
        rootBinding.PlanRow.visibility = visibility
    }

    override fun setupListeners() {
        super.setupListeners()
        rootBinding.Comment.addTextChangedListener(this)
        rootBinding.Title.addTextChangedListener(this)
        rootBinding.Payee.addTextChangedListener(this)
        rootBinding.Number.addTextChangedListener(this)
        mAccountSpinner.setOnItemSelectedListener(this)
        mMethodSpinner.setOnItemSelectedListener(this)
        mStatusSpinner.setOnItemSelectedListener(this)
    }

    override fun linkInputsWithLabels() {
        super.linkInputsWithLabels()
        linkAccountLabels()
        linkInputWithLabel(rootBinding.Title, rootBinding.TitleLabel)
        linkInputWithLabel(dateEditBinding.DateButton, rootBinding.DateTimeLabel)
        linkInputWithLabel(rootBinding.Payee, rootBinding.PayeeLabel)
        with(rootBinding.CommentLabel) {
            linkInputWithLabel(mStatusSpinner.spinner, this)
            linkInputWithLabel(rootBinding.AttachImage, this)
            linkInputWithLabel(rootBinding.PictureContainer.root, this)
            linkInputWithLabel(rootBinding.Comment, this)
        }
        linkInputWithLabel(rootBinding.Category, rootBinding.CategoryLabel)
        linkInputWithLabel(mMethodSpinner.spinner, rootBinding.MethodLabel)
        linkInputWithLabel(rootBinding.Number, rootBinding.MethodLabel)
        linkInputWithLabel(planButton, rootBinding.PlanLabel)
        linkInputWithLabel(mRecurrenceSpinner.spinner, rootBinding.PlanLabel)
        linkInputWithLabel(planExecutionButton, rootBinding.PlanLabel)
        linkInputWithLabel(rootBinding.TransferAmount, rootBinding.TransferAmountLabel)
        linkInputWithLabel(rootBinding.OriginalAmount, rootBinding.OriginalAmountLabel)
        linkInputWithLabel(rootBinding.EquivalentAmount, rootBinding.EquivalentAmountLabel)
    }

    private fun linkAccountLabels() {
        linkInputWithLabel(mAccountSpinner.spinner,
                if (isIncome) rootBinding.TransferAccountLabel else rootBinding.AccountLabel)
        linkInputWithLabel(mTransferAccountSpinner.spinner,
                if (isIncome) rootBinding.AccountLabel else rootBinding.TransferAccountLabel)
    }

    override fun onTypeChanged(isChecked: Boolean) {
        super.onTypeChanged(isChecked)
        if (mIsMainTransactionOrTemplate) {
            mMethodId = null
            loadMethods(currentAccount)
        }
        discoveryHelper.markDiscovered(DiscoveryHelper.Feature.EI_SWITCH)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val oaMenuItem = menu.findItem(R.id.ORIGINAL_AMOUNT_COMMAND)
        if (oaMenuItem != null) {
            oaMenuItem.isChecked = viewholder.originalAmountVisible
        }
        val currentAccount = currentAccount
        val eaMenuItem = menu.findItem(R.id.EQUIVALENT_AMOUNT_COMMAND)
        if (eaMenuItem != null) {
            Utils.menuItemSetEnabledAndVisible(eaMenuItem, !(currentAccount == null || hasHomeCurrency(currentAccount)))
            eaMenuItem.isChecked = viewholder.equivalentAmountVisible
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun hasHomeCurrency(account: Account): Boolean {
        return account.currencyUnit == Utils.getHomeCurrency()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (!(isNoMainTransaction ||
                        mOperationType == Transactions.TYPE_SPLIT &&
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
                switchAccountViews()
                return true
            }
            R.id.ORIGINAL_AMOUNT_COMMAND -> {
                viewholder.toggleOriginalAmount()
                invalidateOptionsMenu()
                return true
            }
            R.id.EQUIVALENT_AMOUNT_COMMAND -> {
                viewholder.toggleEquivalentAmount(currentAccount)
                invalidateOptionsMenu()
                return true
            }
        }
        return false
    }

    private fun checkTransferEnabled(account: Account?): Boolean {
        if (account == null) return false
        if (mAccounts.size <= 1) {
            showMessage(R.string.dialog_command_disabled_insert_transfer)
            return false
        }
        return true
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
    private fun startSelectCategory() {
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
        syncStateAndValidate(true)?.let {
            mIsSaving = true
            viewModel.save(it).observe(this, Observer {
                onSaved(it)
            })
            if (intent.getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false)) {
                when (mOperationType) {
                    Transactions.TYPE_TRANSACTION -> prefHandler.putLong(PrefKey.TRANSACTION_LAST_ACCOUNT_FROM_WIDGET, mAccountId)
                    Transactions.TYPE_TRANSFER -> {
                        prefHandler.putLong(PrefKey.TRANSFER_LAST_ACCOUNT_FROM_WIDGET, mAccountId)
                        prefHandler.putLong(PrefKey.TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET, mTransferAccountId)
                    }
                    Transactions.TYPE_SPLIT -> prefHandler.putLong(PrefKey.SPLIT_LAST_ACCOUNT_FROM_WIDGET, mAccountId)
                }
            }
        } ?: kotlin.run {
            //prevent this flag from being sticky if form was not valid
            mCreateNew = false
        }
    }

    /**
     * sets the state of the UI on mTransaction
     *
     * @return false if any data is not valid, also informs user through snackBar
     */
    private fun syncStateAndValidate(forSave: Boolean): Transaction? {
        var validP = true
        val title: String
        val account = currentAccount ?: return null
        mAccountId = account.id
        val amount = validateAmountInput(forSave)
        if (amount == null) { //Snackbar is shown in validateAmountInput
            validP = false
            return null
        }
        return when(mOperationType) {
            Transactions.TYPE_TRANSFER -> Transfer()
            Transactions.TYPE_SPLIT -> SplitTransaction()
            else -> Transaction()
        }.apply {
            id = mRowId
            accountId = mAccountId
            comment = rootBinding.Comment.text.toString()
            if (!isNoMainTransaction) {
                val transactionDate = readZonedDateTime(dateEditBinding.DateButton)
                setDate(transactionDate)
                if (dateEditBinding.Date2Button.visibility == View.VISIBLE) {
                    setValueDate(if (dateEditBinding.Date2Button.visibility == View.VISIBLE) readZonedDateTime(dateEditBinding.Date2Button) else transactionDate)
                }
            }
            if (mOperationType == Transactions.TYPE_TRANSACTION) {
                catId = mCatId
                label = mLabel
            }
            if (mIsMainTransactionOrTemplate) {
                payee = rootBinding.Payee.text.toString()
                methodId = mMethodId
            }
            if (mOperationType == Transactions.TYPE_TRANSFER) {
                transferAccountId = mTransferAccountSpinner.selectedItemId
                val transferAccount = transferAccount ?: return null
                val isSame = account.currencyUnit == transferAccount.currencyUnit
                if (this is Template) {
/*                if (amount != null) {
                    mTransaction.setAmount(Money(account.currencyUnit, amount))
                } else if (!isSame) {
                    var transferAmount = validateAmountInput(rootBinding.TransferAmount, forSave)
                    if (transferAmount != null) {
                        mTransaction.setAccountId(transferAccount.id)
                        mTransaction.setTransferAccountId(account.id)
                        if (isIncome) {
                            transferAmount = transferAmount.negate()
                        }
                        mTransaction.setAmount(Money(transferAccount.currencyUnit, transferAmount!!))
                        amountInput.setError(null)
                        validP = true //we only need either amount or transfer amount
                    }
                }*/
                } else {
                    var transferAmount: BigDecimal
                    if (isSame) {
                        transferAmount = amount.negate()
                    } else {
                        transferAmount = validateAmountInput(rootBinding.TransferAmount, forSave)
                        if (transferAmount == null) { //Snackbar is shown in validateAmountInput
                            return null
                        } else {
                            if (isIncome) {
                                transferAmount = transferAmount.negate()
                            }
                        }
                    }
                    if (validP) {
                        (this as? Transfer)?.setAmountAndTransferAmount(
                                Money(account.currencyUnit, amount),
                                Money(transferAccount.currencyUnit, transferAmount))
                    }
                }
            } else {
                if (validP) {
                    this.amount = Money(account.currencyUnit, amount)
                }
                if (mIsMainTransaction) {
                    val originalAmount = validateAmountInput(rootBinding.OriginalAmount, false)
                    val selectedItem = rootBinding.OriginalAmount.selectedCurrency
                    if (selectedItem != null && originalAmount != null) {
                        val currency = selectedItem.code()
                        PrefKey.LAST_ORIGINAL_CURRENCY.putString(currency)
                        this.originalAmount = Money(currencyContext[currency], originalAmount)
                    } else {
                        this.originalAmount = null
                    }
                    val equivalentAmount = validateAmountInput(rootBinding.EquivalentAmount, false)
                    this.equivalentAmount = if (equivalentAmount == null) null else Money(Utils.getHomeCurrency(), if (isIncome) equivalentAmount else equivalentAmount.negate())
                }
            }
            if (mIsMainTemplate) {
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
                referenceNumber = rootBinding.Number.text.toString()
                if (forSave && !isSplitPart) {
                    if (mRecurrenceSpinner.selectedItemPosition > 0) {
                        setInitialPlan(Pair.create(mRecurrenceSpinner.selectedItem as Recurrence, dateEditBinding.DateButton.date))
                    }
                }
            }
            crStatus = (mStatusSpinner.selectedItem as CrStatus)
            pictureUri = mPictureUri
        }
    }

    private fun readZonedDateTime(dateEdit: DateButton?): ZonedDateTime {
        return ZonedDateTime.of(dateEdit!!.date,
                if (dateEditBinding.TimeButton.visibility == View.VISIBLE) dateEditBinding.TimeButton.time else LocalTime.now(),
                ZoneId.systemDefault())
    }

    private fun setLocalDateTime(transaction: Transaction?) {
        val zonedDateTime = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(transaction!!.date), ZoneId.systemDefault())
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
            mLabel = intent.getStringExtra(DatabaseConstants.KEY_LABEL)
            categoryIcon = intent.getStringExtra(DatabaseConstants.KEY_ICON)
            viewholder.setCategoryButton(mLabel, categoryIcon)
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

    private fun setPicture() {
        if (mPictureUri != null) {
            rootBinding.PictureContainer.root.visibility = View.VISIBLE
            Picasso.get().load(mPictureUri).fit().into(rootBinding.PictureContainer.picture)
            rootBinding.AttachImage.visibility = View.GONE
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
        rootBinding.PayeeLabel.setText(if (amountInput.type) R.string.payer else R.string.payee)
        if (mOperationType == Transactions.TYPE_SPLIT) {
            updateSplitBalance()
        }
        //setCategoryButton()
    }

    private fun configurePlan() {
        if (mPlan != null) {
            planButton.text = Plan.prettyTimeInfo(this, mPlan!!.rrule, mPlan!!.dtstart)
            if (rootBinding.Title.text.toString() == "") rootBinding.Title.setText(mPlan!!.title)
            planExecutionButton.visibility = View.VISIBLE
            mRecurrenceSpinner.spinner.visibility = View.GONE
            planButton.visibility = View.VISIBLE
            pObserver = PlanObserver().also {
                contentResolver.registerContentObserver(
                        ContentUris.withAppendedId(CalendarContractCompat.Events.CONTENT_URI, mPlan!!.id),
                        false, it)
            }
        }
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

    private fun configureStatusSpinner() {
        val a = currentAccount
        setVisibility(mStatusSpinner.spinner,
                !isNoMainTransaction && a != null && a.type != AccountType.CASH)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val methodId = mMethodSpinner.selectedItemId
        if (methodId > 0) {
            mMethodId = methodId
        }
        if (didUserSetAccount) {
            val accountId = mAccountSpinner.selectedItemId
            if (accountId != AdapterView.INVALID_ROW_ID) {
                mAccountId = accountId
            }
        }
        if (mOperationType == Transactions.TYPE_TRANSFER) {
            val transferAccountId = mTransferAccountSpinner.selectedItemId
            if (transferAccountId != AdapterView.INVALID_ROW_ID) {
                mTransferAccountId = transferAccountId
            }
        }
        val originalInputSelectedCurrency = rootBinding.OriginalAmount.selectedCurrency
        if (originalInputSelectedCurrency != null) {
            originalCurrencyCode = originalInputSelectedCurrency.code()
        }
        Icepick.saveInstanceState(this, outState)
    }

    private fun switchAccountViews() {
        val accountSpinner = mAccountSpinner.spinner
        val transferAccountSpinner = mTransferAccountSpinner.spinner
        with(rootBinding.Table) {
            removeView(amountRow)
            removeView(rootBinding.TransferAmountRow)
            if (isIncome) {
                if (accountSpinner.parent === rootBinding.AccountRow && transferAccountSpinner.parent === rootBinding.TransferAccountRow) {
                    rootBinding.AccountRow.removeView(accountSpinner)
                    rootBinding.TransferAccountRow.removeView(transferAccountSpinner)
                    rootBinding.AccountRow.addView(transferAccountSpinner)
                    rootBinding.TransferAccountRow.addView(accountSpinner)
                }
                addView(rootBinding.TransferAmountRow, 2)
                addView(amountRow, 4)
            } else {
                if (accountSpinner.parent === rootBinding.TransferAccountRow && transferAccountSpinner.parent === rootBinding.AccountRow) {
                    rootBinding.AccountRow.removeView(transferAccountSpinner)
                    rootBinding.TransferAccountRow.removeView(accountSpinner)
                    rootBinding.AccountRow.addView(accountSpinner)
                    rootBinding.TransferAccountRow.addView(transferAccountSpinner)
                }
                addView(amountRow, 2)
                addView(rootBinding.TransferAmountRow, 4)
            }
        }

        linkAccountLabels()
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
        rootBinding.AttachImage.visibility = View.VISIBLE
        rootBinding.PictureContainer.root.visibility = View.GONE
    }

    @get:VisibleForTesting
    val currentAccount: Account?
        get() = getAccountFromSpinner(mAccountSpinner)

    private val transferAccount: Account?
        get() = getAccountFromSpinner(mTransferAccountSpinner)

    private fun getAccountFromSpinner(spinner: SpinnerHelper?): Account? {
        val selected = spinner!!.selectedItemPosition
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

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int,
                                id: Long) {
        if (parent.id != R.id.OperationType) {
            setDirty()
        }
        when (parent.id) {
            R.id.Recurrence -> {
                var visibility = View.GONE
                if (id > 0) {
                    if (PermissionGroup.CALENDAR.hasPermission(this)) {
                        val newSplitTemplateEnabled = prefHandler.getBoolean(PrefKey.NEW_SPLIT_TEMPLATE_ENABLED, true)
                        val newPlanEnabled = prefHandler.getBoolean(PrefKey.NEW_PLAN_ENABLED, true)
                        if (newPlanEnabled && (newSplitTemplateEnabled || mOperationType != Transactions.TYPE_SPLIT /*|| mTransaction is Template*/)) {
                            visibility = View.VISIBLE
                            showCustomRecurrenceInfo()
                        } else {
                            mRecurrenceSpinner.setSelection(0)
                            val contribFeature = if (mOperationType != Transactions.TYPE_SPLIT || newSplitTemplateEnabled) ContribFeature.PLANS_UNLIMITED else ContribFeature.SPLIT_TEMPLATE
                            showContribDialog(contribFeature, null)
                        }
                    } else {
                        requestPermission(PermissionGroup.CALENDAR)
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
                    mMethodId = parent.selectedItemId
                    if (mMethodId!! <= 0) {
                        mMethodId = null
                    }
                } else {
                    mMethodId = null
                }
                setVisibility(rootBinding.ClearMethod, hasSelection)
                setReferenceNumberVisibility()
            }
            R.id.Account -> {
                val account = mAccounts[position]
                if (mOperationType == Transactions.TYPE_SPLIT) {
                    val splitPartList = findSplitPartList()
                    if (splitPartList != null && splitPartList.splitCount > 0) { //call background task for moving parts to new account
                        startTaskExecution(
                                TaskExecutionFragment.TASK_MOVE_UNCOMMITED_SPLIT_PARTS, arrayOf(mRowId),
                                account.id,
                                R.string.progress_dialog_updating_split_parts)
                        return
                    }
                }
                updateAccount(account)
            }
            R.id.OperationType -> {
                discoveryHelper.markDiscovered(DiscoveryHelper.Feature.OPERATION_TYPE_SELECT)
                val newType = mOperationTypeSpinner.getItemAtPosition(position) as Int
                if (newType != mOperationType && isValidType(newType)) {
                    if (newType == Transactions.TYPE_TRANSFER && !checkTransferEnabled(currentAccount)) { //reset to previous
                        resetOperationType()
                    } else if (newType == Transactions.TYPE_SPLIT) {
                        resetOperationType()
/*                        if (mTransaction is Template) {
                            if (PrefKey.NEW_SPLIT_TEMPLATE_ENABLED.getBoolean(true)) {
                                restartWithType(Transactions.TYPE_SPLIT)
                            } else {
                                showContribDialog(ContribFeature.SPLIT_TEMPLATE, null)
                            }
                        } else {*/
                        contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION, null)
                        //}
                    } else {
                        restartWithType(newType)
                    }
                }
            }
            R.id.TransferAccount -> {
                mTransferAccountId = mTransferAccountSpinner.selectedItemId
                configureTransferInput()
            }
        }
    }

    private fun showCustomRecurrenceInfo() {
        if (mRecurrenceSpinner.selectedItem === Recurrence.CUSTOM) {
            showSnackbar(R.string.plan_custom_recurrence_info, Snackbar.LENGTH_LONG)
        }
    }

    private fun isValidType(type: Int): Boolean {
        return type == Transactions.TYPE_SPLIT || type == Transactions.TYPE_TRANSACTION || type == Transactions.TYPE_TRANSFER
    }

    private fun configureAccountDependent(account: Account?) {
        val currencyUnit = account!!.currencyUnit
        addCurrencyToInput(amountLabel, amountInput, currencyUnit.symbol(), R.string.amount)
        rootBinding.OriginalAmount.configureExchange(currencyUnit)
        if (hasHomeCurrency(account)) {
            rootBinding.EquivalentAmountRow.visibility = View.GONE
            equivalentAmountVisible = false
        } else {
            rootBinding.EquivalentAmount.configureExchange(currencyUnit, Utils.getHomeCurrency())
        }
        configureDateInput(account)
    }

    private fun loadMethods(account: Account?) {
        if (account != null) {
            viewModel.loadMethods(isIncome, account.type)
        }
    }

    private fun updateAccount(account: Account) {
        didUserSetAccount = true
        mAccountId = account.id
        configureAccountDependent(account)
        if (mOperationType == Transactions.TYPE_TRANSFER) {
            mTransferAccountSpinner.setSelection(setTransferAccountFilterMap())
            mTransferAccountId = mTransferAccountSpinner.selectedItemId
            configureTransferInput()
        } else {
            if (!isSplitPart) {
                loadMethods(account)
            }
            if (mOperationType == Transactions.TYPE_SPLIT) {
                val splitPartList = findSplitPartList()
                splitPartList?.updateAccount(account)
            }
        }
        configureStatusSpinner()
        amountInput.setFractionDigits(account.currencyUnit.fractionDigits())
    }

    private fun configureDateInput(account: Account?) {
        val dateMode = UiUtils.getDateMode(account, prefHandler)
        setVisibility(dateEditBinding.TimeButton, dateMode == DateMode.DATE_TIME)
        setVisibility(dateEditBinding.Date2Button, dateMode == DateMode.BOOKING_VALUE)
        setVisibility(dateEditBinding.DateLink, dateMode == DateMode.BOOKING_VALUE)
        var dateLabel: String
        if (dateMode == DateMode.BOOKING_VALUE) {
            dateLabel = getString(R.string.booking_date) + "/" + getString(R.string.value_date)
        } else {
            dateLabel = getString(R.string.date)
            if (dateMode == DateMode.DATE_TIME) {
                dateLabel += " / " + getString(R.string.time)
            }
        }
        rootBinding.DateTimeLabel.text = dateLabel
    }

    private fun resetOperationType() {
        mOperationTypeSpinner.setSelection(mOperationTypeAdapter.getPosition(mOperationType))
    }

    private fun restartWithType(newType: Int) {
        val bundle = Bundle()
        bundle.putInt(Tracker.EVENT_PARAM_OPERATION_TYPE, newType)
        logEvent(Tracker.EVENT_SELECT_OPERATION_TYPE, bundle)
        cleanup()
        val restartIntent = intent
        restartIntent.putExtra(Transactions.OPERATION_TYPE, newType)
        syncStateAndValidate(false)?.let {
            restartIntent.putExtra(KEY_CACHED_DATA, it)
            if (it.pictureUri != null) {
                restartIntent.putExtra(KEY_CACHED_PICTURE_URI, it.pictureUri)
            }
        }
        restartIntent.putExtra(KEY_CACHED_RECURRENCE, mRecurrenceSpinner.selectedItem as Recurrence)
        finish()
        startActivity(restartIntent)

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun onSaved(result: Long) {
        if (result < 0L) {
            val errorMsg: String
            when (result) {
                ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE -> errorMsg = getString(R.string.external_storage_unavailable)
                ERROR_PICTURE_SAVE_UNKNOWN -> errorMsg = "Error while saving picture"
                ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE -> {
                    mRecurrenceSpinner.setSelection(0)
                    //mTransaction!!.originTemplate = null
                    errorMsg = "Recurring transactions are not available, because calendar integration is not functional on this device."
                }
                else -> {
                    //possibly the selected category has been deleted
                    mCatId = null
                    rootBinding.Category.setText(R.string.select)
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
                if (mOperationType == Transactions.TYPE_SPLIT) {
                    mRowId =  SplitTransaction.getNewInstance(mAccountId).id
                    val splitPartList = findSplitPartList()
                    splitPartList?.updateParent(mRowId)
                } else {
                    mRowId = 0L
                    mRecurrenceSpinner.spinner.visibility = View.VISIBLE
                    mRecurrenceSpinner.setSelection(0)
                    planButton.visibility = View.GONE
                }
                //while saving the picture might have been moved from temp to permanent
                //mPictureUri = mTransaction!!.pictureUri
                mNewInstance = true
                mClone = false
                isProcessingLinkedAmountInputs = true
                amountInput.clear()
                rootBinding.TransferAmount.clear()
                isProcessingLinkedAmountInputs = false
                showSnackbar(getString(R.string.save_transaction_and_new_success), Snackbar.LENGTH_SHORT)
            } else {
                if (mRecurrenceSpinner.selectedItem === Recurrence.CUSTOM) {
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

    private fun setReferenceNumberVisibility() {
/*        if (mTransaction is Template) {
            return
        }*/
        //ignore first row "select" merged in
        val position = mMethodSpinner.selectedItemPosition
        if (position > 0) {
            val pm = mMethodsAdapter.getItem(position - 1)
            rootBinding.Number.visibility = if (pm != null && pm.isNumbered) View.VISIBLE else View.INVISIBLE
        } else {
            rootBinding.Number.visibility = View.GONE
        }
    }

    private fun setMethodSelection() {
        if (mMethodId != null) {
            var found = false
            for (i in 0 until mMethodsAdapter.count) {
                val pm = mMethodsAdapter.getItem(i)
                if (pm != null) {
                    if (pm.id() == mMethodId) {
                        mMethodSpinner.setSelection(i + 1)
                        found = true
                        break
                    }
                }
            }
            if (!found) {
                mMethodId = null
                mMethodSpinner.setSelection(0)
            }
        } else {
            mMethodSpinner.setSelection(0)
        }
        setVisibility(rootBinding.ClearMethod, mMethodId != null)
        setReferenceNumberVisibility()
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
                mAccountsAdapter.swapCursor(data)
/*                if (didUserSetAccount) {
                    mTransaction!!.accountId = mAccountId
                    if (mOperationType == Transactions.TYPE_TRANSFER) {
                        mTransaction!!.transferAccountId = mTransferAccountId
                    }
                }*/
                data.moveToFirst()
                var selectionSet = false
                val currencyExtra = if (didUserSetAccount) null else intent.getStringExtra(DatabaseConstants.KEY_CURRENCY)
                while (!data.isAfterLast) {
                    val position = data.position
                    val a = Account.fromCursor(data)
                    mAccounts.add(a)
                    if (!selectionSet &&
                            (a.currencyUnit.code() == currencyExtra ||
                                    currencyExtra == null && a.id == mAccountId)) {
                        mAccountSpinner.setSelection(position)
                        configureAccountDependent(a)
                        selectionSet = true
                    }
                    data.moveToNext()
                }
                //if the accountId we have been passed does not exist, we select the first entry
                if (mAccountSpinner.selectedItemPosition == AdapterView.INVALID_POSITION) {
                    mAccountSpinner.setSelection(0)
                    mAccountId = mAccounts[0].id
                    configureAccountDependent(mAccounts[0])
                }
                if (mOperationType == Transactions.TYPE_TRANSFER) {
                    mTransferAccountCursor = FilterCursorWrapper(data)
                    val selectedPosition = setTransferAccountFilterMap()
                    mTransferAccountsAdapter.swapCursor(mTransferAccountCursor)
                    mTransferAccountSpinner.setSelection(selectedPosition)
                    mTransferAccountId = mTransferAccountSpinner.selectedItemId
                    configureTransferInput()
                    if (!mNewInstance /*&& mTransaction !is Template*/) {
                        //TODO
                        /* isProcessingLinkedAmountInputs = true
                         rootBinding.TransferAmount.setAmount(mTransaction!!.transferAmount.amountMajor.abs())
                         updateExchangeRates(rootBinding.TransferAmount)
                         isProcessingLinkedAmountInputs = false*/
                    }
                } else { //the methods cursor is based on the current account,
//hence it is loaded only after the accounts cursor is loaded
                    if (!isSplitPart) {
                        loadMethods(currentAccount)
                    }
                }
                amountInput.setTypeEnabled(true)
                configureType()
                configureStatusSpinner()
                if (mIsResumed) setupListeners()
            }
            AUTOFILL_CURSOR -> if (data.moveToFirst()) {
                var typeHasChanged = false
                val columnIndexCatId = data.getColumnIndex(DatabaseConstants.KEY_CATID)
                val columnIndexLabel = data.getColumnIndex(DatabaseConstants.KEY_LABEL)
                if (mCatId == null && columnIndexCatId != -1 && columnIndexLabel != -1) {
                    mCatId = DbUtils.getLongOrNull(data, columnIndexCatId)
                    mLabel = data.getString(columnIndexLabel)
                    setCategoryButton()
                }
                val columnIndexComment = data.getColumnIndex(DatabaseConstants.KEY_COMMENT)
                if (TextUtils.isEmpty(rootBinding.Comment.text.toString()) && columnIndexComment != -1) {
                    rootBinding.Comment.setText(data.getString(columnIndexComment))
                }
                val columnIndexAmount = data.getColumnIndex(DatabaseConstants.KEY_AMOUNT)
                val columnIndexCurrency = data.getColumnIndex(DatabaseConstants.KEY_CURRENCY)
                if (validateAmountInput(amountInput, false) == null && columnIndexAmount != -1 && columnIndexCurrency != -1) {
                    val beforeType = isIncome
                    fillAmount(Money(currencyContext[data.getString(columnIndexCurrency)], data.getLong(columnIndexAmount)).amountMajor)
                    configureType()
                    typeHasChanged = beforeType != isIncome
                }
                val columnIndexMethodId = data.getColumnIndex(DatabaseConstants.KEY_METHODID)
                if (mMethodId == null && columnIndexMethodId != -1) {
                    mMethodId = DbUtils.getLongOrNull(data, columnIndexMethodId)
                    if (!typeHasChanged) { //if type has changed, we need to wait for methods to be reloaded, method is then selected in onLoadFinished
                        setMethodSelection()
                    }
                }
                val columnIndexAccountId = data.getColumnIndex(DatabaseConstants.KEY_ACCOUNTID)
                if (!didUserSetAccount && columnIndexAccountId != -1) {
                    val accountId = data.getLong(columnIndexAccountId)
                    var i = 0
                    while (i < mAccounts.size) {
                        if (mAccounts[i].id == accountId) {
                            mAccountSpinner.setSelection(i)
                            updateAccount(mAccounts[i])
                            break
                        }
                        i++
                    }
                }
            }
        }
    }

    private fun setTransferAccountFilterMap(): Int {
        val fromAccount = mAccounts[mAccountSpinner.selectedItemPosition]
        val list = ArrayList<Int>()
        var position = 0
        var selectedPosition = 0
        for (i in mAccounts.indices) {
            if (fromAccount.id != mAccounts[i].id) {
                list.add(i)
                if (mTransferAccountId != null && mTransferAccountId == mAccounts[i].id) {
                    selectedPosition = position
                }
                position++
            }
        }
        mTransferAccountCursor.setFilterMap(list)
        mTransferAccountsAdapter.notifyDataSetChanged()
        return selectedPosition
    }

    private fun launchPlanView(forResult: Boolean) {
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
            ACCOUNTS_CURSOR -> mAccountsAdapter.swapCursor(null)
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
            resetOperationType()
        }
    }

    private fun disableAccountSpinner() {
        mAccountSpinner.isEnabled = false
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
    private fun startAutoFill(id: Long, overridePreferences: Boolean) {
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

    private fun findSplitPartList(): SplitPartList? {
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
                        mRecurrenceSpinner.setSelection(0)
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

    private fun updateExchangeRates(other: AmountInput?) {
        val amount = validateAmountInput(amountInput, false)
        val transferAmount = validateAmountInput(other, false)
        exchangeRateEdit.calculateAndSetRate(amount, transferAmount)
    }

    private open inner class MyTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    }

    private inner class LinkedTransferAmountTextWatcher(
            /**
             * true if we are linked to from amount
             */
            var isMain: Boolean) : MyTextWatcher() {

        override fun afterTextChanged(s: Editable) {
            if (isProcessingLinkedAmountInputs) return
            isProcessingLinkedAmountInputs = true
/*            if (mTransaction is Template) {
                (if (isMain) rootBinding.TransferAmount else amountInput).clear()
            } else */if (exchangeRateRow.visibility == View.VISIBLE) {
                val currentFocus = if (isMain) INPUT_AMOUNT else INPUT_TRANSFER_AMOUNT
                if (lastExchangeRateRelevantInputs[0] != currentFocus) {
                    lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0]
                    lastExchangeRateRelevantInputs[0] = currentFocus
                }
                if (lastExchangeRateRelevantInputs[1] == INPUT_EXCHANGE_RATE) {
                    applyExchangeRate(if (isMain) amountInput else rootBinding.TransferAmount,
                            if (isMain) rootBinding.TransferAmount else amountInput,
                            exchangeRateEdit.getRate(!isMain))
                } else {
                    updateExchangeRates(rootBinding.TransferAmount)
                }
            }
            isProcessingLinkedAmountInputs = false
        }

    }

    private inner class LinkedExchangeRateTextWatcher : ExchangeRateWatcher {
        override fun afterExchangeRateChanged(rate: BigDecimal, inverse: BigDecimal) {
            if (isProcessingLinkedAmountInputs) return
            isProcessingLinkedAmountInputs = true
            val constant: AmountInput?
            val variable: AmountInput?
            val exchangeFactor: BigDecimal
            if (lastExchangeRateRelevantInputs[0] != INPUT_EXCHANGE_RATE) {
                lastExchangeRateRelevantInputs[1] = lastExchangeRateRelevantInputs[0]
                lastExchangeRateRelevantInputs[0] = INPUT_EXCHANGE_RATE
            }
            if (lastExchangeRateRelevantInputs[1] == INPUT_AMOUNT) {
                constant = amountInput
                variable = rootBinding.TransferAmount
                exchangeFactor = rate
            } else {
                constant = rootBinding.TransferAmount
                variable = amountInput
                exchangeFactor = inverse
            }
            applyExchangeRate(constant, variable, exchangeFactor)
            isProcessingLinkedAmountInputs = false
        }
    }

    private fun applyExchangeRate(from: AmountInput?, to: AmountInput?, rate: BigDecimal?) {
        val input = validateAmountInput(from, false)
        to!!.setAmount(if (rate != null && input != null) input.multiply(rate) else BigDecimal(0), false)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        isProcessingLinkedAmountInputs = true
        exchangeRateEdit.setBlockWatcher(true)
        super.onRestoreInstanceState(savedInstanceState)
        exchangeRateEdit.setBlockWatcher(false)
        isProcessingLinkedAmountInputs = false
        if (mRowId == 0L/* && mTemplateId == 0L*/) {
            configureTransferDirection()
        }
    }

    private fun configureTransferDirection() {
        if (isIncome && mOperationType == Transactions.TYPE_TRANSFER) {
            switchAccountViews()
        }
    }

    fun clearMethodSelection(view: View?) {
        mMethodId = null
        setMethodSelection()
    }

    fun clearCategorySelection(view: View?) {
        mCatId = null
        mLabel = null
        setCategoryButton()
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
        private const val INPUT_EXCHANGE_RATE = 1
        private const val INPUT_AMOUNT = 2
        private const val INPUT_TRANSFER_AMOUNT = 3
        const val ACCOUNTS_CURSOR = 3
        const val TRANSACTION_CURSOR = 5
        const val SUM_CURSOR = 6
        const val AUTOFILL_CURSOR = 8
    }
}