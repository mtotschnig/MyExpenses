package org.totschnig.myexpenses.activity

import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.AmountEdit
import eltos.simpledialogfragment.form.Hint
import eltos.simpledialogfragment.form.SimpleFormDialog
import eltos.simpledialogfragment.form.Spinner
import eltos.simpledialogfragment.input.SimpleInputDialog
import eltos.simpledialogfragment.list.CustomListDialog.SELECTED_SINGLE_ID
import eltos.simpledialogfragment.list.MenuDialog
import icepick.State
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit.Companion.KEY_OCR_RESULT
import org.totschnig.myexpenses.activity.FilterHandler.Companion.FILTER_COMMENT_DIALOG
import org.totschnig.myexpenses.compose.*
import org.totschnig.myexpenses.compose.MenuEntry.Companion.delete
import org.totschnig.myexpenses.compose.MenuEntry.Companion.edit
import org.totschnig.myexpenses.compose.MenuEntry.Companion.select
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.databinding.ActivityMainBinding
import org.totschnig.myexpenses.dialog.*
import org.totschnig.myexpenses.feature.*
import org.totschnig.myexpenses.feature.Payee
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Account.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.model.Sort.Companion.fromCommandId
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enableAutoFill
import org.totschnig.myexpenses.preference.requireString
import org.totschnig.myexpenses.provider.CheckSealedHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.requestSync
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.*
import org.totschnig.myexpenses.util.AppDirHelper.ensureContentUri
import org.totschnig.myexpenses.util.TextUtils.withAmountColor
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.distrib.ReviewManager
import org.totschnig.myexpenses.viewmodel.*
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import timber.log.Timber
import java.io.File
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import javax.inject.Inject
import kotlin.math.sign

const val DIALOG_TAG_OCR_DISAMBIGUATE = "DISAMBIGUATE"
const val DIALOG_TAG_NEW_BALANCE = "NEW_BALANCE"

@OptIn(ExperimentalPagerApi::class)
abstract class BaseMyExpenses : LaunchActivity(), OcrHost, OnDialogResultListener, ContribIFace {
    @JvmField
    @State
    var scanFile: File? = null

    var accountId: Long
        get() = viewModel.selectedAccount.value
        set(value) {
            viewModel.selectedAccount.value = value
            moveToAccount()
        }

    private fun moveToAccount() {
        val position =
            viewModel.accountData.value.indexOfFirst { it.id == accountId }.takeIf { it > -1 } ?: 0
        if (viewModel.pagerState.currentPage != position) {
            lifecycleScope.launch {
                viewModel.pagerState.scrollToPage(position)
            }
        } else {
            setCurrentAccount(position)
        }
    }

    val currentAccount: FullAccount
        get() = viewModel.accountData.value[viewModel.pagerState.currentPage]

    var currentCurrency: String? = null

    private val currentCurrencyUnit: CurrencyUnit?
        get() = currentCurrency?.let { currencyContext.get(it) }

    @Inject
    lateinit var discoveryHelper: IDiscoveryHelper

    @Inject
    lateinit var reviewManager: ReviewManager

    lateinit var toolbar: Toolbar

    private var currentBalance: String? = null

    val viewModel: MyExpensesViewModel by viewModels()
    private val upgradeHandlerViewModel: UpgradeHandlerViewModel by viewModels()
    private val exportViewModel: ExportViewModel by viewModels()

    lateinit var binding: ActivityMainBinding

    val accountCount
        get() = viewModel.accountData.value.count { it.id > 0 }

    private val accountGrouping: MutableState<AccountGrouping> =
        mutableStateOf(AccountGrouping.TYPE)

    lateinit var accountSort: Sort

    private var actionMode: ActionMode? = null

    var selectionState
        get() = viewModel.selectionState.value
        set(value) {
            viewModel.selectionState.value = value
        }

    var sumInfo: SumInfo = SumInfoUnknown
        set(value) {
            field = value
            invalidateOptionsMenu()
        }

    fun finishActionMode() {
        actionMode?.let {
            it.finish()
            viewModel.selectedTransactionSum = 0L
        }
    }

    private val formattedSelectedTransactionSum
        get() = currencyFormatter.convAmount(
            viewModel.selectedTransactionSum,
            currentAccount.currency
        ).withAmountColor(
            resources,
            viewModel.selectedTransactionSum.sign
        )

    private fun updateActionModeTitle() {
        actionMode?.title = if (selectionState.size > 1) {
            android.text.TextUtils.concat(
                selectionState.size.toString(),
                " (Σ: ",
                formattedSelectedTransactionSum,
                ")"
            )
        } else selectionState.size.toString()
    }

    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = startSupportActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(
                    mode: ActionMode,
                    menu: Menu
                ): Boolean {
                    if (!currentAccount.sealed) {
                        menuInflater.inflate(R.menu.transactionlist_context, menu)
                    }
                    return true
                }

                override fun onPrepareActionMode(
                    mode: ActionMode,
                    menu: Menu
                ) = with(menu) {
                    findItem(R.id.REMAP_ACCOUNT_COMMAND).isVisible = accountCount > 1
                    val hasTransfer = selectionState.any { it.isTransfer }
                    val hasSplit = selectionState.any { it.isSplit }
                    val noMethods = currentAccount.type == AccountType.CASH ||
                            (currentAccount.isAggregate && selectionState.any { it.accountType == AccountType.CASH })
                    findItem(R.id.REMAP_PAYEE_COMMAND).isVisible = !hasTransfer
                    findItem(R.id.REMAP_CATEGORY_COMMAND).isVisible = !hasTransfer && !hasSplit
                    findItem(R.id.REMAP_METHOD_COMMAND).isVisible = !hasTransfer && !noMethods
                    true
                }

                override fun onActionItemClicked(
                    mode: ActionMode,
                    item: MenuItem
                ): Boolean {
                    if (remapHandler.handleActionItemClick(item.itemId)) return true
                    when (item.itemId) {
                        R.id.DELETE_COMMAND -> delete(selectionState)
                        R.id.MAP_TAG_COMMAND -> tagHandler.tag()
                        else -> return false
                    }
                    return true
                }

                override fun onDestroyActionMode(mode: ActionMode) {
                    actionMode = null
                    selectionState = emptyList()
                }

            })
        } else actionMode?.invalidate()
    }

    lateinit var remapHandler: RemapHandler
    lateinit var tagHandler: TagHandler
    lateinit var filterHandler: FilterHandler

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (savedInstanceState == null) {
            floatingActionButton.let {
                discoveryHelper.discover(
                    this,
                    it,
                    3,
                    DiscoveryHelper.Feature.fab_long_press
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readAccountGroupingFromPref()
        accountSort = readAccountSortFromPref()
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
            inject(upgradeHandlerViewModel)
            inject(exportViewModel)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        toolbar = setupToolbar(false)
        if (savedInstanceState == null) {
            accountId = prefHandler.getLong(PrefKey.CURRENT_ACCOUNT, 0L)
        }
        binding.viewPagerMain.viewPager.setContent {
            val accountData = viewModel.accountData.collectAsState()
            AppTheme(context = this@BaseMyExpenses) {
                LaunchedEffect(viewModel.pagerState.currentPage) {
                    if (setCurrentAccount(viewModel.pagerState.currentPage)) {
                        finishActionMode()
                        sumInfo = SumInfoUnknown
                        viewModel.sumInfo(currentAccount).collect {
                            sumInfo = it
                        }
                    }
                }
                LaunchedEffect(accountData.value) {
                    if (accountData.value.isNotEmpty()) {
                        moveToAccount()
                        viewModel.sumInfo(currentAccount).collect {
                            sumInfo = it
                        }
                    } else {
                        setTitle(R.string.app_name)
                        toolbar.subtitle = null
                    }
                }
                HorizontalPager(
                    verticalAlignment = Alignment.Top,
                    count = accountData.value.count(),
                    state = viewModel.pagerState
                ) { page ->
                    val account = remember { derivedStateOf { accountData.value[page] } }.value
                    val data =
                        remember(account.sortDirection) { viewModel.loadData(account) }
                    val headerData = viewModel.headerData(account)
                    if (page == currentPage) {
                        LaunchedEffect(selectionState.size) {
                            if (selectionState.isNotEmpty()) {
                                startActionMode()
                                updateActionModeTitle()
                            } else {
                                finishActionMode()
                            }
                        }
                    }
                    Column {
                        viewModel.filterPersistence.getValue(account.id)
                            .whereFilterAsFlow
                            .collectAsState(WhereFilter.empty())
                            .value
                            .takeIf { !it.isEmpty }?.let {
                                Row {
                                    androidx.compose.material.Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = stringResource(R.string.menu_search),
                                        tint = Color.Green
                                    )
                                    FlowRow(modifier = Modifier.weight(1f)) {
                                        it.criteria.forEach {
                                            SuggestionChip(
                                                onClick = { },
                                                label = {
                                                    Text(it.prettyPrint(LocalContext.current))
                                                }

                                            )
                                        }
                                    }
                                    androidx.compose.material.Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.clear_all_filters)
                                    )
                                }
                            }
                        TransactionList(
                            pagingSourceFactory = data,
                            headerData = headerData.collectAsState(HeaderData.EMPTY).value,
                            accountId = account.id,
                            selectionHandler = object : SelectionHandler {
                                override fun toggle(transaction: Transaction2) {
                                    if (viewModel.selectionState.toggle(transaction)) {
                                        viewModel.selectedTransactionSum += transaction.amount.amountMinor
                                    } else {
                                        viewModel.selectedTransactionSum -= transaction.amount.amountMinor
                                    }
                                }

                                override fun isSelected(transaction: Transaction2) =
                                    selectionState.contains(transaction)

                                override val selectionCount: Int
                                    get() = selectionState.size

                            },
                            menuGenerator = remember {
                                { transaction ->
                                    Menu(
                                        buildList {
                                            add(MenuEntry(
                                                icon = Icons.Filled.Loupe,
                                                label = R.string.details
                                            ) { showDetails(it.id) })
                                            if (!account.sealed) {
                                                if (transaction.crStatus != CrStatus.VOID) {
                                                    add(edit { edit(transaction) })
                                                }
                                                add(MenuEntry(
                                                    icon = Icons.Filled.ContentCopy,
                                                    label = R.string.menu_clone_transaction
                                                ) {
                                                    edit(transaction, true)
                                                })
                                                add(delete { delete(listOf(transaction)) })
                                                add(MenuEntry(
                                                    icon = myiconpack.IcActionTemplateAdd,
                                                    label = R.string.menu_create_template_from_transaction
                                                ) { createTemplate(transaction) })
                                                if (transaction.crStatus == CrStatus.VOID) {
                                                    add(MenuEntry(
                                                        icon = Icons.Filled.RestoreFromTrash,
                                                        label = R.string.menu_undelete_transaction
                                                    ) { undelete(transaction) })
                                                }
                                                add(
                                                    select {
                                                        viewModel.selectionState.value = listOf(it)
                                                        viewModel.selectedTransactionSum =
                                                            transaction.amount.amountMinor
                                                    })
                                            }
                                        }
                                    )
                                }
                            },
                            onToggleCrStatus = if (account.type == AccountType.CASH) null else {
                                {
                                    checkSealed(listOf(it)) {
                                        viewModel.toggleCrStatus(it)
                                    }
                                }
                            },
                            dateTimeFormatter = dateTimeFormatterFor(account, prefHandler, this@BaseMyExpenses)
                        )
                    }
                }
            }
        }
        setupToolbarPopupMenu()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                upgradeHandlerViewModel.upgradeInfo.collect { info ->
                    info?.let {
                        showDismissibleSnackBar(it, object : Snackbar.Callback() {
                            override fun onDismissed(
                                transientBottomBar: Snackbar,
                                event: Int
                            ) {
                                if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION)
                                    upgradeHandlerViewModel.messageShown()
                            }
                        })
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                exportViewModel.publishProgress.collect { progress ->
                    progress?.let {
                        progressDialogFragment?.appendToMessage(progress)
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                exportViewModel.result.collect { result ->
                    result?.let {
                        progressDialogFragment?.onTaskCompleted()
                        if (result.second.isNotEmpty()) {
                            shareExport(result.first, result.second)
                        }
                        exportViewModel.resultProcessed()
                    }
                }
            }
        }

        if (resources.getDimensionPixelSize(R.dimen.drawerWidth) > resources.displayMetrics.widthPixels) {
            binding.accountPanel.root.layoutParams.width = resources.displayMetrics.widthPixels
        }

        binding.accountPanel.accountList.setContent {
            AppTheme(this) {
                AccountList(
                    accountData = viewModel.accountData.collectAsState(initial = emptyList()).value,
                    grouping = accountGrouping.value,
                    selectedAccount = accountId,
                    onSelected = {
                        lifecycleScope.launch {
                            viewModel.pagerState.scrollToPage(it)
                        }
                        closeDrawer()
                    },
                    onEdit = {
                        closeDrawer()
                        startActivityForResult(Intent(this, AccountEdit::class.java).apply {
                            putExtra(KEY_ROWID, it)
                        }, EDIT_ACCOUNT_REQUEST)
                    },
                    onDelete = {
                        closeDrawer()
                        confirmAccountDelete(it)
                    },
                    onHide = {
                        viewModel.setAccountVisibility(true, it)
                    },
                    onToggleSealed = { id, isSealed ->
                        setAccountSealed(id, isSealed)
                    }
                )
            }
        }
        remapHandler = RemapHandler(this)
        tagHandler = TagHandler(this)
        filterHandler = FilterHandler(this)

        viewModel.cloneAndRemapProgress.observe(
            this
        ) { (first, second): Pair<Int, Int> ->
            val progressDialog =
                supportFragmentManager.findFragmentByTag(PROGRESS_TAG) as? ProgressDialogFragment
            val totalProcessed = first + second
            if (progressDialog != null) {
                if (totalProcessed < progressDialog.max) {
                    progressDialog.setProgress(totalProcessed)
                } else {
                    if (second == 0) {
                        showSnackBar(R.string.clone_and_remap_result)
                    } else {
                        showSnackBar(
                            String.format(
                                Locale.ROOT,
                                "%d out of %d failed",
                                second,
                                totalProcessed
                            )
                        )
                    }
                    supportFragmentManager.beginTransaction().remove(progressDialog).commit()
                }
            }
        }
    }

    fun showDetails(transactionId: Long) {
        lifecycleScope.launchWhenResumed {
            TransactionDetailFragment.show(transactionId, supportFragmentManager)
        }
    }


    private fun undelete(transaction: Transaction2) {
        checkSealed(listOf(transaction.id)) {
            viewModel.undeleteTransactions(transaction.id).observe(this) { result: Int ->
                if (result == 0) showDeleteFailureFeedback(null)
            }
        }
    }

    private fun createTemplate(transaction: Transaction2) {
        checkSealed(listOf(transaction.id)) {
            if (transaction.isSplit && !prefHandler.getBoolean(
                    PrefKey.NEW_SPLIT_TEMPLATE_ENABLED,
                    true
                )
            ) {
                showContribDialog(ContribFeature.SPLIT_TEMPLATE, null)
            } else {
                startActivity(Intent(this, ExpenseEdit::class.java).apply {
                    putExtra(KEY_ROWID, transaction.id)
                    putExtra(ExpenseEdit.KEY_TEMPLATE_FROM_TRANSACTION, true)
                })
            }
        }
    }

    private fun edit(transaction: Transaction2, clone: Boolean = false) {
        val isTransferPartPeer =
            false //TODO DbUtils.getLongOrNull(mTransactionsCursor, KEY_TRANSFER_PEER_PARENT) != null
        checkSealed(listOf(transaction.id)) {
            if (isTransferPartPeer) {
                showSnackBar(R.string.warning_splitpartcategory_context)
            } else {
                val i = Intent(this, ExpenseEdit::class.java)
                i.putExtra(KEY_ROWID, transaction.id)
                if (clone) {
                    i.putExtra(ExpenseEdit.KEY_CLONE, true)
                }
                startActivityForResult(i, EDIT_REQUEST)
            }
        }
    }

    private fun delete(transactions: List<Transaction2>) {
        val hasReconciled = transactions.any { it.crStatus == CrStatus.RECONCILED }
        val hasNotVoid = transactions.any { it.crStatus != CrStatus.VOID }
        val itemIds = transactions.map { it.id }
        checkSealed(itemIds) {
            var message = resources.getQuantityString(
                R.plurals.warning_delete_transaction,
                transactions.size,
                transactions.size
            )
            if (hasReconciled) {
                message += " " + getString(R.string.warning_delete_reconciled)
            }
            val b = Bundle().apply {
                putInt(
                    ConfirmationDialogFragment.KEY_TITLE,
                    R.string.dialog_title_warning_delete_transaction
                )
                putString(ConfirmationDialogFragment.KEY_MESSAGE, message)
                putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.DELETE_COMMAND_DO)
                putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
                    R.id.CANCEL_CALLBACK_COMMAND
                )
                if (hasNotVoid) {
                    putString(
                        ConfirmationDialogFragment.KEY_CHECKBOX_LABEL,
                        getString(R.string.mark_void_instead_of_delete)
                    )
                }
                putLongArray(KEY_ROW_IDS, itemIds.toLongArray())
            }
            showConfirmationDialog(b, "DELETE_TRANSACTION")
        }
    }

    fun showConfirmationDialog(bundle: Bundle?, tag: String) {
        lifecycleScope.launchWhenResumed {
            ConfirmationDialogFragment.newInstance(bundle).show(supportFragmentManager, tag)
        }
    }

    private fun readAccountGroupingFromPref() {
        accountGrouping.value = try {
            AccountGrouping.valueOf(
                prefHandler.requireString(PrefKey.ACCOUNT_GROUPING, AccountGrouping.TYPE.name)
            )
        } catch (e: IllegalArgumentException) {
            AccountGrouping.TYPE
        }
    }

    private fun readAccountSortFromPref() = try {
        Sort.valueOf(
            prefHandler.requireString(PrefKey.SORT_ORDER_ACCOUNTS, Sort.USAGES.name)
        )
    } catch (e: IllegalArgumentException) {
        Sort.USAGES
    }

    fun closeDrawer() {
        binding.drawer?.closeDrawers()
    }

    override fun injectDependencies() {
        (applicationContext as MyApplication).appComponent.inject(this)
    }

    override fun onFeatureAvailable(feature: Feature) {
        if (feature == Feature.OCR) {
            activateOcrMode()
        }
    }

    private fun displayDateCandidate(pair: Pair<LocalDate, LocalTime?>) =
        (pair.second?.let { pair.first.atTime(pair.second) } ?: pair.first).toString()

    override fun processOcrResult(result: kotlin.Result<OcrResult>) {
        result.onSuccess {
            if (it.needsDisambiguation()) {
                SimpleFormDialog.build()
                    .cancelable(false)
                    .autofocus(false)
                    .neg(android.R.string.cancel)
                    .extra(Bundle().apply {
                        putParcelable(KEY_OCR_RESULT, it)
                    })
                    .title(getString(R.string.scan_result_multiple_candidates_dialog_title))
                    .fields(
                        when (it.amountCandidates.size) {
                            0 -> Hint.plain(getString(R.string.scan_result_no_amount))
                            1 -> Hint.plain(
                                "%s: %s".format(
                                    getString(R.string.amount),
                                    it.amountCandidates[0]
                                )
                            )
                            else -> Spinner.plain(KEY_AMOUNT)
                                .placeholder(R.string.amount)
                                .items(*it.amountCandidates.toTypedArray())
                                .preset(0)
                        },
                        when (it.dateCandidates.size) {
                            0 -> Hint.plain(getString(R.string.scan_result_no_date))
                            1 -> Hint.plain(
                                "%s: %s".format(
                                    getString(R.string.date),
                                    displayDateCandidate(it.dateCandidates[0])
                                )
                            )
                            else -> Spinner.plain(KEY_DATE)
                                .placeholder(R.string.date)
                                .items(
                                    *it.dateCandidates.map(this::displayDateCandidate)
                                        .toTypedArray()
                                )
                                .preset(0)
                        },
                        when (it.payeeCandidates.size) {
                            0 -> Hint.plain(getString(R.string.scan_result_no_payee))
                            1 -> Hint.plain(
                                "%s: %s".format(
                                    getString(R.string.payee),
                                    it.payeeCandidates[0].name
                                )
                            )
                            else -> Spinner.plain(KEY_PAYEE_NAME)
                                .placeholder(R.string.payee)
                                .items(*it.payeeCandidates.map(Payee::name).toTypedArray())
                                .preset(0)
                        }
                    )
                    .show(this, DIALOG_TAG_OCR_DISAMBIGUATE)
            } else {
                startEditFromOcrResult(
                    if (it.isEmpty()) {
                        Toast.makeText(
                            this,
                            getString(R.string.scan_result_no_data),
                            Toast.LENGTH_LONG
                        ).show()
                        null
                    } else {
                        it.selectCandidates()
                    }
                )
            }
        }.onFailure {
            Timber.e(it)
            Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * start ExpenseEdit Activity for a new transaction/transfer/split
     * Originally the form for transaction is rendered, user can change from spinner in toolbar
     */
    open fun createRowIntent(type: Int, isIncome: Boolean) =
        Intent(this, ExpenseEdit::class.java).apply {
            putExtra(Transactions.OPERATION_TYPE, type)
            putExtra(ExpenseEdit.KEY_INCOME, isIncome)
            //if we are called from an aggregate account, we also hand over the currency
            if (accountId < 0) {
                putExtra(KEY_CURRENCY, currentAccount.currency.code)
                putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true)
            } else {
                //if accountId is 0 ExpenseEdit will retrieve the first entry from the accounts table
                putExtra(KEY_ACCOUNTID, accountId)
            }
        }

    private fun createRow(type: Int, isIncome: Boolean) {
        if (type == Transactions.TYPE_SPLIT) {
            contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION, null)
        } else {
            createRowDo(type, isIncome)
        }
    }

    fun createRowDo(type: Int, isIncome: Boolean) {
        startEdit(createRowIntent(type, isIncome))
    }

    private fun startEdit(intent: Intent) {
        floatingActionButton.hide()
        startActivityForResult(intent, EDIT_REQUEST)
    }

    private fun startEditFromOcrResult(result: OcrResultFlat?) {
        recordUsage(ContribFeature.OCR)
        startEdit(
            createRowIntent(Transactions.TYPE_TRANSACTION, false).apply {
                putExtra(KEY_OCR_RESULT, result)
                putExtra(KEY_PICTURE_URI, Uri.fromFile(scanFile))
            }
        )
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean =
        if (which == OnDialogResultListener.BUTTON_POSITIVE) {
            when (dialogTag) {
                FILTER_COMMENT_DIALOG -> {
                    extras.getString(SimpleInputDialog.TEXT)?.let {
                        addFilterCriterion(CommentCriterion(it))
                    }
                    true
                }
                DIALOG_TAG_GROUPING ->
                    handleAccountsGrouping(extras.getLong(SELECTED_SINGLE_ID).toInt())
                DIALOG_TAG_SORTING -> handleSortOption(extras.getLong(SELECTED_SINGLE_ID).toInt())
                DIALOG_TAG_OCR_DISAMBIGUATE -> {
                    startEditFromOcrResult(
                        extras.getParcelable<OcrResult>(KEY_OCR_RESULT)!!.selectCandidates(
                            extras.getInt(KEY_AMOUNT),
                            extras.getInt(KEY_DATE),
                            extras.getInt(KEY_PAYEE_NAME)
                        )
                    )
                    true
                }
                DIALOG_TAG_NEW_BALANCE -> {
                    startEdit(
                        createRowIntent(Transactions.TYPE_TRANSACTION, false).apply {
                            putExtra(
                                KEY_AMOUNT,
                                (extras.getSerializable(KEY_AMOUNT) as BigDecimal) -
                                        Money(
                                            currentAccount.currency,
                                            currentAccount.currentBalance
                                        ).amountMajor
                            )
                        }
                    )
                    true
                }
                else -> false
            }
        } else false

    private val shareTarget: String
        get() = prefHandler.requireString(PrefKey.SHARE_TARGET, "").trim { it <= ' ' }

    private fun shareExport(format: ExportFormat, uriList: List<Uri>) {
        shareViewModel.share(
            this, uriList,
            shareTarget,
            "text/" + format.name.lowercase(Locale.US)
        )
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) {
            true
        } else when (command) {
            R.id.GROUPING_ACCOUNTS_COMMAND -> {
                MenuDialog.build()
                    .menu(this, R.menu.accounts_grouping)
                    .choiceIdPreset(accountGrouping.value.commandId.toLong())
                    .title(R.string.menu_grouping)
                    .show(this, DIALOG_TAG_GROUPING)
                true
            }
            R.id.SHARE_PDF_COMMAND -> {
                shareViewModel.share(
                    this, listOf(ensureContentUri(Uri.parse(tag as String?), this)),
                    shareTarget,
                    "application/pdf"
                )
                true
            }
            R.id.OCR_DOWNLOAD_COMMAND -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=org.totschnig.ocr.tesseract")
                }
                packageManager.queryIntentActivities(intent, 0)
                    .find { it.activityInfo.packageName == "org.fdroid.fdroid" }
                    ?.activityInfo?.let {
                        intent.component = ComponentName(it.applicationInfo.packageName, it.name)
                        startActivity(intent)
                    }
                    ?: run {
                        Toast.makeText(this, "F-Droid not installed", Toast.LENGTH_LONG).show()
                    }
                true
            }
            R.id.DELETE_ACCOUNT_COMMAND_DO -> {
                val accountIds = tag as Array<Long>
                if (accountIds.any { it == accountId }) {
                    accountId = 0L
                }
                val manageHiddenFragment =
                    supportFragmentManager.findFragmentByTag(MANAGE_HIDDEN_FRAGMENT_TAG)
                if (manageHiddenFragment != null) {
                    supportFragmentManager.beginTransaction().remove(manageHiddenFragment).commit()
                }
                showSnackBarIndefinite(R.string.progress_dialog_deleting)
                viewModel.deleteAccounts(accountIds).observe(
                    this
                ) { result ->
                    result.onSuccess {
                        showSnackBar(
                            resources.getQuantityString(
                                R.plurals.delete_success,
                                accountIds.size,
                                accountIds.size
                            )
                        )
                    }.onFailure {
                        if (it is AccountSealedException) {
                            showSnackBar(R.string.object_sealed_debt)
                        } else {
                            showDeleteFailureFeedback(null)
                        }
                    }
                }
                true
            }
            R.id.PRINT_COMMAND -> {
                //if (hasItems) { //TODO
                if (true) {
                    AppDirHelper.checkAppDir(this).onSuccess {
                        contribFeatureRequested(ContribFeature.PRINT, null)
                    }.onFailure {
                        showDismissibleSnackBar(it.safeMessage)
                    }
                } else {
                    showExportDisabledCommand()
                }
                true
            }
            R.id.BALANCE_COMMAND -> {
                with(currentAccount) {
                    if (hasCleared) {
                        BalanceDialogFragment.newInstance(Bundle().apply {

                            putLong(KEY_ROWID, id)
                            putString(KEY_LABEL, label)
                            putString(
                                KEY_RECONCILED_TOTAL,
                                currencyFormatter.formatMoney(
                                    Money(currency, reconciledTotal)
                                )
                            )
                            putString(
                                KEY_CLEARED_TOTAL,
                                currencyFormatter.formatMoney(
                                    Money(currency, clearedTotal)
                                )
                            )
                        }).show(supportFragmentManager, "BALANCE_ACCOUNT")
                    } else {
                        showMessage(R.string.dialog_command_disabled_balance)
                    }
                }
                true
            }
            R.id.SYNC_COMMAND -> {
                currentAccount.takeIf { it.syncAccountName != null }?.let {
                    requestSync(accountName = it.syncAccountName!!, uuid = it.uuid)
                }
                true
            }
            else -> false
        }

    fun setupFabSubMenu() {
        floatingActionButton.setOnLongClickListener { fab ->
            discoveryHelper.markDiscovered(DiscoveryHelper.Feature.fab_long_press)
            val popup = PopupMenu(this, fab)
            val popupMenu = popup.menu
            popup.setOnMenuItemClickListener { item ->
                createRow(
                    when (item.itemId) {
                        R.string.split_transaction -> Transactions.TYPE_SPLIT
                        R.string.transfer -> Transactions.TYPE_TRANSFER
                        else -> Transactions.TYPE_TRANSACTION
                    }, item.itemId == R.string.income
                )
                true
            }
            popupMenu.add(Menu.NONE, R.string.expense, Menu.NONE, R.string.expense)
                .setIcon(R.drawable.ic_expense)
            popupMenu.add(Menu.NONE, R.string.income, Menu.NONE, R.string.income).icon =
                AppCompatResources.getDrawable(this, R.drawable.ic_menu_add)?.also {
                    DrawableCompat.setTint(
                        it,
                        ResourcesCompat.getColor(resources, R.color.colorIncome, null)
                    )
                }
            popupMenu.add(Menu.NONE, R.string.transfer, Menu.NONE, R.string.transfer)
                .setIcon(R.drawable.ic_menu_forward)
            popupMenu.add(
                Menu.NONE,
                R.string.split_transaction,
                Menu.NONE,
                R.string.split_transaction
            ).setIcon(R.drawable.ic_menu_split)
            //noinspection RestrictedApi
            (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)
            popup.show()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.expenses, menu)
        menuInflater.inflate(R.menu.grouping, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (viewModel.accountData.value.isNotEmpty()) {
            menu.findItem(R.id.SCAN_MODE_COMMAND)?.let {
                it.isChecked = prefHandler.getBoolean(PrefKey.OCR, false)
            }
            with(currentAccount) {
                menu.findItem(R.id.GROUPING_COMMAND)?.subMenu?.let {
                    Utils.configureGroupingMenu(it, grouping)
                }

                menu.findItem(R.id.SORT_DIRECTION_COMMAND)?.subMenu?.let {
                    Utils.configureSortDirectionMenu(it, sortDirection)
                }

                menu.findItem(R.id.BALANCE_COMMAND)?.let {
                    Utils.menuItemSetEnabledAndVisible(
                        it,
                        type != AccountType.CASH && !sealed
                    )
                }

                menu.findItem(R.id.SYNC_COMMAND)?.let {
                    Utils.menuItemSetEnabledAndVisible(it, syncAccountName != null)
                }
            }
            filterHandler.configureSearchMenu(menu.findItem(R.id.SEARCH_COMMAND))
        } else {
            for (item in listOf(
                R.id.SEARCH_COMMAND, R.id.DISTRIBUTION_COMMAND, R.id.HISTORY_COMMAND,
                R.id.SCAN_MODE_COMMAND, R.id.RESET_COMMAND, R.id.SYNC_COMMAND, R.id.BALANCE_COMMAND,
                R.id.SORT_DIRECTION_COMMAND, R.id.PRINT_COMMAND, R.id.GROUPING_COMMAND
            )) {
                Utils.menuItemSetEnabledAndVisible(menu.findItem(item), false)
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    private fun setupToolbarPopupMenu() {
        toolbar.setOnClickListener {
            if (accountCount > 0) {
                val popup = PopupMenu(this, toolbar)
                val popupMenu = popup.menu
                popupMenu.add(
                    Menu.NONE,
                    R.id.COPY_TO_CLIPBOARD_COMMAND,
                    Menu.NONE,
                    R.string.copy_text
                )
                popupMenu.add(
                    Menu.NONE,
                    R.id.NEW_BALANCE_COMMAND,
                    Menu.NONE,
                    getString(R.string.new_balance)
                )
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.COPY_TO_CLIPBOARD_COMMAND -> copyToClipBoard()
                        R.id.NEW_BALANCE_COMMAND -> if (accountId > 0) {
                            SimpleFormDialog.build().fields(
                                AmountEdit.plain(KEY_AMOUNT).label(R.string.new_balance)
                                    .fractionDigits(currentCurrencyUnit!!.fractionDigits)
                            ).show(this, DIALOG_TAG_NEW_BALANCE)
                        }
                    }
                    true
                }
                popup.show()
            }
        }
    }

    /**
     *  @return true if we have moved to a new account
     */
    private fun setCurrentAccount(position: Int) =
        viewModel.accountData.value.getOrNull(position)?.let { account ->
            val newAccountId = account.id
            val changed = if (accountId != newAccountId) {
                accountId = account.id
                prefHandler.putLong(PrefKey.CURRENT_ACCOUNT, newAccountId)
                true
            } else false
            tintSystemUiAndFab(account.color(resources))
            currentCurrency = account.currency.code
            setBalance(account)
            if (account.sealed) {
                floatingActionButton.hide()
            } else {
                floatingActionButton.show()
            }
            invalidateOptionsMenu()
            changed
        } ?: false

    private fun setBalance(account: FullAccount) {
        val isHome = account.id == HOME_AGGREGATE_ID
        currentBalance = if (isHome) " ≈ " else "" +
                currencyFormatter.formatMoney(Money(account.currency, account.currentBalance))
        title = if (isHome) getString(R.string.grand_total) else account.label
        toolbar.subtitle = currentBalance
        toolbar.setSubtitleTextColor(
            ResourcesCompat.getColor(
                resources,
                if (account.currentBalance < 0) R.color.colorExpense else R.color.colorIncome,
                null
            )
        )
    }

    private fun copyToClipBoard() {
        currentBalance?.let { copyToClipboard(it) }
    }

    fun updateFab() {
        val scanMode = isScanMode()
        configureFloatingActionButton(
            if (scanMode)
                getString(R.string.contrib_feature_ocr_label)
            else
                TextUtils.concatResStrings(
                    this,
                    ". ",
                    R.string.menu_create_transaction,
                    R.string.menu_create_transfer,
                    R.string.menu_create_split
                )
        )
        floatingActionButton.setImageResource(if (scanMode) R.drawable.ic_scan else R.drawable.ic_menu_add_fab)
    }

    fun isScanMode(): Boolean = prefHandler.getBoolean(PrefKey.OCR, false)

    private fun activateOcrMode() {
        prefHandler.putBoolean(PrefKey.OCR, true)
        updateFab()
        invalidateOptionsMenu()
    }

    private fun Intent.fillIntentForGroupingFromTag(tag: Int) {
        val year = (tag / 1000)
        val groupingSecond = (tag % 1000)
        putExtra(KEY_YEAR, year)
        putExtra(KEY_SECOND_GROUP, groupingSecond)
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (feature) {
            ContribFeature.DISTRIBUTION -> {
                recordUsage(feature)
                startActivity(Intent(this, DistributionActivity::class.java).apply {
                    putExtra(KEY_ACCOUNTID, accountId)
                    putExtra(KEY_GROUPING, currentAccount.grouping.name)
                    (tag as? Int)?.let { tag -> fillIntentForGroupingFromTag(tag) }
                })
            }
            ContribFeature.HISTORY -> {
                recordUsage(feature)
                startActivity(Intent(this, HistoryActivity::class.java).apply {
                    putExtra(KEY_ACCOUNTID, accountId)
                    putExtra(KEY_GROUPING, currentAccount.grouping.name)
                })
            }
            ContribFeature.SPLIT_TRANSACTION -> {
                if (tag != null) {
                    val b = Bundle()
                    b.putString(
                        ConfirmationDialogFragment.KEY_MESSAGE,
                        getString(R.string.warning_split_transactions)
                    )
                    b.putInt(
                        ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                        R.id.SPLIT_TRANSACTION_COMMAND
                    )
                    b.putInt(
                        ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
                        R.id.CANCEL_CALLBACK_COMMAND
                    )
                    b.putInt(
                        ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                        R.string.menu_split_transaction
                    )
                    b.putLongArray(TaskExecutionFragment.KEY_LONG_IDS, tag as LongArray?)
                    ConfirmationDialogFragment.newInstance(b)
                        .show(supportFragmentManager, "SPLIT_TRANSACTION")
                } else {
                    createRowDo(Transactions.TYPE_SPLIT, false)
                }
            }
            ContribFeature.PRINT -> {
                val args = Bundle().apply {

                    putParcelableArrayList(
                        KEY_FILTER,
                        //TODO //it.filterCriteria
                        arrayListOf()
                    )
                    putLong(KEY_ROWID, accountId)
                    putLong(KEY_CURRENT_BALANCE, currentAccount.currentBalance)
                }
                if (!supportFragmentManager.isStateSaved) {
                    supportFragmentManager.beginTransaction()
                        .add(
                            TaskExecutionFragment.newInstanceWithBundle(
                                args,
                                TaskExecutionFragment.TASK_PRINT
                            ), ProtectedFragmentActivity.ASYNC_TAG
                        )
                        .add(
                            ProgressDialogFragment.newInstance(
                                getString(
                                    R.string.progress_dialog_printing,
                                    "PDF"
                                )
                            ),
                            ProtectedFragmentActivity.PROGRESS_TAG
                        )
                        .commit()
                }
            }
            ContribFeature.BUDGET -> {
                if (tag != null) {
                    val (budgetId, headerId) = tag as Pair<Long, Int>
                    startActivity(Intent(this, BudgetActivity::class.java).apply {
                        putExtra(KEY_ROWID, budgetId)
                        fillIntentForGroupingFromTag(headerId)
                    })
                } else if (accountId != 0L && currentCurrency != null) {
                    recordUsage(feature)
                    val i = Intent(this, ManageBudgets::class.java)
                    startActivity(i)
                }
            }
            ContribFeature.OCR -> {
                if (featureViewModel.isFeatureAvailable(this, Feature.OCR)) {
                    if ((tag as Boolean)) {
                        /*scanFile = File("/sdcard/OCR_bg.jpg")
                        ocrViewModel.startOcrFeature(scanFile!!, supportFragmentManager);*/
                        ocrViewModel.getScanFiles { pair ->
                            scanFile = pair.second
                            CropImage.activity()
                                .setCameraOnly(true)
                                .setAllowFlipping(false)
                                .setOutputUri(Uri.fromFile(scanFile))
                                .setCaptureImageOutputUri(ocrViewModel.getScanUri(pair.first))
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .start(this)
                        }
                    } else {
                        activateOcrMode()
                    }
                } else {
                    featureViewModel.requestFeature(this, Feature.OCR)
                }
            }
            else -> {}
        }
    }

    private fun confirmAccountDelete(accountId: Long) {
        viewModel.account(accountId, once = true).observe(this) { account ->
            MessageDialogFragment.newInstance(
                resources.getQuantityString(
                    R.plurals.dialog_title_warning_delete_account,
                    1,
                    1
                ),
                getString(
                    R.string.warning_delete_account,
                    account.label
                ) + " " + getString(R.string.continue_confirmation),
                MessageDialogFragment.Button(
                    R.string.menu_delete,
                    R.id.DELETE_ACCOUNT_COMMAND_DO,
                    arrayOf(accountId)
                ),
                null,
                MessageDialogFragment.noButton(), 0
            )
                .show(supportFragmentManager, "DELETE_ACCOUNT")
        }
    }

    private fun setAccountSealed(accountId: Long, isSealed: Boolean) {
        if (isSealed) {
            viewModel.account(accountId, once = true).observe(this) { account ->
                if (account.syncAccountName == null) {
                    viewModel.setSealed(accountId, true)
                } else {
                    showSnackBar(
                        getString(R.string.warning_synced_account_cannot_be_closed),
                        Snackbar.LENGTH_LONG, null, null, binding.accountPanel.accountList
                    )
                }
            }
        } else {
            viewModel.setSealed(accountId, false)
        }
    }

    val navigationView: NavigationView
        get() {
            return binding.accountPanel.expansionContent
        }

    open fun buildCheckSealedHandler() = lazy { CheckSealedHandler(contentResolver) }.value

    fun checkSealed(itemIds: List<Long>, onChecked: Runnable) {
        buildCheckSealedHandler().check(itemIds) { result ->
            lifecycleScope.launchWhenResumed {
                result.onSuccess {
                    if (it.first && it.second) {
                        onChecked.run()
                    } else {
                        warnSealedAccount(!it.first, !it.second, itemIds.size > 1)
                    }
                }.onFailure {
                    showSnackBar(it.safeMessage)
                }
            }
        }
    }

    private fun warnSealedAccount(sealedAccount: Boolean, sealedDebt: Boolean, multiple: Boolean) {
        val resIds = mutableListOf<Int>()
        if (multiple) {
            resIds.add(R.string.warning_account_for_transaction_is_closed)
        }
        if (sealedAccount) {
            resIds.add(R.string.object_sealed)
        }
        if (sealedDebt) {
            resIds.add(R.string.object_sealed_debt)
        }
        showSnackBar(TextUtils.concatResStrings(this, " ", *resIds.toIntArray()))
    }

    fun doReset() {
        //TODO
        //if (hasItems) {
        if (true) {
            exportViewModel.checkAppDir().observe(this) { result ->
                result.onSuccess {
                    with(currentAccount) {
                        exportViewModel.hasExported(accountId)
                            .observe(this@BaseMyExpenses) { hasExported ->
                                ExportDialogFragment.newInstance(
                                    ExportDialogFragment.AccountInfo(
                                        id,
                                        label,
                                        currency.code,
                                        sealed,
                                        hasExported,
                                        false // TODO fragment.isFiltered
                                    )
                                ).show(supportFragmentManager, "EXPORT")
                            }
                    }
                }.onFailure {
                    showDismissibleSnackBar(it.safeMessage)
                }
            }
        } else {
            showExportDisabledCommand()
        }
    }

    fun showExportDisabledCommand() {
        showMessage(R.string.dialog_command_disabled_reset_account)
    }

    /**
     * check if this is the first invocation of a new version
     * in which case help dialog is presented
     * also is used for hooking version specific upgrade procedures
     * and display information to be presented upon app launch
     */
    fun newVersionCheck() {
        val prevVersion = prefHandler.getInt(PrefKey.CURRENT_VERSION, -1)
        val currentVersion = DistributionHelper.versionNumber
        if (prevVersion < currentVersion) {
            if (prevVersion == -1) {
                return
            }
            upgradeHandlerViewModel.upgrade(prevVersion, currentVersion)
            val showImportantUpgradeInfo = ArrayList<Int>()
            prefHandler.putInt(PrefKey.CURRENT_VERSION, currentVersion)
            if (prevVersion < 19) {
                prefHandler.putString(PrefKey.SHARE_TARGET, prefHandler.getString("ftp_target", ""))
                prefHandler.remove("ftp_target")
            }
            if (prevVersion < 28) {
                Timber.i(
                    "Upgrading to version 28: Purging %d transactions from database",
                    contentResolver.delete(
                        TransactionProvider.TRANSACTIONS_URI,
                        "$KEY_ACCOUNTID not in (SELECT _id FROM accounts)", null
                    )
                )
            }
            if (prevVersion < 30) {
                if ("" != prefHandler.getString(PrefKey.SHARE_TARGET, "")) {
                    prefHandler.putBoolean(PrefKey.SHARE_TARGET, true)
                }
            }
            if (prevVersion < 40) {
                //this no longer works since we migrated time to utc format
                //  DbUtils.fixDateValues(getContentResolver());
                //we do not want to show both reminder dialogs too quickly one after the other for upgrading users
                //if they are already above both thresholds, so we set some delay
                prefHandler.putLong("nextReminderContrib", Transaction.getSequenceCount() + 23)
            }
            if (prevVersion < 163) {
                prefHandler.remove("qif_export_file_encoding")
            }
            if (prevVersion < 199) {
                //filter serialization format has changed
                val edit = settings.edit()
                for (entry in settings.all.entries) {
                    val key = entry.key
                    val keyParts =
                        key.split(("_").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (keyParts[0] == "filter") {
                        val `val` = settings.getString(key, "")!!
                        when (keyParts[1]) {
                            "method", "payee", "cat" -> {
                                val sepIndex = `val`.indexOf(";")
                                edit.putString(
                                    key,
                                    `val`.substring(sepIndex + 1) + ";" + Criterion.escapeSeparator(
                                        `val`.substring(0, sepIndex)
                                    )
                                )
                            }
                            "cr" -> edit.putString(
                                key,
                                CrStatus.values()[Integer.parseInt(`val`)].name
                            )
                        }
                    }
                }
                edit.apply()
            }
            if (prevVersion < 202) {
                val appDir = prefHandler.getString(PrefKey.APP_DIR, null)
                if (appDir != null) {
                    prefHandler.putString(PrefKey.APP_DIR, Uri.fromFile(File(appDir)).toString())
                }
            }
            if (prevVersion < 221) {
                prefHandler.putString(
                    PrefKey.SORT_ORDER_LEGACY,
                    if (prefHandler.getBoolean(PrefKey.CATEGORIES_SORT_BY_USAGES_LEGACY, true))
                        "USAGES"
                    else
                        "ALPHABETIC"
                )
            }
            if (prevVersion < 303) {
                if (prefHandler.getBoolean(PrefKey.AUTO_FILL_LEGACY, false)) {
                    enableAutoFill(prefHandler)
                }
                prefHandler.remove(PrefKey.AUTO_FILL_LEGACY)
            }
            if (prevVersion < 316) {
                prefHandler.putString(PrefKey.HOME_CURRENCY, Utils.getHomeCurrency().code)
                invalidateHomeCurrency()
            }
            if (prevVersion < 354 && GenericAccountService.getAccounts(this).isNotEmpty()) {
                showImportantUpgradeInfo.add(R.string.upgrade_information_cloud_sync_storage_format)
            }

            showVersionDialog(prevVersion, showImportantUpgradeInfo)
        } else {
            if ((!licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION) && !prefHandler.getBoolean(
                    PrefKey.SYNC_UPSELL_NOTIFICATION_SHOWN,
                    false
                ))
            ) {
                prefHandler.putBoolean(PrefKey.SYNC_UPSELL_NOTIFICATION_SHOWN, true)
                ContribUtils.showContribNotification(this, ContribFeature.SYNCHRONIZATION)
            }
        }
        checkCalendarPermission()
    }

    private fun checkCalendarPermission() {
        if ("-1" != prefHandler.getString(PrefKey.PLANNER_CALENDAR_ID, "-1")) {
            if (!PermissionHelper.PermissionGroup.CALENDAR.hasPermission(this)) {
                requestPermission(PermissionHelper.PermissionGroup.CALENDAR)
            }
        }
    }

    fun balance(accountId: Long, reset: Boolean) {
        viewModel.balanceAccount(accountId, reset).observe(
            this
        ) { result ->
            result.onFailure {
                showSnackBar(it.safeMessage)
            }
        }
    }

    fun startExport(args: Bundle) {
        //TODO
        /*args.putParcelableArrayList(
            KEY_FILTER,
            currentFragment!!.filterCriteria
        )*/
        supportFragmentManager.beginTransaction()
            .add(
                ProgressDialogFragment.newInstance(
                    getString(R.string.pref_category_title_export),
                    null,
                    ProgressDialog.STYLE_SPINNER,
                    true
                ), ProtectedFragmentActivity.PROGRESS_TAG
            )
            .commitNow()
        exportViewModel.startExport(args)
    }

    private fun handleAccountsGrouping(itemId: Int): Boolean {
        val newGrouping: AccountGrouping? = when (itemId) {
            R.id.GROUPING_ACCOUNTS_CURRENCY_COMMAND -> AccountGrouping.CURRENCY
            R.id.GROUPING_ACCOUNTS_TYPE_COMMAND -> AccountGrouping.TYPE
            R.id.GROUPING_ACCOUNTS_NONE_COMMAND -> AccountGrouping.NONE
            else -> null
        }
        return if (newGrouping != null && newGrouping != accountGrouping.value) {
            accountGrouping.value = newGrouping
            prefHandler.putString(PrefKey.ACCOUNT_GROUPING, newGrouping.name)
            viewModel.triggerAccountListRefresh()
            true
        } else false
    }

    private fun handleSortOption(itemId: Int): Boolean {
        val newSort = fromCommandId(itemId)
        var result = false
        if (newSort != null) {
            if (newSort != accountSort) {
                accountSort = newSort
                prefHandler.putString(PrefKey.SORT_ORDER_ACCOUNTS, newSort.name)
            }
            viewModel.triggerAccountListRefresh()
            result = true
            if (itemId == R.id.SORT_CUSTOM_COMMAND) {
                SortUtilityDialogFragment.newInstance(
                    ArrayList(viewModel.accountData.value
                        .filter { it.id > 0 }
                        .map { AbstractMap.SimpleEntry(it.id, it.label) }
                    ))
                    .show(supportFragmentManager, "SORT_ACCOUNTS")
            }
        }
        return result
    }

    fun addFilterCriterion(c: Criterion<*>) {
        invalidateOptionsMenu()
        viewModel.addFilterCriteria(c, currentAccount.id)
    }

    fun removeFilter(id: Int) = if (viewModel.removeFilter(id, currentAccount.id)) {
        invalidateOptionsMenu()
        true
    } else false

    companion object {
        const val MANAGE_HIDDEN_FRAGMENT_TAG = "MANAGE_HIDDEN"
        const val DIALOG_TAG_GROUPING = "GROUPING"
        const val DIALOG_TAG_SORTING = "SORTING"
    }
}