package org.totschnig.myexpenses.compose

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.compose.scrollbar.LazyColumnWithScrollbar
import org.totschnig.myexpenses.compose.scrollbar.STICKY_HEADER_CONTENT_TYPE
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.viewmodel.data.BudgetData
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.HeaderDataEmpty
import org.totschnig.myexpenses.viewmodel.data.HeaderDataError
import org.totschnig.myexpenses.viewmodel.data.HeaderDataResult
import org.totschnig.myexpenses.viewmodel.data.HeaderRow
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Collections
import kotlin.math.absoluteValue

data class ScrollCalculationResult(
    val index: Int,
    val visibleIndex: Int,
    val lastHeaderId: Int?,
    val found: Boolean,
) {
    companion object {
        val INITIAL = ScrollCalculationResult(0, 0, null, false)
    }
}

private fun LazyPagingItems<Transaction2>.getCurrentPosition(
    start: ScrollCalculationResult,
    sortDirection: SortDirection,
    headerData: HeaderDataResult,
    collapsedIds: Set<String>,
): ScrollCalculationResult {
    var (index, visibleIndex, lastHeader) = start
    val limit = when (sortDirection) {
        SortDirection.ASC -> LocalDateTime.now()
            .truncatedTo(ChronoUnit.DAYS)
            .toEpoch() //startOfToday
        SortDirection.DESC -> LocalDateTime.now()
            .truncatedTo(ChronoUnit.DAYS)
            .plusDays(1)
            .toEpoch() //endOfToday
    }
    Timber.d("limit: %d", limit)
    while (index < itemCount) {
        val transaction2 = get(index) ?: return start.copy(found = true)
        val comparisonResult = transaction2._date.compareTo(limit)
        if ((sortDirection == SortDirection.ASC && comparisonResult > 0) || sortDirection == SortDirection.DESC && comparisonResult < 0) {
            Timber.d("index/visibleIndex: %d/%d", index, visibleIndex)
            return ScrollCalculationResult(index, visibleIndex, lastHeader, true)
        }
        val headerId = headerData.calculateGroupId(transaction2)
        if (headerId != lastHeader) {
            visibleIndex++
            Timber.d("increasing visibleIndex %d for header: %d", visibleIndex, headerId)
            lastHeader = headerId
        }
        val isVisible = !collapsedIds.contains(headerId.toString())
        index++
        if (isVisible) visibleIndex++
    }
    Timber.d("index/visibleIndex: %d/%d", index, visibleIndex)
    return ScrollCalculationResult(index, visibleIndex, lastHeader, false)
}

const val COMMENT_SEPARATOR = " / "

enum class FutureCriterion {
    EndOfDay, Current
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    modifier: Modifier,
    lazyPagingItems: LazyPagingItems<Transaction2>,
    headerData: HeaderDataResult,
    budgetData: State<BudgetData?>,
    selectionHandler: SelectionHandler?,
    menuGenerator: (Transaction2) -> Menu? = { null },
    futureCriterion: FutureCriterion,
    expansionHandler: ExpansionHandler?,
    onBudgetClick: (Long, Int) -> Unit,
    showSumDetails: Boolean,
    scrollToCurrentDate: MutableState<Boolean>,
    renderer: ItemRenderer,
    isFiltered: Boolean,
    splitInfoResolver: suspend (Long) -> List<Pair<String , String?>>? = { null }
) {
    val listState = rememberLazyListState()
    val collapsedIds = if (expansionHandler != null)
        expansionHandler.state.collectAsState(initial = null).value
    else emptySet()

    val splitInfoCache = remember(lazyPagingItems.loadState.refresh) {
        Collections.synchronizedMap(HashMap<Long, List<Pair<String , String?>>?>())
    }
    val scope = rememberCoroutineScope()

    if (lazyPagingItems.itemCount == 0) {
        if (lazyPagingItems.loadState.refresh != LoadState.Loading) {
            Text(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentSize(),
                text = stringResource(
                    id = if (isFiltered) R.string.no_matches_found else R.string.no_expenses
                )
            )
        }
    } else {
        val futureBackgroundColor = colorResource(id = R.color.future_background)
        val scrollToCurrentDateStartIndex = remember(scrollToCurrentDate.value) {
            mutableStateOf(if (scrollToCurrentDate.value) ScrollCalculationResult.INITIAL else null)
        }
        val scrollToCurrentDateResultIndex = remember {
            mutableIntStateOf(0)
        }

        if (lazyPagingItems.itemCount > 0 && collapsedIds != null) {
            scrollToCurrentDateStartIndex.value?.let {
                LaunchedEffect(lazyPagingItems.itemCount) {
                    val scrollCalculationResult = lazyPagingItems.getCurrentPosition(
                        start = it,
                        sortDirection = headerData.account.sortDirection,
                        headerData = headerData,
                        collapsedIds = collapsedIds
                    )
                    scrollToCurrentDateStartIndex.value =
                        if (scrollCalculationResult.found || lazyPagingItems.loadState.append.endOfPaginationReached) {
                            scrollToCurrentDateResultIndex.intValue =
                                scrollCalculationResult.visibleIndex
                            null
                        } else scrollCalculationResult
                }
            }
        }
        val headerCorrection = remember { mutableStateOf<Int?>(null) }
        if (scrollToCurrentDateStartIndex.value == null) {
            if (scrollToCurrentDate.value) {
                LaunchedEffect(Unit) {
                    Timber.i(
                        "Scroll to current date result: %d",
                        scrollToCurrentDateResultIndex.intValue
                    )
                    listState.scrollToItem(
                        scrollToCurrentDateResultIndex.intValue,
                        headerCorrection.value ?: 0
                    )
                    scrollToCurrentDate.value = false
                }
            }
            val headersWithSumDetails = rememberMutableStateMapOf<Int, Boolean>(
                defaultValue = showSumDetails,
                showSumDetails
            )
            val nestedScrollInterop = rememberNestedScrollInteropConnection()

            LazyColumnWithScrollbar(
                modifier = modifier.nestedScroll(nestedScrollInterop),
                state = listState,
                fastScroll = true,
                contentPadding = WindowInsets.navigationBars
                    .add(WindowInsets.displayCutout)
                    .add(WindowInsets(bottom = dimensionResource(R.dimen.fab_related_bottom_padding)))
                    .asPaddingValues(),
                itemsAvailable = lazyPagingItems.itemCount,
                groupCount = (headerData as? HeaderData)?.groups?.size ?: 0
            ) {

                var lastHeader: Int? = null

                for (index in 0 until lazyPagingItems.itemCount) {

                    val item = lazyPagingItems.peek(index)
                    val headerId = item?.let { headerData.calculateGroupId(it) }
                    val isGroupHidden = collapsedIds?.contains(headerId.toString()) == true
                    if (headerId !== null && headerId != lastHeader) {
                        when (headerData) {
                            is HeaderData -> {
                                headerData.groups[headerId]?.let { headerRow ->
                                    val budget = budgetData.value?.let { data ->
                                        val amount =
                                            (data.data.find { it.headerId == headerId && it.amount != null }
                                                ?: data.data.lastOrNull {
                                                    !it.oneTime && it.headerId < headerId && it.amount != null
                                                })?.amount ?: 0L
                                        val rollOverPrevious =
                                            data.data.find { it.headerId == headerId }
                                                ?.rollOverPrevious
                                                ?: 0L
                                        data.budgetId to amount + rollOverPrevious
                                    }
                                    stickyHeader(
                                        key = headerId,
                                        contentType = STICKY_HEADER_CONTENT_TYPE
                                    ) {
                                        HeaderRenderer(
                                            account = headerData.account,
                                            headerId = headerId,
                                            headerRow = headerRow,
                                            dateInfo = headerData.dateInfo,
                                            budget = budget,
                                            isExpanded = !isGroupHidden,
                                            toggle = expansionHandler?.let {
                                                { expansionHandler.toggle(headerId.toString()) }
                                            },
                                            onBudgetClick = onBudgetClick,
                                            showSumDetails = headersWithSumDetails.getValue(
                                                headerId
                                            ),
                                            showOnlyDelta = headerData.account.isHomeAggregate || headerData.isFiltered,
                                            onHeaderSize = if (headerCorrection.value == null) {
                                                {
                                                    headerCorrection.value = -it
                                                }
                                            } else null
                                        ) {
                                            headersWithSumDetails[headerId] = it
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }

                            is HeaderDataEmpty -> {}
                            is HeaderDataError -> {
                                stickyHeader(
                                    key = headerId,
                                    contentType = STICKY_HEADER_CONTENT_TYPE
                                ) {
                                    val context = LocalActivity.current as? BaseActivity
                                    Text(
                                        "Error loading group header data. Click to start safe mode.",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable {
                                            context?.dispatchCommand(
                                                R.id.SAFE_MODE_COMMAND,
                                                null
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    val isLast = index == lazyPagingItems.itemCount - 1
                    val futureCriterionDate = when (futureCriterion) {
                        FutureCriterion.Current -> ZonedDateTime.now(ZoneId.systemDefault())
                        FutureCriterion.EndOfDay -> LocalDate.now().plusDays(1).atStartOfDay()
                            .atZone(ZoneId.systemDefault())
                    }
                    if (!isGroupHidden || isLast) {
                        item(key = item?.id) {
                            lazyPagingItems[index]?.let {
                                if (!isGroupHidden) {
                                    val resolvedSplitInfo = remember {
                                        mutableStateOf(splitInfoCache[it.id])
                                    }

                                    if (it.isSplit && !splitInfoCache.contains(it.id)) {
                                        scope.launch {
                                            resolvedSplitInfo.value =
                                                splitInfoResolver(it.id).also { info ->
                                                    splitInfoCache[it.id] = info
                                                }
                                        }
                                    }
                                    renderer.Render(
                                        transaction = it,
                                        modifier = Modifier
                                            .conditional(it.date >= futureCriterionDate) {
                                                background(futureBackgroundColor)
                                            }
                                            .semantics {
                                                collectionItemInfo = CollectionItemInfo(
                                                    rowIndex = index,
                                                    columnIndex = 1,
                                                    rowSpan = 1,
                                                    columnSpan = 1
                                                )
                                            },
                                        selectionHandler = selectionHandler,
                                        menuGenerator = menuGenerator,
                                        resolvedSplitInfo = resolvedSplitInfo.value
                                    )
                                }
                            }
                            if (isLast) GroupDivider() else HorizontalDivider()
                        }
                    }

                    lastHeader = headerId
                }
            }
        } else {
            Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = listOf(
                        stringResource(id = R.string.pref_scroll_to_current_date_title),
                        stringResource(id = R.string.loading),
                        "(${scrollToCurrentDateStartIndex.value?.index})"
                    ).joinToString("\n"), textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeaderData(
    displayTitle: String,
    headerRow: HeaderRow,
    showSumDetails: Boolean,
    showOnlyDelta: Boolean,
    updateShowSumDetails: (Boolean) -> Unit,
    alignStart: Boolean = false,
) {
    val amountFormatter = LocalCurrencyFormatter.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (alignStart) Alignment.Start else Alignment.CenterHorizontally
    ) {
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleMedium,
        )
        val delta = " " +
                (if (headerRow.delta.amountMinor >= 0) "+" else "\u2212") + " " + amountFormatter.formatMoney(
            Money(
                headerRow.delta.currencyUnit,
                headerRow.delta.amountMinor.absoluteValue
            )
        )
        FlowRow(
            modifier = Modifier
                .testTag(TEST_TAG_GROUP_SUMMARY)
                .fillMaxWidth(),
            horizontalArrangement = if (alignStart) Arrangement.Start else Arrangement.Center
        ) {
            if (!showOnlyDelta) {
                Text(
                    modifier = Modifier.amountSemantics(headerRow.previousBalance),
                    text = amountFormatter.formatMoney(headerRow.previousBalance)
                )
            }
            Text(
                modifier = Modifier
                    .amountSemantics(headerRow.delta)
                    .padding(horizontal = 6.dp)
                    .clearAndSetSemantics {
                        contentDescription = delta
                    }
                    .clickable {
                        updateShowSumDetails(!showSumDetails)
                    },
                text = delta
            )
            if (!showOnlyDelta) {
                Text(
                    modifier = Modifier.amountSemantics(headerRow.interimBalance),
                    text = " = " + amountFormatter.formatMoney(headerRow.interimBalance)
                )
            }
        }
        if (showSumDetails) {
            SumDetails(headerRow.incomeSum, headerRow.expenseSum, headerRow.transferSum, alignStart)
        }
    }
}

@Composable
fun HeaderRenderer(
    account: PageAccount,
    headerId: Int,
    headerRow: HeaderRow,
    dateInfo: DateInfo,
    budget: Pair<Long, Long>?,
    isExpanded: Boolean,
    toggle: (() -> Unit)?,
    onBudgetClick: (Long, Int) -> Unit,
    showSumDetails: Boolean,
    showOnlyDelta: Boolean,
    onHeaderSize: ((Int) -> Unit)? = null,
    updateShowSumDetails: (Boolean) -> Unit = {},
) {

    Box(
        modifier = Modifier
            .headerSemantics(headerId)
            .optional(onHeaderSize) { onHeaderSize ->
                onGloballyPositioned { layoutCoordinates ->
                    onHeaderSize(layoutCoordinates.size.height)
                }
            }
            .background(MaterialTheme.colorScheme.background)
    ) {
        GroupDivider()
        val context = LocalContext.current
        val displayTitle = account.grouping.getDisplayTitle(
            context,
            headerRow.year,
            headerRow.second,
            dateInfo,
            headerRow.weekStart
        )
        toggle?.let {
            ExpansionHandle(
                modifier = Modifier.align(Alignment.TopEnd),
                contentDescription = displayTitle,
                isExpanded = isExpanded,
                toggle = toggle
            )
        }
        if (budget?.second != null && budget.second != 0L) {
            val progress = (-headerRow.expenseSum.amountMinor * 100F / budget.second)
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutInABox(
                    modifier = Modifier
                        .padding(horizontal = mainScreenPadding)
                        .clickable { onBudgetClick(budget.first, headerId) }
                        .size(42.dp),
                    progress = progress,
                    fontSize = 12.sp,
                    color = Color(account.color(LocalContext.current.resources)),
                    excessColor = LocalColors.current.expense
                )

                HeaderData(
                    displayTitle,
                    headerRow,
                    showSumDetails,
                    showOnlyDelta,
                    updateShowSumDetails,
                    alignStart = true
                )
            }
        } else {
            HeaderData(
                displayTitle,
                headerRow,
                showSumDetails,
                showOnlyDelta,
                updateShowSumDetails
            )
        }
    }
}

@Composable
fun GroupDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = colorResource(id = R.color.emphasis))
}

val mainScreenPadding
    @Composable get() = dimensionResource(id = R.dimen.padding_main_screen)

interface SelectionHandler {
    fun toggle(transaction: Transaction2)
    fun isSelected(transaction: Transaction2): Boolean
    fun select(transaction: Transaction2)
    val selectionCount: Int
    fun isSelectable(transaction: Transaction2): Boolean

    fun selectConditional(transaction: Transaction2) {
        if (isSelectable(transaction)) select(transaction)
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Preview(locale = "ar")
@Composable
private fun RowRTL() {
    Column {
        Row {
            Text("1")
            Text("2")
            Text("3")
        }
        FlowRow {
            Text("1")
            Text("2")
            Text("3")
        }
    }
}

@Preview(locale = "ar")
@Composable
private fun Header() {
    val amount = Money(CurrencyUnit.DebugInstance, 1234)
    val headerRow = HeaderRow(
        2022, 11, amount, amount, amount, amount, amount, amount, LocalDate.now()
    )
    HeaderRenderer(
        account = PageAccount(
            1,
            AccountType.CASH,
            DatabaseConstants.KEY_DATE,
            SortDirection.DESC,
            Grouping.NONE,
            CurrencyUnit.DebugInstance,
            false,
            1234,
            0
        ),
        headerId = 2022001,
        headerRow = headerRow,
        dateInfo = DateInfo.EMPTY,
        budget = null,
        isExpanded = true,
        toggle = { },
        onBudgetClick = { _, _ -> },
        showSumDetails = true,
        showOnlyDelta = false
    )
}

@Preview(name = "Tablet", device = "spec:width=1280dp,height=800dp,dpi=240", locale = "en")
@Composable
private fun HeaderWithBudgetProgress() {
    val amount = Money(CurrencyUnit.DebugInstance, 1234)
    val headerRow = HeaderRow(
        2022, 11, amount, amount, amount, amount, amount, amount, LocalDate.now()
    )
    HeaderRenderer(
        account = PageAccount(
            1,
            AccountType.CASH,
            DatabaseConstants.KEY_DATE,
            SortDirection.DESC,
            Grouping.NONE,
            CurrencyUnit.DebugInstance,
            false,
            1234,
            0
        ),
        headerId = 2022001,
        headerRow = headerRow,
        dateInfo = DateInfo.EMPTY,
        budget = 1L to 75L,
        isExpanded = true,
        toggle = { },
        onBudgetClick = { _, _ -> },
        showSumDetails = true,
        showOnlyDelta = false
    )
}