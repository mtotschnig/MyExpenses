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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
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
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.StartScreen
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.OverFlowMenu
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.compose.TooltipIconButton
import org.totschnig.myexpenses.compose.accounts.AccountIndicator
import org.totschnig.myexpenses.compose.accounts.AccountSummaryV2
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.compose.main.parseMenu
import org.totschnig.myexpenses.compose.main.rememberCollapsingTabRowState
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
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
    selectedAccountId: Long,
    viewModel: MyExpensesV2ViewModel,
    bottomBar: @Composable () -> Unit,
    onEvent: AppEventHandler,
    onPrepareContextMenuItem: (Int) -> Boolean,
    onPrepareMenuItem: (Int) -> Boolean,
    bankIcon: (@Composable (Modifier, Long) -> Unit)? = null,
    pageContent: @Composable (PageAccount) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.setLastVisited(StartScreen.Transactions)
    }

    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val tabRowState = rememberCollapsingTabRowState()

    val accountList = viewModel.accountList.collectAsState(emptyList()).value.takeIf {
        it.isNotEmpty()
    } ?: return

    val pagerState = rememberPagerState(pageCount = { accountList.size })

    LaunchedEffect(accountList.size) {
        if (pagerState.currentPage >= accountList.size) {
            pagerState.scrollToPage(accountList.lastIndex)
        }
    }

    val currentAccount by remember(accountList) {
        derivedStateOf {
            if (accountList.size > 1) {
                accountList[pagerState.settledPage.coerceIn(0, accountList.lastIndex)]
            } else {
                accountList.first()
            }
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
                            currentAccount = currentAccount,
                            displayBalanceType = selectedBalanceType,
                            onDisplayBalanceTypeChange = { newType ->
                                selectedBalanceType = newType
                            },
                            bankIcon = bankIcon
                        )
                    },
                    actions = {

                        TooltipIconButton(
                            tooltip = stringResource(R.string.menu_search),
                            imageVector = Icons.Default.Search
                        ) { onEvent(AppEvent.Search) }
                        ViewOptionsMenu(
                            currentAccount = currentAccount,
                            onEvent = onEvent
                        )
                        ActionMenu(
                            onEvent = onEvent,
                            onPrepareMenuItem = onPrepareMenuItem
                        )
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
                                currency = currentAccount.currencyUnit,
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
                                        onPrepareMenuItem = onPrepareContextMenuItem
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
        bottomBar = bottomBar
    ) { paddingValues ->

        if (accountList.size > 1) {
            LaunchedEffect(selectedAccountId) {
                val currentPage =
                    accountList.indexOfFirst { it.id == selectedAccountId }
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
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (accountList.size > 1) {
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
                            accountList.getOrNull(pageIndex)?.id ?: pageIndex                        },
                        verticalAlignment = Alignment.Top,
                    ) { page ->
                        TransactionListPage(accountList[page], pageContent)
                    }
                }
            } else {
                TransactionListPage(accountList.first(), pageContent)
            }

            val scope = rememberCoroutineScope()

            FloatingActionToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                lastAction = viewModel.lastAction.flow.collectAsState(Action.Expense).value
            ) { action ->

                scope.launch {
                    viewModel.lastAction.set(action)
                }

                onEvent(
                    AppEvent.CreateTransaction(
                        action = action,
                        transferEnabled = accounts.size > 1
                    )
                )
            }
        }
    }
}

@Composable
private fun TransactionListPage(
    account: BaseAccount,
    pageContent: @Composable (PageAccount) -> Unit
) {
    val context = LocalContext.current
    val pageAccount = remember(account) { account.toPageAccount(context = context) }
    pageContent(pageAccount)
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
    modifier: Modifier = Modifier,
    bankIcon: (@Composable (Modifier, Long) -> Unit)? = null,
    onDisplayBalanceTypeChange: (BalanceType) -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                (currentAccount as? FullAccount)?.let {
                    AccountIndicator(12.dp, currentAccount, bankIcon)
                }
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
