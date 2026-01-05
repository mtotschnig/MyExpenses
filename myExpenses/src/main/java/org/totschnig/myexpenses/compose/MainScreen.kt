@file:OptIn(ExperimentalMaterial3Api::class)

package org.totschnig.myexpenses.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.data.AggregateAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.HeaderDataEmpty

sealed class Screen(val route: String, val icon: ImageVector, @StringRes val resourceId: Int) {
    object Accounts : Screen("accounts", Icons.Default.AccountBalance, R.string.accounts)
    object Transactions :
        Screen("transactions", Icons.AutoMirrored.Default.ReceiptLong, R.string.transaction)
}

@Composable
fun MainScreen(
    viewModel: MyExpensesV2ViewModel,
    onNavigateToSettings: () -> Unit,
    onEdit: (FullAccount) -> Unit,
    onDelete: (FullAccount) -> Unit,
    onSetFlag: (Long, Long?) -> Unit,
    onToggleSealed: (FullAccount) -> Unit,
    onToggleExcludeFromTotals: (FullAccount) -> Unit,
    onToggleDynamicExchangeRate: ((FullAccount) -> Unit)?,
    flags: List<AccountFlag> = emptyList(),
) {
    val result = viewModel.accountDataV2.collectAsStateWithLifecycle().value

    val accountGrouping = viewModel.accountGrouping.asState()

    val navigationIcon: @Composable () -> Unit = {
        IconButton(onClick = onNavigateToSettings) {
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
                            navController = navController
                        )
                    }
                    composable(Screen.Accounts.route) {
                        AccountsScreen(
                            navigationIcon = navigationIcon,
                            accounts = accounts,
                            accountGrouping = accountGrouping.value,
                            viewModel = viewModel,
                            navController = navController,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onSetFlag = onSetFlag,
                            onToggleSealed = onToggleSealed,
                            onToggleExcludeFromTotals = onToggleExcludeFromTotals,
                            onToggleDynamicExchangeRate = onToggleDynamicExchangeRate,
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
                    navController.navigate(screen.route) {
                        // This logic is for bottom nav clicks ONLY
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
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
) {

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = navigationIcon,
                title = {
                    Column {
                        Text("TODO")
                        Text(
                            text = stringResource(id = R.string.current_balance),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                },
                actions = {},
            )
        },
        bottomBar = {
            MyBottomAppBar(navController)
        },
        floatingActionButton = {
            MyFloatingActionButton(onClick = {}, contentDescription = "Add Transaction")
        }
    ) { paddingValues ->
        val availableFilters by viewModel.availableGroupFilters.collectAsStateWithLifecycle()
        val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
        val coroutineScope = rememberCoroutineScope()
        val tabRowState = rememberCollapsingTabRowState()

        val accountList = remember {
            derivedStateOf {
                val filtered = if (activeFilter == null || accountGrouping == AccountGrouping.NONE)
                    accounts
                else
                    accounts.filter { account -> accountGrouping.getGroupKey(account) == activeFilter }
                val visible = filtered.filter { it.visible }
                visible + AggregateAccount(
                    currencyUnit = if (accountGrouping == AccountGrouping.CURRENCY) {
                        activeFilter as? CurrencyUnit ?: viewModel.currencyContext.homeCurrencyUnit
                    } else viewModel.currencyContext.homeCurrencyUnit,
                    type = if (accountGrouping == AccountGrouping.TYPE) activeFilter as? AccountType else null,
                    flag = if (accountGrouping == AccountGrouping.FLAG) activeFilter as? AccountFlag else null,
                    accountGrouping = if (activeFilter != null) accountGrouping else AccountGrouping.NONE,
                )
            }
        }.value

        val pagerState = rememberPagerState(pageCount = { accountList.size })

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(tabRowState.nestedScrollConnection)
        ) {
            Column(
                modifier = Modifier
                    .height(with(LocalDensity.current) { tabRowState.heightPx.toDp() })
                    .clipToBounds()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (accountGrouping != AccountGrouping.NONE && availableFilters.size > 1) {
                        FilterMenu(
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
                                text = { Text(account.labelV2(LocalContext.current), maxLines = 1) }
                            )
                        }
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                key = { activeFilter to accountList[it].id },
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val account = accountList[page]
                val context = LocalContext.current
                val pageAccount = remember { account.toPageAccount(context = context) }
                val lazyPagingItems =
                    viewModel.items.getValue(pageAccount).collectAsLazyPagingItems()
                TransactionList(
                    lazyPagingItems = lazyPagingItems,
                    headerData = HeaderDataEmpty(pageAccount),
                    budgetData = rememberStaticState(null),
                    selectionHandler = null,
                    futureCriterion = viewModel.futureCriterion.collectAsState(initial = FutureCriterion.EndOfDay).value,
                    expansionHandler = null,
                    onBudgetClick = { _, _ -> },
                    showSumDetails = viewModel.showSumDetails.collectAsState(initial = true).value,
                    scrollToCurrentDate = viewModel.scrollToCurrentDate.getValue(account.id),
                    renderer = NewTransactionRenderer(LocalDateFormatter.current),
                    isFiltered = false
                )
            }
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
    onEdit: (FullAccount) -> Unit,
    onDelete: (FullAccount) -> Unit,
    onSetFlag: (Long, Long?) -> Unit,
    onToggleSealed: (FullAccount) -> Unit,
    onToggleExcludeFromTotals: (FullAccount) -> Unit,
    onToggleDynamicExchangeRate: ((FullAccount) -> Unit)?,
    flags: List<AccountFlag> = emptyList(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = navigationIcon,
                title = { Text("Accounts") },
                actions = {

                    GroupingMenu(
                        activeGrouping = accountGrouping,
                        onGroupingChange = viewModel::setGrouping,
                    )
                    // Action specific to the Accounts screen
                    IconButton(onClick = { /* TODO: Add new account */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Account")
                    }
                }
            )
        },
        bottomBar = {
            // It shares the same bottom navigation component
            MyBottomAppBar(navController = navController)
        },
        floatingActionButton = {
            MyFloatingActionButton(onClick = {}, contentDescription = "Add Account")
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
                navController.navigate(Screen.Transactions.route)
            },
            onGroupSelected = {
                viewModel.navigateToGroup(it)
                navController.navigate(Screen.Transactions.route)
            },
            onEdit = onEdit,
            onDelete = onDelete,
            onSetFlag = onSetFlag,
            onToggleSealed = onToggleSealed,
            onToggleExcludeFromTotals = onToggleExcludeFromTotals,
            onToggleDynamicExchangeRate = onToggleDynamicExchangeRate,
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
fun GroupingMenu(
    activeGrouping: AccountGrouping<*>,
    onGroupingChange: (AccountGrouping<*>) -> Unit,
) {
    Box {
        val showMenu = remember { mutableStateOf(false) }
        val groupingOptions = remember { AccountGrouping.ALL_VALUES }

        IconButton(onClick = { showMenu.value = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.List,
                contentDescription = "Filter and Group"
            )
        }
        HierarchicalMenu(
            expanded = showMenu,
            menu = Menu(groupingOptions.map { option ->
                CheckableMenuEntry(
                    label = UiText.StringValue(option.toString()),
                    action = { onGroupingChange(option) },
                    command = "CHANGE_GROUPING",
                    isChecked = activeGrouping == option,
                    isRadio = true
                )
            }),
            title = stringResource(R.string.menu_grouping)
        )
    }
}

@Composable
fun FilterMenu(
    activeFilter: AccountGroupingKey?,
    availableFilters: List<AccountGroupingKey>,
    onFilterChange: (AccountGroupingKey?) -> Unit,
) {
    Box {
        val showMenu = remember { mutableStateOf(false) }
        val context = LocalContext.current
        IconButton(onClick = { showMenu.value = true }) {
            Icon(
                imageVector = Icons.Default.FilterAlt,
                contentDescription = "Filter"
            )
        }
        HierarchicalMenu(
            expanded = showMenu,
            menu = Menu(
                listOf(
                    CheckableMenuEntry(
                        label = UiText.StringResource(R.string.show_all),
                        action = { onFilterChange(null) },
                        command = "RESET_FILTER",
                        isChecked = activeFilter == null,
                        isRadio = true
                    )
                ) +
                        availableFilters.map { filter ->
                            CheckableMenuEntry(
                                label = UiText.StringValue(filter.title(context)),
                                action = { onFilterChange(filter) },
                                command = "CHANGE_FILTER",
                                isChecked = activeFilter == filter,
                                isRadio = true
                            )
                        }),
        )
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


@Preview(showBackground = true)
@Composable
fun EmptyStatePreview() {
    EmptyState()
}