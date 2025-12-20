@file:OptIn(ExperimentalMaterial3Api::class)

package org.totschnig.myexpenses.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.HeaderDataEmpty

sealed class Screen(val route: String, val icon: ImageVector, @StringRes val resourceId: Int) {
    object Accounts : Screen("accounts", Icons.Default.AccountBalance, R.string.accounts)
    object Transactions : Screen("transactions", Icons.AutoMirrored.Default.ReceiptLong, R.string.transaction)
}

@Composable
fun MainScreen(viewModel: MyExpensesV2ViewModel) {
    val result = viewModel.filteredAccounts.collectAsStateWithLifecycle().value

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
                        TransactionScreen(accounts, viewModel, navController)
                    }
                    composable(Screen.Accounts.route) {
                        AccountsScreen(accounts, viewModel, navController)
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

    BottomAppBar(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        actions = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            items.forEach { screen ->
                IconButton(
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = stringResource(screen.resourceId),
                        tint = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Open new transaction screen */ },
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Default.Add, "Add transaction")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    accounts: List<FullAccount>,
    viewModel: MyExpensesV2ViewModel,
    navController: NavController
) {

    Scaffold(
        topBar = {
            TopAppBar(
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
        }
    ) { paddingValues ->

        val pagerState = rememberPagerState(pageCount = { accounts.size })
        val activeGrouping by viewModel.activeGrouping.collectAsStateWithLifecycle()
        val availableFilters by viewModel.availableGroupFilters.collectAsStateWithLifecycle()
        val activeFilter by viewModel.activeGroupFilter.collectAsStateWithLifecycle()
        val coroutineScope = rememberCoroutineScope()
        val tabRowState = rememberCollapsingTabRowState()

        val currentPage = accounts.indexOfFirst { it.id == viewModel.selectedAccountId }.coerceAtLeast(0)

        LaunchedEffect(viewModel.selectedAccountId) {
            if (pagerState.currentPage != currentPage) {
                pagerState.scrollToPage(currentPage)
            }
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
                    Box {
                        GroupingMenu(
                            anchor = { showMenu ->
                                IconButton(onClick = showMenu) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Configure grouping"
                                    )
                                }
                            },
                            activeGrouping = activeGrouping,
                            availableFilters = availableFilters,
                            activeFilter = activeFilter,
                            onGroupingChange = viewModel::setGrouping,
                            onFilterChange = viewModel::setGroupFilter
                        )
                    }
                    SecondaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 0.dp
                    ) {
                        accounts.forEachIndexed { index, account ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = { Text(account.label, maxLines = 1) }
                            )
                        }
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxSize() // The pager fills the rest of the column
            ) { page ->
                val account = accounts[page]
                val pageAccount = account.toPageAccount
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
                    renderer = NewTransactionRenderer(null),
                    isFiltered = false
                )
            }
        }
    }
}

@Composable
fun AccountsScreen(
    accounts: List<FullAccount>,
    viewModel: MyExpensesV2ViewModel,
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                actions = {
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
        }
    ) { paddingValues ->
        val grouping = AccountGrouping.NONE
        AccountList(
            Modifier.padding(paddingValues),
            accountData = accounts,
            grouping = grouping,
            selectedAccount = viewModel.selectedAccountId,
            listState = viewModel.listState,
            expansionHandlerGroups = viewModel.expansionHandler("collapsedHeadersDrawer_${grouping}"),
            expansionHandlerAccounts = viewModel.expansionHandler("expandedAccounts"),
            onSelected = {
                viewModel.selectAccount(it)
                navController.navigate(Screen.Transactions.route)
            }
        )
    }
}


@Composable
fun GroupingMenu(
    anchor: @Composable (() -> Unit) -> Unit,
    activeGrouping: AccountGrouping,
    availableFilters: List<String>,
    activeFilter: String?,
    onGroupingChange: (AccountGrouping) -> Unit,
    onFilterChange: (String?) -> Unit,
) {
    var showMainMenu by remember { mutableStateOf(false) }
    var showGroupingSubMenu by remember { mutableStateOf(false) }
    var showFilterSubMenu by remember { mutableStateOf(false) }
    val groupingOptions = remember { AccountGrouping.entries.toTypedArray() }
    val allLabel = "ALL"

    // The anchor composable (our gear icon) is passed in
    anchor { showMainMenu = true }

    // Main Dropdown
    DropdownMenu(
        expanded = showMainMenu,
        onDismissRequest = { showMainMenu = false }
    ) {
        // "Group by" entry, which opens a submenu
        DropdownMenuItem(
            text = { Text("Group by") },
            onClick = { showGroupingSubMenu = true },
            trailingIcon = {
                Icon(
                    Icons.AutoMirrored.Default.ArrowRight,
                    contentDescription = "Open group by options"
                )
            }
        )
        // "Filter by" entry, also opens a submenu
        // Only enable it if grouping is active and there are filters
        DropdownMenuItem(
            text = { Text("Show") },
            onClick = { showFilterSubMenu = true },
            enabled = activeGrouping != AccountGrouping.NONE && availableFilters.isNotEmpty(),
            trailingIcon = {
                Icon(
                    Icons.AutoMirrored.Default.ArrowRight,
                    contentDescription = "Open filter by options"
                )
            }
        )
    }

    // --- Submenus ---

    // Grouping Submenu
    DropdownMenu(
        expanded = showGroupingSubMenu,
        onDismissRequest = { showGroupingSubMenu = false }
    ) {
        groupingOptions.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.toString()) },
                onClick = {
                    onGroupingChange(option)
                    // Close all menus
                    showGroupingSubMenu = false
                    showMainMenu = false
                }
            )
        }
    }

    // Filter Submenu
    DropdownMenu(
        expanded = showFilterSubMenu,
        onDismissRequest = { showFilterSubMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text(allLabel) },
            onClick = {
                onFilterChange(null)
                // Close all menus
                showFilterSubMenu = false
                showMainMenu = false
            }
        )
        availableFilters.forEach { filter ->
            DropdownMenuItem(
                text = { Text(filter) },
                onClick = {
                    onFilterChange(filter)
                    // Close all menus
                    showFilterSubMenu = false
                    showMainMenu = false
                }
            )
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


@Preview(showBackground = true)
@Composable
fun EmptyStatePreview() {
    EmptyState()
}