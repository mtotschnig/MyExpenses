package org.totschnig.myexpenses.compose.main

import androidx.activity.compose.BackHandler
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.StartScreen
import org.totschnig.myexpenses.compose.TooltipIconButton
import org.totschnig.myexpenses.compose.accounts.AccountEventHandler
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
import timber.log.Timber

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
    object Sort : AppEvent()
    data class CopyToClipBoard(val text: String) : AppEvent()
}

interface AppEventHandler {
    operator fun invoke(event: AppEvent)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
suspend fun ThreePaneScaffoldNavigator<*>.navigateToRoot(pane: ThreePaneScaffoldRole) {
    if (canNavigateBack()) {
        navigateBack()
    } else {
        navigateTo(pane)
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreenAdaptive(
    viewModel: MyExpensesV2ViewModel,
    accounts: List<FullAccount>,
    availableFilters: List<AccountGroupingKey>,
    selectedAccountId: Long,
    onAppEvent: AppEventHandler,
    onAccountEvent: AccountEventHandler,
    onPrepareContextMenuItem: (itemId: Int) -> Boolean,
    onPrepareMenuItem: (itemId: Int) -> Boolean,
    flags: List<AccountFlag> = emptyList(),
    bankIcon: (@Composable (Modifier, Long) -> Unit)? = null,
    pageContent: @Composable (pageAccount: PageAccount, isCurrent: Boolean) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val navigator = rememberListDetailPaneScaffoldNavigator<FullAccount>(
        initialDestinationHistory = listOf(
            ThreePaneScaffoldDestinationItem(
                pane = when (viewModel.startScreen) {
                    StartScreen.Accounts, StartScreen.BalanceSheet -> ListDetailPaneScaffoldRole.List
                    else -> ListDetailPaneScaffoldRole.Detail
                }
            )
        ),
        isDestinationHistoryAware = false
    )
    val showBottomSheetFor = remember { mutableStateOf<Screen?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val onNavigateToSettings =
        { onAppEvent(event = AppEvent.MenuItemClicked(R.id.SETTINGS_COMMAND)) }

    val navigationIcon: @Composable () -> Unit = {
        TooltipIconButton(
            tooltip = stringResource(R.string.settings_label),
            imageVector = Icons.Default.Settings, onNavigateToSettings,
        )
    }

    val accountGrouping = viewModel.accountGrouping.asState()

    val defaultExpanded = navigator.scaffoldDirective.maxHorizontalPartitions > 1
    var forceExpanded by remember { mutableStateOf(false) }
    var forceCollapsed by remember { mutableStateOf(false) }
    val isExpanded = (defaultExpanded && !forceCollapsed) || forceExpanded

    if (isExpanded) {

        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective.copy(
                horizontalPartitionSpacerSize = 12.dp
            ),
            value = ThreePaneScaffoldValue(
                primary = PaneAdaptedValue.Expanded,
                secondary = PaneAdaptedValue.Expanded,
                tertiary = PaneAdaptedValue.Hidden
            ),
            listPane = {
                AnimatedPane {
                    Surface(
                        modifier = Modifier
                            .padding(
                                start = 8.dp
                            ) // Add small end padding to create a center gutter
                            .windowInsetsPadding(
                                WindowInsets.safeContent.only(
                                    WindowInsetsSides.Vertical
                                )
                            ),
                        shape = RoundedCornerShape(24.dp), // Typical Reply-style rounding
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        // Add a special "Tablet Header" here with Reports/Tools
                        //TabletUtilityHeader(onAppEvent)

                        AccountsScreen(
                            containerColor = Color.Transparent,
                            navigationIcon = {
                                TooltipIconButton(
                                    tooltip = stringResource(R.string.collapse),
                                    imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                                    {
                                        if (defaultExpanded) {
                                            forceCollapsed = true
                                        } else {
                                            forceExpanded = false
                                        }
                                    },
                                )
                            },
                            accounts = accounts,
                            accountGrouping = accountGrouping.value,
                            selectedAccountId = selectedAccountId,
                            viewModel = viewModel,
                            onEvent = onAppEvent,
                            onAccountEvent = onAccountEvent,
                            flags = flags,
                            bankIcon = bankIcon
                        ) {
                            scope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                            }
                        }
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    Surface(
                        modifier = Modifier
                            .padding(
                                end = 8.dp
                            ) // Add small start padding for the gutter
                            .windowInsetsPadding(
                                WindowInsets.safeContent.only(
                                    WindowInsetsSides.Vertical
                                )
                            ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        TransactionScreen(
                            containerColor = Color.Transparent,
                            navigationIcon = navigationIcon,
                            accounts = accounts,
                            accountGrouping = accountGrouping.value,
                            availableFilters = availableFilters,
                            selectedAccountId = selectedAccountId,
                            viewModel = viewModel,
                            onEvent = onAppEvent,
                            onPrepareContextMenuItem = onPrepareContextMenuItem,
                            onPrepareMenuItem = onPrepareMenuItem,
                            pageContent = pageContent,
                            bankIcon = bankIcon
                        )
                    }
                }
            }
        )
    } else {
        val adaptiveInfo = currentWindowAdaptiveInfo()
        val layoutType =
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

        NavigationSuiteScaffold(
            layoutType = layoutType,
            navigationSuiteItems = {
                if (layoutType == NavigationSuiteType.NavigationRail) {
                    item(
                        selected = false,
                        onClick = {
                            if (defaultExpanded) {
                                forceCollapsed = false
                            } else {
                                forceExpanded = true
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.expand)) },
                        alwaysShowLabel = false
                    )
                }
                listOf(Screen.Accounts, Screen.Transactions, Screen.Reports, Screen.Tools).forEach {
                    // Define your items (Accounts, Transactions, etc.)
                    item(
                        selected = when (it) {
                            Screen.Accounts -> navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.List
                            Screen.Transactions -> navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail
                            else -> showBottomSheetFor.value == it
                        },
                        onClick = {
                            Timber.d("Clicked on ${it.route}")
                            when (it) {
                                Screen.Accounts -> {
                                    scope.launch {
                                        navigator.navigateToRoot(ListDetailPaneScaffoldRole.List)
                                    }
                                }

                                Screen.Transactions -> {
                                    scope.launch {
                                        navigator.navigateToRoot(ListDetailPaneScaffoldRole.Detail)
                                    }
                                }

                                Screen.Reports, Screen.Tools -> {
                                    // Trigger the bottom sheet for non-pane destinations
                                    showBottomSheetFor.value = it
                                }
                            }
                        },
                        icon = { Icon(it.icon, null) },
                        label = { Text(stringResource(it.resourceId)) }
                    )
                }
            }
        ) {

            BackHandler(enabled = navigator.canNavigateBack()) {
                scope.launch {
                    navigator.navigateBack()
                }
            }

            Crossfade(
                targetState = navigator.currentDestination?.pane,
                label = "ScreenTransition"
            ) { pane ->
                when (pane) {
                    ListDetailPaneScaffoldRole.List -> {
                        AccountsScreen(
                            navigationIcon = navigationIcon,
                            accounts = accounts,
                            accountGrouping = accountGrouping.value,
                            selectedAccountId = selectedAccountId,
                            viewModel = viewModel,
                            onEvent = onAppEvent,
                            onAccountEvent = onAccountEvent,
                            flags = flags,
                            bankIcon = bankIcon
                        ) {
                            scope.launch {
                                navigator.navigateTo(
                                    pane = ListDetailPaneScaffoldRole.Detail
                                )
                            }
                        }
                    }

                    ListDetailPaneScaffoldRole.Detail -> {
                        TransactionScreen(
                            navigationIcon = navigationIcon,
                            accounts = accounts,
                            accountGrouping = accountGrouping.value,
                            availableFilters = availableFilters,
                            selectedAccountId = selectedAccountId,
                            viewModel = viewModel,
                            onEvent = onAppEvent,
                            onPrepareContextMenuItem = onPrepareContextMenuItem,
                            onPrepareMenuItem = onPrepareMenuItem,
                            pageContent = pageContent,
                            bankIcon = bankIcon
                        )
                    }

                    else -> {}
                }
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
                        leadingContent = {
                            Icon(
                                painterResource(it.icon),
                                contentDescription = null
                            )
                        },
                    )
                }
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