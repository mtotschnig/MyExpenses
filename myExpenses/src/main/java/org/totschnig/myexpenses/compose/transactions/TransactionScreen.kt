package org.totschnig.myexpenses.compose.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.StartScreen
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.compose.OverFlowMenu
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.compose.TooltipIconButton
import org.totschnig.myexpenses.compose.accounts.AccountIndicator
import org.totschnig.myexpenses.compose.accounts.AccountSummaryV2
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.compose.main.getBalanceContentDescription
import org.totschnig.myexpenses.compose.main.balanceForType
import org.totschnig.myexpenses.compose.main.icon
import org.totschnig.myexpenses.compose.main.parseMenu
import org.totschnig.myexpenses.compose.main.rememberCollapsingTabRowState
import org.totschnig.myexpenses.compose.main.validatedBalanceType
import org.totschnig.myexpenses.compose.optional
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.BalanceType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import timber.log.Timber
import kotlin.math.absoluteValue

enum class FabStyle {
    Standard, Compact
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    containerColor: Color = MaterialTheme.colorScheme.background,
    accounts: List<FullAccount>,
    availableFilters: List<AccountGroupingKey>,
    selectedAccountId: Long,
    viewModel: MyExpensesV2ViewModel,
    bottomBar: @Composable () -> Unit = {},
    visibleActionItems: Int,
    onEvent: AppEventHandler,
    onPrepareContextMenuItem: (Int) -> Boolean,
    onPrepareMenuItem: (Int) -> Boolean,
    bankIcon: (@Composable (Modifier, Long) -> Unit)? = null,
    pageContent: @Composable (PageAccount, Boolean) -> Unit,
    windowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    isFramed: Boolean,
    navigationIcon: @Composable () -> Unit = {},
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

    val initialIndex = remember(accountList, selectedAccountId) {
        accountList.indexOfFirst { it.id == selectedAccountId }
            .takeIf { it != -1 } ?: 0
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { accountList.size }
    )

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
    val accountColor = Color(currentAccount.color(LocalResources.current))

    Scaffold(
        contentWindowInsets = windowInsets,
        containerColor = containerColor,
        topBar = {
            val isInSelectionMode = viewModel.selectionState.value.isNotEmpty()
            val height = 52.dp  + 30.dp * (LocalDensity.current.fontScale -1)
            Crossfade(
                targetState = isInSelectionMode,
                label = "TopBarTransition"
            ) { selectionMode ->
                if (selectionMode) {
                    BackHandler {
                        viewModel.clearSelection()
                    }
                    val context = LocalContext.current
                    TopAppBar(
                        modifier = Modifier.height(height),
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
                } else {
                    @Composable
                    fun isChecked(menuItem: MenuItem): Boolean = when (menuItem) {
                        MenuItem.Search -> viewModel.filterPersistence.getValue(selectedAccountId)
                            .whereFilter
                            .collectAsState(null).value != null

                        MenuItem.ShowStatusHandle -> viewModel.showStatusHandle.flow.collectAsState(
                            initial = false
                        ).value

                        else -> false
                    }

                    TopAppBar(
                        modifier = Modifier.height(height),
                        navigationIcon = navigationIcon,
                        title = {
                            BalanceHeader(
                                modifier = Modifier.conditional(isFramed) {
                                    padding(start = 4.dp, top = 4.dp)
                                },
                                currentAccount = currentAccount,
                                onDisplayBalanceTypeChange = { newType ->
                                    viewModel.persistBalanceType(newType)
                                },
                                onCopyBalance = {
                                    onEvent(AppEvent.CopyToClipBoard(it))
                                },
                                onSetNewBalance = {
                                    onEvent(AppEvent.MenuItemClicked(R.id.NEW_BALANCE_COMMAND))
                                },
                                bankIcon = bankIcon
                            )
                        },
                        actions = {
                            val menuConfig = viewModel.transactionScreenMenu.collectAsState()

                            val filteredItems = menuConfig.value.filter {
                                onPrepareMenuItem(it.id)
                            }

                            //no need to show overflow menu if there is only one item
                            val quickItems = if (filteredItems.size > 1) filteredItems.take(visibleActionItems) else filteredItems
                            val overflowItems = filteredItems - quickItems.toSet()

                            quickItems.forEach {
                                if (it == MenuItem.Tune) {
                                    ViewOptionsMenu(
                                        currentAccount = currentAccount,
                                        onEvent = onEvent
                                    )
                                } else {
                                    val isChecked = if (it.isCheckable) isChecked(it) else null
                                    TooltipIconButton(
                                        tooltip = it.getLabel(LocalContext.current),
                                        painter = it.painter,
                                        isChecked = isChecked == true
                                    ) { onEvent(AppEvent.MenuItemClicked(it.id, isChecked?.not())) }
                                }
                            }

                            ActionMenu(
                                currentAccount = currentAccount,
                                items = overflowItems,
                                onEvent = onEvent,
                                isChecked = ::isChecked
                            )
                        },
                    )
                }
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            val scope = rememberCoroutineScope()

            if (currentAccount is FullAccount && (currentAccount as FullAccount).sealed) {
                FloatingActionButton(
                    onClick = { },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Icon(Icons.Default.Lock, stringResource(R.string.account_closed))
                }
            } else {
                FloatingActionButtonMenu(
                    lastAction = viewModel.lastAction.flow.collectAsState(Action.Expense).value,
                    isStandard = viewModel.fabStyle.collectAsState(FabStyle.Standard).value == FabStyle.Standard,
                    containerColor = accountColor,
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
                if (selected != selectedAccountId) {
                    viewModel.selectAccount(selected)
                    viewModel.scrollToAccountIfNeeded(
                        pagerState.currentPage,
                        selected,
                        true
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(tabRowState.nestedScrollConnection)
        ) {
            if (availableFilters.size > 1 || accountList.size > 1) {
                Row(
                    modifier = Modifier
                        .optional(tabRowState.heightPx) {
                            height(with(LocalDensity.current) { it.toDp() })
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (availableFilters.size > 1) {
                        AccountFilterMenu(
                            activeFilter = activeFilter,
                            availableFilters = availableFilters,
                            onFilterChange = viewModel::setFilter,
                        )
                    }
                    val selectedTabIndex =
                        pagerState.currentPage.coerceAtMost(accountList.lastIndex)
                    Timber.d("selectedTabIndex: $selectedTabIndex")
                    SecondaryScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier
                            .onSizeChanged { size ->
                                if (size.height > (tabRowState.maxHeightPx ?: 0f)) {
                                    tabRowState.maxHeightPx = size.height.toFloat()
                                }
                            },
                        edgePadding = 0.dp,
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(
                                    selectedTabIndex,
                                    matchContentSize = false
                                ),
                                color = accountColor
                            )
                        }
                    ) {
                        val density = LocalDensity.current
                        val maxTabWidth = with(density) { MaterialTheme.typography.labelLarge.fontSize.toDp() } * 10
                        Timber.d("maxTabWidth: $maxTabWidth")
                        accountList.forEachIndexed { index, account ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                modifier = Modifier.height(40.dp),
                                text = {
                                    Text(
                                        modifier = Modifier.widthIn(max = maxTabWidth),
                                        text = account.labelV2(LocalContext.current),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
            if (accountList.size > 1) {
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TEST_TAG_PAGER)
                        .semantics {
                            collectionInfo = CollectionInfo(1, accounts.size)
                        },
                    state = pagerState,
                    pageSpacing = 10.dp,
                    key = { pageIndex ->
                        accountList.getOrNull(pageIndex)?.id ?: pageIndex
                    },
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    val isCurrentPage = pagerState.currentPage == page
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset =
                                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                // Apply a slight shadow or dimming to non-focused pages
                                alpha = (1f - pageOffset.absoluteValue).coerceIn(0f, 1f)
                            }
                    ) {
                        TransactionListPage(accountList[page], isCurrentPage, pageContent)
                    }
                }
            } else {
                TransactionListPage(accountList.first(), true, pageContent)
            }
        }
    }
}

@Composable
private fun TransactionListPage(
    account: BaseAccount,
    isCurrent: Boolean,
    pageContent: @Composable (PageAccount, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val pageAccount = remember(account) { account.toPageAccount(context = context) }
    pageContent(pageAccount, isCurrent)
}

@Composable
private fun BalanceHeader(
    currentAccount: BaseAccount,
    modifier: Modifier = Modifier,
    bankIcon: (@Composable (Modifier, Long) -> Unit)? = null,
    onDisplayBalanceTypeChange: (BalanceType) -> Unit = {},
    onCopyBalance: (String) -> Unit = {},
    onSetNewBalance: () -> Unit = {},
) {
    var isSummaryPopupVisible by rememberSaveable { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isSummaryPopupVisible) 0F else 180F
    )

    val displayBalance = currentAccount.balanceForType

    Row(
        modifier = modifier
            .semantics {
                role = Role.Button
            }
            .clickable(onClickLabel = stringResource(R.string.content_description_show_balance_details)) {
                isSummaryPopupVisible = !isSummaryPopupVisible
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        (currentAccount as? FullAccount)?.let {
            AccountIndicator(currentAccount, bankIcon, true)
        }
        BoxWithConstraints(
            Modifier
                .weight(1f, fill = false)
                .padding(end = 8.dp)
        ) {

            val isWideLayout = maxWidth > 300.dp

            // Adaptive Content: Switch between Column and Row
            if (isWideLayout) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccountLabel(currentAccount, Modifier.weight(1f, fill = false))
                    BalanceSection(displayBalance, currentAccount)
                }
            } else {
                Column {
                    AccountLabel(currentAccount)
                    BalanceSection(displayBalance, currentAccount)
                }
            }
        }

        Icon(
            modifier = Modifier.rotate(rotationAngle),
            imageVector = Icons.Default.ExpandLess,
            contentDescription = null
        )

        // The Popup that shows the full summary
        if (isSummaryPopupVisible) {

            val currencyFormatter = LocalCurrencyFormatter.current

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
                    Box(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp
                                )
                            ) {
                                AccountSummaryV2(
                                    currentAccount,
                                    onDisplayBalanceTypeChange
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = {
                                        onCopyBalance(
                                            currencyFormatter.convAmount(
                                                displayBalance,
                                                currentAccount.currencyUnit
                                            )
                                        )
                                        isSummaryPopupVisible = false
                                    }) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.copy_text))
                                    }

                                    if (currentAccount is FullAccount) {
                                        TextButton(onClick = {
                                            onSetNewBalance()
                                            isSummaryPopupVisible = false
                                        }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.new_balance))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountLabel(
    account: BaseAccount,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = account.labelV2(LocalContext.current),
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun BalanceSection(
    balance: Long,
    account: BaseAccount,
) {
    val type = account.validatedBalanceType
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer(clip = false)
    ) {
        val iconTint = when (type) {
            BalanceType.CLEARED -> colorResource(id = R.color.CLEARED)
            BalanceType.RECONCILED -> colorResource(id = R.color.RECONCILED)
            else -> colorResource(id = R.color.UNRECONCILED)
        }
        Icon(
            imageVector = type.icon,
            contentDescription = stringResource(account.getBalanceContentDescription(type)),
            modifier = Modifier
                .padding(end = 4.dp)
                .size(12.dp),
            tint = iconTint
        )
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        ) {
            AmountText(
                balance, account.currencyUnit,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                overflow = TextOverflow.Visible,
                softWrap = false
            )
        }
    }
}

@Preview(fontScale = 2f)
@Composable
fun HeaderPreview() {
    BalanceHeader(
        currentAccount = FullAccount(
            id = 1,
            label = "Account",
            description = "Description",
            currencyUnit = CurrencyUnit.DebugInstance,
            color = android.graphics.Color.RED,
            openingBalance = 0,
            currentBalance = 1000,
            sumIncome = 2000,
            sumExpense = 1000,
            sealed = true,
            type = AccountType.CASH,
            criterion = 5000,
            excludeFromTotals = true
        )
    )
}
