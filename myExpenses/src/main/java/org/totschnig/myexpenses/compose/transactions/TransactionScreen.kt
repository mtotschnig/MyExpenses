package org.totschnig.myexpenses.compose.transactions

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.StartScreen
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.OverFlowMenu
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.compose.TooltipIconButton
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.compose.accounts.AccountSummaryV2
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.compose.main.MyBottomAppBar
import org.totschnig.myexpenses.compose.main.parseMenu
import org.totschnig.myexpenses.compose.main.rememberCollapsingTabRowState
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.TransactionSort
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.data.AggregateAccount
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.PageAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    navigationIcon: @Composable (() -> Unit) = {},
    accounts: List<FullAccount>,
    accountGrouping: AccountGrouping<*>,
    availableFilters: List<AccountGroupingKey>,
    viewModel: MyExpensesV2ViewModel,
    navController: NavController,
    onEvent: AppEventHandler,
    onPrepareMenuItem: (itemId: Int, accountCount: Int) -> Boolean,
    pageContent: @Composable ((pageAccount: PageAccount, accountCount: Int) -> Unit),
) {
    LaunchedEffect(Unit) {
        viewModel.setLastVisited(StartScreen.Transactions)
    }

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
            val filteredByGroupFilter =
                if (activeFilter == null || accountGrouping == AccountGrouping.NONE)
                    accounts
                else
                    accounts.filter { account -> accountGrouping.getGroupKey(account) == activeFilter }
            val aggregateAccountGrouping =
                if (activeFilter != null) accountGrouping else AccountGrouping.NONE
            //if we group by flag, and filter by a given flag,
            // we want to show all accounts with that flag ignoring visibility
            val filteredByVisibility =
                if (accountGrouping == AccountGrouping.FLAG && activeFilter != null)
                    filteredByGroupFilter
                else
                    filteredByGroupFilter.filter { it.visible }
            if (filteredByGroupFilter.size < 2)
                filteredByVisibility
            else filteredByVisibility + AggregateAccount(
                currencyUnit = activeFilter as? CurrencyUnit
                    ?: viewModel.currencyContext.homeCurrencyUnit,
                type = if (accountGrouping == AccountGrouping.TYPE) activeFilter as? AccountType else null,
                flag = if (accountGrouping == AccountGrouping.FLAG) activeFilter as? AccountFlag else null,
                grouping = aggregateTransactionGrouping,
                accountGrouping = aggregateAccountGrouping,
                sortBy = aggregateTransactionSort.column,
                sortDirection = aggregateTransactionSort.sortDirection,
                equivalentOpeningBalance = filteredByGroupFilter.sumOf { it.equivalentOpeningBalance },
                equivalentCurrentBalance = filteredByGroupFilter.sumOf { it.equivalentCurrentBalance },
                equivalentSumIncome = filteredByGroupFilter.sumOf { it.equivalentSumIncome },
                equivalentSumExpense = filteredByGroupFilter.sumOf { it.equivalentSumExpense },
                equivalentSumTransfer = filteredByGroupFilter.sumOf { it.equivalentSumTransfer },
                equivalentTotal = filteredByGroupFilter.sumOf {
                    it.equivalentTotal ?: it.equivalentCurrentBalance
                },
            ).let { aggregateAccount ->
                if (aggregateAccountGrouping == AccountGrouping.CURRENCY) aggregateAccount.copy(
                    openingBalance = filteredByGroupFilter.sumOf { it.openingBalance },
                    currentBalance = filteredByGroupFilter.sumOf { it.currentBalance },
                    sumIncome = filteredByGroupFilter.sumOf { it.sumIncome },
                    sumExpense = filteredByGroupFilter.sumOf { it.sumExpense },
                    sumTransfer = filteredByGroupFilter.sumOf { it.sumTransfer },
                    total = filteredByGroupFilter.sumOf { it.total ?: it.currentBalance },
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
            val isInSelectionMode = viewModel.selectionState.value.isNotEmpty()
            Box(modifier = Modifier.fillMaxWidth()) {
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
                            MenuItem.Search,
                            MenuItem.Sort,
                            if (currentAccount.value.sortBy == KEY_DATE) MenuItem.Grouping else null,
                        )
                        menu.forEach { item ->
                            when (item) {
                                MenuItem.Search -> TooltipIconButton(
                                    tooltip = stringResource(R.string.menu_search),
                                    imageVector = Icons.Default.Search
                                ) { onEvent(AppEvent.Search) }

                                MenuItem.Sort -> TransactionSortMenu(currentAccount.value) {
                                    onEvent(AppEvent.SetTransactionSort(it))
                                }

                                MenuItem.Grouping -> TransactionGroupingMenu(
                                    currentGroup = currentAccount.value.grouping
                                ) {
                                    onEvent(AppEvent.SetTransactionGrouping(it))
                                }

                                else -> {}
                            }
                        }
                    },
                )
                AnimatedVisibility(
                    visible = isInSelectionMode,
                    enter = scaleIn(transformOrigin = TransformOrigin.Center) + fadeIn(),
                    exit = scaleOut(transformOrigin = TransformOrigin.Center) + fadeOut()
                ) {
                    BackHandler {
                        viewModel.clearSelection()
                    }
                    val context = LocalContext.current
                    TopAppBar(
                        navigationIcon = {
                            TooltipIconButton(
                                tooltip = stringResource(R.string.menu_close),
                                imageVector = Icons.AutoMirrored.Default.ArrowBack
                            ) { viewModel.clearSelection() }
                        },
                        title = {
                            ColoredAmountText(
                                prefix = "${viewModel.selectionState.value.size}  (Σ: ",
                                amount = viewModel.selectedTransactionSum,
                                currency = currentAccount.value.currencyUnit,
                                postfix = ")",
                                colorFix = false
                            )
                        },
                        actions = {
                            OverFlowMenu(
                                menu = remember(viewModel.selectionState.value.size) {
                                    parseMenu(
                                        context = context,
                                        menuRes = R.menu.transactionlist_context,
                                        onPrepareMenuItem = {
                                            onPrepareMenuItem(
                                                it,
                                                accountList.size
                                            )
                                        }
                                    ) {
                                        onEvent(AppEvent.ContextMenuItemClicked(it))
                                    }
                                }
                            )
                        }
                    )
                }
            }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface)
                        .testTag(TEST_TAG_PAGER)
                        .semantics {
                            collectionInfo = CollectionInfo(1, accounts.size)
                        },
                    state = pagerState,
                    pageSpacing = 10.dp,
                    key = { pageIndex ->
                        "${accountGrouping.name}_${activeFilter?.id}_${accountList[pageIndex].id}"
                    },
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    val account = accountList[page]
                    val context = LocalContext.current
                    val pageAccount = remember(account) { account.toPageAccount(context = context) }
                    pageContent(pageAccount, accounts.size)
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
private fun FloatingActionToolbar(
    onNewTransaction: (type: Int, isIncome: Boolean) -> Unit,
    modifier: Modifier = Modifier.Companion,
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
                onClick = {
                    onNewTransaction(
                        TransactionsContract.Transactions.TYPE_TRANSACTION,
                        true
                    )
                },
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
                    menu = listOf(
                        MenuEntry(
                            icon = { painterResource(R.drawable.ic_expense) },
                            tint = Color.Unspecified,
                            label = UiText.StringResource(
                                R.string.expense
                            ),
                            command = "Expense",
                            action = {
                                onNewTransaction(
                                    TransactionsContract.Transactions.TYPE_TRANSACTION,
                                    false
                                )
                            }
                        ),
                        MenuEntry(
                            icon = Icons.Default.Add,
                            tint = LocalColors.current.income,
                            label = R.string.income,
                            command = "Income",
                            action = {
                                onNewTransaction(
                                    TransactionsContract.Transactions.TYPE_TRANSACTION,
                                    true
                                )
                            }
                        ),
                        MenuEntry(
                            icon = { painterResource(R.drawable.ic_menu_forward) },
                            label = UiText.StringResource(
                                R.string.transfer
                            ),
                            command = "Transfer",
                            action = {
                                onNewTransaction(
                                    TransactionsContract.Transactions.TYPE_TRANSFER,
                                    false
                                )
                            }
                        ),
                        MenuEntry(
                            icon = { painterResource(R.drawable.ic_menu_split) },
                            label = UiText.StringResource(
                                R.string.split_transaction
                            ),
                            command = "Split transaction",
                            action = {
                                onNewTransaction(
                                    TransactionsContract.Transactions.TYPE_SPLIT,
                                    false
                                )
                            }
                        ),
                    )
                )
            }
        }
    }
}

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
    modifier: Modifier = Modifier.Companion,
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