package org.totschnig.myexpenses.activity

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.BundleCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.AmountInput
import eltos.simpledialogfragment.form.AmountInputHostDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.compose.accounts.AccountList
import org.totschnig.myexpenses.compose.accounts.EmptyState
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.databinding.ActivityMainBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_NEGATIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_POSITIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_MESSAGE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_POSITIVE_BUTTON_LABEL
import org.totschnig.myexpenses.dialog.CriterionInfo
import org.totschnig.myexpenses.dialog.CriterionReachedDialogFragment
import org.totschnig.myexpenses.dialog.HelpDialogFragment
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.TransactionListComposeDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.retrofit.Vote
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.ads.AdHandler
import org.totschnig.myexpenses.util.ads.NoOpAdHandler
import org.totschnig.myexpenses.util.checkMenuIcon
import org.totschnig.myexpenses.util.configureSortDirectionMenu
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import org.totschnig.myexpenses.util.distrib.DistributionHelper.isGithub
import org.totschnig.myexpenses.util.distrib.ReviewManager
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.getSortDirectionFromMenuItemId
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.util.ui.DisplayProgress
import org.totschnig.myexpenses.util.ui.displayProgress
import org.totschnig.myexpenses.util.ui.getAmountColor
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import org.totschnig.myexpenses.viewmodel.RoadmapViewModel
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.TransactionListViewModel
import org.totschnig.myexpenses.viewmodel.UpgradeHandlerViewModel
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.repository.RoadmapRepository
import timber.log.Timber
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import javax.inject.Inject
import kotlin.math.sign

const val DIALOG_TAG_OCR_DISAMBIGUATE = "DISAMBIGUATE"
const val DIALOG_TAG_NEW_BALANCE = "NEW_BALANCE"

open class MyExpenses : BaseMyExpenses<MyExpensesViewModel>(), OnDialogResultListener, ContribIFace {

    private lateinit var adHandler: AdHandler

    override val fabActionName = "CREATE_TRANSACTION"

    private val accountData: List<FullAccount>
        get() = viewModel.accountData.value?.getOrNull() ?: emptyList()

    override suspend fun accountForNewTransaction() = currentAccount?.let { current ->
            current.takeIf { !it.isAggregate } ?: viewModel.accountData.value?.getOrNull()
                ?.filter { !it.isAggregate && (current.isHomeAggregate || it.currency == current.currency) }
                ?.maxByOrNull { it.lastUsed }
        }?.let { Optional.of(it) } ?:
            if (getHiddenAccountCount() > 0) Optional.empty() else null

    override val currentAccount: FullAccount?
        get() = accountData.find { it.id == selectedAccountId }

    private val currentPage: Int
        get() = accountData.indexOfFirst { it.id == selectedAccountId }.coerceAtLeast(0)

    @get:Composable
    override val transactionListWindowInsets: WindowInsets
        get() = WindowInsets.navigationBars.add(WindowInsets.displayCutout)

    @Inject
    lateinit var discoveryHelper: IDiscoveryHelper

    @Inject
    lateinit var reviewManager: ReviewManager

    @Inject
    lateinit var modelClass: Class<out MyExpensesViewModel>

    private var drawerToggle: ActionBarDrawerToggle? = null

    private var currentBalance: String = ""

    private val roadmapViewModel: RoadmapViewModel by viewModels()

    lateinit var binding: ActivityMainBinding

    override val accountCount
        get() = accountData.count { it.id > 0 }

    suspend fun getHiddenAccountCount() = withContext(Dispatchers.IO) {
        viewModel.accountFlagsRaw.first().filter { !it.isVisible }.sumOf { it.count ?: 0 }
    }

    private var actionMode: ActionMode? = null

    override fun finishActionMode() {
        actionMode?.finish()
    }

    override fun onWebUiActivated() {
        invalidateOptionsMenu()
    }

    private fun updateActionModeTitle() {
        actionMode?.title = viewModel.actionModeTitle(currencyFormatter, currentAccount!!.currencyUnit, resources)
    }

    private fun startMyActionMode() {
        if (actionMode == null) {
            actionMode = startSupportActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(
                    mode: ActionMode,
                    menu: Menu,
                ): Boolean {
                    return currentAccount?.let { account ->
                        if (!account.sealed) {
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
                                    menu.findItem(it)
                                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                                }
                            }
                        }
                    } != null
                }

                override fun onPrepareActionMode(
                    mode: ActionMode,
                    menu: Menu,
                ) = with(menu) {
                    listOf(
                        R.id.REMAP_ACCOUNT_COMMAND,
                        R.id.REMAP_PAYEE_COMMAND,
                        R.id.REMAP_CATEGORY_COMMAND,
                        R.id.REMAP_METHOD_COMMAND,
                        R.id.SPLIT_TRANSACTION_COMMAND,
                        R.id.LINK_TRANSFER_COMMAND,
                        R.id.UNDELETE_COMMAND
                    ).forEach {
                        findItem(it).isVisible = isContextMenuItemVisible(it)
                    }
                    true
                }

                override fun onActionItemClicked(
                    mode: ActionMode,
                    item: MenuItem,
                ) = onContextItemClicked(item.itemId)

                override fun onDestroyActionMode(mode: ActionMode) {
                    actionMode = null
                    selectionState = emptyList()
                }

            })
        } else actionMode?.invalidate()
        updateActionModeTitle()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (savedInstanceState == null) {
            floatingActionButton.let {
                discoveryHelper.discover(
                    this,
                    it,
                    3,
                    DiscoveryHelper.Feature.FabLongPress
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
        return (drawerToggle?.onOptionsItemSelected(item) == true) || when (item.itemId) {
            R.id.SCAN_MODE_COMMAND -> {
                toggleScanMode()
                true
            }

            R.id.SHOW_STATUS_HANDLE_COMMAND -> {
                currentAccount?.let {
                    lifecycleScope.launch {
                        viewModel.showStatusHandle.set(!item.isChecked)
                        invalidateOptionsMenu()
                    }
                }
                true
            }

            R.id.WEB_UI_COMMAND -> {
                toggleWebUI()
                true
            }

            R.id.SEARCH_COMMAND -> {
                showFilterDialog = true
                true
            }

            else -> handleGrouping(item) ||
                    handleSortDirection(item) ||
                    super.onOptionsItemSelected(item)
        }
    }

    protected open fun handleSortDirection(item: MenuItem) =
        getSortDirectionFromMenuItemId(item.itemId)?.let { (sort, direction) ->
            if (!item.isChecked) {
                viewModel.persistSort(sort, direction)
            }
            true
        } == true

    private fun handleGrouping(item: MenuItem) =
        Utils.getGroupingFromMenuItemId(item.itemId)?.let { newGrouping ->
            if (!item.isChecked) {
                viewModel.persistGrouping(newGrouping)
            }
            true
        } == true

    private fun toggleScanMode() {
        if (isScanMode()) {
            prefHandler.putBoolean(PrefKey.OCR, false)
            updateFab()
            invalidateOptionsMenu()
        } else {
            contribFeatureRequested(ContribFeature.OCR, false)
        }
    }

    private fun toggleWebUI() {
        if (prefHandler.getBoolean(PrefKey.UI_WEB, false)) {
            prefHandler.putBoolean(PrefKey.UI_WEB, false)
        } else {
            contribFeatureRequested(ContribFeature.WEB_UI, false)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        if (key != null && prefHandler.matches(key, PrefKey.UI_WEB)) {
            invalidateOptionsMenu()
        }
    }

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLocaleContext()
        viewModel = ViewModelProvider(this)[modelClass]
        with(injector) {
            inject(viewModel)
            inject(roadmapViewModel)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(false)
        toolbar.isVisible = false

        binding.viewPagerMain.viewPager.setContent {
            val upgradeInfo = upgradeHandlerViewModel.upgradeInfo.collectAsState().value
            if (upgradeInfo == null || upgradeInfo is UpgradeHandlerViewModel.UpgradeSuccess) {
                MainContent()
            }
        }

        setupToolbarClickHandlers()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                upgradeHandlerViewModel.upgradeInfo.collect { info ->
                    when (info) {
                        is UpgradeHandlerViewModel.UpgradeError -> {
                            showMessage(
                                info.info,
                                null,
                                null,
                                MessageDialogFragment.Button(
                                    R.string.button_label_close,
                                    R.id.QUIT_COMMAND,
                                    null
                                ),
                                false
                            )
                        }

                        is UpgradeHandlerViewModel.UpgradeSuccess -> {
                            showDismissibleSnackBar(
                                message = info.info,
                                actionLabel = getString(R.string.dialog_dismiss) +
                                        if (info.count > 1) " (${info.index} / ${info.count})" else "",
                                callback = object : Snackbar.Callback() {
                                    override fun onDismissed(
                                        transientBottomBar: Snackbar,
                                        event: Int,
                                    ) {
                                        if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION)
                                            upgradeHandlerViewModel.messageShown()
                                    }
                                })
                        }

                        else -> {}
                    }
                }
            }
        }

        if (resources.getDimensionPixelSize(R.dimen.drawerWidth) > resources.displayMetrics.widthPixels) {
            binding.accountPanel.root.layoutParams.width = resources.displayMetrics.widthPixels
        }

        binding.accountPanel.accountList.setContent {
            AppTheme {
                viewModel.accountData.collectAsStateWithLifecycle().value.let { result ->
                    result?.onSuccess { data ->
                        val banks = viewModel.banks.collectAsState()
                        LaunchedEffect(Unit) {
                            toolbar.isVisible = true
                        }
                        LaunchedEffect(data) {
                            val selectedIndex = data.indexOfFirst { it.id == selectedAccountId }
                            if (selectedIndex == -1) {
                                selectedAccountId = data.firstOrNull()?.id ?: 0L
                            } else {
                                viewModel.scrollToAccountIfNeeded(
                                    selectedIndex,
                                    selectedAccountId,
                                    false
                                )
                            }
                            navigationView.menu.findItem(R.id.EQUIVALENT_WORTH_COMMAND).isVisible =
                                data.any { it.isHomeAggregate }
                        }

                        val accountGrouping = viewModel.accountGrouping.asState()
                            .value
                            .takeIf { it != AccountGrouping.FLAG }
                            ?: AccountGrouping.DEFAULT

                        AccountList(
                            accountData = data,
                            grouping = accountGrouping,
                            selectedAccount = selectedAccountId,
                            onSelected = {
                                selectedAccountId = it
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
                            onSetFlag = { accountId, flagId ->
                                viewModel.setFlag(
                                    accountId,
                                    flagId
                                )
                            },
                            onToggleSealed = { toggleAccountSealed(it) },
                            onToggleExcludeFromTotals = { toggleExcludeFromTotals(it) },
                            onToggleDynamicExchangeRate = if (viewModel.dynamicExchangeRatesPerAccount.collectAsState(
                                    true
                                ).value
                            ) {
                                { toggleDynamicExchangeRate(it) }
                            } else null,
                            listState = viewModel.listState,
                            showEquivalentWorth = viewModel.showEquivalentWorth.flow
                                .collectAsState(false).value &&
                                    data.any { it.isHomeAggregate },
                            expansionHandlerGroups = viewModel.expansionHandler("collapsedHeadersDrawer_${accountGrouping}"),
                            expansionHandlerAccounts = viewModel.expansionHandler("expandedAccounts"),
                            bankIcon = { modifier, id ->
                                banks.value.find { it.id == id }
                                    ?.let { bank ->
                                        bankingFeature.bankIconRenderer?.invoke(
                                            modifier,
                                            bank
                                        )
                                    }
                            },
                            flags = viewModel.accountFlags.collectAsState(emptyList()).value
                        )
                    }?.onFailure {
                        val (message, forceQuit) = it.processDataLoadingFailure()
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

        if (resources.getInteger(R.integer.window_size_class) == 1) {
            toolbar.setNavigationIcon(R.drawable.ic_menu)
            binding.accountPanel.root.isVisible =
                prefHandler.getBoolean(PrefKey.ACCOUNT_PANEL_VISIBLE, false) ||
                        viewModel.showBalanceSheet
            toolbar.setNavigationOnClickListener {
                val newState = !binding.accountPanel.root.isVisible
                prefHandler.putBoolean(PrefKey.ACCOUNT_PANEL_VISIBLE, newState)
                binding.accountPanel.root.isVisible = newState
                toolbar.setNavigationContentDescription(
                    if (newState) R.string.drawer_close else R.string.drawer_open
                )
                ViewCompat.requestApplyInsets(binding.mainContent)
            }
        }

        floatingActionButton = binding.fab.CREATECOMMAND
        updateFab()
        setupFabSubMenu()

        ViewCompat.setOnApplyWindowInsetsListener(binding.accountPanel.root) { v, insets ->
            val isLtr = v.layoutDirection == View.LAYOUT_DIRECTION_LTR
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val left =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()).left
                val right =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()).right
                leftMargin = when {
                    isLtr -> left
                    else -> 0
                }
                rightMargin = when {
                    !isLtr -> right
                    else -> 0
                }
            }
            //make account list and balance sheet aware of bottom inset
            ViewCompat.dispatchApplyWindowInsets(v.findViewById(R.id.accountList), insets)
            ViewCompat.dispatchApplyWindowInsets(v.findViewById(R.id.balanceSheet), insets)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContent) { v, insets ->
            val accountPanelIsVisible = binding.accountPanel.root.isVisible
            val isLtr = v.layoutDirection == View.LAYOUT_DIRECTION_LTR
            val originalNavigationBarInsets =
                insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val originalDisplayCutoutInsets =
                insets.getInsets(WindowInsetsCompat.Type.displayCutout())

            val systemBarsInsetsForChildren = Insets.of(
                if (accountPanelIsVisible && isLtr) 0 else originalNavigationBarInsets.left,
                originalNavigationBarInsets.top,
                if (accountPanelIsVisible && !isLtr) 0 else originalNavigationBarInsets.right,
                originalNavigationBarInsets.bottom
            )

            val displayCutoutInsetsForChildren: Insets = Insets.of(
                if (accountPanelIsVisible && isLtr) 0 else originalDisplayCutoutInsets.left,
                originalDisplayCutoutInsets.top,
                if (accountPanelIsVisible && !isLtr) 0 else originalDisplayCutoutInsets.right,
                originalDisplayCutoutInsets.bottom
            )

            val insetsForChildren = WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.systemBars(), systemBarsInsetsForChildren)
                .setInsets(
                    WindowInsetsCompat.Type.displayCutout(),
                    displayCutoutInsetsForChildren
                )
                .build()

            insetsForChildren
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar.root) { v, insets ->
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()).left
                rightMargin =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()).right
            }
            WindowInsetsCompat.CONSUMED
        }
        binding.drawer?.let { drawer ->
            drawerToggle = object : ActionBarDrawerToggle(
                this, drawer,
                toolbar, R.string.drawer_open, R.string.drawer_close
            ) {
                //at the moment we finish action if drawer is opened;
                //and do NOT open it again when drawer is closed
                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    onBackPressedCallback.isEnabled = true
                    finishActionMode()
                }

                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    super.onDrawerSlide(drawerView, 0f) // this disables the animation
                }

                override fun onDrawerClosed(drawerView: View) {
                    super.onDrawerClosed(drawerView)
                    onBackPressedCallback.isEnabled = false
                }
            }.also {
                drawer.addDrawerListener(it)
            }
        }

        with(navigationView) {
            setNavigationItemSelectedListener(::handleNavigationClick)
            getChildAt(0)?.isVerticalScrollBarEnabled = false
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.showEquivalentWorth.flow.collect {
                        configureEquivalentWorthMenuItemIcon(
                            menu.findItem(R.id.EQUIVALENT_WORTH_COMMAND),
                            it
                        )
                    }
                }
            }
        }

        onBackPressedCallback = object : OnBackPressedCallback(viewModel.showBalanceSheet) {
            override fun handleOnBackPressed() {
                if (!closeBalanceSheet() &&
                    binding.drawer?.isDrawerOpen(GravityCompat.START) == true
                ) {
                    binding.drawer?.closeDrawer(GravityCompat.START)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        if (viewModel.showBalanceSheet) {
            openBalanceSheet()
        }

        adHandler = adHandlerFactory.create(binding.viewPagerMain.adContainer, this)
        if (adHandler != NoOpAdHandler) {
            binding.viewPagerMain.adContainer.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        binding.viewPagerMain.adContainer.viewTreeObserver
                            .removeOnGlobalLayoutListener(this)
                        adHandler.startBanner()
                    }
                })
            try {
                adHandler.maybeRequestNewInterstitial()
            } catch (e: Exception) {
                report(e)
            }
        }

        if (!isScanMode()) {
            floatingActionButton.visibility = View.INVISIBLE
        }

        reviewManager.init(this)
    }

    override val snackBarContainerId = R.id.main_content

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun MainContent() {

        val selectedAccountIdFromState = viewModel.selectedAccountId.collectAsState().value
        LaunchedEffect(selectedAccountIdFromState) {
            with(currentAccount) {
                configureUiWithCurrentAccount(this, false)
                if (this != null) {
                    finishActionMode()
                    sumInfo.value = SumInfo.EMPTY
                    invalidateOptionsMenu()
                    viewModel.sumInfo(toPageAccount).collect {
                        invalidateOptionsMenu()
                        sumInfo.value = it
                    }
                }
            }
        }
        val result = viewModel.accountData.collectAsState()

        if (result.value?.isSuccess == true) {
            val accountData = result.value!!.getOrThrow()
            AppTheme {
                LaunchedEffect(accountData) {
                    if (accountData.isNotEmpty()) {
                        currentAccount?.let { account ->
                            lifecycleScope.launch {
                                viewModel.sumInfo(account.toPageAccount).collect {
                                    invalidateOptionsMenu()
                                    sumInfo.value = it
                                }
                            }
                        }
                        configureUiWithCurrentAccount(currentAccount, true)
                    } else {
                        setTitle(R.string.app_name)
                        toolbar.subtitle = null
                    }
                }

                if (accountData.isNotEmpty()) {
                    val pagerState = rememberPagerState(
                        accountData.indexOfFirst { selectedAccountId == it.id }
                            .coerceAtLeast(0)
                    ) { accountData.count() }
                    LaunchedEffect(selectedAccountIdFromState) {
                        if (pagerState.currentPage != currentPage) {
                            pagerState.scrollToPage(currentPage)
                        }
                    }
                    LaunchedEffect(pagerState.settledPage) {
                        selectedAccountId = accountData[pagerState.settledPage].id
                        viewModel.scrollToAccountIfNeeded(
                            pagerState.currentPage,
                            selectedAccountId,
                            true
                        )
                    }
                    HorizontalPager(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onSurface)
                            .testTag(TEST_TAG_PAGER)
                            .semantics {
                                collectionInfo = CollectionInfo(1, accountData.size)
                            },
                        verticalAlignment = Alignment.Top,
                        state = pagerState,
                        pageSpacing = 10.dp,
                        key = { accountData[it].id }
                    ) {
                        Timber.i("Rendering page $it")
                        LaunchedEffect(selectionState.size) {
                            if (selectionState.isNotEmpty()) {
                                startMyActionMode()
                            } else {
                                finishActionMode()
                            }
                        }
                        Page(account = accountData[it].toPageAccount, accountCount)
                    }
                } else {
                    EmptyState(::createAccountDo)
                }
            }
        }
    }


    override fun createAccountWithCheck() {
        lifecycleScope.launch {
            if (accountCount + getHiddenAccountCount() < ContribFeature.FREE_ACCOUNTS) {
                createAccountDo()
            } else {
                showContribDialog(ContribFeature.ACCOUNTS_UNLIMITED, null)
            }
        }
    }

    private fun closeDrawer() = binding.drawer?.closeDrawers() != null

    override fun injectDependencies() {
        injector.inject(this)
    }

    override fun onFeatureAvailable(feature: Feature) {
        super.onFeatureAvailable(feature)
        if (feature == Feature.OCR) {
            activateOcrMode()
        }
    }

    override fun startEdit(intent: Intent) {
        floatingActionButton.hide()
        super.startEdit(intent)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean =
        if (super.onResult(dialogTag, which, extras)) true
        else if (which == BUTTON_POSITIVE) {
            when (dialogTag) {

                DIALOG_TAG_NEW_BALANCE -> {
                    lifecycleScope.launch {
                        createRowIntent(Transactions.TYPE_TRANSACTION, false)?.apply {
                            putExtra(
                                KEY_AMOUNT,
                                (BundleCompat.getSerializable(
                                    extras,
                                    KEY_AMOUNT,
                                    BigDecimal::class.java
                                ))!! -
                                        Money(
                                            currentAccount!!.currencyUnit,
                                            currentAccount!!.currentBalance
                                        ).amountMajor
                            )
                        }?.let {
                            startEdit(it)
                        }
                    }
                    true
                }

                else -> false
            }
        } else false


    override fun createAccountDo() {
        closeDrawer()
        super.createAccountDo()
    }

    private fun configureEquivalentWorthMenuItemIcon(menuItem: MenuItem, checked: Boolean) {
        menuItem.isChecked = checked
        menuItem.icon = if (checked) {
            ContextCompat.getDrawable(this, R.drawable.checkbox_checked)
        } else {
            ContextCompat.getDrawable(this, R.drawable.checkbox_not_checked)
        }
    }

    fun handleNavigationClick(item: MenuItem) = when (item.itemId) {
        R.id.EQUIVALENT_WORTH_COMMAND -> {
            lifecycleScope.launch {
                viewModel.showEquivalentWorth.set(!item.isChecked)
            }
            true
        }

        else -> dispatchCommand(item.itemId, null)
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        } else when (command) {
            R.id.MANAGE_ACCOUNT_TYPES_COMMAND -> {
                startActivity(Intent(this, ManageAccountTypes::class.java))
            }

            R.id.ACCOUNT_FLAGS_COMMAND -> {
                startActivity(Intent(this, ManageAccountFlags::class.java))
            }

            R.id.CREATE_ACCOUNT_COMMAND -> {
                createAccount()
            }

            R.id.TOGGLE_SEALED_COMMAND -> currentAccount?.let { toggleAccountSealed(it) }

            R.id.EXCLUDE_FROM_TOTALS_COMMAND -> currentAccount?.let { toggleExcludeFromTotals(it) }

            R.id.DYNAMIC_EXCHANGE_RATE_COMMAND -> currentAccount?.let { toggleDynamicExchangeRate(it) }

            R.id.HELP_COMMAND_DRAWER -> startActivity(Intent(this, Help::class.java).apply {
                putExtra(HelpDialogFragment.KEY_CONTEXT, "NavigationDrawer")
            })

            R.id.SHARE_COMMAND -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                putExtra(
                    Intent.EXTRA_TEXT,
                    Utils.getTellAFriendMessage(this@MyExpenses).toString()
                )
                type = "text/plain"
            }, getResources().getText(R.string.menu_share)))

            R.id.CANCEL_CALLBACK_COMMAND -> finishActionMode()

            R.id.ROADMAP_COMMAND -> startActivity(Intent(this, RoadmapVoteActivity::class.java))

            R.id.BACKUP_COMMAND -> startActivity(
                Intent(
                    this,
                    BackupRestoreActivity::class.java
                ).apply {
                    action = BackupRestoreActivity.ACTION_BACKUP
                })


            R.id.RESTORE_COMMAND -> startActivity(
                Intent(
                    this,
                    BackupRestoreActivity::class.java
                ).apply {
                    action = BackupRestoreActivity.ACTION_RESTORE
                })

            R.id.BALANCE_SHEET_COMMAND -> {
                openBalanceSheet()
            }

            else -> return false
        }
        return true
    }

    private fun toggleAccountSealed(account: FullAccount) {
        toggleAccountSealed(account, binding.accountPanel.root)
    }

    fun openBalanceSheet() {
        viewModel.showBalanceSheet = true
        onBackPressedCallback.isEnabled = true
        val screenWidth = resources.displayMetrics.widthPixels
        val preferredBalanceSheetWidth = resources.getDimensionPixelSize(R.dimen.drawerWidth) * 2
        val veryLarge = screenWidth >= preferredBalanceSheetWidth * 2
        binding.accountPanel.root.layoutParams.width = when {
            binding.drawer != null -> screenWidth
            veryLarge -> preferredBalanceSheetWidth
            else -> ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.accountPanel.root.displayedChild = 1
        binding.accountPanel.balanceSheet.setContent {
            AppTheme {
                val data =
                    viewModel.accountsForBalanceSheet.collectAsState(LocalDate.now() to emptyList()).value
                val paddingValues =
                    WindowInsets.navigationBars
                        .add(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.End)
                        .asPaddingValues()
                BalanceSheetView(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    data.second,
                    viewModel.debtSum.collectAsState(0).value,
                    data.first,
                    onClose = { closeBalanceSheet() },
                    onNavigate = {
                        if (it.isVisible) {
                            selectedAccountId = it.id
                            if (!closeDrawer() && !veryLarge) {
                                closeBalanceSheet()
                            }
                        } else {
                            TransactionListComposeDialogFragment.newInstance(
                                TransactionListViewModel.LoadingInfo(
                                    accountId = it.id,
                                    currency = it.currencyUnit,
                                    label = it.label,
                                    color = it.color,
                                    withTransfers = true,
                                    withNewButton = true
                                )
                            ).show(supportFragmentManager, "HIDDEN_ACCOUNT_LIST")
                        }
                    },
                    onSetDate = {
                        viewModel.setBalanceDate(it)
                    },
                    onPrint = ::printBalanceSheet,
                    showHiddenState = viewModel.balanceSheetShowHidden.asState(),
                    showZeroState = viewModel.balanceSheetShowZero.asState(),
                    showChartState = viewModel.balanceSheetShowChart.asState(),
                )
            }
        }
    }

    fun closeBalanceSheet() = if (binding.accountPanel.root.displayedChild == 1) {
        viewModel.showBalanceSheet = false
        binding.accountPanel.root.layoutParams.width =
            resources.getDimensionPixelSize(R.dimen.drawerWidth)
                .coerceAtMost(resources.displayMetrics.widthPixels)
        binding.accountPanel.root.displayedChild = 0
        if (binding.drawer == null) {
            onBackPressedCallback.isEnabled = false
        }
        true
    } else false

    fun setupFabSubMenu() {
        floatingActionButton.setOnLongClickListener { fab ->
            discoveryHelper.markDiscovered(DiscoveryHelper.Feature.FabLongPress)
            val popup = PopupMenu(this, fab)
            val popupMenu = popup.menu
            popup.setOnMenuItemClickListener { item ->
                trackCommand(item.itemId)
                createRow(
                    type = when (item.itemId) {
                        R.string.split_transaction -> TYPE_SPLIT
                        R.string.transfer -> TYPE_TRANSFER
                        else -> Transactions.TYPE_TRANSACTION
                    },
                    transferEnabled = accountCount > 1,
                    isIncome = item.itemId == R.string.income
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
            @Suppress("UsePropertyAccessSyntax", "RestrictedApi")
            (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)
            popup.show()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        prefHandler.mainMenu.forEach { menuItem ->
            if (menuItem.subMenu != null) {
                val subMenu =
                    menu.addSubMenu(Menu.NONE, menuItem.id, Menu.NONE, menuItem.getLabel(this))
                menuInflater.inflate(menuItem.subMenu, subMenu)
                subMenu.item
            } else {
                menu.add(Menu.NONE, menuItem.id, Menu.NONE, menuItem.getLabel(this))
            }
                .apply {
                    setIcon(menuItem.icon)
                    isCheckable = menuItem.isCheckable
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.WEB_UI_COMMAND)?.let {
            it.isChecked = isWebUiActive()
            checkMenuIcon(it, R.drawable.ic_computer)
        }
        if (accountData.isNotEmpty() && currentAccount != null) {
            menu.findItem(R.id.SCAN_MODE_COMMAND)?.let {
                it.isChecked = isScanMode()
                checkMenuIcon(it, R.drawable.ic_scan)
            }
            with(currentAccount!!) {
                listOf(
                    R.id.DISTRIBUTION_COMMAND,
                    R.id.HISTORY_COMMAND,
                    R.id.RESET_COMMAND,
                    R.id.PRINT_COMMAND,
                    R.id.SYNC_COMMAND,
                    R.id.FINTS_SYNC_COMMAND,
                    R.id.ARCHIVE_COMMAND
                ).forEach {
                    menu.findItem(it)?.setEnabledAndVisible(isMenuItemVisible(it))
                }

                val reconciliationAvailable = type.supportsReconciliation && !sealed
                val groupingMenu = menu.findItem(R.id.GROUPING_COMMAND)
                val groupingEnabled = sortBy == KEY_DATE
                groupingMenu?.setEnabledAndVisible(groupingEnabled)

                if (groupingEnabled) {
                    groupingMenu?.subMenu?.let {
                        Utils.configureGroupingMenu(it, grouping)
                    }
                }

                menu.findItem(R.id.SORT_MENU)?.subMenu?.let {
                    configureSortDirectionMenu(this@MyExpenses, it, sortBy, sortDirection)
                }

                menu.findItem(R.id.SHOW_STATUS_HANDLE_COMMAND)?.apply {
                    setEnabledAndVisible(reconciliationAvailable)
                    if (reconciliationAvailable) {
                        lifecycleScope.launch {
                            isChecked = viewModel.showStatusHandle.flow.first()
                            checkMenuIcon(this@apply, R.drawable.ic_square)
                        }
                    }
                }

                menu.findItem(R.id.FINTS_SYNC_COMMAND)?.apply {
                    title = bankingFeature.syncMenuTitle(this@MyExpenses)
                }
            }
            menu.findItem(R.id.SEARCH_COMMAND)?.let {
                it.setEnabledAndVisible(hasItems)
                it.isChecked = currentFilter.whereFilter.value != null
                checkMenuIcon(it, R.drawable.ic_menu_search)
            }

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
                R.id.FINTS_SYNC_COMMAND
            )) {
                menu.findItem(item)?.setEnabledAndVisible(false)
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    private fun setupToolbarClickHandlers() {
        listOf(binding.toolbar.subtitle, binding.toolbar.title).forEach {
            it.setOnClickListener {
                if (accountCount > 0) {
                    val popup = PopupMenu(this, toolbar)
                    val popupMenu = popup.menu
                    popupMenu.add(
                        Menu.NONE,
                        R.id.COPY_TO_CLIPBOARD_COMMAND,
                        Menu.NONE,
                        R.string.copy_text
                    )
                    if (currentAccount?.isAggregate == false) {
                        popupMenu.add(
                            Menu.NONE,
                            R.id.NEW_BALANCE_COMMAND,
                            Menu.NONE,
                            getString(R.string.new_balance)
                        )
                    }
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.COPY_TO_CLIPBOARD_COMMAND -> copyToClipBoard()
                            R.id.NEW_BALANCE_COMMAND -> if (selectedAccountId > 0) {
                                AmountInputHostDialog.build().title(R.string.new_balance)
                                    .fields(
                                        AmountInput.plain(KEY_AMOUNT)
                                            .label(R.string.new_balance)
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
        listOf(binding.toolbar.donutView, binding.toolbar.progressPercent).forEach {
            it.setOnClickListener {
                currentAccount?.run {
                    criterion?.also {
                        CriterionReachedDialogFragment.newInstance(
                            CriterionInfo(
                                id,
                                currentBalance,
                                criterion,
                                0,
                                color,
                                currencyUnit,
                                label,
                                sealed
                            ), withAnimation = false
                        )
                            .show(supportFragmentManager, "CRITERION")
                    } ?: run {
                        report(Exception("Progress is visible, but no criterion is defined"))
                    }
                }
            }
        }
    }

    override fun onFabClicked() {
        super.onFabClicked()
        if (preCreateRowCheckForSealed()) {
            if (isScanMode()) contribFeatureRequested(ContribFeature.OCR, true)
            else createRowDo(Transactions.TYPE_TRANSACTION, false)
        }
    }

    /**
     * adapt UI to currently selected account
     * @return currently selected account
     */
    private fun configureUiWithCurrentAccount(account: FullAccount?, animateProgress: Boolean) {
        if (account != null) {
            tintFab(account.color)
            setBalance(account, animateProgress)
        }
        updateFab()
        invalidateOptionsMenu()
    }

    private fun setBalance(account: FullAccount, animateProgress: Boolean) {

        val isHome = account.isHomeAggregate
        currentBalance = (if (isHome) " ≈ " else "") +
                currencyFormatter.formatMoney(
                    Money(
                        account.currencyUnit,
                        if (isHome) account.equivalentCurrentBalance else account.currentBalance
                    )
                )
        binding.toolbar.title.text =
            if (isHome) getString(R.string.grand_total) else account.label
        with(binding.toolbar.subtitle) {
            text = currentBalance
            setTextColor(getAmountColor(account.currentBalance.sign))
        }

        val progress = account.progress

        val accountVisual = when {
            account.isAggregate -> ACCOUNT_VISUAL_NONE
            progress != null -> ACCOUNT_VISUAL_PROGRESS
            account.bankId != null -> ACCOUNT_VISUAL_ICON
            else -> ACCOUNT_VISUAL_COLOR
        }

        binding.toolbar.donutView.isVisible = accountVisual == ACCOUNT_VISUAL_PROGRESS
        binding.toolbar.progressPercent.isVisible = accountVisual == ACCOUNT_VISUAL_PROGRESS
        binding.toolbar.accountColorIndicator.isVisible = accountVisual == ACCOUNT_VISUAL_COLOR
        binding.toolbar.bankIcon.isVisible = accountVisual == ACCOUNT_VISUAL_ICON

        when (accountVisual) {
            ACCOUNT_VISUAL_ICON -> {
                viewModel.banks.value.find { it.id == account.bankId }?.let {
                    bankingFeature.bankIcon(it)
                }?.let {
                    binding.toolbar.bankIcon.setImageResource(it)
                }
            }

            ACCOUNT_VISUAL_COLOR -> {
                (binding.toolbar.accountColorIndicator.background as GradientDrawable).setColor(
                    account.color
                )
            }

            ACCOUNT_VISUAL_PROGRESS -> {
                val (sign, progress) = progress!!
                with(binding.toolbar.donutView) {
                    animateChanges = animateProgress
                    submitData(
                        sections = DisplayProgress.calcProgressVisualRepresentation(progress)
                            .forViewSystem(
                                account.color,
                                getAmountColor(sign)
                            ).also {
                                Timber.d("Sections: %s", progress)
                            }
                    )
                    contentDescription =
                        getString(if (sign > 0) R.string.saving_goal else R.string.credit_limit) + ": " +
                                DisplayProgress.contentDescription(this@MyExpenses, progress)
                }

                with(binding.toolbar.progressPercent) {
                    text = progress.displayProgress
                    setTextColor(this@MyExpenses.getAmountColor(sign))
                }
            }
        }
        progress?.let { (sign, progress) ->
            with(binding.toolbar.donutView) {
                animateChanges = animateProgress
                submitData(
                    sections = DisplayProgress.calcProgressVisualRepresentation(progress)
                        .forViewSystem(
                            account.color,
                            getAmountColor(sign)
                        ).also {
                            Timber.d("Sections: %s", progress)
                        }
                )
                contentDescription =
                    getString(if (sign > 0) R.string.saving_goal else R.string.credit_limit) + ": " +
                            DisplayProgress.contentDescription(this@MyExpenses, progress)
            }

            with(binding.toolbar.progressPercent) {
                text = progress.displayProgress
                setTextColor(this@MyExpenses.getAmountColor(sign))
            }
        }
    }

    private fun copyToClipBoard() {
        currentBalance.takeIf { it.isNotEmpty() }?.let { copyToClipboard(it) }
    }

    fun updateFab() {
        if (accountCount > 0) {
            updateFabDo()
        } else {
            lifecycleScope.launch {
                if (getHiddenAccountCount() > 0) {
                    updateFabDo()
                } else {
                    floatingActionButton.hide()
                }
            }
        }
    }

    fun updateFabDo() {
        val scanMode = isScanMode()
        val sealed = currentAccount?.sealed == true
        with(floatingActionButton) {
            show()
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
                    this@MyExpenses,
                    ". ",
                    R.string.menu_create_transaction,
                    R.string.menu_create_transfer,
                    R.string.menu_create_split
                )
            }
        }
    }

    fun isScanMode() = prefHandler.getBoolean(PrefKey.OCR, false)
    private fun isWebUiActive() = prefHandler.getBoolean(PrefKey.UI_WEB, false)

    private fun activateOcrMode() {
        prefHandler.putBoolean(PrefKey.OCR, true)
        updateFab()
        invalidateOptionsMenu()
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (feature == ContribFeature.OCR && (tag as? Boolean) == false) {
            if (featureViewModel.isFeatureAvailable(this, Feature.OCR)) {
                activateOcrMode()
            } else {
                featureViewModel.requestFeature(this, Feature.OCR)
            }
        }
        else super.contribFeatureCalled(feature, tag)
    }

    val navigationView: NavigationView
        get() {
            return binding.accountPanel.expansionContent
        }

    override fun onNegative(args: Bundle) {
        val command = args.getInt(KEY_COMMAND_NEGATIVE)
        if (command != 0) {
            dispatchCommand(command, null)
        }
    }

    /**
     * Currently not used
     */
    protected fun voteReminderCheck() {
        val prefKey = "vote_reminder_shown_${RoadmapRepository.VERSION}"
        if (!prefHandler.getBoolean(prefKey, false) && Utils.getDaysSinceUpdate(this) > 1) {
            roadmapViewModel.getLastVote().observe(this) { vote: Vote? ->
                val hasNotVoted = vote == null
                if (hasNotVoted) {
                    ConfirmationDialogFragment.newInstance(Bundle().apply {
                        putCharSequence(KEY_MESSAGE, getString(R.string.roadmap_intro))
                        putInt(KEY_COMMAND_POSITIVE, R.id.ROADMAP_COMMAND)
                        putString(ConfirmationDialogFragment.KEY_PREFKEY, prefKey)
                        putInt(KEY_POSITIVE_BUTTON_LABEL, R.string.roadmap_vote)
                    }).show(supportFragmentManager, "ROAD_MAP_VOTE_REMINDER")
                }
            }
        }
    }

    /**
     * Can be used to ask user who has already voted to update their vote. Currently not used
     */
    /*  private void voteReminderCheck2() {
        roadmapViewModel.getShouldShowVoteReminder().observe(this, shouldShow -> {
          if (shouldShow) {
            prefHandler.putLong(PrefKey.VOTE_REMINDER_LAST_CHECK, System.currentTimeMillis());
            showSnackBar(getString(R.string.reminder_vote_update), Snackbar.LENGTH_INDEFINITE,
                    new SnackbarAction(getString(R.string.vote_reminder_action), v -> {
              Intent intent = new Intent(this, RoadmapVoteActivity.class);
              startActivity(intent);
            }));
          }
        });
      }*/

    override val scrollsHorizontally: Boolean = true

    override fun contribFeatureNotCalled(feature: ContribFeature) {
        if (!isGithub && feature == ContribFeature.AD_FREE) {
            finish()
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        intent: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == EDIT_REQUEST) {
            floatingActionButton.show()
            if (resultCode == RESULT_OK) {
                if (!adHandler.onEditTransactionResult()) {
                    reviewManager.onEditTransactionResult(this)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adHandler.onResume()
    }

    public override fun onDestroy() {
        adHandler.onDestroy()
        super.onDestroy()
    }

    override fun onPause() {
        adHandler.onPause()
        super.onPause()
    }

    companion object {
        const val MANAGE_HIDDEN_FRAGMENT_TAG = "MANAGE_HIDDEN"
        const val ACCOUNT_VISUAL_NONE = 0
        const val ACCOUNT_VISUAL_PROGRESS = 1
        const val ACCOUNT_VISUAL_ICON = 2
        const val ACCOUNT_VISUAL_COLOR = 3
    }
}