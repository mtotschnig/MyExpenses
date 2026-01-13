@file:OptIn(ExperimentalMaterial3Api::class)

package org.totschnig.myexpenses.compose.main

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AccountListV2
import org.totschnig.myexpenses.compose.AccountSummaryV2
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.FutureCriterion
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.NewTransactionRenderer
import org.totschnig.myexpenses.compose.TooltipIconButton
import org.totschnig.myexpenses.compose.TransactionList
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.compose.rememberStaticState
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.TransactionSort
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.data.AggregateAccount
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount

sealed class Screen(
    val route: String,
    val icon: ImageVector,
    @param:StringRes val resourceId: Int,
) {
    object Accounts : Screen("accounts", Icons.Default.AccountBalance, R.string.accounts)
    object Transactions :
        Screen("transactions", Icons.AutoMirrored.Default.ReceiptLong, R.string.transaction)
}

sealed class AppEvent {
    object NavigateToSettings : AppEvent()
    data class EditAccount(val account: FullAccount) : AppEvent()
    object CreateAccount : AppEvent()
    data class DeleteAccount(val account: FullAccount) : AppEvent()
    data class SetFlag(val accountId: Long, val flagId: Long?) : AppEvent()
    data class ToggleSealed(val account: FullAccount) : AppEvent()
    data class ToggleExcludeFromTotals(val account: FullAccount) : AppEvent()
    data class ToggleDynamicExchangeRate(val account: FullAccount) : AppEvent()
    data class CreateTransaction(
        val type: Int,
        val isIncome: Boolean = false,
        val transferEnabled: Boolean = true,
    ) : AppEvent()

    data class SetAccountGrouping(val newGrouping: AccountGrouping<*>) : AppEvent()
    data class SetTransactionGrouping(val grouping: Grouping) : AppEvent()
    data class SetTransactionSort(val transactionSort: TransactionSort) : AppEvent()
    data class ToggleCrStatus(val transactionId: Long) : AppEvent()
}

interface EventHandler {
    operator fun invoke(event: AppEvent)
    val canToggleDynamicExchangeRate: Boolean
}

@Composable
fun MainScreen(
    viewModel: MyExpensesV2ViewModel,
    onEvent: EventHandler,
    flags: List<AccountFlag> = emptyList(),
) {
    val result = viewModel.accountDataV2.collectAsStateWithLifecycle().value

    val accountGrouping = viewModel.accountGrouping.asState()

    val navigationIcon: @Composable () -> Unit = {
        IconButton(onClick = { onEvent(AppEvent.NavigateToSettings) }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }

    when {
        result == null -> {
            Text("Loading")
        }

        result.isSuccess -> {
            val accounts = result.getOrNull()
            if (accounts.isNullOrEmpty()) {
                EmptyState()
            } else {

                require(accounts.isNotEmpty())
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Accounts.route, //TODO should be configurable
                ) {
                    composable(Screen.Transactions.route) {
                        TransactionScreen(
                            navigationIcon = navigationIcon,
                            accounts = accounts,
                            accountGrouping = accountGrouping.value,
                            viewModel = viewModel,
                            navController = navController,
                            onEvent = onEvent
                        )
                    }
                    composable(Screen.Accounts.route) {
                        AccountsScreen(
                            navigationIcon = navigationIcon,
                            accounts = accounts,
                            accountGrouping = accountGrouping.value,
                            viewModel = viewModel,
                            navController = navController,
                            onEvent = onEvent,
                            flags = flags
                        )
                    }
                }
            }
        }

        result.isFailure -> {
            Text("Error: ${result.exceptionOrNull()}")
        }
    }
}

@Composable
fun EmptyState() {
    Text("No accounts found")
}

fun NavController.navigateSingleTopTo(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}


@Composable
fun MyBottomAppBar(navController: NavController) {
    val items = listOf(Screen.Accounts, Screen.Transactions) // Define your nav items
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    ShortNavigationBar {
        items.forEach { screen ->
            ShortNavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigateSingleTopTo(screen)
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = stringResource(screen.resourceId),
                        tint = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                label = { Text(stringResource(screen.resourceId)) }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    navigationIcon: @Composable () -> Unit = {},
    accounts: List<FullAccount>,
    accountGrouping: AccountGrouping<*>,
    viewModel: MyExpensesV2ViewModel,
    navController: NavController,
    onEvent: EventHandler,
) {
    val availableFilters by viewModel.availableGroupFilters.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val tabRowState = rememberCollapsingTabRowState()
    val aggregateTransactionGrouping by viewModel.currentAggregateGrouping.collectAsStateWithLifecycle(
        Grouping.NONE
    )
    val aggregateTransactionSort by viewModel.currentAggregateSort.collectAsStateWithLifecycle(
        TransactionSort.DATE_DESC
    )

    val accountList by remember(accounts) {
        derivedStateOf {
            val filtered = if (activeFilter == null || accountGrouping == AccountGrouping.NONE)
                accounts
            else
                accounts.filter { account -> accountGrouping.getGroupKey(account) == activeFilter }
            val aggregateAccountGrouping =
                if (activeFilter != null) accountGrouping else AccountGrouping.NONE
            filtered.filter { it.visible } + AggregateAccount(
                currencyUnit = activeFilter as? CurrencyUnit
                    ?: viewModel.currencyContext.homeCurrencyUnit,
                type = if (accountGrouping == AccountGrouping.TYPE) activeFilter as? AccountType else null,
                flag = if (accountGrouping == AccountGrouping.FLAG) activeFilter as? AccountFlag else null,
                grouping = aggregateTransactionGrouping,
                accountGrouping = aggregateAccountGrouping,
                sortBy = aggregateTransactionSort.column,
                sortDirection = aggregateTransactionSort.sortDirection,
                equivalentOpeningBalance = filtered.sumOf { it.equivalentOpeningBalance },
                equivalentCurrentBalance = filtered.sumOf { it.equivalentCurrentBalance },
                equivalentSumIncome = filtered.sumOf { it.equivalentSumIncome },
                equivalentSumExpense = filtered.sumOf { it.equivalentSumExpense },
                equivalentSumTransfer = filtered.sumOf { it.equivalentSumTransfer },
                equivalentTotal = filtered.sumOf {
                    it.equivalentTotal ?: it.equivalentCurrentBalance
                },
            ).let { aggregateAccount ->
                if (aggregateAccountGrouping == AccountGrouping.CURRENCY) aggregateAccount.copy(
                    openingBalance = filtered.sumOf { it.openingBalance },
                    currentBalance = filtered.sumOf { it.currentBalance },
                    sumIncome = filtered.sumOf { it.sumIncome },
                    sumExpense = filtered.sumOf { it.sumExpense },
                    sumTransfer = filtered.sumOf { it.sumTransfer },
                    total = filtered.sumOf { it.total ?: it.currentBalance },
                ) else aggregateAccount
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { accountList.size })

    LaunchedEffect(accountList.size) {
        if (pagerState.currentPage >= accountList.size) {
            pagerState.scrollToPage(accountList.lastIndex)
        }
    }

    val currentAccount = remember(accountList) {
        derivedStateOf {
            accountList[pagerState.settledPage.coerceIn(0, accountList.lastIndex)]
        }
    }

    var selectedBalanceType by rememberSaveable { mutableStateOf(BalanceType.CURRENT) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = navigationIcon,
                title = {
                    BalanceHeader(
                        currentAccount = currentAccount.value,
                        displayBalanceType = selectedBalanceType,
                        onDisplayBalanceTypeChange = { newType ->
                            selectedBalanceType = newType
                        }
                    )
                },
                actions = {
                    val menu = listOfNotNull(
                        MenuItem.Sort,
                        if (currentAccount.value.sortBy == KEY_DATE) MenuItem.Grouping else null,
                    )
                    menu.forEach {
                        when (it) {
                            MenuItem.Sort -> TransactionSortMenu(currentAccount.value) {
                                onEvent(AppEvent.SetTransactionSort(it))
                            }

                            MenuItem.Grouping -> {
                                TransactionGroupingMenu(
                                    currentGroup = currentAccount.value.grouping
                                ) {
                                    onEvent(AppEvent.SetTransactionGrouping(it))
                                }
                            }

                            else -> {}
                        }
                    }
                },
            )
        },
        bottomBar = {
            MyBottomAppBar(navController)
        }
    ) { paddingValues ->

        LaunchedEffect(viewModel.selectedAccountId) {
            val currentPage =
                accountList.indexOfFirst { it.id == viewModel.selectedAccountId }
            if (currentPage > -1 && pagerState.currentPage != currentPage) {
                pagerState.scrollToPage(currentPage)
            }
        }

        LaunchedEffect(pagerState.settledPage) {
            val selected = accountList[pagerState.settledPage].id
            viewModel.selectAccount(selected)
            viewModel.scrollToAccountIfNeeded(
                pagerState.currentPage,
                selected,
                true
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(tabRowState.nestedScrollConnection)
            ) {
                Column(
                    modifier = Modifier
                        .height(with(LocalDensity.current) { tabRowState.heightPx.toDp() })
                        .clipToBounds()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (accountGrouping != AccountGrouping.NONE && availableFilters.size > 1) {
                            AccountFilterMenu(
                                activeFilter = activeFilter,
                                availableFilters = availableFilters,
                                onFilterChange = viewModel::setFilter,
                            )
                        }
                        val selectedTabIndex =
                            pagerState.currentPage.coerceAtMost(accountList.lastIndex)
                        SecondaryScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            edgePadding = 0.dp
                        ) {
                            accountList.forEachIndexed { index, account ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = {
                                        Text(
                                            account.labelV2(LocalContext.current),
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    key = { pageIndex ->
                        "${accountGrouping.name}_${activeFilter?.id}_${accountList[pageIndex].id}"
                    },
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val account = accountList[page]
                    val context = LocalContext.current
                    val pageAccount = remember(account) { account.toPageAccount(context = context) }
                    val lazyPagingItems =
                        viewModel.items.getValue(pageAccount).collectAsLazyPagingItems()

                    val showStatusHandle =
                        if (account.isAggregate || account.type?.supportsReconciliation == false)
                            false
                        else
                            viewModel.showStatusHandle.flow.collectAsState(initial = true).value

                    val onToggleCrStatus: ((Long) -> Unit)? = if (showStatusHandle) {
                        { onEvent(AppEvent.ToggleCrStatus(it)) }
                    } else null

                    TransactionList(
                        lazyPagingItems = lazyPagingItems,
                        headerData = remember(account) { viewModel.headerDataV2(pageAccount) }.collectAsState().value,
                        budgetData = rememberStaticState(null),
                        selectionHandler = null,
                        futureCriterion = viewModel.futureCriterion.collectAsState(initial = FutureCriterion.EndOfDay).value,
                        expansionHandler = null,
                        onBudgetClick = { _, _ -> },
                        showSumDetails = viewModel.showSumDetails.collectAsState(initial = true).value,
                        scrollToCurrentDate = viewModel.scrollToCurrentDate.getValue(account.id),
                        renderer = NewTransactionRenderer(
                            LocalDateFormatter.current,
                            onToggleCrStatus = onToggleCrStatus
                        ),
                        isFiltered = false
                    )
                }
            }
            FloatingActionToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                onNewTransaction = { type, isIncome ->
                    onEvent(
                        AppEvent.CreateTransaction(
                            type = type, isIncome = isIncome, transferEnabled = accounts.size > 1
                        )
                    )
                },
            )
        }
    }
}

@Composable
fun AccountsScreen(
    navigationIcon: @Composable () -> Unit = {},
    accounts: List<FullAccount>,
    accountGrouping: AccountGrouping<*>,
    viewModel: MyExpensesV2ViewModel,
    navController: NavController,
    onEvent: EventHandler,
    flags: List<AccountFlag> = emptyList(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = navigationIcon,
                title = { Text("Accounts") },
                actions = {

                    TooltipIconButton(
                        tooltip = stringResource(R.string.balance_sheet),
                        imageVector = Icons.Default.TableChart
                    ) { }

                    AccountGroupingMenu(
                        activeGrouping = accountGrouping,
                        onGroupingChange = { onEvent(AppEvent.SetAccountGrouping(it)) },
                    )
                }
            )
        },
        bottomBar = {
            // It shares the same bottom navigation component
            MyBottomAppBar(navController = navController)
        },
        floatingActionButton = {
            MyFloatingActionButton(
                onClick = { onEvent(AppEvent.CreateAccount) },
                contentDescription = "Add Account"
            )
        }
    ) { paddingValues ->
        AccountListV2(
            accountData = accounts,
            scaffoldPadding = paddingValues,
            grouping = accountGrouping,
            selectedAccount = viewModel.selectedAccountId,
            listState = viewModel.listState,
            expansionHandlerGroups = viewModel.expansionHandler("collapsedHeadersDrawer_${accountGrouping}"),
            onSelected = {
                if (accountGrouping != AccountGrouping.NONE) {
                    viewModel.maybeResetFilter(accountGrouping.getGroupKey(it))
                }
                viewModel.selectAccount(it.id)
                navController.navigateSingleTopTo(Screen.Transactions)
            },
            onGroupSelected = {
                viewModel.navigateToGroup(it)
                navController.navigateSingleTopTo(Screen.Transactions)
            },
            onEvent = onEvent,
            flags = flags
        )
    }
}

@Composable
private fun MyFloatingActionButton(onClick: () -> Unit, contentDescription: String) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
    ) {
        Icon(Icons.Default.Add, contentDescription)
    }
}


@Composable
private fun FloatingActionToolbar(
    onNewTransaction: (type: Int, isIncome: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showMenu = remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Main Action Text - make it clickable
            IconButton(
                onClick = { onNewTransaction(TYPE_TRANSACTION, true) },
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.menu_create_transaction)
                )
            }

            // Divider to create the "split button" look
            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
            )

            // Box to anchor the dropdown menu
            Box {
                val rotationAngle by animateFloatAsState(
                    targetValue = if (showMenu.value) 180F else 0F,
                    label = "DropdownArrowRotation"
                )

                // The "expand" part of the split button
                IconButton(onClick = { showMenu.value = true }) {
                    Icon(
                        modifier = Modifier.rotate(rotationAngle),
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description)
                    )
                }
                HierarchicalMenu(
                    expanded = showMenu,
                    menu = Menu(
                        listOf(
                            MenuEntry(
                                icon = { painterResource(R.drawable.ic_expense) },
                                tint = Color.Unspecified,
                                label = UiText.StringResource(
                                    R.string.expense
                                ),
                                command = "Expense",
                                action = { onNewTransaction(TYPE_TRANSACTION, false) }
                            ),
                            MenuEntry(
                                icon = Icons.Default.Add,
                                tint = LocalColors.current.income,
                                label = R.string.income,
                                command = "Income",
                                action = { onNewTransaction(TYPE_TRANSACTION, true) }
                            ),
                            MenuEntry(
                                icon = { painterResource(R.drawable.ic_menu_forward) },
                                label = UiText.StringResource(
                                    R.string.transfer
                                ),
                                command = "Transfer",
                                action = { onNewTransaction(TYPE_TRANSFER, false) }
                            ),
                            MenuEntry(
                                icon = { painterResource(R.drawable.ic_menu_split) },
                                label = UiText.StringResource(
                                    R.string.split_transaction
                                ),
                                command = "Split transaction",
                                action = { onNewTransaction(TYPE_SPLIT, false) }
                            ),
                        )
                    )
                )
            }
        }
    }
}

private val TabRowHeight = 48.dp

@Composable
fun rememberCollapsingTabRowState(): CollapsingTabRowState {
    val heightPx = with(LocalDensity.current) { TabRowHeight.toPx() }
    return remember {
        CollapsingTabRowState(heightPx)
    }
}

class CollapsingTabRowState(
    private val maxHeightPx: Float,
) {
    var offsetPx by mutableFloatStateOf(0f)
        private set

    val heightPx: Float
        get() = (maxHeightPx + offsetPx).coerceIn(0f, maxHeightPx)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val delta = available.y
            val newOffset = (offsetPx + delta).coerceIn(-maxHeightPx, 0f)
            val consumed = newOffset - offsetPx
            offsetPx = newOffset
            return Offset(0f, consumed)
        }
    }
}

// You can place this in your model package or near the TransactionScreen

enum class BalanceType(
    @param:StringRes val resourceId: Int,
    val icon: ImageVector,
) {
    CURRENT(R.string.current_balance, Icons.Default.DragHandle),
    TOTAL(R.string.menu_aggregates, Icons.Default.Functions),
    CLEARED(R.string.total_cleared, Icons.Default.Check),
    RECONCILED(R.string.total_reconciled, Icons.Default.DoneAll)
}

@Composable
private fun BalanceHeader(
    currentAccount: BaseAccount,
    displayBalanceType: BalanceType,
    onDisplayBalanceTypeChange: (BalanceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSummaryPopupVisible by rememberSaveable { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isSummaryPopupVisible) 0F else 180F
    )

    val validatedBalanceType = displayBalanceType.takeIf {
        when (displayBalanceType) {
            BalanceType.CURRENT -> true
            BalanceType.TOTAL -> (currentAccount.total ?: currentAccount.equivalentTotal) != null
            BalanceType.CLEARED, BalanceType.RECONCILED -> currentAccount is FullAccount && currentAccount.type.supportsReconciliation
        }
    } ?: BalanceType.CURRENT

    Box {
        Row(
            modifier = modifier.clickable {
                isSummaryPopupVisible = !isSummaryPopupVisible
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentAccount.labelV2(LocalContext.current),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconTint = when (validatedBalanceType) {
                        BalanceType.CLEARED -> colorResource(id = R.color.CLEARED)
                        BalanceType.RECONCILED -> colorResource(id = R.color.RECONCILED)
                        else -> colorResource(id = R.color.UNRECONCILED)
                    }

                    Icon(
                        imageVector = validatedBalanceType.icon,
                        contentDescription = stringResource(validatedBalanceType.resourceId),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        tint = iconTint
                    )

                    AmountText(
                        getBalanceForType(
                            currentAccount,
                            validatedBalanceType
                        ), currentAccount.currencyUnit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
            Icon(
                modifier = Modifier.rotate(rotationAngle),
                imageVector = Icons.Default.ExpandLess,
                contentDescription = "stringResource(R.string.show_balance_summary)"
            )
        }

        // The Popup that shows the full summary
        if (isSummaryPopupVisible) {

            Popup(
                onDismissRequest = { isSummaryPopupVisible = false },
                properties = PopupProperties(focusable = true),
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize,
                    ): IntOffset {
                        return anchorBounds.bottomLeft
                    }
                }
            ) {
                //overwrite TitleTypography
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            AccountSummaryV2(
                                currentAccount,
                                displayBalanceType,
                                onDisplayBalanceTypeChange
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getBalanceForType(account: BaseAccount, type: BalanceType): Long {
    return when (account) {
        is FullAccount -> when (type) {
            BalanceType.CURRENT -> account.currentBalance
            BalanceType.TOTAL -> account.total!!
            BalanceType.CLEARED -> account.clearedTotal
            BalanceType.RECONCILED -> account.reconciledTotal
        }

        is AggregateAccount -> when (type) {
            BalanceType.CURRENT -> account.currentBalance ?: account.equivalentCurrentBalance
            BalanceType.TOTAL -> account.total ?: account.equivalentTotal
            else -> account.equivalentCurrentBalance
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStatePreview() {
    EmptyState()
}
