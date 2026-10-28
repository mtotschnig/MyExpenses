// Copy of compose/main/MainScreen.kt (MainScreenAdaptive) as starting point for the new UI.
// Changes to the new main screen go here; the original stays untouched.
package org.totschnig.myexpenses.next

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import org.totschnig.myexpenses.designsystem.ActionSheet
import org.totschnig.myexpenses.designsystem.SheetAction
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import androidx.navigation3.runtime.NavKey
import org.totschnig.myexpenses.next.core.model.transactions.ShownTransaction
import org.totschnig.myexpenses.next.core.ui.transactions.FixedTransactions
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.next.search.TransactionSearch
import org.totschnig.myexpenses.activity.HELP_VARIANT_ACCOUNTS
import org.totschnig.myexpenses.activity.HELP_VARIANT_BALANCE_SHEET
import org.totschnig.myexpenses.activity.HELP_VARIANT_PORTFOLIO
import org.totschnig.myexpenses.activity.HELP_VARIANT_TRANSACTIONS
import org.totschnig.myexpenses.activity.StartScreen
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.compose.accounts.AccountEventHandler
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.compose.isTablet
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.preference.PreferenceState
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.viewmodel.AccountsScreenTab
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel.AccountPanelState
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.PageAccount

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
fun NextMainScreen(
    viewModel: MyExpensesV2ViewModel,
    accounts: List<FullAccount>,
    allCurrencies: List<CurrencyUnit>,
    selectedAccountId: Long,
    onAppEvent: AppEventHandler,
    onAccountEvent: AccountEventHandler,
    onPrepareContextMenuItem: (itemId: Int) -> Boolean,
    onPrepareMenuItem: (itemId: Int) -> Boolean,
    bankIcon: (@Composable (Modifier, Long) -> Unit)? = null,
    adView: @Composable (MutableState<Boolean>) -> Unit,
    isNavigationVisible: Boolean,
    isCurrencyUsed: suspend (String) -> Boolean = { false },
    onCreateAsset: suspend (code: String, symbol: String, fractionDigits: Int, label: String?, commodityType: CommodityType) -> CurrencyUnit? = { _, _, _, _, _ -> null },
    pageContent: @Composable (pageAccount: PageAccount, isCurrent: Boolean) -> Unit,
    transactionList: @Composable (PageAccount, TransactionSearch) -> Unit,
    /** Lists of transactions outside of the accounts, for the pages of the areas */
    fixedTransactions: FixedTransactions,
    /** What all areas add to the details of a transaction, for the pages of the areas */
    transactionDetails: @Composable (ShownTransaction) -> Unit,
    /** What the areas add to the overview, their tabs and their pages, see [NextFeature] */
    features: List<NextFeature> = emptyList(),
) {

    LaunchedEffect(Unit) {
        viewModel.setStartFilter()
    }

    val scope = rememberCoroutineScope()

    val forcedAccountPanelLoadingState = viewModel.accountPanelState.statefulFlow
        .collectAsState(PreferenceState.Loading).value

    val preferredNavModeLoadingState = viewModel.navigationMode.statefulFlow
        .collectAsState(PreferenceState.Loading).value

    val accountGroupingLoadingState = viewModel.accountGrouping.statefulFlow
        .collectAsState(PreferenceState.Loading).value

    val mainMenuLoadingState = viewModel.mainMenuAccessor.statefulFlow
        .collectAsState(PreferenceState.Loading).value

    if (forcedAccountPanelLoadingState !is PreferenceState.Loaded ||
        preferredNavModeLoadingState !is PreferenceState.Loaded ||
        accountGroupingLoadingState !is PreferenceState.Loaded ||
        mainMenuLoadingState !is PreferenceState.Loaded
    ) return

    val forcedAccountPanelState = forcedAccountPanelLoadingState.value
    val preferredNavMode = preferredNavModeLoadingState.value.validate(isTablet)
    val accountGrouping = accountGroupingLoadingState.value

    val adaptiveInfo = currentWindowAdaptiveInfoV2()

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
        // The overview (list pane) is always the root, so that back from the account page leads there,
        // even if the app starts on the account page
        initialDestinationHistory = buildList {
            add(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List))
            if (viewModel.startScreen != StartScreen.Accounts && viewModel.startScreen != StartScreen.BalanceSheet) {
                add(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail))
            }
        },
        isDestinationHistoryAware = false
    )

    val intentEvents by viewModel.intentEvents.collectAsStateWithLifecycle(null)

    LaunchedEffect(intentEvents) {
        if (intentEvents != null) {
            navigator.navigateToRoot(ListDetailPaneScaffoldRole.Detail)
        }
    }

    val menuConfig = mainMenuLoadingState.value

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showBottomSheet by remember { mutableStateOf(false) }

    val calculatedLayoutType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
    val layoutType = navigationSuiteType(preferredNavMode, calculatedLayoutType, isNavigationVisible, isLandscape)

    // The tabs are in a bar at the bottom where the navigation suite would show a bar, else in a rail
    val useNextBar = layoutType.isBar()
    val backStack = rememberNavBackStack(NextKey.Overview)
    val navigation = remember(backStack) {
        object : NextNavigation {
            // A page on top, e.g. a contract opened from a saving tip, so that back leads to where it was opened
            override fun show(key: NextKey) {
                backStack.remove(key)
                backStack.add(key)
            }

            override fun close(key: NextKey) {
                backStack.remove(key)
            }
        }
    }
    /** Shows the page of a tab of the bar or rail on top of the overview, so that back leads to the overview */
    fun showTab(key: NextKey) {
        if (backStack.last() == key) return
        backStack.retainAll { it == NextKey.Overview }
        if (key != NextKey.Overview) backStack.add(key)
    }

    val isAdLoaded = remember { mutableStateOf(false) }

    val isWebUiActive by viewModel.isWebUiActive.collectAsState(false)

    fun MenuItem.onMenuItemClicked() {
        val tag = when (this) {
            MenuItem.WebUI -> !isWebUiActive
            MenuItem.Help -> helpVariant(viewModel, navigator.currentDestination?.pane, selectedAccountId)
            else -> null
        }
        onAppEvent(
            AppEvent.MenuItemClicked(id, tag)
        )
    }

    fun onTabClick(tab: NextTab) {
        when {
            tab == NextTab.Menu -> showBottomSheet = true
            tab == NextTab.Overview -> {
                showTab(NextKey.Overview)
                scope.launch {
                    navigator.navigateToRoot(ListDetailPaneScaffoldRole.List)
                }
            }
            else -> tab.key?.let(::showTab)
        }
    }
    val tabs = listOf(NextTab.Overview) + features.mapNotNull { it.tab } + NextTab.Menu
    val selectedTab = tabs.firstOrNull { it.selects(backStack.last()) } ?: NextTab.Overview
    val badges = features.filter { it.tab != null && it.hasNews() }.mapNotNull { it.tab }.toSet()
    val shell = NextShell(navigation, fixedTransactions, transactionDetails)

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Each page draws its background behind the status bar and keeps only its content
            // clear of it, unless an ad takes the top
            .conditional(isAdLoaded.value) {
                windowInsetsPadding(WindowInsets.statusBars)
            }
    ) {
        adView(isAdLoaded)
        Row(Modifier.weight(1f)) {
            // In wide windows, e.g. tablets and phones in landscape, the tabs are in a rail at the
            // start instead of the bar at the bottom
            if (!useNextBar) {
                NextNavigationRail(tabs, selectedTab = selectedTab, onTabClick = ::onTabClick, badges = badges)
            }
            Box(
                Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .conditional(!useNextBar) {
                        // Without the bar at the bottom, the pages keep clear of the navigation bar here
                        windowInsetsPadding(
                            WindowInsets.navigationBars.union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.End + WindowInsetsSides.Bottom)
                        )
                    }
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<NextKey.Overview> {
                            AccountPanes(
                                viewModel = viewModel,
                                navigator = navigator,
                                adaptiveInfo = adaptiveInfo,
                                useNextBar = useNextBar,
                                accounts = accounts,
                                accountGrouping = accountGrouping,
                                allCurrencies = allCurrencies,
                                selectedAccountId = selectedAccountId,
                                features = features,
                                navigation = navigation,
                                bankIcon = bankIcon,
                                onAppEvent = onAppEvent,
                                onAccountEvent = onAccountEvent,
                                onPrepareContextMenuItem = onPrepareContextMenuItem,
                                onPrepareMenuItem = onPrepareMenuItem,
                                isCurrencyUsed = isCurrencyUsed,
                                onCreateAsset = onCreateAsset,
                                pageContent = pageContent,
                                transactionList = transactionList
                            )
                        }
                        features.forEach { with(it) { pages(shell) } }
                    }
                )
            }
        }
        if (useNextBar) {
            NextBottomBar(tabs, selectedTab = selectedTab, badges = badges, onTabClick = ::onTabClick)
        }
    }

    if (showBottomSheet) {
        MenuSheet(
            items = menuConfig,
            isWebUiActive = isWebUiActive,
            onClick = { it.onMenuItemClicked() },
            onDismiss = { showBottomSheet = false }
        )
    }
}

/** The variant of the help for the shown pane */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun helpVariant(viewModel: MyExpensesV2ViewModel, pane: ThreePaneScaffoldRole?, selectedAccountId: Long) =
    when (pane) {
        ListDetailPaneScaffoldRole.List ->
            if (viewModel.currentAccountsTab.value == AccountsScreenTab.BALANCE_SHEET) HELP_VARIANT_BALANCE_SHEET
            else HELP_VARIANT_ACCOUNTS
        ListDetailPaneScaffoldRole.Detail ->
            if ((viewModel.accountList.value.find { it.id == selectedAccountId } as? FullAccount)?.isPortfolio == true) HELP_VARIANT_PORTFOLIO
            else HELP_VARIANT_TRANSACTIONS
        else -> null
    }

/** The overview and the account page, side by side on wide screens */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AccountPanes(
    viewModel: MyExpensesV2ViewModel,
    navigator: ThreePaneScaffoldNavigator<FullAccount>,
    adaptiveInfo: WindowAdaptiveInfo,
    useNextBar: Boolean,
    accounts: List<FullAccount>,
    accountGrouping: AccountGrouping<*>,
    allCurrencies: List<CurrencyUnit>,
    selectedAccountId: Long,
    features: List<NextFeature>,
    navigation: NextNavigation,
    bankIcon: (@Composable (Modifier, Long) -> Unit)?,
    onAppEvent: AppEventHandler,
    onAccountEvent: AccountEventHandler,
    onPrepareContextMenuItem: (itemId: Int) -> Boolean,
    onPrepareMenuItem: (itemId: Int) -> Boolean,
    isCurrencyUsed: suspend (String) -> Boolean,
    onCreateAsset: suspend (code: String, symbol: String, fractionDigits: Int, label: String?, commodityType: CommodityType) -> CurrencyUnit?,
    pageContent: @Composable (pageAccount: PageAccount, isCurrent: Boolean) -> Unit,
    transactionList: @Composable (PageAccount, TransactionSearch) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val is2Pane = navigator.scaffoldDirective.maxHorizontalPartitions > 1
    val fontScale = LocalDensity.current.fontScale
    BackHandler(enabled = !is2Pane && navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
        }
    }

    // The rail and the box around the pages keep clear of the other sides
    val customInsets = with(ScaffoldDefaults.contentWindowInsets) {
        if (useNextBar) only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top) else only(WindowInsetsSides.Top)
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            Surface {
                AnimatedPane {
                    OverviewPane(
                        viewModel = viewModel,
                        accounts = accounts,
                        accountGrouping = accountGrouping,
                        features = features,
                        navigation = navigation,
                        bankIcon = bankIcon,
                        onAppEvent = onAppEvent,
                        onAccountClick = {
                            viewModel.selectAccount(it.id)
                            scope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                            }
                        }
                    )
                }
            }
        },
        detailPane = {
            Surface {
                AnimatedPane {
                    NextAccountScreen(
                        viewModel = viewModel,
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        visibleActionItems = visibleActionItems(
                            isExpanded = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
                                WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                            ),
                            isMedium = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
                                WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                            ),
                            is2Pane = is2Pane,
                            fontScale = fontScale
                        ),
                        onEvent = onAppEvent,
                        onAccountEvent = onAccountEvent,
                        onPrepareContextMenuItem = onPrepareContextMenuItem,
                        onPrepareMenuItem = onPrepareMenuItem,
                        pageContent = pageContent,
                        transactionList = transactionList,
                        allCurrencies = allCurrencies,
                        isCurrencyUsed = isCurrencyUsed,
                        onCreateAsset = onCreateAsset,
                        windowInsets = with(customInsets) {
                            if (is2Pane) only(WindowInsetsSides.Vertical + WindowInsetsSides.End) else this
                        },
                        bankIcon = bankIcon,
                        onBack = if (is2Pane) null else {
                            { scope.launch { navigator.navigateToRoot(ListDetailPaneScaffoldRole.List) } }
                        }
                    )
                }
            }
        }
    )
}

/** The overview with what the areas add to it */
@Composable
private fun OverviewPane(
    viewModel: MyExpensesV2ViewModel,
    accounts: List<FullAccount>,
    accountGrouping: AccountGrouping<*>,
    features: List<NextFeature>,
    navigation: NextNavigation,
    bankIcon: (@Composable (Modifier, Long) -> Unit)?,
    onAppEvent: AppEventHandler,
    onAccountClick: (OverviewAccount) -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.setLastVisited(StartScreen.Accounts)
    }
    val context = LocalContext.current
    val format = LocalCurrencyFormatter.current
    val resources = LocalResources.current
    Box(Modifier.fillMaxSize()) {
        NextOverviewScreen(
            header = { modifier ->
                Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column { features.forEach { it.OverviewBanner(navigation) } }
                    features.forEach { it.OverviewCard(navigation) }
                }
            },
            budgets = features.mapNotNull { it.overviewBudgets(navigation) }.firstOrNull(),
            sections = overviewSections(
                accounts.filter { it.visible }, accountGrouping, context, resources
            ) { amount, currency -> format.convAmount(amount, currency) },
            bankIcon = bankIcon,
            onAddAccount = { onAppEvent(AppEvent.CreateAccount) },
            onReorderAccounts = { shownIds ->
                // Hidden accounts are not shown in the overview, they keep their relative order behind the shown ones
                val hiddenIds = accounts.filter { !it.visible }.sortedBy { it.sortKey }.map { it.id }
                viewModel.sortAccounts((shownIds + hiddenIds).toLongArray())
            },
            onAccountClick = onAccountClick
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) { features.forEach { it.OverviewAction() } }
    }
}


/**
 * A page that fills the status bar with its [color], so that the color reaches the top of the
 * screen, while its content stays below the status bar
 */
@Composable
internal fun BehindStatusBar(color: Color, content: @Composable () -> Unit) {
    Surface(color = color) {
        Box(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            content()
        }
    }
}

/** The items of the menu, behind the tab "Menu" of the bar or rail */
@Composable
private fun MenuSheet(
    items: List<MenuItem>,
    isWebUiActive: Boolean,
    onClick: (MenuItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    ActionSheet(
        actions = items.map { item ->
            SheetAction(
                label = item.getLabel(context),
                icon = {
                    if (item == MenuItem.WebUI) rememberVectorPainter(
                        if (isWebUiActive) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank
                    ) else item.painter
                },
                testTag = item.testTag,
                // The web UI is switched on and off, so it is announced as a checkbox
                checked = isWebUiActive.takeIf { item == MenuItem.WebUI },
                onClick = { onClick(item) }
            )
        },
        onDismiss = onDismiss
    )
}

/** The pages of the new design, the overview with the accounts at the bottom of the back stack; each area adds the keys of its pages here */
@Serializable
sealed interface NextKey : NavKey {
    @Serializable
    data object Overview : NextKey
}
