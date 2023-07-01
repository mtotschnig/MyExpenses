package org.totschnig.myexpenses.activity

import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.*
import eltos.simpledialogfragment.input.SimpleInputDialog
import eltos.simpledialogfragment.list.CustomListDialog.SELECTED_SINGLE_ID
import eltos.simpledialogfragment.list.MenuDialog
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Sort.Companion.fromCommandId
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.CheckSealedHandler
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.isAggregate
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionDatabase.SQLiteDowngradeFailedException
import org.totschnig.myexpenses.provider.TransactionDatabase.SQLiteUpgradeFailedException
import org.totschnig.myexpenses.provider.filter.*
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
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel.DeleteState.DeleteComplete
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel.DeleteState.DeleteProgress
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import timber.log.Timber
import java.io.Serializable
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import javax.inject.Inject
import kotlin.Result
import kotlin.math.sign

const val DIALOG_TAG_OCR_DISAMBIGUATE = "DISAMBIGUATE"
const val DIALOG_TAG_NEW_BALANCE = "NEW_BALANCE"

@OptIn(ExperimentalFoundationApi::class)
abstract class BaseMyExpenses : LaunchActivity(), OcrHost, OnDialogResultListener, ContribIFace {

    private val accountData: List<FullAccount>
        get() = viewModel.accountData.value?.getOrNull() ?: emptyList()

    var selectedAccountId: Long
        get() = viewModel.accountData.value?.getOrNull()?.getOrNull(pagerState.currentPage)?.id ?: 0
        set(value) {
            deferredAccountId = if (value != 0L) {
                value
            } else {
                viewModel.accountData.value?.getOrNull()?.firstOrNull()?.id
            }
        }

    private val accountForNewTransaction: FullAccount?
        get() = currentAccount?.let { current ->
            current.takeIf { !it.isAggregate } ?: viewModel.accountData.value?.getOrNull()
                ?.filter { !it.isAggregate && (current.isHomeAggregate || it.currency == current.currency) }
                ?.maxBy { it.lastUsed }
        }

    lateinit var pagerState: PagerState

    private var deferredAccountId: Long? = null

    val currentFilter: FilterPersistence
        get() = viewModel.filterPersistence.getValue(selectedAccountId)

    val currentAccount: FullAccount?
        get() = accountData.getOrNull(pagerState.currentPage)

    @Inject
    lateinit var discoveryHelper: IDiscoveryHelper

    @Inject
    lateinit var reviewManager: ReviewManager

    @Inject
    lateinit var imageViewIntentProvider: ImageViewIntentProvider

    @Inject
    lateinit var modelClass: Class<out MyExpensesViewModel>

    private var drawerToggle: ActionBarDrawerToggle? = null

    private var currentBalance: String? = null

    lateinit var viewModel: MyExpensesViewModel
    private val upgradeHandlerViewModel: UpgradeHandlerViewModel by viewModels()
    private val exportViewModel: ExportViewModel by viewModels()

    lateinit var binding: ActivityMainBinding

    val accountCount
        get() = accountData.count { it.id > 0 }

    private val accountGrouping: MutableState<AccountGrouping> =
        mutableStateOf(AccountGrouping.TYPE)

    lateinit var accountSort: Sort

    private var actionMode: ActionMode? = null

    var selectionState
        get() = viewModel.selectionState.value
        set(value) {
            viewModel.selectionState.value = value
        }

    private val selectAllState: MutableState<Boolean> = mutableStateOf(false)

    var sumInfo: SumInfo = SumInfoUnknown
        set(value) {
            field = value
            invalidateOptionsMenu()
        }

    fun finishActionMode() {
        actionMode?.finish()
    }

    private val formattedSelectedTransactionSum
        get() = with(viewModel.selectedTransactionSum) {
            currencyFormatter.convAmount(this, currentAccount!!.currencyUnit)
                .withAmountColor(resources, sign)
        }

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

    private fun startMyActionMode() {
        if (actionMode == null) {
            actionMode = startSupportActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(
                    mode: ActionMode,
                    menu: Menu
                ): Boolean {
                    if (!currentAccount!!.sealed) {
                        menuInflater.inflate(R.menu.transactionlist_context, menu)
                        if (resources.getBoolean(R.bool.showTransactionBulkActions)) {
                            listOf(
                                R.id.DELETE_COMMAND,
                                R.id.MAP_TAG_COMMAND,
                                R.id.SPLIT_TRANSACTION_COMMAND,
                                R.id.REMAP_PARENT,
                                R.id.LINK_TRANSFER_COMMAND,
                                R.id.SELECT_ALL_COMMAND,
                                R.id.UNDELETE_COMMAND
                            ).forEach {
                                menu.findItem(it).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                            }
                        }
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
                    val hasVoid = selectionState.any { it.crStatus == CrStatus.VOID }
                    val noMethods = currentAccount!!.type == AccountType.CASH ||
                            (currentAccount!!.isAggregate && selectionState.any { it.accountType == AccountType.CASH })
                    findItem(R.id.REMAP_PAYEE_COMMAND).isVisible = !hasTransfer
                    findItem(R.id.REMAP_CATEGORY_COMMAND).isVisible = !hasTransfer && !hasSplit
                    findItem(R.id.REMAP_METHOD_COMMAND).isVisible = !hasTransfer && !noMethods
                    findItem(R.id.SPLIT_TRANSACTION_COMMAND).isVisible = !hasSplit && !hasVoid
                    findItem(R.id.LINK_TRANSFER_COMMAND).isVisible =
                        selectionState.count() == 2 &&
                                !hasSplit && !hasTransfer && !hasVoid &&
                                viewModel.canLinkSelection()
                    findItem(R.id.UNDELETE_COMMAND).isVisible = hasVoid
                    true
                }

                override fun onActionItemClicked(
                    mode: ActionMode,
                    item: MenuItem
                ): Boolean {
                    if (remapHandler.handleActionItemClick(item.itemId)) return true
                    when (item.itemId) {
                        R.id.DELETE_COMMAND -> delete(selectionState.map { it.id to it.crStatus })
                        R.id.MAP_TAG_COMMAND -> tagHandler.tag()
                        R.id.SPLIT_TRANSACTION_COMMAND -> split(selectionState.map { it.id })
                        R.id.LINK_TRANSFER_COMMAND -> linkTransfer()
                        R.id.SELECT_ALL_COMMAND -> selectAll()
                        R.id.UNDELETE_COMMAND -> undelete(selectionState.map { it.id })
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
        updateActionModeTitle()
    }

    private fun selectAll() {
        selectAllState.value = true
    }

    private fun linkTransfer() {
        val itemIds = selectionState.map { it.id }
        checkSealed(itemIds) {
            showConfirmationDialog(Bundle().apply {
                putString(
                    ConfirmationDialogFragment.KEY_MESSAGE,
                    getString(R.string.warning_link_transfer) + " " + getString(R.string.continue_confirmation)
                )
                putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                    R.id.LINK_TRANSFER_COMMAND
                )
                putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
                    R.id.CANCEL_CALLBACK_COMMAND
                )
                putInt(
                    ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                    R.string.menu_create_transfer
                )
                putLongArray(KEY_ROW_IDS, itemIds.toLongArray())
            }, "LINK_TRANSFER")
        }

    }

    lateinit var remapHandler: RemapHandler
    lateinit var tagHandler: TagHandler
    private lateinit var filterHandler: FilterHandler

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
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle?.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle?.onOptionsItemSelected(item) == true) {
            return true
        }

        if (item.itemId == R.id.SCAN_MODE_COMMAND) {
            toggleScanMode()
            return true
        }
        if (item.itemId == R.id.SHOW_STATUS_HANDLE_COMMAND) {
            currentAccount?.let {
                lifecycleScope.launch {
                    viewModel.persistShowStatusHandle(!item.isChecked)
                    invalidateOptionsMenu()
                }
            }
        }
        return handleGrouping(item) ||
                handleSortDirection(item) ||
                filterHandler.handleFilter(item.itemId) ||
                super.onOptionsItemSelected(item)
    }

    protected open fun handleSortDirection(item: MenuItem) =
        getSortDirectionFromMenuItemId(item.itemId)?.let { newSortDirection ->
            if (!item.isChecked) {
                viewModel.persistSortDirection(selectedAccountId, newSortDirection)
            }
            true
        } ?: false

    private fun handleGrouping(item: MenuItem) =
        Utils.getGroupingFromMenuItemId(item.itemId)?.let { newGrouping ->
            if (!item.isChecked) {
                viewModel.persistGrouping(selectedAccountId, newGrouping)
            }
            true
        } ?: false

    open fun toggleScanMode() {
        val oldMode = prefHandler.getBoolean(PrefKey.OCR, false)
        val newMode = !oldMode
        if (newMode) {
            contribFeatureRequested(ContribFeature.OCR, false)
        } else {
            prefHandler.putBoolean(PrefKey.OCR, false)
            updateFab()
            invalidateOptionsMenu()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLocaleContext()
        maybeRepairRequerySchema()
        readAccountGroupingFromPref()
        pagerState = PagerState(initialPage = savedInstanceState?.getInt(KEY_CURRENT_PAGE) ?: 0)
        accountSort = readAccountSortFromPref()
        viewModel = ViewModelProvider(this)[modelClass]
        with(injector) {
            inject(viewModel)
            inject(upgradeHandlerViewModel)
            inject(exportViewModel)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(false)
        toolbar.isVisible = false
        if (savedInstanceState == null) {
            selectedAccountId = prefHandler.getLong(PrefKey.CURRENT_ACCOUNT, 0L)
        }

        binding.viewPagerMain.viewPager.setContent {
            MainContent()
        }
        setupToolbarPopupMenu()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                upgradeHandlerViewModel.upgradeInfo.collect { info ->
                    info?.let {
                        showDismissibleSnackBar(
                            message = info.info,
                            actionLabel = getString(R.string.dialog_dismiss) +
                                    if (info.count > 1) " (${info.index} / ${info.count})" else "",
                            callback = object : Snackbar.Callback() {
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasHiddenAccounts.collect { result ->
                    navigationView.menu.findItem(R.id.HIDDEN_ACCOUNTS_COMMAND).isVisible =
                        result > 0
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bulkDeleteState.filterNotNull().collect { result ->
                    when (result) {
                        is DeleteProgress -> {
                            showProgressSnackBar(
                                getString(R.string.progress_dialog_deleting),
                                result.total,
                                result.count
                            )
                        }

                        is DeleteComplete -> {
                            showSnackBar(
                                buildList {
                                    if (result.success > 0) {
                                        add(
                                            resources.getQuantityString(
                                                R.plurals.delete_success,
                                                result.success,
                                                result.success
                                            )
                                        )
                                    }
                                    if (result.failure > 0) {
                                        add(deleteFailureMessage(null))
                                    }
                                }.joinToString(" ")
                            )
                            viewModel.bulkDeleteCompleteShown()
                        }
                    }
                }
            }
        }

        if (resources.getDimensionPixelSize(R.dimen.drawerWidth) > resources.displayMetrics.widthPixels) {
            binding.accountPanel.root.layoutParams.width = resources.displayMetrics.widthPixels
        }

        binding.accountPanel.accountList.setContent {
            AppTheme {
                viewModel.accountData.collectAsState().value.let { result ->
                    result?.onSuccess { data ->
                        LaunchedEffect(Unit) {
                            toolbar.isVisible = true
                        }
                        AccountList(
                            accountData = data,
                            grouping = accountGrouping.value,
                            selectedAccount = selectedAccountId,
                            onSelected = {
                                lifecycleScope.launch {
                                    pagerState.scrollToPage(it)
                                }
                                closeDrawer()
                            },
                            onEdit = {
                                closeDrawer()
                                editAccount(it)
                            },
                            onDelete = {
                                closeDrawer()
                                confirmAccountDelete(it)
                            },
                            onHide = {
                                viewModel.setAccountVisibility(true, it)
                            },
                            onToggleSealed = {
                                toggleAccountSealed(it)
                            },
                            onToggleExcludeFromTotals = {
                                toggleExcludeFromTotals(it)
                            },
                            expansionHandlerGroups = viewModel.expansionHandler("collapsedHeadersDrawer_${accountGrouping.value}"),
                            expansionHandlerAccounts = viewModel.expansionHandler("collapsedAccounts")
                        )
                    }?.onFailure {
                        val (message, forceQuit) = when (it) {
                            is SQLiteDowngradeFailedException -> "Database cannot be downgraded from a newer version. Please either uninstall MyExpenses, before reinstalling, or upgrade to a new version." to true
                            is SQLiteUpgradeFailedException -> "Database upgrade failed. Please contact support@myexpenses.mobi !" to true
                            else -> "Data loading failed" to false
                        }
                        showMessage(
                            message,
                            if (!forceQuit) {
                                MessageDialogFragment.Button(
                                    R.string.safe_mode,
                                    R.id.SAFE_MODE_COMMAND,
                                    null
                                )
                            } else null,
                            null,
                            MessageDialogFragment.Button(
                                R.string.button_label_close,
                                R.id.QUIT_COMMAND,
                                null
                            ),
                            false
                        )
                    }
                }

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
        if (resources.getInteger(R.integer.window_size_class) == 1) {
            toolbar.setNavigationIcon(R.drawable.ic_menu)
            binding.accountPanel.root.isVisible =
                prefHandler.getBoolean(PrefKey.ACCOUNT_PANEL_VISIBLE, false)
            toolbar.setNavigationOnClickListener {
                val newState = !binding.accountPanel.root.isVisible
                prefHandler.putBoolean(PrefKey.ACCOUNT_PANEL_VISIBLE, newState)
                binding.accountPanel.root.isVisible = newState
            }
        }
        binding.drawer?.let { drawer ->
            drawerToggle = object : ActionBarDrawerToggle(
                this, drawer,
                toolbar, R.string.drawer_open, R.string.drawer_close
            ) {
                //at the moment we finish action if drawer is opened;
                // and do NOT open it again when drawer is closed
                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    finishActionMode()
                }

                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    super.onDrawerSlide(drawerView, 0f) // this disables the animation
                }
            }.also {
                drawer.addDrawerListener(it)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_PAGE, pagerState.currentPage)
    }

    private fun editAccount(accountId: Long) {
        startActivityForResult(Intent(this, AccountEdit::class.java).apply {
            putExtra(KEY_ROWID, accountId)
        }, EDIT_ACCOUNT_REQUEST)

    }

    private suspend fun deferredLoad() {
        deferredAccountId?.let {
            viewModel.accountData.value!!.getOrThrow().indexOfFirst { it.id == deferredAccountId }
                .takeIf { it != -1 }.let {
                    pagerState.scrollToPage(it ?: 0)
                }
            deferredAccountId = null
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun MainContent() {

        LaunchedEffect(pagerState.currentPage) {
            setCurrentAccount()?.let { account ->
                finishActionMode()
                sumInfo = SumInfoUnknown
                viewModel.sumInfo(account.toPageAccount).collect {
                    sumInfo = it
                }
            }
        }
        val result = viewModel.accountData.collectAsState()

        if (result.value?.isSuccess == true) {
            val accountData = result.value!!.getOrThrow()
            AppTheme {
                LaunchedEffect(accountData) {
                    if (accountData.isNotEmpty()) {
                        currentAccount?.let {
                            lifecycleScope.launch {
                                viewModel.sumInfo(it.toPageAccount).collect {
                                    sumInfo = it
                                }
                            }
                        }
                        deferredLoad()
                        setCurrentAccount()
                    } else {
                        setTitle(R.string.app_name)
                        toolbar.subtitle = null
                    }
                }
                if (accountData.isNotEmpty()) {
                    HorizontalPager(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onSurface)
                            .testTag(TEST_TAG_PAGER)
                            .semantics {
                                collectionInfo = CollectionInfo(1, accountData.size)
                            },
                        verticalAlignment = Alignment.Top,
                        pageCount = accountData.count(),
                        state = pagerState,
                        pageSpacing = 10.dp,
                        key = { accountData[it].id }
                    ) {
                        Timber.i("Rendering page $it")
                        Page(account = accountData[it].toPageAccount)
                    }
                }
            }
        }
    }

    @Composable
    fun Page(account: PageAccount) {
        LaunchedEffect(key1 = account.sealed) {
            if (account.sealed) finishActionMode()
        }

        val showStatusHandle = if (account.type == AccountType.CASH)
            false
        else
            viewModel.showStatusHandle().collectAsState(initial = true).value

        val onToggleCrStatus: ((Long) -> Unit)? = if (showStatusHandle) {
            {
                checkSealed(listOf(it)) {
                    viewModel.toggleCrStatus(it)
                }
            }
        } else null

        val headerData = remember(account) { viewModel.headerData(account) }
        LaunchedEffect(selectionState.size) {
            if (selectionState.isNotEmpty()) {
                startMyActionMode()
            } else {
                finishActionMode()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            viewModel.filterPersistence.getValue(account.id)
                .whereFilterAsFlow
                .collectAsState(WhereFilter.empty())
                .value
                .takeIf { !it.isEmpty }?.let {
                    FilterCard(it, ::clearFilter)
                }
            headerData.collectAsState(null).value?.let { headerData ->
                val withCategoryIcon =
                    viewModel.withCategoryIcon.collectAsState(initial = true).value
                val lazyPagingItems =
                    viewModel.items.getValue(account).collectAsLazyPagingItems()
                if (!account.sealed) {
                    LaunchedEffect(selectAllState.value) {
                        if (selectAllState.value) {
                            if (lazyPagingItems.loadState.prepend.endOfPaginationReached &&
                                lazyPagingItems.loadState.append.endOfPaginationReached
                            ) {
                                var jndex = 0
                                while (jndex < lazyPagingItems.itemCount) {
                                    lazyPagingItems.peek(jndex)?.let {
                                        viewModel.selectionHandler.select(it)
                                    }
                                    jndex++
                                }
                            } else {
                                showSnackBar(
                                    getString(
                                        R.string.select_all_list_too_large,
                                        getString(android.R.string.selectAll)
                                    )
                                )
                            }
                            selectAllState.value = false
                        }
                    }
                }
                val bulkDeleteState = viewModel.bulkDeleteState.collectAsState(initial = null)
                val modificationAllowed =
                    !account.sealed && bulkDeleteState.value !is DeleteProgress
                TransactionList(
                    modifier = Modifier.weight(1f),
                    lazyPagingItems = lazyPagingItems,
                    headerData = headerData,
                    budgetData = viewModel.budgetData(account).collectAsState(null),
                    selectionHandler = if (modificationAllowed) viewModel.selectionHandler else null,
                    menuGenerator = remember(modificationAllowed) {
                        { transaction ->
                            Menu(
                                buildList {
                                    add(MenuEntry(
                                        icon = Icons.Filled.Loupe,
                                        label = R.string.details,
                                        command = "DETAILS"
                                    ) { showDetails(transaction.id) })
                                    transaction.pictureUri?.let { uri ->
                                        add(MenuEntry(
                                            icon = Icons.Filled.Attachment,
                                            label = R.string.menu_view_picture,
                                            command = "VIEW_PICTURE"
                                        ) {
                                            imageViewIntentProvider.startViewIntent(
                                                this@BaseMyExpenses,
                                                uri
                                            )
                                        })
                                    }
                                    if (modificationAllowed) {
                                        add(MenuEntry(
                                            icon = Icons.Filled.ContentCopy,
                                            label = R.string.menu_clone_transaction,
                                            command = "CLONE"
                                        ) {
                                            edit(transaction, true)
                                        })
                                        add(MenuEntry(
                                            icon = myiconpack.IcActionTemplateAdd,
                                            label = R.string.menu_create_template_from_transaction,
                                            command = "CREATE_TEMPLATE_FROM_TRANACTION"
                                        ) { createTemplate(transaction) })
                                        if (transaction.crStatus == CrStatus.VOID) {
                                            add(MenuEntry(
                                                icon = Icons.Filled.RestoreFromTrash,
                                                label = R.string.menu_undelete_transaction,
                                                command = "UNDELETE_TRANSACTION"
                                            ) {
                                                undelete(listOf(transaction.id))
                                            })
                                        }
                                        if (transaction.crStatus != CrStatus.VOID) {
                                            add(edit("EDIT_TRANSACTION") {
                                                edit(transaction)
                                            })
                                        }
                                        add(delete("DELETE_TRANSACTION") {
                                            delete(listOf(transaction.id to transaction.crStatus))
                                        })
                                        add(
                                            select("SELECT_TRANSACTION") {
                                                viewModel.selectionState.value = listOf(
                                                    MyExpensesViewModel.SelectionInfo(transaction)
                                                )
                                            }
                                        )
                                        if (transaction.isSplit) {
                                            add(MenuEntry(
                                                icon = Icons.Filled.CallSplit,
                                                label = R.string.menu_ungroup_split_transaction,
                                                command = "UNGROUP_SPLIT"
                                            ) {
                                                ungroupSplit(transaction)
                                            })
                                        }
                                    }
                                }
                            )
                        }
                    },
                    futureCriterion = viewModel.futureCriterion.collectAsState(initial = FutureCriterion.EndOfDay).value,
                    expansionHandler = viewModel.expansionHandlerForTransactionGroups(account),
                    onBudgetClick = { budgetId, headerId ->
                        contribFeatureRequested(ContribFeature.BUDGET, budgetId to headerId)
                    },
                    showSumDetails = viewModel.showSumDetails.collectAsState(initial = true).value,
                    scrollToCurrentDate = viewModel.scrollToCurrentDate.getValue(account.id),
                    renderer = when (viewModel.renderer.collectAsState(initial = RenderType.New).value) {
                        RenderType.New -> {
                            NewTransactionRenderer(
                                dateTimeFormatter(account, prefHandler, this@BaseMyExpenses),
                                withCategoryIcon,
                                onToggleCrStatus
                            )
                        }

                        RenderType.Legacy -> {
                            CompactTransactionRenderer(
                                dateTimeFormatterLegacy(
                                    account,
                                    prefHandler,
                                    this@BaseMyExpenses
                                )?.let {
                                    Pair(
                                        (it.first as SimpleDateFormat).asDateTimeFormatter,
                                        with(LocalDensity.current) {
                                            LocalTextStyle.current.fontSize.toDp()
                                        } * it.second
                                    )
                                },
                                withCategoryIcon,
                                onToggleCrStatus
                            )
                        }
                    }
                )
            }
        }
    }

    private fun undelete(itemIds: List<Long>) {
        checkSealed(itemIds) {
            viewModel.undeleteTransactions(itemIds).observe(this) { result: Int ->
                finishActionMode()
                showSnackBar("${getString(R.string.menu_undelete_transaction)}: $result")
            }
        }
    }

    private fun ungroupSplit(transaction: Transaction2) {
        val b = Bundle()
        b.putString(
            ConfirmationDialogFragment.KEY_MESSAGE,
            getString(R.string.warning_ungroup_split_transactions)
        )
        b.putInt(
            ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
            R.id.UNGROUP_SPLIT_COMMAND
        )
        b.putInt(
            ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
            R.id.CANCEL_CALLBACK_COMMAND
        )
        b.putInt(
            ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
            R.string.menu_ungroup_split_transaction
        )
        b.putLong(KEY_ROWID, transaction.id)
        showConfirmationDialog(b, "UNSPLIT_TRANSACTION")

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
        checkSealed(listOf(transaction.id)) {
            if (transaction.transferPeerParent != null) {
                showSnackBar(R.string.warning_splitpartcategory_context)
            } else {
                startActivityForResult(
                    Intent(this, ExpenseEdit::class.java).apply {
                        putExtra(KEY_ROWID, transaction.id)
                        putExtra(KEY_COLOR, transaction.color)
                        if (clone) {
                            putExtra(ExpenseEdit.KEY_CLONE, true)
                        }

                    }, EDIT_REQUEST
                )
            }
        }
    }

    private fun split(itemIds: List<Long>) {
        checkSealed(itemIds) {
            contribFeatureRequested(
                ContribFeature.SPLIT_TRANSACTION,
                itemIds.toLongArray()
            )
        }

    }

    private fun delete(transactions: List<Pair<Long, CrStatus>>) {
        val hasReconciled = transactions.any { it.second == CrStatus.RECONCILED }
        val hasNotVoid = transactions.any { it.second != CrStatus.VOID }
        val itemIds = transactions.map { it.first }
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
                putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.menu_delete)
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

    fun showConfirmationDialog(bundle: Bundle, tag: String) {
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

    private fun closeDrawer() {
        binding.drawer?.closeDrawers()
    }

    override fun injectDependencies() {
        injector.inject(this)
    }

    override fun onFeatureAvailable(feature: Feature) {
        super.onFeatureAvailable(feature)
        if (feature == Feature.OCR) {
            activateOcrMode()
        }
    }

    private fun displayDateCandidate(pair: Pair<LocalDate, LocalTime?>) =
        (pair.second?.let { pair.first.atTime(pair.second) } ?: pair.first).toString()

    override fun processOcrResult(result: Result<OcrResult>, scanUri: Uri) {
        result.onSuccess {
            if (it.needsDisambiguation()) {
                SimpleFormDialog.build()
                    .cancelable(false)
                    .autofocus(false)
                    .neg(android.R.string.cancel)
                    .extra(Bundle().apply {
                        putParcelable(KEY_OCR_RESULT, it)
                        putParcelable(KEY_PICTURE_URI, scanUri)
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
                    },
                    scanUri
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
        accountForNewTransaction?.let {
            Intent(this, ExpenseEdit::class.java).apply {
                putExtra(Transactions.OPERATION_TYPE, type)
                putExtra(ExpenseEdit.KEY_INCOME, isIncome)
                putExtra(KEY_ACCOUNTID, it.id)
                putExtra(KEY_CURRENCY, it.currency)
                putExtra(KEY_COLOR, it._color)
                val accountId = selectedAccountId
                if (isAggregate(accountId)) {
                    putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true)
                }
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
        createRowIntent(type, isIncome)?.let { startEdit(it) }
    }

    private fun startEdit(intent: Intent) {
        floatingActionButton.hide()
        startActivityForResult(intent, EDIT_REQUEST)
    }

    private fun startEditFromOcrResult(result: OcrResultFlat?, scanUri: Uri) {
        recordUsage(ContribFeature.OCR)
        createRowIntent(Transactions.TYPE_TRANSACTION, false)?.apply {
            putExtra(KEY_OCR_RESULT, result)
            putExtra(KEY_PICTURE_URI, scanUri)
        }?.let {
            startEdit(it)
        }
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
                        ),
                        extras.getParcelable(KEY_PICTURE_URI)!!
                    )
                    true
                }

                DIALOG_TAG_NEW_BALANCE -> {
                    createRowIntent(Transactions.TYPE_TRANSACTION, false)?.apply {
                        putExtra(
                            KEY_AMOUNT,
                            (extras.getSerializable(KEY_AMOUNT) as BigDecimal) -
                                    Money(
                                        currentAccount!!.currencyUnit,
                                        currentAccount!!.currentBalance
                                    ).amountMajor
                        )
                    }?.let {
                        startEdit(it)
                    }
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

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        } else when (command) {
            R.id.CREATE_ACCOUNT_COMMAND -> {
                if (licenceHandler.hasAccessTo(ContribFeature.ACCOUNTS_UNLIMITED)
                    || accountCount + viewModel.hasHiddenAccounts.value < ContribFeature.FREE_ACCOUNTS
                ) {
                    closeDrawer()
                    startActivityForResult(Intent(this, AccountEdit::class.java).apply {
                        if (tag != null) putExtra(KEY_CURRENCY, tag as String?)
                    }, CREATE_ACCOUNT_REQUEST)
                } else {
                    showContribDialog(ContribFeature.ACCOUNTS_UNLIMITED, null)
                }
            }

            R.id.SAFE_MODE_COMMAND -> {
                prefHandler.putBoolean(PrefKey.DB_SAFE_MODE, true)
                viewModel.triggerAccountListRefresh()
            }

            R.id.CLEAR_FILTER_COMMAND -> {
                currentFilter.clear()
                invalidateOptionsMenu()
            }

            R.id.HISTORY_COMMAND -> {
                if ((sumInfo as? SumInfoLoaded)?.hasItems == true) {
                    contribFeatureRequested(ContribFeature.HISTORY, null)
                } else {
                    showSnackBar(R.string.no_expenses)
                }
            }

            R.id.DISTRIBUTION_COMMAND -> {
                if ((sumInfo as? SumInfoLoaded)?.mappedCategories == true) {
                    contribFeatureRequested(ContribFeature.DISTRIBUTION, null)
                } else {
                    showSnackBar(R.string.dialog_command_disabled_distribution)
                }
            }

            R.id.GROUPING_ACCOUNTS_COMMAND -> {
                MenuDialog.build()
                    .menu(this, R.menu.accounts_grouping)
                    .choiceIdPreset(accountGrouping.value.commandId.toLong())
                    .title(R.string.menu_grouping)
                    .show(this, DIALOG_TAG_GROUPING)
            }

            R.id.SHARE_PDF_COMMAND -> {
                shareViewModel.share(
                    this, listOf(ensureContentUri(Uri.parse(tag as String?), this)),
                    shareTarget,
                    "application/pdf"
                )
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
            }

            R.id.DELETE_ACCOUNT_COMMAND_DO -> {
                val accountIds = tag as LongArray
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
                        if (accountIds.any { it == selectedAccountId }) {
                            selectedAccountId = 0
                        }

                    }.onFailure {
                        if (it is AccountSealedException) {
                            showSnackBar(R.string.object_sealed_debt)
                        } else {
                            showDeleteFailureFeedback(null)
                        }
                    }
                }
            }

            R.id.PRINT_COMMAND -> {
                if ((sumInfo as? SumInfoLoaded)?.hasItems == true) {
                    AppDirHelper.checkAppDir(this).onSuccess {
                        contribFeatureRequested(ContribFeature.PRINT, null)
                    }.onFailure {
                        showDismissibleSnackBar(it.safeMessage)
                    }
                } else {
                    showExportDisabledCommand()
                }
            }

            R.id.BALANCE_COMMAND -> {
                with(currentAccount!!) {
                    if (hasCleared) {
                        BalanceDialogFragment.newInstance(Bundle().apply {

                            putLong(KEY_ROWID, id)
                            putString(KEY_LABEL, label)
                            putString(
                                KEY_RECONCILED_TOTAL,
                                currencyFormatter.formatMoney(
                                    Money(currencyUnit, reconciledTotal)
                                )
                            )
                            putString(
                                KEY_CLEARED_TOTAL,
                                currencyFormatter.formatMoney(
                                    Money(currencyUnit, clearedTotal)
                                )
                            )
                        }).show(supportFragmentManager, "BALANCE_ACCOUNT")
                    } else {
                        showSnackBar(R.string.dialog_command_disabled_balance)
                    }
                }
            }

            R.id.SYNC_COMMAND -> currentAccount?.takeIf { it.syncAccountName != null }?.let {
                requestSync(accountName = it.syncAccountName!!, uuid = it.uuid)
            }

            R.id.EDIT_ACCOUNT_COMMAND -> currentAccount?.let { editAccount(it.id) }
            R.id.DELETE_ACCOUNT_COMMAND -> currentAccount?.let { confirmAccountDelete(it) }
            R.id.HIDE_ACCOUNT_COMMAND -> currentAccount?.let {
                viewModel.setAccountVisibility(true, it.id)
            }

            R.id.TOGGLE_SEALED_COMMAND -> currentAccount?.let { toggleAccountSealed(it) }
            R.id.EXCLUDE_FROM_TOTALS_COMMAND -> currentAccount?.let { toggleExcludeFromTotals(it) }
            else -> return false
        }
        return true
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
        if (accountData.isNotEmpty() && currentAccount != null) {
            menu.findItem(R.id.SCAN_MODE_COMMAND)?.let {
                it.isChecked = prefHandler.getBoolean(PrefKey.OCR, false)
            }
            with(currentAccount!!) {
                val reconciliationAvailable = type != AccountType.CASH && !sealed
                val groupingMenu = menu.findItem(R.id.GROUPING_COMMAND)
                val groupingEnabled = sortBy == KEY_DATE
                groupingMenu.setEnabledAndVisible(groupingEnabled)

                if (groupingEnabled) {
                    groupingMenu?.subMenu?.let {
                        Utils.configureGroupingMenu(it, grouping)
                    }
                }

                menu.findItem(R.id.SORT_MENU)?.subMenu?.let {
                    configureSortDirectionMenu(this@BaseMyExpenses, it, sortBy, sortDirection)
                }

                menu.findItem(R.id.BALANCE_COMMAND)
                    ?.setEnabledAndVisible(reconciliationAvailable && !isAggregate)

                menu.findItem(R.id.SHOW_STATUS_HANDLE_COMMAND)?.let {
                    it.setEnabledAndVisible(reconciliationAvailable)
                    if (reconciliationAvailable) {
                        lifecycleScope.launch {
                            it.isChecked = viewModel.showStatusHandle().first()
                        }
                    }
                }

                menu.findItem(R.id.SYNC_COMMAND)?.setEnabledAndVisible(syncAccountName != null)

                menu.findItem(R.id.MANAGE_ACCOUNTS_COMMAND)?.let { item ->
                    item.setEnabledAndVisible(!isAggregate)
                    if (!isAggregate) {
                        with(item) {
                            title = label
                            subMenu?.findItem(R.id.TOGGLE_SEALED_COMMAND)?.setTitle(
                                if (sealed) R.string.menu_reopen else R.string.menu_close
                            )
                            subMenu?.findItem(R.id.EDIT_ACCOUNT_COMMAND)
                                ?.setEnabledAndVisible(!sealed)
                            subMenu?.findItem(R.id.EXCLUDE_FROM_TOTALS_COMMAND)
                                ?.setChecked(excludeFromTotals)
                        }
                    }
                }
            }
            filterHandler.configureSearchMenu(menu.findItem(R.id.SEARCH_COMMAND))
        } else {
            for (item in listOf(
                R.id.SEARCH_COMMAND,
                R.id.DISTRIBUTION_COMMAND,
                R.id.HISTORY_COMMAND,
                R.id.SCAN_MODE_COMMAND,
                R.id.RESET_COMMAND,
                R.id.SYNC_COMMAND,
                R.id.BALANCE_COMMAND,
                R.id.SORT_MENU,
                R.id.PRINT_COMMAND,
                R.id.GROUPING_COMMAND,
                R.id.SHOW_STATUS_HANDLE_COMMAND,
                R.id.MANAGE_ACCOUNTS_COMMAND
            )) {
                menu.findItem(item)?.setEnabledAndVisible(false)
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
                        R.id.NEW_BALANCE_COMMAND -> if (selectedAccountId > 0) {
                            AmountInputHostDialog.build().title(R.string.new_balance)
                                .fields(
                                    AmountInput.plain(KEY_AMOUNT)
                                        .fractionDigits(currentAccount!!.currencyUnit.fractionDigits)
                                        .withTypeSwitch(currentAccount!!.currentBalance > 0)
                                ).show(this, DIALOG_TAG_NEW_BALANCE)
                        }
                    }
                    true
                }
                popup.show()
            }
        }
    }

    override fun onFabClicked() {
        if (accountCount == 0) {
            showSnackBar(R.string.warning_no_account)
        } else {
            if (isScanMode()) {
                contribFeatureRequested(ContribFeature.OCR, true)
            } else {
                createRowDo(Transactions.TYPE_TRANSACTION, false)
            }
        }
    }

    /**
     * adapt UI to currently selected account
     * @return currently selected account
     */
    private fun setCurrentAccount() =
        currentAccount?.also { account ->
            prefHandler.putLong(PrefKey.CURRENT_ACCOUNT, account.id)
            tintSystemUiAndFab(account.color(resources))
            setBalance(account)
            updateFab()
            invalidateOptionsMenu()
        }

    private fun setBalance(account: FullAccount) {
        val isHome = account.id == HOME_AGGREGATE_ID
        currentBalance = (if (isHome) " ≈ " else "") +
                currencyFormatter.formatMoney(Money(account.currencyUnit, account.currentBalance))
        title = if (isHome) getString(R.string.grand_total) else account.label
        toolbar.subtitle = currentBalance
        setSignedToolbarColor(account.currentBalance)
    }

    private fun copyToClipBoard() {
        currentBalance?.let { copyToClipboard(it) }
    }

    fun updateFab() {
        val scanMode = isScanMode()
        val sealed = currentAccount?.sealed == true
        with(floatingActionButton) {
            show()
            isEnabled = !sealed
            alpha = if (sealed) 0.5f else 1f
            setImageResource(
                when {
                    sealed -> R.drawable.ic_lock
                    scanMode -> R.drawable.ic_scan
                    else -> R.drawable.ic_menu_add_fab
                }
            )
            contentDescription = when {
                sealed -> getString(R.string.content_description_closed)
                scanMode -> getString(R.string.contrib_feature_ocr_label)
                else -> TextUtils.concatResStrings(
                    this@BaseMyExpenses,
                    ". ",
                    R.string.menu_create_transaction,
                    R.string.menu_create_transfer,
                    R.string.menu_create_split
                )
            }
        }
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
        currentAccount?.let { currentAccount ->
            when (feature) {
                ContribFeature.DISTRIBUTION -> {
                    recordUsage(feature)
                    startActivity(Intent(this, DistributionActivity::class.java).apply {
                        putExtra(KEY_ACCOUNTID, selectedAccountId)
                        putExtra(KEY_GROUPING, currentAccount.grouping.name)
                    })
                }

                ContribFeature.HISTORY -> {
                    recordUsage(feature)
                    startActivity(Intent(this, HistoryActivity::class.java).apply {
                        putExtra(KEY_ACCOUNTID, selectedAccountId)
                        putExtra(KEY_GROUPING, currentAccount.grouping)
                    })
                }

                ContribFeature.SPLIT_TRANSACTION -> {
                    if (tag != null) {
                        showConfirmationDialog(Bundle().apply {
                            putString(
                                ConfirmationDialogFragment.KEY_MESSAGE,
                                getString(R.string.warning_split_transactions)
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                                R.id.SPLIT_TRANSACTION_COMMAND
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE,
                                R.id.CANCEL_CALLBACK_COMMAND
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                                R.string.menu_split_transaction
                            )
                            putLongArray(KEY_ROW_IDS, tag as LongArray?)
                        }, "SPLIT_TRANSACTION")
                    } else {
                        createRowDo(Transactions.TYPE_SPLIT, false)
                    }
                }

                ContribFeature.PRINT -> {
                    val args = Bundle().apply {
                        addFilter()
                        putLong(KEY_ROWID, selectedAccountId)
                        putLong(KEY_CURRENT_BALANCE, currentAccount.currentBalance)
                    }
                    if (!supportFragmentManager.isStateSaved) {
                        supportFragmentManager.beginTransaction()
                            .add(
                                TaskExecutionFragment.newInstanceWithBundle(
                                    args,
                                    TaskExecutionFragment.TASK_PRINT
                                ), ASYNC_TAG
                            )
                            .add(
                                ProgressDialogFragment.newInstance(
                                    getString(
                                        R.string.progress_dialog_printing,
                                        "PDF"
                                    )
                                ),
                                PROGRESS_TAG
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
                    } else if (selectedAccountId != 0L) {
                        recordUsage(feature)
                        val i = Intent(this, ManageBudgets::class.java)
                        startActivity(i)
                    }
                }

                ContribFeature.OCR -> {
                    if (featureViewModel.isFeatureAvailable(this, Feature.OCR)) {
                        if ((tag as Boolean)) {
                            /*ocrViewModel.startOcrFeature(Uri.fromFile(File("/sdcard/OCR_bg.jpg")), supportFragmentManager);*/
                            startMediaChooserDo("SCAN", true)
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
    }

    private fun confirmAccountDelete(account: FullAccount) {
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
                longArrayOf(account.id)
            ),
            null,
            MessageDialogFragment.noButton(), 0
        )
            .show(supportFragmentManager, "DELETE_ACCOUNT")
    }

    private fun toggleAccountSealed(account: FullAccount) {
        if (account.sealed) {
            viewModel.setSealed(account.id, false)
        } else {
            if (account.syncAccountName == null) {
                viewModel.setSealed(account.id, true)
            } else {
                showSnackBar(
                    getString(R.string.warning_synced_account_cannot_be_closed),
                    Snackbar.LENGTH_LONG, null, null, binding.accountPanel.accountList
                )
            }
        }
    }

    private fun toggleExcludeFromTotals(account: FullAccount) {
        viewModel.setExcludeFromTotals(account.id, !account.excludeFromTotals)
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
        if ((sumInfo as? SumInfoLoaded)?.hasItems == true) {
            exportViewModel.checkAppDir().observe(this) { result ->
                result.onSuccess {
                    currentAccount?.let {
                        with(it) {
                            exportViewModel.hasExported(selectedAccountId)
                                .observe(this@BaseMyExpenses) { hasExported ->
                                    ExportDialogFragment.newInstance(
                                        ExportDialogFragment.AccountInfo(
                                            id,
                                            label,
                                            currency,
                                            sealed,
                                            hasExported,
                                            !currentFilter.whereFilter.isEmpty
                                        )
                                    ).show(supportFragmentManager, "EXPORT")
                                }
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

    private fun showExportDisabledCommand() {
        showSnackBar(R.string.dialog_command_disabled_reset_account)
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
            prefHandler.putInt(PrefKey.CURRENT_VERSION, currentVersion)

            showVersionDialog(prevVersion)
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
            checkPermissionsForPlaner()
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

    private fun Bundle.addFilter() {
        putParcelableArrayList(
            KEY_FILTER,
            ArrayList(currentFilter.whereFilter.criteria)
        )
    }

    fun startExport(args: Bundle) {
        args.addFilter()
        supportFragmentManager.beginTransaction()
            .add(
                ProgressDialogFragment.newInstance(
                    getString(R.string.pref_category_title_export),
                    null,
                    ProgressDialog.STYLE_SPINNER,
                    true
                ), PROGRESS_TAG
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
                    ArrayList(accountData
                        .filter { it.id > 0 }
                        .map { AbstractMap.SimpleEntry(it.id, it.label) }
                    ))
                    .show(supportFragmentManager, "SORT_ACCOUNTS")
            }
        }
        return result
    }

    fun addFilterCriterion(c: Criterion<*>, accountId: Long) {
        invalidateOptionsMenu()
        viewModel.filterPersistence.getValue(accountId).addCriteria(c)
    }

    fun addFilterCriterion(c: Criterion<*>) {
        invalidateOptionsMenu()
        currentFilter.addCriteria(c)
    }

    fun removeFilter(id: Int) = if (currentFilter.removeFilter(id)) {
        invalidateOptionsMenu()
        true
    } else false

    private fun clearFilter() {
        ConfirmationDialogFragment.newInstance(Bundle().apply {
            putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(R.string.clear_all_filters))
            putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.CLEAR_FILTER_COMMAND)
        }).show(supportFragmentManager, "CLEAR_FILTER")
    }

    override fun onPositive(args: Bundle, checked: Boolean) {
        super.onPositive(args, checked)
        when (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
            R.id.DELETE_COMMAND_DO -> {
                finishActionMode()
                viewModel.deleteTransactions(args.getLongArray(KEY_ROW_IDS)!!, checked)
            }

            R.id.BALANCE_COMMAND_DO -> {
                balance(args.getLong(KEY_ROWID), checked)
            }

            R.id.REMAP_COMMAND -> {
                remapHandler.remap(args, checked)
                finishActionMode()
            }

            R.id.SPLIT_TRANSACTION_COMMAND -> {
                finishActionMode()
                val ids = args.getLongArray(KEY_ROW_IDS)!!
                viewModel.split(ids).observe(this) {
                    recordUsage(ContribFeature.SPLIT_TRANSACTION)
                    it.onSuccess {
                        showSnackBar(
                            if (ids.size > 1)
                                getString(R.string.split_transaction_one_success)
                            else
                                getString(R.string.split_transaction_group_success, ids.size)
                        )
                    }.onFailure {
                        showSnackBar(getString(R.string.split_transaction_not_possible))
                    }
                }
            }

            R.id.UNGROUP_SPLIT_COMMAND -> {
                finishActionMode()
                viewModel.revokeSplit(args.getLong(KEY_ROWID)).observe(this) {
                    it.onSuccess {
                        showSnackBar(getString(R.string.ungroup_split_transaction_success))
                    }.onFailure {
                        showSnackBar("ERROR")
                    }
                }
            }

            R.id.LINK_TRANSFER_COMMAND -> {
                finishActionMode()
                viewModel.linkTransfer(args.getLongArray(KEY_ROW_IDS)!!)
            }
        }
    }

    override fun onNegative(args: Bundle) {
        val command = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE)
        if (command != 0) {
            dispatchCommand(command, null)
        }
    }

    companion object {
        const val MANAGE_HIDDEN_FRAGMENT_TAG = "MANAGE_HIDDEN"
        const val DIALOG_TAG_GROUPING = "GROUPING"
        const val DIALOG_TAG_SORTING = "SORTING"
        const val KEY_CURRENT_PAGE = "CURRENT_PAGE"
    }
}