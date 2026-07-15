package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.SortableItem
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.accounts.AccountEvent
import org.totschnig.myexpenses.compose.accounts.AccountEventHandler
import org.totschnig.myexpenses.compose.accounts.PortfolioSetupDialog
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.compose.main.MainScreenAdaptive
import org.totschnig.myexpenses.compose.transactions.Action
import org.totschnig.myexpenses.compose.transactions.TradeEvent
import org.totschnig.myexpenses.compose.transactions.TradeList
import org.totschnig.myexpenses.compose.transactions.TradeScreen
import org.totschnig.myexpenses.dialog.SortSelect
import org.totschnig.myexpenses.dialog.SortUtilityDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.KEY_COMMODITY
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_SORT_KEY
import org.totschnig.myexpenses.util.ads.AdHandlerV2
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.data.AggregateAccount
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Trade
import java.util.Optional
import javax.inject.Inject

enum class StartScreen {
    LastVisited, Accounts, Transactions, BalanceSheet
}

/**
 * TBD: ReviewManager, Tests,
 * Help,
 * initial state after first install
 */
class MyExpensesV2 : BaseMyExpenses<MyExpensesV2ViewModel>(),
    SortUtilityDialogFragment.OnConfirmListener {

    @Inject
    lateinit var modelClass: Class<out MyExpensesV2ViewModel>

    private val currencyViewModel: CurrencyViewModel by viewModels()

    private lateinit var adHandler: AdHandlerV2

    private var showPortfolioSetup by mutableStateOf(false)
    private var portfolioToEditId by mutableStateOf<Long?>(null)

    override fun handleRootWindowInsets() {}

    override val currentAccount: BaseAccount?
        get() = viewModel.accountList.value.find { it.id == selectedAccountId }

    @get:Composable
    override val transactionListWindowInsets: WindowInsets
        get() = WindowInsets()

    override val accountCount: Int
        get() = viewModel.accountDataV2.value?.getOrNull()?.size ?: 0

    override fun finishActionMode() {
        viewModel.selectionState.value = emptyList()
    }

    val shouldShowAds
        get() = !adHandlerFactory.isAdDisabled && adHandlerFactory.isInitialized

    private fun maybeRequestNewInterstitial() {
        if (shouldShowAds) {
            try {
                adHandler.maybeRequestNewInterstitial(this)
            } catch (e: Exception) {
                report(e)
            }
        }
    }

    override fun onEditTransactionResult() {
        val adHandled = shouldShowAds && adHandler.onEditTransactionResult(this)

        if (!adHandled) {
            reviewManager.onEditTransactionResult(this)
        }
    }

    override fun editAccount(account: FullAccount) {
        if (account.isPortfolio) {
            portfolioToEditId = account.id
        } else {
            super.editAccount(account)
        }
    }

    override fun injectDependencies() {
        injector.inject(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[modelClass]

        with(injector) {
            inject(viewModel)
            inject(currencyViewModel)
        }

        adHandler = adHandlerFactory.createV2()
        maybeRequestNewInterstitial()

        setContent {
            AppTheme {
                val result = viewModel.accountDataV2.collectAsStateWithLifecycle().value
                val availableFilters =
                    viewModel.availableGroupFilters.collectAsStateWithLifecycle().value
                when {
                    result?.isFailure == true -> {
                        val (message, forceQuit) = result.exceptionOrNull()!!
                            .processDataLoadingFailure()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(dimensionResource(R.dimen.padding_main_screen)),
                            // These two lines replace the Box's contentAlignment and the Column's horizontalAlignment
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(message)
                            // Applying spacing manually since we are using Arrangement.Center
                            // which takes precedence over Arrangement.spacedBy
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!forceQuit) {
                                Button(onClick = {
                                    dispatchCommand(
                                        R.id.SAFE_MODE_COMMAND,
                                        null
                                    )
                                }) {
                                    Text(stringResource(R.string.safe_mode))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Button(onClick = { dispatchCommand(R.id.QUIT_COMMAND, null) }) {
                                Text(stringResource(R.string.button_label_close))
                            }
                        }

                    }

                    result == null || availableFilters == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        val selectedAccountIdFromState =
                            viewModel.selectedAccountId.collectAsState().value
                        LaunchedEffect(
                            viewModel.accountList.collectAsState().value.isNotEmpty(),
                            selectedAccountIdFromState,
                            viewModel.activeFilter.collectAsState().value //reloading when filter changes, because aggregate accounts have same id (0)
                        ) {
                            with(currentAccount) {
                                if (this != null) {
                                    sumInfo.value = SumInfo.EMPTY
                                    viewModel.sumInfo(toPageAccount(this@MyExpensesV2)).collect {
                                        sumInfo.value = it
                                    }
                                }
                            }
                        }
                        val accounts = result.getOrThrow()
                        val banks = viewModel.banks.collectAsState()
                        val showSortDialog = rememberSaveable { mutableStateOf(false) }
                        var isNavigationVisible by rememberSaveable { mutableStateOf(false) }
                        val portfolioToEdit =
                            portfolioToEditId?.let { id -> accounts.find { it.id == id } }

                        val currencies by currencyViewModel.currencyUnits.collectAsState(emptyList())

                        if (showPortfolioSetup || portfolioToEditId != null) {
                            PortfolioSetupDialog(
                                onDismiss = {
                                    showPortfolioSetup = false
                                    portfolioToEditId = null
                                },
                                onConfirm = { label, currency, color ->
                                    if (portfolioToEditId != null) {
                                        viewModel.updatePortfolio(
                                            portfolioToEditId!!,
                                            label,
                                            currency,
                                            color
                                        )
                                    } else {
                                        viewModel.createPortfolio(label, currency, color)
                                    }
                                    showPortfolioSetup = false
                                    portfolioToEditId = null
                                },
                                availableCurrencies = currencies,
                                selectedCurrency = currencyContext.homeCurrencyUnit,
                                initialPortfolio = portfolioToEdit
                            )
                        }

                        MainScreenAdaptive(
                            viewModel,
                            accounts,
                            allCurrencies = currencies,
                            availableFilters,
                            selectedAccountId = selectedAccountIdFromState,
                            isCurrencyUsed = { currencyViewModel.isCurrencyUsed(it) },
                            onCreateAsset = { code, symbol, fractionDigits, label, type ->
                                currencyViewModel.createAsset(
                                    code,
                                    symbol,
                                    fractionDigits,
                                    label,
                                    type
                                )
                            },
                            onAppEvent = object : AppEventHandler {
                                override fun invoke(event: AppEvent) {
                                    when (event) {

                                        AppEvent.CreateAccount -> createAccount()
                                        AppEvent.CreatePortfolio -> showPortfolioSetup = true
                                        is AppEvent.CreateTransaction ->
                                            if (preCreateRowCheckForSealed()) {
                                                when (event.action) {
                                                    Action.Scan -> contribFeatureRequested(
                                                        ContribFeature.OCR,
                                                        true
                                                    )

                                                    else -> createRow(
                                                        event.action.type,
                                                        event.transferEnabled,
                                                        event.action == Action.Income || event.action == Action.Deposit
                                                    )
                                                }
                                            }

                                        is AppEvent.SetAccountGrouping -> viewModel.setGrouping(
                                            event.newGrouping
                                        )

                                        is AppEvent.SetTransactionGrouping -> viewModel.persistGroupingV2(
                                            event.grouping
                                        )

                                        is AppEvent.SetTransactionSort -> viewModel.persistSortV2(
                                            event.transactionSort
                                        )

                                        AppEvent.PrintBalanceSheet -> printBalanceSheet()
                                        is AppEvent.ContextMenuItemClicked -> onContextItemClicked(
                                            event.itemId
                                        )

                                        is AppEvent.MenuItemClicked -> dispatchCommand(
                                            event.itemId,
                                            event.tag
                                        )

                                        AppEvent.Sort -> showSortDialog.value = true

                                        is AppEvent.CopyToClipBoard -> copyToClipboard(event.text)
                                        AppEvent.ToggleNavigation -> isNavigationVisible =
                                            !isNavigationVisible

                                        is AppEvent.SaveTrade -> {
                                            (currentAccount as? FullAccount)?.let {
                                                viewModel.saveTrade(it, event.intent)
                                            }
                                        }
                                    }
                                }
                            },
                            onAccountEvent = object : AccountEventHandler {
                                override fun invoke(
                                    event: AccountEvent,
                                    account: FullAccount,
                                ) {
                                    when (event) {
                                        is AccountEvent.Delete -> confirmAccountDelete(account)
                                        is AccountEvent.Edit -> editAccount(account)
                                        is AccountEvent.SetFlag -> viewModel.setFlag(
                                            account.id,
                                            event.flagId
                                        )

                                        is AccountEvent.ToggleDynamicExchangeRate ->
                                            toggleDynamicExchangeRate(account)

                                        is AccountEvent.ToggleExcludeFromTotals -> toggleExcludeFromTotals(
                                            account
                                        )

                                        is AccountEvent.ToggleSealed -> toggleAccountSealed(account)
                                        is AccountEvent.ViewPriceHistory -> {
                                            val intent = Intent(
                                                this@MyExpensesV2,
                                                PriceHistory::class.java
                                            ).apply {
                                                putExtra(KEY_COMMODITY, event.commodity)
                                                putExtra(KEY_CURRENCY, account.currencyUnit.code)
                                            }
                                            startActivity(intent)
                                        }
                                    }
                                }

                            },
                            onPrepareContextMenuItem = ::isContextMenuItemVisible,
                            onPrepareMenuItem = { itemId -> currentAccount.isMenuItemVisible(itemId) },
                            flags = viewModel.accountFlags.collectAsState(emptyList()).value,
                            adView = { isLoadedState ->
                                adHandler.Banner(isLoadedState)
                            },
                            bankIcon = { modifier, id ->
                                banks.value.find { it.id == id }
                                    ?.let { bank ->
                                        bankingFeature.bankIconRenderer?.invoke(
                                            modifier,
                                            bank
                                        )
                                    }
                            },
                            isNavigationVisible = isNavigationVisible
                        ) { pageAccount, isCurrent ->
                            if (pageAccount.isPortfolio) {
                                PortfolioPage(
                                    pageAccount,
                                    currencies,
                                    viewModel.accountList.collectAsState().value
                                )
                            } else {
                                Page(
                                    pageAccount,
                                    accounts.size,
                                    isCurrent,
                                    v2 = true
                                )
                            }
                        }

                        if (showSortDialog.value) {
                            val sortByFlagFirst = rememberSaveable {
                                mutableStateOf(false)
                            }
                            LaunchedEffect(Unit) {
                                sortByFlagFirst.value = viewModel.sortByFlagFirst.get()
                            }

                            val selectedSort = rememberSaveable {
                                mutableStateOf(viewModel.sortOrderAccounts)
                            }
                            val scope = rememberCoroutineScope()
                            AlertDialog(
                                onDismissRequest = { showSortDialog.value = false },
                                confirmButton = {
                                    Button(onClick = {
                                        viewModel.setSortOrderAccounts(
                                            selectedSort.value,
                                            sortByFlagFirst.value
                                        )
                                        showSortDialog.value = false
                                    }) {
                                        Text(stringResource(id = android.R.string.ok))
                                    }
                                },
                                text = {
                                    Column {
                                        SortSelect(sortByFlagFirst, selectedSort) {
                                            scope.launch {
                                                SortUtilityDialogFragment.newInstance(
                                                    ArrayList(
                                                        viewModel.accountsMinimal(
                                                            withAggregates = false,
                                                            sortOrder = KEY_SORT_KEY
                                                        ).first()
                                                            .map { SortableItem(it.id, it.label) }
                                                    ))
                                                    .show(supportFragmentManager, "SORT_ACCOUNTS")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun accountForNewTransaction() = Optional.ofNullable(
        when (currentAccount) {
            is FullAccount -> currentAccount as? FullAccount
            is AggregateAccount -> viewModel.accountDataV2.value?.getOrNull()
                ?.filter { it.currency == currentAccount?.currency }
                ?.maxByOrNull { it.lastUsed }

            null -> null
        }
    )

    override fun onSortOrderConfirmed(sortedIds: LongArray) {
        viewModel.sortAccounts(sortedIds)
    }

    //Short circuit calls we receive from BaseMyExpenses, can be removed, once V1 is abandoned
    override fun invalidateOptionsMenu() {}

    override fun handleIntent(intent: Intent) {
        viewModel.handleIntent(intent)
        showTransactionFromIntent(intent)
    }

    @Composable
    fun PortfolioPage(
        account: PageAccount,
        allCurrencies: List<CurrencyUnit>,
        accountList: List<BaseAccount>,
    ) {
        val lazyPagingItems = viewModel.getTrades(account).collectAsLazyPagingItems()
        var tradeToEdit by remember { mutableStateOf<Trade?>(null) }

        Column(modifier = Modifier.fillMaxSize()) {
            TradeList(
                trades = lazyPagingItems,
                modifier = Modifier.weight(1f),
                onEvent = { event, trade ->
                    when (event) {
                        TradeEvent.Edit -> {
                            tradeToEdit = trade
                        }

                        TradeEvent.Delete -> {
                            lifecycleScope.launch {
                                // Trades are usually unreconciled upon creation
                                delete(listOf(trade.id to CrStatus.UNRECONCILED))
                            }
                        }
                    }
                }
            )
        }
        tradeToEdit?.let { trade ->
            (accountList.find { it.id == account.id } as? FullAccount)?.let { fullAccount ->
                Dialog(
                    onDismissRequest = { tradeToEdit = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    TradeScreen(
                        onDismiss = { tradeToEdit = null },
                        onSave = { intent ->
                            viewModel.saveTrade(fullAccount, intent, trade.id)
                            tradeToEdit = null
                        },
                        portfolio = fullAccount,
                        reportingCurrency = fullAccount.currencyUnit,
                        assets = allCurrencies,
                        fundingAccounts = accountList
                            .filterIsInstance<FullAccount>()
                            .filter {
                                !it.isPortfolio &&
                                        it.currencyUnit.code == fullAccount.currencyUnit.code
                            }
                            .map {
                                it.id to it.labelV2(this)
                            },
                        initialTrade = trade,
                        onLookupMatchingTransactions = { accountId, total, date, isBuy ->
                            viewModel.findMatchingTransactions(
                                accountId,
                                total,
                                date,
                                fullAccount.currencyUnit,
                                isBuy
                            )
                        }
                    )
                }
            }
        }
    }
}