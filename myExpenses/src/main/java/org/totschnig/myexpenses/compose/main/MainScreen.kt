package org.totschnig.myexpenses.compose.main

import androidx.activity.compose.BackHandler
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.StartScreen
import org.totschnig.myexpenses.compose.accounts.AccountEventHandler
import org.totschnig.myexpenses.compose.accounts.AccountsScreen
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.compose.transactions.Action
import org.totschnig.myexpenses.compose.transactions.TransactionScreen
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.TransactionSort
import org.totschnig.myexpenses.preference.PreferenceState
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel.AccountPanelState
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.PageAccount

sealed class Screen(
    val icon: ImageVector,
    @param:StringRes val resourceId: Int,
    val paneRole: ThreePaneScaffoldRole,
) {
    object Accounts : Screen(
        icon = Icons.Default.AccountBalance,
        resourceId = R.string.accounts,
        paneRole = ListDetailPaneScaffoldRole.List
    )

    object Transactions : Screen(
        icon = Icons.AutoMirrored.Default.ReceiptLong,
        resourceId = R.string.import_select_transactions,
        paneRole = ListDetailPaneScaffoldRole.Detail
    )
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
    data class MenuItemClicked(@param:IdRes val itemId: Int, val tag: Any? = null) : AppEvent()
    object Sort : AppEvent()
    data class CopyToClipBoard(val text: String) : AppEvent()
}

interface AppEventHandler {
    operator fun invoke(event: AppEvent)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
suspend fun ThreePaneScaffoldNavigator<*>.navigateToRoot(pane: ThreePaneScaffoldRole) {
    if (currentDestination?.pane == pane) return
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
    adView: @Composable () -> Unit,
    pageContent: @Composable (pageAccount: PageAccount, isCurrent: Boolean) -> Unit,
) {

    LaunchedEffect(Unit) {
        viewModel.setStartFilter()
    }

    val scope = rememberCoroutineScope()

    val forcedAccountPanelLoadingState = viewModel.accountPanelState.statefulFlow
        .collectAsState(PreferenceState.Loading).value

    if (forcedAccountPanelLoadingState !is PreferenceState.Loaded) return

    val forcedAccountPanelState = forcedAccountPanelLoadingState.value

    val adaptiveInfo = currentWindowAdaptiveInfo()

    val defaultDirective = calculatePaneScaffoldDirective(adaptiveInfo)
    val defaultIs2Pane = defaultDirective.maxHorizontalPartitions > 1

    val navigator = rememberListDetailPaneScaffoldNavigator<FullAccount>(
        scaffoldDirective = defaultDirective.run {
            copy(
                horizontalPartitionSpacerSize = 12.dp,
                maxHorizontalPartitions = when (forcedAccountPanelState) {
                    AccountPanelState.EXPANDED -> 2
                    AccountPanelState.COLLAPSED -> 1
                    AccountPanelState.DEFAULT -> maxHorizontalPartitions
                },
            )
        },
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

    val menuConfig = viewModel.mainMenu.collectAsState()

    val accountGrouping = viewModel.accountGrouping.asState()

    val is2Pane = navigator.scaffoldDirective.maxHorizontalPartitions > 1

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val layoutType =
        NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
    val isRail = layoutType.isRail()
    val context = LocalContext.current

    val maxQuickItems = if (layoutType == NavigationSuiteType.ShortNavigationBarMedium) 2 else 1

    val quickItems = menuConfig.value.take(maxQuickItems)
    val overflowItems = menuConfig.value.drop(maxQuickItems)

    val isWebUiActive by viewModel.isWebUiActive.collectAsState(false)
    Column {
        adView()
        NavigationSuiteScaffold(
            layoutType = layoutType,
            navigationSuiteItems = {
                if (!is2Pane) {
                    listOfNotNull(
                        Screen.Accounts,
                        Screen.Transactions.takeIf { accounts.isNotEmpty() }
                    ).forEach { screen ->
                        item(
                            selected = navigator.currentDestination?.pane == screen.paneRole,
                            onClick = {
                                scope.launch {
                                    // Use your navigateToRoot extension to prevent backstack bloat
                                    navigator.navigateToRoot(screen.paneRole)
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = {
                                Text(
                                    text = stringResource(screen.resourceId),
                                    maxLines = 1
                                )
                            },
                        )
                    }
                }

                (if (isRail) menuConfig.value else quickItems).forEach {
                    item(
                        selected = if (it == MenuItem.WebUI) isWebUiActive else false,
                        onClick = {
                            onAppEvent(
                                AppEvent.MenuItemClicked(
                                    it.id,
                                    if (it == MenuItem.WebUI) !isWebUiActive else null
                                )
                            )
                        },
                        icon = { Icon(it.painter, null) },
                        label = {
                            Text(
                                text = it.getLabel(context),
                                maxLines = 1
                            )
                        },
                    )
                }
                if (layoutType.isBar()) {

                    if (overflowItems.isNotEmpty()) {
                        item(
                            selected = showBottomSheet,
                            onClick = { showBottomSheet = true },
                            icon = { Icon(Icons.Default.MoreHoriz, null) },
                            label = {
                                Text(
                                    stringResource(com.android.setupwizardlib.R.string.suw_more_button_label),
                                    maxLines = 1
                                )
                            },
                        )
                    }
                }
            }
        ) {

            BackHandler(enabled = !is2Pane && navigator.canNavigateBack()) {
                scope.launch {
                    navigator.navigateBack()
                }
            }

            val customInsets = with(ScaffoldDefaults.contentWindowInsets) {
                if (layoutType.isBar()) only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top) else this
            }

            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                listPane = {
                    PaneSurface(
                        isRail = isRail,
                        extraPadding = !is2Pane
                    ) {
                        AnimatedPane {
                            AccountsScreen(
                                containerColor = Color.Transparent,
                                accounts = accounts,
                                accountGrouping = accountGrouping.value,
                                selectedAccountId = selectedAccountId,
                                viewModel = viewModel,
                                onEvent = onAppEvent,
                                onAccountEvent = onAccountEvent,
                                flags = flags,
                                bankIcon = bankIcon,
                                windowInsets = customInsets,
                                isFullScreen = !is2Pane,
                                onToggleFullScreen = if (adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
                                        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                                    )
                                ) {
                                    {
                                        scope.launch {
                                            viewModel.accountPanelState.set(
                                                when {
                                                    is2Pane && defaultIs2Pane -> AccountPanelState.COLLAPSED
                                                    !is2Pane && !defaultIs2Pane -> AccountPanelState.EXPANDED
                                                    else -> AccountPanelState.DEFAULT
                                                }
                                            )
                                        }
                                    }
                                } else null
                            ) {
                                scope.launch {
                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                }
                            }
                        }
                    }
                },
                detailPane = {

                    PaneSurface(
                        isRail = isRail,
                        extraPadding = true
                    ) {
                        AnimatedPane {
                            val fontScale = LocalDensity.current.fontScale
                            TransactionScreen(
                                containerColor = Color.Transparent,
                                accounts = accounts,
                                accountGrouping = accountGrouping.value,
                                availableFilters = availableFilters,
                                selectedAccountId = selectedAccountId,
                                viewModel = viewModel,
                                onEvent = onAppEvent,
                                onPrepareContextMenuItem = onPrepareContextMenuItem,
                                onPrepareMenuItem = onPrepareMenuItem,
                                pageContent = pageContent,
                                bankIcon = bankIcon,
                                visibleActionItems = when {
                                    adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
                                        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                                    ) -> if (is2Pane) 4 else 6

                                    adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
                                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                                    ) && !is2Pane -> 4

                                    else -> when {
                                        fontScale > 1.5f -> 0
                                        fontScale > 1.1f -> 1
                                        else -> 2
                                    }
                                },
                                windowInsets = with(customInsets) {
                                    if (is2Pane) only(WindowInsetsSides.Vertical + WindowInsetsSides.End) else this
                                },
                                isFramed = isRail
                            )
                        }
                    }
                }
            )
        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            contentWindowInsets = { WindowInsets.navigationBars }
        ) {

            overflowItems
                .filter { onPrepareMenuItem(it.id) }
                .forEach {
                    ListItem(
                        modifier = Modifier.clickable {
                            showBottomSheet = false
                            onAppEvent(
                                AppEvent.MenuItemClicked(
                                    it.id,
                                    if (it == MenuItem.WebUI) !isWebUiActive else null
                                )
                            )
                        },
                        headlineContent = { Text(it.getLabel(LocalContext.current)) },
                        leadingContent = {
                            Icon(
                                if (it == MenuItem.WebUI) {
                                    rememberVectorPainter(
                                        if (isWebUiActive) Icons.Filled.CheckBox
                                        else Icons.Filled.CheckBoxOutlineBlank
                                    )
                                } else it.painter,
                                contentDescription = null
                            )
                        },
                    )
                }
        }
    }
}

private fun NavigationSuiteType.isBar(): Boolean {
    return this == NavigationSuiteType.NavigationBar ||
            this == NavigationSuiteType.ShortNavigationBarMedium ||
            this == NavigationSuiteType.ShortNavigationBarCompact
}

private fun NavigationSuiteType.isRail(): Boolean {
    return this == NavigationSuiteType.NavigationRail ||
            this == NavigationSuiteType.WideNavigationRailCollapsed ||
            this == NavigationSuiteType.WideNavigationRailExpanded
}


@Composable
private fun PaneSurface(
    isRail: Boolean,
    extraPadding: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .conditional(isRail) {
                conditional(extraPadding) { padding(end = 8.dp) }
                    .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Vertical))
            },
        shape = if (isRail) RoundedCornerShape(24.dp) else RectangleShape,
        border = if (isRail) {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        } else null,
        content = content
    )
}