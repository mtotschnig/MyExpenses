package org.totschnig.myexpenses.compose.main

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.totschnig.myexpenses.compose.accounts.AccountScreenTab
import org.totschnig.myexpenses.compose.accounts.AccountsScreen
import org.totschnig.myexpenses.compose.transactions.TransactionScreen
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.TransactionSort
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.data.PageAccount

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
    object CreateAccount : AppEvent()
    data class CreateTransaction(
        val type: Int,
        val isIncome: Boolean = false,
        val transferEnabled: Boolean = true,
    ) : AppEvent()

    data class SetAccountGrouping(val newGrouping: AccountGrouping<*>) : AppEvent()
    data class SetTransactionGrouping(val grouping: Grouping) : AppEvent()
    data class SetTransactionSort(val transactionSort: TransactionSort) : AppEvent()
    object PrintBalanceSheet : AppEvent()
    data class ContextMenuItemClicked(@param:IdRes val itemId: Int) : AppEvent()
    object Search: AppEvent()
}

interface AppEventHandler {
    operator fun invoke(event: AppEvent)
}

@Composable
fun MainScreen(
    viewModel: MyExpensesV2ViewModel,
    startScreen: StartScreen,
    onAppEvent: AppEventHandler,
    onAccountEvent: org.totschnig.myexpenses.compose.accounts.AccountEventHandler,
    onPrepareMenuItem: (itemId: Int, accountCount: Int) -> Boolean,
    flags: List<AccountFlag> = emptyList(),
    pageContent: @Composable (pageAccount: PageAccount, accountCount: Int) -> Unit,
) {
    val result = viewModel.accountDataV2.collectAsStateWithLifecycle().value

    val accountGrouping = viewModel.accountGrouping.asState()

    val availableFilters = viewModel.availableGroupFilters.collectAsStateWithLifecycle().value

    val navigationIcon: @Composable () -> Unit = {
        TooltipIconButton(
            tooltip = stringResource(R.string.settings_label),
            imageVector = Icons.Default.Settings,
        ) { onAppEvent(AppEvent.NavigateToSettings) }
    }

    when {
        result == null || availableFilters == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        result.isSuccess -> {
            LaunchedEffect(Unit) {
                viewModel.setStartFilter()
            }
            val accounts = result.getOrNull()
            if (accounts.isNullOrEmpty()) {
                EmptyState()
            } else {

                require(accounts.isNotEmpty())
                val navController = rememberNavController()

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
                            viewModel = viewModel,
                            navController = navController,
                            onEvent = onAppEvent,
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
                            viewModel = viewModel,
                            navController = navController,
                            onEvent = onAppEvent,
                            onAccountEvent = onAccountEvent,
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
                    navigateSingleTopTo(navController, screen)
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