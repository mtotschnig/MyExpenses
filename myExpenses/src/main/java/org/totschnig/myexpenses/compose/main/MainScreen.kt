package org.totschnig.myexpenses.compose.main

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.StartScreen
import org.totschnig.myexpenses.compose.TooltipIconButton
import org.totschnig.myexpenses.compose.accounts.AccountEventHandler
import org.totschnig.myexpenses.compose.accounts.AccountScreenTab
import org.totschnig.myexpenses.compose.accounts.AccountsScreen
import org.totschnig.myexpenses.compose.transactions.Action
import org.totschnig.myexpenses.compose.transactions.TransactionScreen
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.TransactionSort
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.PageAccount

sealed class Screen(
    val route: String,
    val icon: ImageVector,
    @param:StringRes val resourceId: Int,
) {
    object Accounts : Screen("accounts", Icons.Default.AccountBalance, R.string.accounts)
    object Transactions :
        Screen(
            "transactions",
            Icons.AutoMirrored.Default.ReceiptLong,
            R.string.import_select_transactions
        )

    object Reports :
        Screen("reports", Icons.AutoMirrored.Default.ShowChart, R.string.reports)

    object Tools :
        Screen("tools", Icons.Default.Build, R.string.tools)

}

sealed class AppEvent {
    object CreateAccount : AppEvent()
    data class CreateTransaction(
        val action: Action,
        val transferEnabled: Boolean = true,
    ) : AppEvent()

    data class SetAccountGrouping(val newGrouping: AccountGrouping<*>) : AppEvent()
    data class SetTransactionGrouping(val grouping: Grouping) : AppEvent()
    data class SetTransactionSort(val transactionSort: TransactionSort) : AppEvent()
    object PrintBalanceSheet : AppEvent()
    data class ContextMenuItemClicked(@param:IdRes val itemId: Int) : AppEvent()
    object Search : AppEvent()
    data class MenuItemClicked(@param:IdRes val itemId: Int) : AppEvent()
}

interface AppEventHandler {
    operator fun invoke(event: AppEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MyExpensesV2ViewModel,
    startScreen: StartScreen,
    accounts: List<FullAccount>,
    availableFilters: List<AccountGroupingKey>,
    selectedAccountId: Long,
    onAppEvent: AppEventHandler,
    onAccountEvent: AccountEventHandler,
    onPrepareContextMenuItem: (itemId: Int) -> Boolean,
    onPrepareMenuItem: (itemId: Int) -> Boolean,
    flags: List<AccountFlag> = emptyList(),
    pageContent: @Composable (pageAccount: PageAccount, accountCount: Int) -> Unit,
) {

    val accountGrouping = viewModel.accountGrouping.asState()

    val navigationIcon: @Composable () -> Unit = {
        TooltipIconButton(
            tooltip = stringResource(R.string.settings_label),
            imageVector = Icons.Default.Settings,
        ) { onAppEvent(AppEvent.MenuItemClicked(R.id.SETTINGS_COMMAND)) }
    }

    LaunchedEffect(Unit) {
        viewModel.setStartFilter()
    }

    if (accounts.isEmpty()) {
        EmptyState()
    } else {

        require(accounts.isNotEmpty())
        val navController = rememberNavController()

        val showBottomSheetFor = remember { mutableStateOf<Screen?>(null) }
        val sheetState = rememberModalBottomSheetState()

        fun onNavigation(screen: Screen) {
            when (screen) {
                Screen.Accounts, Screen.Transactions -> {
                    navigateSingleTopTo(navController, screen)
                }

                Screen.Reports, Screen.Tools -> {
                    showBottomSheetFor.value = screen
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = when (startScreen) {
                StartScreen.Accounts, StartScreen.BalanceSheet -> Screen.Accounts
                else -> Screen.Transactions
            }.route
        ) {
            composable(Screen.Transactions.route) {
                TransactionScreen(
                    navigationIcon = navigationIcon,
                    accounts = accounts,
                    accountGrouping = accountGrouping.value,
                    availableFilters = availableFilters,
                    selectedAccountId = selectedAccountId,
                    viewModel = viewModel,
                    bottomBar = {
                        MyBottomAppBar(
                            navController = navController,
                            onSelected = ::onNavigation
                        )
                    },
                    onEvent = onAppEvent,
                    onPrepareContextMenuItem = onPrepareContextMenuItem,
                    onPrepareMenuItem = onPrepareMenuItem,
                    pageContent = pageContent
                )
            }
            composable(Screen.Accounts.route) {
                AccountsScreen(
                    if (startScreen == StartScreen.BalanceSheet) AccountScreenTab.BALANCE_SHEET else AccountScreenTab.LIST,
                    navigationIcon = navigationIcon,
                    accounts = accounts,
                    accountGrouping = accountGrouping.value,
                    selectedAccountId = selectedAccountId,
                    viewModel = viewModel,
                    bottomBar = {
                        MyBottomAppBar(
                            navController = navController,
                            onSelected = ::onNavigation
                        )
                    },
                    onEvent = onAppEvent,
                    onAccountEvent = onAccountEvent,
                    flags = flags
                ) {
                    navigateTo(navController, Screen.Transactions)
                }
            }
        }
        if (showBottomSheetFor.value != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheetFor.value = null },
                sheetState = sheetState,
                contentWindowInsets = { WindowInsets.navigationBars }
            ) {
                val items = when (showBottomSheetFor.value) {
                    Screen.Tools -> listOf(
                        MenuItem.Templates, MenuItem.Parties
                    )

                    Screen.Reports -> listOf(
                        MenuItem.Budget, MenuItem.Distribution, MenuItem.History
                    )

                    else -> return@ModalBottomSheet
                }

                items
                    .filter { onPrepareMenuItem(it.id) }
                    .forEach {
                    ListItem(
                        modifier = Modifier.clickable {
                            showBottomSheetFor.value = null
                            onAppEvent(AppEvent.MenuItemClicked(it.id))
                        },
                        headlineContent = { Text(it.getLabel(LocalContext.current)) },
                        leadingContent = { Icon(painterResource(it.icon), contentDescription = null) },
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Text("No accounts found")
}

fun navigateTo(
    controller: NavController,
    screen: Screen,
) {
    controller.navigate(screen.route)
}

fun navigateSingleTopTo(
    controller: NavController,
    screen: Screen,
) {
    controller.navigate(screen.route) {
        popUpTo(controller.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBottomAppBar(
    navController: NavController,
    onSelected: (Screen) -> Unit,
) {

    val items = listOf(Screen.Accounts, Screen.Transactions, Screen.Reports, Screen.Tools)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    ShortNavigationBar {
        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelected(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = stringResource(screen.resourceId),
                        tint = if (selected) {
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


@Composable
fun MyFloatingActionButton(onClick: () -> Unit, contentDescription: String) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
    ) {
        Icon(Icons.Default.Add, contentDescription)
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStatePreview() {
    EmptyState()
}