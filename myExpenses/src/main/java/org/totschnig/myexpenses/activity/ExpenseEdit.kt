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
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.provider.CalendarContract
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.MenuRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.loader.app.LoaderManager
import com.evernote.android.state.State
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageCategories.Companion.KEY_PROTECTION_INFO
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.databinding.AttachmentItemBinding
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.db2.asCategoryType
import org.totschnig.myexpenses.delegate.CategoryDelegate
import org.totschnig.myexpenses.delegate.MainDelegate
import org.totschnig.myexpenses.delegate.SplitDelegate
import org.totschnig.myexpenses.delegate.TransactionDelegate
import org.totschnig.myexpenses.delegate.TransferDelegate
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_POSITIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_MESSAGE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_NEGATIVE_BUTTON_LABEL
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_POSITIVE_BUTTON_LABEL
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_TAG_POSITIVE_BUNDLE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.dialog.CriterionInfo
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment
import org.totschnig.myexpenses.dialog.OnCriterionDialogDismissedListener
import org.totschnig.myexpenses.exception.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.exception.UnknownPictureSaveException
import org.totschnig.myexpenses.feature.OcrResultFlat
import org.totschnig.myexpenses.fragment.PartiesList.Companion.DIALOG_EDIT_PARTY
import org.totschnig.myexpenses.fragment.PlanMonthFragment
import org.totschnig.myexpenses.fragment.TemplatesList
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Plan.Recurrence
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.disableAutoFill
import org.totschnig.myexpenses.preference.enableAutoFill
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ContextAwareRecyclerView
import org.totschnig.myexpenses.ui.DateButton
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.checkMenuIcon
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.util.ui.attachmentInfoMap
import org.totschnig.myexpenses.util.ui.setAttachmentInfo
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel.DeleteState.DeleteComplete
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.TagBaseViewModel
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.InstantiationTask
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.InstantiationTask.FROM_INTENT_EXTRAS
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.InstantiationTask.TEMPLATE
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.InstantiationTask.TEMPLATE_FROM_TRANSACTION
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.InstantiationTask.TRANSACTION
import org.totschnig.myexpenses.viewmodel.TransactionEditViewModel.InstantiationTask.TRANSACTION_FROM_TEMPLATE
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.AttachmentInfo
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Tag
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import org.totschnig.myexpenses.viewmodel.data.Template as DataTemplate


const val HELP_VARIANT_TRANSACTION = "transaction"
const val HELP_VARIANT_TRANSFER = "transfer"
const val HELP_VARIANT_SPLIT = "split"
const val HELP_VARIANT_TEMPLATE_CATEGORY = "templateCategory"
const val HELP_VARIANT_TEMPLATE_TRANSFER = "templateTransfer"
const val HELP_VARIANT_TEMPLATE_SPLIT = "templateSplit"
const val HELP_VARIANT_SPLIT_PART_CATEGORY = "splitPartCategory"
const val HELP_VARIANT_SPLIT_PART_TRANSFER = "splitPartTransfer"

/**
 * Activity for editing a transaction
 *
 * @author Michael Totschnig
 */
open class ExpenseEdit : AmountActivity<TransactionEditViewModel>(), ContribIFace,
    ConfirmationDialogListener, ExchangeRateEdit.Host, OnCriterionDialogDismissedListener {
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

    @State
    var parentId = 0L

    val accountId: Long
        get() = currentAccount?.id ?: 0L

    /**
     * transaction, transfer or split
     */
    @State
    var operationType = 0
    private lateinit var mManager: LoaderManager

    @State
    var createNew = false

    @State
    var createTemplate = false

    @State
    var isTemplate = false

    @State
    var shouldShowCreateTemplate = false

    @State
    var areDatesLinked = false

    @State
    var withTypeSpinner = false

    private var mIsResumed = false
    private var accountsLoaded = false
    private var pObserver: ContentObserver? = null
    private lateinit var currencyViewModel: CurrencyViewModel

    private lateinit var attachmentInfoMap: Map<Uri, AttachmentInfo>

    override val date: LocalDate
        get() = dateEditBinding.DateButton.date

    @Inject
    lateinit var viewIntentProvider: ViewIntentProvider

    @Inject
    lateinit var discoveryHelper: IDiscoveryHelper

    lateinit var delegate: TransactionDelegate<*>

    private var menuItem2TemplateMap: MutableMap<Int, DataTemplate> = mutableMapOf()

    private val isSplitParent: Boolean
        get() = operationType == TYPE_SPLIT

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

    val parentPayeeId: Long?
        get() = intent.getLongExtra(KEY_PAYEEID, 0).takeIf { it != 0L }

    @Suppress("UNCHECKED_CAST")
    val parentOriginalAmountExchangeRate: Pair<BigDecimal, Currency>?
        get() = (intent.getSerializableExtra(KEY_PARENT_ORIGINAL_AMOUNT_EXCHANGE_RATE) as? Pair<BigDecimal, Currency>)

    private val isMainTransaction: Boolean
        get() = operationType != TYPE_TRANSFER && !isSplitPartOrTemplate

    private val isClone: Boolean
        get() = intent.getBooleanExtra(KEY_CLONE, false)

    private val withAutoFill: Boolean
        get() = newInstance && !isClone && !hasCreateFromTemplateAction && !hasCreateTemplateFromTransactionAction

    private val hasCreateFromTemplateAction
        get() = intent.action == ACTION_CREATE_FROM_TEMPLATE || planInstanceId > 0L

    private val hasCreateTemplateFromTransactionAction
        get() = intent.action == ACTION_CREATE_TEMPLATE_FROM_TRANSACTION

    private val planInstanceId: Long
        get() = intent.getLongExtra(KEY_INSTANCEID, 0L)

    override fun injectDependencies() {
        injector.inject(this)
    }

    override val fabActionName = "SAVE_TRANSACTION"

    fun updateContentColor(color: Int) {
        this.color = color
        if (!canUseContentColor) {
            tintFab(color)
        }
    }

    private val createAccountForTransfer =
        registerForActivityResult(StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                restartWithType(TYPE_TRANSFER)
            }
        }

    val categorySelectionLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let {
                delegate.setCategory(
                    it.getStringExtra(KEY_LABEL),
                    it.getStringExtra(KEY_ICON),
                    it.getLongExtra(KEY_ROWID, 0),
                    it.getByteExtra(KEY_TYPE, FLAG_NEUTRAL)
                )
                setDirty()
                showCategoryWarning()
            }
        }
    }

    private fun showCategoryWarning() {
        delegate.shouldShowCategoryWarning?.let { type ->
            val prefKey = "category_type_warning_shown"
            if (!prefHandler.getBoolean(prefKey, false)) {
                ConfirmationDialogFragment.newInstance(
                    Bundle().apply {
                        putCharSequence(
                            KEY_MESSAGE,
                            getString(
                                if (type == FLAG_EXPENSE)
                                    R.string.warning_expense_category_credit
                                else
                                    R.string.warning_income_category_debit
                            )
                        )
                        putInt(KEY_COMMAND_POSITIVE, R.id.FAQ_COMMAND)
                        putInt(KEY_POSITIVE_BUTTON_LABEL, R.string.learn_more)
                        putBundle(KEY_TAG_POSITIVE_BUNDLE, Bundle(1).apply {
                            putString(KEY_PATH, "category-types")
                        })
                        putInt(KEY_NEGATIVE_BUTTON_LABEL, R.string.menu_close)
                        putString(ConfirmationDialogFragment.KEY_PREFKEY, prefKey)
                    }
                ).show(supportFragmentManager, "CATEGORY_TYPE")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHelpVariant(HELP_VARIANT_TRANSACTION, false)
        rootBinding = OneExpenseBinding.inflate(LayoutInflater.from(this))
        rootBinding.TagRow.TagLabel.setText(R.string.tags)
        dateEditBinding = DateEditBinding.bind(rootBinding.root)
        methodRowBinding = MethodRowBinding.bind(rootBinding.root)
        setContentView(rootBinding.root)
        floatingActionButton = rootBinding.fab.CREATECOMMAND
        setupToolbarWithClose()
        mManager = LoaderManager.getInstance(this)
        val viewModelProvider = ViewModelProvider(this)
        viewModel = viewModelProvider[TransactionEditViewModel::class.java]
        currencyViewModel = viewModelProvider[CurrencyViewModel::class.java]
        with(injector) {
            inject(viewModel)
            inject(currencyViewModel)
        }
        attachmentInfoMap = attachmentInfoMap(this)

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
            setupObservers(
                if (intent.getBooleanExtra(KEY_IS_MANUAL_RECREATE, false)) {
                    intent.removeExtra(KEY_IS_MANUAL_RECREATE)
                    true
                } else false
            )
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
            updateOnBackPressedCallbackEnabled()
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
                task = if (hasCreateTemplateFromTransactionAction) {
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
                if (isSplitParent) {
                    val (contribFeature, allowed) = if (isNewTemplate) {
                        ContribFeature.SPLIT_TEMPLATE to
                                prefHandler.getBoolean(PrefKey.NEW_SPLIT_TEMPLATE_ENABLED, true)
                    } else {
                        ContribFeature.SPLIT_TRANSACTION to
                                licenceHandler.hasTrialAccessTo(ContribFeature.SPLIT_TRANSACTION)
                    }
                    if (!allowed) {
                        contribFeatureRequested(contribFeature)
                        finish()
                        return
                    }
                }
                parentId = intent.getLongExtra(KEY_PARENTID, 0)
                val currencyUnit = intent.getStringExtra(KEY_CURRENCY)
                    ?.let { currencyContext[it] }

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
                        DiscoveryHelper.Feature.ExpenseIncomeSwitch
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
        rootBinding.TagRow.bindListener()
        rootBinding.newAttachment.setOnClickListener { view ->
            showPicturePopupMenu(view, R.menu.create_attachment_options) { item ->
                val types = prefHandler.requireString(
                    PrefKey.ATTACHMENT_MIME_TYPES,
                    getString(R.string.default_attachment_mime_types)
                ).split(',').map { it.trim() }.toTypedArray()
                when (item.itemId) {
                    R.id.PHOTO_COMMAND -> startMediaChooserDo()
                    R.id.ATTACH_COMMAND -> try {
                        pickAttachment.launch(types)
                    } catch (e: ActivityNotFoundException) {
                        showSnackBar(e.safeMessage)
                    }
                }
                true
            }
        }
    }

    private val pickAttachment: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                setDirty()
                viewModel.addAttachmentUris(uri)
            }
        }

    private fun showAttachments(uris: List<Pair<Uri, AttachmentInfo>>) {
        rootBinding.AttachmentGroup.removeViews(0, rootBinding.AttachmentGroup.childCount - 1)

        uris.forEach { (uri, info) ->
            AttachmentItemBinding.inflate(
                layoutInflater,
                rootBinding.AttachmentGroup,
                false
            ).root.apply {
                contentDescription = info.contentDescription
                rootBinding.AttachmentGroup.addView(
                    this,
                    rootBinding.AttachmentGroup.childCount - 1
                )

                setAttachmentInfo(info)

                setOnClickListener {
                    showPicturePopupMenu(it, R.menu.picture) { item ->
                        when (item.itemId) {
                            R.id.VIEW_COMMAND ->
                                viewIntentProvider.startViewAction(this@ExpenseEdit, uri, info.type)

                            R.id.DELETE_COMMAND -> {
                                setDirty()
                                viewModel.removeAttachmentUri(uri)
                            }
                        }
                        true
                    }
                }
            }
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View,
        menuInfo: ContextMenu.ContextMenuInfo?,
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
        val info = item.menuInfo as? ContextAwareRecyclerView.RecyclerContextMenuInfo
            ?: return super.onContextItemSelected(item)
        return when (item.itemId) {
            R.id.EDIT_COMMAND -> {
                startActivityForResult(Intent(this, ExpenseEdit::class.java).apply {
                    putExtra(if (isTemplate) KEY_TEMPLATEID else KEY_ROWID, info.id)
                    putExtra(KEY_COLOR, color)
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

    private fun setupObservers(isInitialSetup: Boolean) {
        loadAccounts(isInitialSetup)
        loadTemplates()
        linkInputsWithLabels()
        loadTags()
        loadCurrencies()
        observeMoveResult()
        observeAutoFillData()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.attachmentUris
                    .map { list ->
                        list.map { it to attachmentInfoMap.getValue(it) }
                    }
                    .flowOn(Dispatchers.IO)
                    .collect { showAttachments(it) }
            }
        }
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
                        val menuId = View.generateViewId()
                        menuItem2TemplateMap[menuId] = template
                        invalidateOptionsMenu()
                    }
                }
            }
        }
    }

    @VisibleForTesting
    open fun setAccounts(accounts: List<Account>, isInitialSetup: Boolean) {
        if (accounts.isEmpty()) {
            abortWithMessage(getString(R.string.no_accounts))
        } else if (accounts.size == 1 && operationType == TYPE_TRANSFER) {
            abortWithMessage(getString(R.string.dialog_command_disabled_insert_transfer))
        } else {
            if (::delegate.isInitialized) {
                delegate.setAccounts(accounts, !accountsLoaded, isInitialSetup)
                loadDebts()
                if (wasStartedFromWidget && accountsLoaded && prefHandler.getBoolean(
                        PrefKey.UI_HOME_SCREEN_SHORTCUTS_SHOW_NEW_BALANCE,
                        true
                    )
                ) {
                    currentAccount?.let { newData ->
                        Toast.makeText(
                            this,
                            getString(R.string.new_balance) + " : " +
                                    currencyFormatter.formatMoney(
                                        Money(newData.currency, newData.currentBalance)
                                    ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                accountsLoaded = true
                if (mIsResumed) setupListeners()
            }
        }
    }

    private fun loadAccounts(isInitialSetup: Boolean) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.accounts.collect {
                    val firstLoad = !accountsLoaded
                    setAccounts(it, isInitialSetup)
                    if (firstLoad) {
                        collectSplitParts()
                        if (isSplitParent) {
                            viewModel.loadSplitParts(delegate.rowId, isTemplate)
                        }
                    }
                }
            }
        }
    }

    private fun loadCurrencies() {
        lifecycleScope.launchWhenStarted {
            currencyViewModel.currencies.collect { currencies ->
                if (::delegate.isInitialized) {
                    (delegate as? MainDelegate)?.setCurrencies(currencies)
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
            } catch (_: IllegalStateException) { // Do Nothing.  Observer has already been unregistered.
            }
        }
        if (::delegate.isInitialized) delegate.onDestroy()
    }

    private fun populateFromTask(
        transaction: Transaction?,
        task: InstantiationTask,
    ) {
        transaction?.also {
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
        transaction?.also { populate(it, withAutoFill) } ?: run {
            val errMsg = getString(R.string.no_accounts)
            abortWithMessage(errMsg)
            return
        }
        intent.getParcelableExtra<OcrResultFlat>(KEY_OCR_RESULT)?.let { ocrResultFlat ->
            if (ocrResultFlat.isEmpty) {
                showSnackBar(R.string.no_data)
            } else {
                ocrResultFlat.amount?.let { amountInput.setRaw(it) }
                ocrResultFlat.date?.let { pair ->
                    dateEditBinding.DateButton.setDate(pair.first)
                    pair.second?.let { dateEditBinding.TimeButton.setTime(it) }
                }
                ocrResultFlat.payee?.let {
                    rootBinding.Payee.party = DisplayParty(it.id, it.name)
                    startAutoFill(it.id, true)
                }
            }
        }
        IntentCompat.getParcelableExtra(intent, KEY_URI, Uri::class.java)?.let {
            viewModel.addAttachmentUris(it)
        }
        if (!intent.hasExtra(KEY_CACHED_DATA)) {
            delegate.setType(intent.getBooleanExtra(KEY_INCOME, false))
            IntentCompat.getSerializableExtra(intent, KEY_AMOUNT, BigDecimal::class.java)?.let {
                delegate.fillAmount(it)
                (delegate as? TransferDelegate)?.configureTransferDirection()
            }
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
            transaction.party = cached.party
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
            intent.clipData?.let {
                viewModel.addAttachmentUris(
                    *buildList {
                        for (i in 0 until it.itemCount) {
                            add(it.getItemAt(i).uri)
                        }
                    }.toTypedArray()
                )
            }
            setDirty()
        } else {
            intent.getLongExtra(KEY_DATE, 0).takeIf { it != 0L }?.let {
                transaction.date = it
                transaction.valueDate = it
            }
        }
        operationType = transaction.operationType()
        updateOnBackPressedCallbackEnabled()
        delegate = TransactionDelegate.create(
            transaction,
            rootBinding,
            dateEditBinding,
            methodRowBinding,
            injector
        )
        setupObservers(newInstance)
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
            delegate.setAccount(newInstance)
        }
        setHelpVariant(delegate.helpVariant)
        setTitle()
        shouldShowCreateTemplate = transaction.originTemplateId == null
        if (!isTemplate) {
            createNew = newInstance && prefHandler.getBoolean(saveAndNewPrefKey, false)
            configureFloatingActionButton()
        }
        invalidateOptionsMenu()
        cached?.tags?.let {
            viewModel.updateTags(it, true)
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
                (delegate as? MainDelegate)?.loadPrice()
            }
        }
    }

    private fun updateDateLink() {
        dateEditBinding.DateLink.setImageResource(if (areDatesLinked) R.drawable.ic_link else R.drawable.ic_link_off)
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

    private val transferAccount: Account?
        get() = if (::delegate.isInitialized) (delegate as? TransferDelegate)?.transferAccount() else null

    override fun onTypeChanged(isChecked: Boolean) {
        super.onTypeChanged(isChecked)
        if (shouldLoadMethods) {
            loadMethods(currentAccount)
        }
        discoveryHelper.markDiscovered(DiscoveryHelper.Feature.ExpenseIncomeSwitch)
        showCategoryWarning()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (::delegate.isInitialized) {
            menu.findItem(R.id.SAVE_AND_NEW_COMMAND)?.let {
                it.isChecked = createNew
                checkMenuIcon(
                    it,
                   R.drawable.ic_action_save_new
                )
            }
            menu.findItem(R.id.CREATE_TEMPLATE_COMMAND)?.let {
                it.isChecked = createTemplate
                checkMenuIcon(it, R.drawable.ic_action_template_add)
            }
            menu.findItem(R.id.ORIGINAL_AMOUNT_COMMAND)?.let {
                it.isChecked = (delegate as? MainDelegate)?.originalAmountVisible == true
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
            menu.findItem(R.id.CATEGORY_COMMAND)?.let {
                it.isChecked = (delegate as? TransferDelegate)?.categoryVisible == true
            }
        }
        return super.onPrepareOptionsMenu(menu)
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
            menu.add(Menu.NONE, R.id.INVERT_COMMAND, 0, R.string.menu_invert_transfer)
                .setIcon(R.drawable.ic_menu_move)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(Menu.NONE, R.id.CATEGORY_COMMAND, 0, R.string.category).isCheckable = true
        } else {
            if (!isSplitPart) {
                menu.add(
                    Menu.NONE,
                    R.id.ORIGINAL_AMOUNT_COMMAND,
                    0,
                    R.string.menu_original_amount
                ).isCheckable = true
            }
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
                        KEY_MESSAGE,
                        getString(R.string.confirmation_load_template_discard_data)
                    )
                    putInt(KEY_COMMAND_POSITIVE, R.id.LOAD_TEMPLATE_DO)
                    putLong(KEY_ROWID, it.id)
                    ConfirmationDialogFragment.newInstance(this)
                        .show(supportFragmentManager, "CONFIRM_LOAD")
                }
            } else {
                loadTemplate(it.id)
            }
            true
        } == true
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
                configureFloatingActionButton()
                invalidateOptionsMenu()
                return true
            }

            R.id.INVERT_COMMAND -> {
                if (::delegate.isInitialized) {
                    (delegate as? TransferDelegate)?.invert()
                    return true
                }
            }

            R.id.ORIGINAL_AMOUNT_COMMAND -> {
                if (::delegate.isInitialized) {
                    (delegate as? MainDelegate)?.toggleOriginalAmount()
                    invalidateOptionsMenu()
                    return true
                }
            }

            R.id.CATEGORY_COMMAND -> {
                if (::delegate.isInitialized) {
                    (delegate as? TransferDelegate)?.toggleCategory()
                    invalidateOptionsMenu()
                    return true
                }
            }

            R.id.CREATE_ACCOUNT_FOR_TRANSFER_COMMAND -> {
                createAccountForTransfer.launch(createAccountIntent)
            }
        }
        return false
    }

    fun createRow(prefillAmount: BigDecimal?) {
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
            putExtra(
                KEY_PARENT_ORIGINAL_AMOUNT_EXCHANGE_RATE,
                (delegate as? MainDelegate)?.originalAmountExchangeRate
            )
            putExtra(KEY_PAYEEID, (delegate as? MainDelegate)?.payeeId)
            putExtra(KEY_NEW_TEMPLATE, isMainTemplate)
            putExtra(KEY_INCOME, delegate.isIncome)
            putExtra(KEY_COLOR, color)
            prefillAmount?.let { putExtra(KEY_AMOUNT, prefillAmount) }
        }, EDIT_REQUEST)
    }

    /**
     * calls the activity for selecting (and managing) categories
     */
    fun startSelectCategory() {
        categorySelectionLauncher.launch(
            Intent(this, ManageCategories::class.java).apply {
                forwardDataEntryFromWidget(this)
                //we pass the currently selected category in to prevent
                //it from being deleted, which can theoretically lead
                //to crash upon saving https://github.com/mtotschnig/MyExpenses/issues/71
                (delegate as? CategoryDelegate)?.catId?.let<Long, Unit> {
                    putExtra(KEY_PROTECTION_INFO, ManageCategories.ProtectionInfo(it, isTemplate))
                }
                putExtra(KEY_COLOR, color)
                putExtra(
                    KEY_TYPE, if (delegate is TransferDelegate) FLAG_TRANSFER
                    else delegate.isIncome.asCategoryType
                )
            }
        )
    }

    val wasStartedFromWidget: Boolean
        get() = intent.getBooleanExtra(EXTRA_START_FROM_WIDGET, false)

    override fun saveState() {
        if (::delegate.isInitialized) {
            delegate.syncStateAndValidate(true)?.let { transaction ->
                isSaving = true
                if (planInstanceId > 0L) {
                    transaction.originPlanInstanceId = planInstanceId
                }
                viewModel.save(transaction, (delegate as? MainDelegate)?.userSetExchangeRate)
                    .observe(this) {
                        onSaved(it, transaction)
                    }
                if (wasStartedFromWidget) {
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
                            (delegate as? TransferDelegate)?.transferAccountId?.let {
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

    override fun onCropResultOK(result: CropImage.ActivityResult) {
        viewModel.addAttachmentUris(result.uri)
        setDirty()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PLAN_REQUEST -> finish()
            EDIT_REQUEST -> if (resultCode == RESULT_OK) {
                setDirty()
            }
        }
    }

    override val onBackPressedCallbackEnabled: Boolean
        get() = super.onBackPressedCallbackEnabled || isSplitParent || backwardCanceledTagsIntent() != null

    override fun dispatchOnBackPressed() {
        hideKeyboard()
        cleanup {
            backwardCanceledTagsIntent()?.let {
                setResult(RESULT_CANCELED, it)
            }
            doHome()
        }
    }

    private fun cleanup(onComplete: () -> Unit) {
        if (isSplitParent && ::delegate.isInitialized) {
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
            Money(it.currency, validateAmountInput(showToUser = false) ?: BigDecimal.ZERO)
        }

    fun isValidType(type: Int): Boolean {
        return type == TYPE_SPLIT || type == TYPE_TRANSACTION || type == TYPE_TRANSFER
    }

    fun loadMethods(account: Account?) {
        if (account != null) {
            delegate.methodsLoaded = false
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
                delegate.syncStateAndValidate(false)?.let {
                    putExtra(
                        KEY_CACHED_DATA, it.toCached(
                            delegate.recurrenceSpinner.selectedItem as? Recurrence,
                            delegate.planButton.date
                        )
                    )
                }
                putExtra(KEY_CREATE_TEMPLATE, createTemplate)
                val attachments = viewModel.attachmentUris.value
                if (attachments.isNotEmpty()) {
                    clipData = ClipData.newRawUri("Attachments", attachments.first()).apply {
                        if (attachments.size > 1) {
                            attachments.subList(1, attachments.size).forEach {
                                addItem(ClipData.Item(it))
                            }
                        }
                    }
                    flags = FLAG_GRANT_READ_URI_PERMISSION
                }
            }
            finish()
            startActivity(restartIntent)
        }
    }

    private fun doFinish() {
        setResult(RESULT_OK, backwardCanceledTagsIntent())
        finish()
    }

    private fun onSaved(result: Result<Unit>, transaction: ITransaction) {
        result.onSuccess {
            if (isSplitParent) {
                recordUsage(ContribFeature.SPLIT_TRANSACTION)
            }
            if (!isSplitPartOrTemplate) {
                val criterionInfos = listOfNotNull(
                    currentAccount!!.run {
                        val previousAmount = with(delegate) {
                            passedInAmount?.takeIf { passedInAccountId == id } ?: 0
                        }
                        val previousTransferAmount =
                            (delegate as? TransferDelegate)?.run { passedInTransferAmount?.takeIf { passedInTransferAccountId == id } }
                                ?: 0
                        criterion?.let {
                            CriterionInfo(
                                id,
                                currentBalance,
                                criterion,
                                //if we are editing the transaction the difference between the new and the old value define the delta, as long as user did not select a different account
                                transaction.amount.amountMinor - previousAmount - previousTransferAmount,
                                color,
                                currency,
                                label,
                                false
                            )
                        }
                    }?.takeIf { it.hasReached() },
                    transferAccount?.run {
                        val transaction = transaction as Transfer
                        val delegate = delegate as TransferDelegate
                        val previousAmount = with(delegate) {
                            passedInAmount?.takeIf { passedInAccountId == id } ?: 0
                        }
                        val previousTransferAmount =
                            with(delegate) { passedInTransferAmount?.takeIf { passedInTransferAccountId == id } }
                                ?: 0
                        criterion?.let {
                            CriterionInfo(
                                id,
                                currentBalance,
                                criterion,
                                //if we are editing the transaction the difference between the new and the old value define the delta, as long as user did not select a different account
                                transaction.transferAmount!!.amountMinor - previousAmount - previousTransferAmount,
                                color,
                                currency,
                                label,
                                false
                            )
                        }
                    }?.takeIf { it.hasReached() }
                )
                when (criterionInfos.size) {
                    //if a transfer leads to a credit limit and a saving goal being hit at the same time
                    //in two different accounts, we give a priority to the credit limit and show saving goal in toast
                    2 -> criterionInfos.first { it.criterion < 0 }
                    1 -> criterionInfos.first()
                    else -> null
                }?.let {
                    CriterionReachedDialogFragment
                        .newInstance(
                            it,
                            if (criterionInfos.size == 2) with(criterionInfos.first { it.criterion > 0 }) {
                                "$accountLabel: ${getString(dialogTitle)}"
                            } else null
                        )
                        .show(supportFragmentManager, "CRITERION")
                    if (!createNew) return
                }
            }
            if (createNew) {
                if (delegate.prepareForNew()) {
                    newInstance = true
                    clearDirty()
                    showSnackBar(
                        getString(R.string.save_transaction_and_new_success),
                        Snackbar.LENGTH_SHORT
                    )
                } else doFinish()
            } else {
                if (delegate.recurrenceSpinner.selectedItem === Recurrence.CUSTOM) {
                    if (isTemplate) {
                        (transaction as? Template)?.planId
                    } else {
                        transaction.originPlanId
                    }?.let { launchPlanView(true, it) }
                } else { //make sure soft keyboard is closed
                    hideKeyboard()
                    doFinish()
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
                        delegate.resetCategory()
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

    private fun showPicturePopupMenu(
        v: View,
        @MenuRes menuRes: Int,
        listener: OnMenuItemClickListener,
    ) {
        with(PopupMenu(this, v)) {
            setOnMenuItemClickListener(listener)
            inflate(menuRes)
            //noinspection RestrictedApi
            (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
            show()
        }
    }

    override fun contribFeatureRequested(feature: ContribFeature, tag: Serializable?) {
        hideKeyboard()
        super.contribFeatureRequested(feature, tag)
    }

    private fun startMediaChooserDo() {
        startMediaChooserDo(PictureDirHelper.defaultFileName)
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
        updateDateLink()
        if (!isSplitPartOrTemplate) {
            delegate.setCreateTemplate(createTemplate)
        }
    }

    override val fabIcon: Int
        get() = if (createNew && delegate.createNewOverride) R.drawable.ic_action_save_new else super.fabIcon
    override val fabDescription: Int
        get() = if (createNew && delegate.createNewOverride) R.string.menu_save_and_new_content_description else super.fabDescription

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
        val party: DisplayParty?,
        val cachedTemplate: CachedTemplate?,
        val referenceNumber: String?,
        val amount: Money,
        val originalAmount: Money?,
        val equivalentAmount: Money?,
        val recurrence: Recurrence?,
        val tags: List<Tag>?,
    ) : Parcelable

    @Parcelize
    data class CachedTemplate(
        val title: String?,
        val isPlanExecutionAutomatic: Boolean,
        val planExecutionAdvance: Int,
        val date: LocalDate,
    ) : Parcelable

    private fun ITransaction.toCached(withRecurrence: Recurrence?, withPlanDate: LocalDate) =
        CachedTransaction(
            accountId,
            methodId,
            if (this is Template) plan?.dtStart ?: 0L else date,
            valueDate,
            crStatus,
            comment,
            party,
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
            withRecurrence,
            viewModel.tagsLiveData.value
        )

    companion object {
        const val KEY_NEW_TEMPLATE = "newTemplate"
        const val KEY_CLONE = "clone"
        private const val KEY_CACHED_DATA = "cachedData"
        const val KEY_CREATE_TEMPLATE = "createTemplate"
        const val KEY_AUTOFILL_MAY_SET_ACCOUNT = "autoFillMaySetAccount"
        const val KEY_OCR_RESULT = "ocrResult"
        const val KEY_INCOME = "income"
        const val KEY_PARENT_HAS_DEBT = "parentHasDebt"

        /**
         * holds pair of rate and currency
         */
        const val KEY_PARENT_ORIGINAL_AMOUNT_EXCHANGE_RATE = "parentOriginalAmountExchangeRate"

        const val ACTION_CREATE_FROM_TEMPLATE = "CREATE_FROM_TEMPLATE"
        const val ACTION_CREATE_TEMPLATE_FROM_TRANSACTION = "TEMPLATE_FROM_TRANSACTION"
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
            updateOnBackPressedCallbackEnabled()
        }
    }

    override fun onCriterionDialogDismissed() {
        if (!createNew) {
            doFinish()
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle) =
        super.onResult(dialogTag, which, extras) || if (which == BUTTON_POSITIVE) {
            when (dialogTag) {
                DIALOG_EDIT_PARTY -> {
                    val name = extras.getString(KEY_PAYEE_NAME)!!
                    val shortName = extras.getString(KEY_SHORT_NAME)
                    val id = extras.getLong(KEY_ROWID)
                    viewModel.saveParty(
                        id,
                        name,
                        shortName
                    ).observe(this) {
                        if (it) {
                            rootBinding.Payee.party = DisplayParty(id, name, shortName)
                        } else {
                            showSnackBar(
                                getString(
                                    R.string.already_defined,
                                    name
                                )
                            )
                        }
                    }
                    true
                }
                else -> false
            }
        } else false
}