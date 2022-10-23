package org.totschnig.myexpenses.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.*
import java.time.ZonedDateTime
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

const val COMMENT_SEPARATOR = " / "

enum class RenderType {
    legacy, new
}

interface ItemRenderer {

    @Composable
    fun RowScope.RenderInner(transaction: Transaction2)

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(
        modifier: Modifier,
        transaction: Transaction2,
        selectionHandler: SelectionHandler,
        menuGenerator: (Transaction2) -> Menu<Transaction2>?,
        futureCriterion: ZonedDateTime
    ) {
        val showMenu = remember { mutableStateOf(false) }
        val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)
        val voidMarkerHeight = with(LocalDensity.current) { 2.dp.toPx() }
        val futureBackgroundColor = colorResource(id = R.color.future_background)
        val voidStatus = stringResource(id = R.string.status_void)
        Row(modifier = modifier
            .conditional(transaction.date >= futureCriterion) {
                background(futureBackgroundColor)
            }
            .height(IntrinsicSize.Min)
            .combinedClickable(
                onLongClick = { selectionHandler.toggle(transaction) },
                onClick = {
                    if (selectionHandler.selectionCount == 0) {
                        showMenu.value = true
                    } else {
                        selectionHandler.toggle(transaction)
                    }
                }
            )
            .conditional(selectionHandler.isSelected(transaction)) {
                background(activatedBackgroundColor)
            }
            .conditional(transaction.crStatus == CrStatus.VOID) {
                drawWithContent {
                    drawContent()
                    drawLine(
                        Color.Red,
                        Offset(0F, size.height / 2),
                        Offset(size.width, size.height / 2),
                        voidMarkerHeight
                    )
                }
                    .semantics { contentDescription = voidStatus }
            }
            .padding(horizontal = mainScreenPadding, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RenderInner(transaction = transaction)
            if (showMenu.value) {
                remember { menuGenerator(transaction) }?.let {
                    HierarchicalMenu(showMenu, it, transaction)
                }
            }
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    modifier: Modifier,
    pagingSourceFactory: () -> PagingSource<Int, Transaction2>,
    headerData: HeaderData,
    budgetData: State<BudgetData?>,
    selectionHandler: SelectionHandler,
    menuGenerator: (Transaction2) -> Menu<Transaction2>? = { null },
    futureCriterion: ZonedDateTime,
    expansionHandler: ExpansionHandler,
    onBudgetClick: (Long, Int) -> Unit,
    showSumDetails: Boolean,
    renderer: ItemRenderer
) {
    val pager = remember(pagingSourceFactory) {
        Pager(
            PagingConfig(
                pageSize = 100,
                enablePlaceholders = false
            ),
            pagingSourceFactory = pagingSourceFactory
        )
    }
    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
    val collapsedIds = expansionHandler.collapsedIds().value

    if (lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.refresh != LoadState.Loading) {
        Text(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentSize(), text = stringResource(id = R.string.no_expenses)
        )
    } else {
        LazyColumn(modifier = modifier
            .testTag(TEST_TAG_LIST)
            .semantics {
                collectionInfo = CollectionInfo(lazyPagingItems.itemCount, 1)
            }) {

            var lastHeader: Int? = null

            for (index in 0 until lazyPagingItems.itemCount) {
                // Gets item without notifying Paging of the item access,
                // which would otherwise trigger page loads
                val headerId = lazyPagingItems.peek(index)?.let { headerData.calculateGroupId(it) }
                val isGroupHidden = collapsedIds.contains(headerId.toString())
                if (headerId !== null && headerId != lastHeader) {
                    stickyHeader(key = headerId) {
                        headerData.groups[headerId]
                            ?.let { headerRow ->
                                // reimplement DbConstants.budgetColumn outside of Database
                                val budget = budgetData.value?.let { data ->
                                    (data.data.find { it.headerId == headerId } ?: data.data.lastOrNull { !it.oneTime && it.headerId < headerId })?.let {
                                        data.budgetId to it.amount
                                    }
                                }
                                HeaderRenderer(
                                    account = headerData.account,
                                    headerId = headerId,
                                    headerRow = headerRow,
                                    dateInfo = headerData.dateInfo,
                                    budget = budget,
                                    isExpanded = !isGroupHidden,
                                    toggle = {
                                        expansionHandler.toggle(headerId.toString())
                                    },
                                    onBudgetClick = onBudgetClick,
                                    showSumDetails = showSumDetails
                                )
                                Divider()
                            }
                    }
                }

                // Gets item, triggering page loads if needed
                lazyPagingItems[index]?.let {
                    val isLast = index == lazyPagingItems.itemCount - 1
                    if (!isGroupHidden || isLast) {
                        item(key = it.id) {
                            if (!isGroupHidden) {
                                renderer.Render(
                                        modifier = Modifier.animateItemPlacement(),
                                        transaction = it,
                                        selectionHandler = selectionHandler,
                                        menuGenerator = menuGenerator,
                                        futureCriterion = futureCriterion
                                    )
                            }
                            if (isLast) GroupDivider() else Divider()
                        }
                    }
                }

                lastHeader = headerId
            }
        }
    }
}

@Composable
fun HeaderData(
    grouping: Grouping,
    headerRow: HeaderRow,
    dateInfo: DateInfo2,
    showSumDetails: Boolean,
    alignStart: Boolean = false
) {
    val context = LocalContext.current
    val amountFormatter = LocalCurrencyFormatter.current
    val showSumDetailsState = remember { mutableStateOf(showSumDetails) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (alignStart) Alignment.Start else Alignment.CenterHorizontally
    ) {
        Text(
            text = grouping.getDisplayTitle(
                context, headerRow.year, headerRow.second,
                DateInfo(
                    dateInfo.day,
                    dateInfo.week,
                    dateInfo.month,
                    dateInfo.year,
                    dateInfo.yearOfWeekStart,
                    dateInfo.yearOfMonthStart,
                    headerRow.weekStart,
                    headerRow.weekEnd
                ),
            ),
            style = MaterialTheme.typography.subtitle1,
        )
        val delta =
            (if (headerRow.delta.amountMinor >= 0) " + " else " - ") + amountFormatter.formatMoney(
                Money(
                    headerRow.delta.currencyUnit,
                    headerRow.delta.amountMinor.absoluteValue
                )
            )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (alignStart) Arrangement.Start else Arrangement.Center
        ) {
            Text(amountFormatter.formatMoney(headerRow.previousBalance))
            Text(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clickable {
                        showSumDetailsState.value = !showSumDetailsState.value
                    },
                text = delta
            )
            Text( " = " + amountFormatter.formatMoney(headerRow.interimBalance))
        }
        if (showSumDetailsState.value) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (alignStart) Arrangement.Start else Arrangement.SpaceEvenly
            ) {
                Text(
                    "⊕ " + amountFormatter.formatMoney(headerRow.incomeSum),
                    color = LocalColors.current.income
                )
                Text(
                    modifier = Modifier.padding(horizontal = generalPadding),
                    text = "⊖ " + amountFormatter.formatMoney(headerRow.expenseSum),
                    color = LocalColors.current.expense
                )
                Text(Transfer.BI_ARROW + " " + amountFormatter.formatMoney(headerRow.transferSum),
                    color = LocalColors.current.transfer)
            }
        }
    }
}

@Composable
fun HeaderRenderer(
    account: FullAccount,
    headerId: Int,
    headerRow: HeaderRow,
    dateInfo: DateInfo2,
    budget: Pair<Long, Long>?,
    isExpanded: Boolean,
    toggle: () -> Unit,
    onBudgetClick: (Long, Int) -> Unit,
    showSumDetails: Boolean
) {

    Box {
        GroupDivider()
        if (account.grouping != Grouping.NONE) {
            ExpansionHandle(
                modifier = Modifier.align(Alignment.TopEnd),
                isExpanded = isExpanded,
                toggle = toggle
            )
        }
        if (budget?.second != null) {
            val progress = (-headerRow.expenseSum.amountMinor * 100F / budget.second).roundToInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutInABox(
                    modifier = Modifier
                        .padding(mainScreenPadding)
                        .clickable { onBudgetClick(budget.first, headerId) }
                        .size(42.dp),
                    progress = progress,
                    fontSize = 12.sp,
                    color = Color(account.color(LocalContext.current.resources))
                )
                HeaderData(account.grouping, headerRow, dateInfo, showSumDetails, alignStart = true)
            }
        } else {
            HeaderData(account.grouping, headerRow, dateInfo, showSumDetails)
        }
    }
}

@Composable
private fun GroupDivider() {
    Divider(color = colorResource(id = R.color.emphasis))
}

val mainScreenPadding
    @Composable get() = dimensionResource(id = R.dimen.padding_main_screen)

interface SelectionHandler {
    fun toggle(transaction: Transaction2)
    fun isSelected(transaction: Transaction2): Boolean
    val selectionCount: Int
}