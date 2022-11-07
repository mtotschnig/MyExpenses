package org.totschnig.myexpenses.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

const val COMMENT_SEPARATOR = " / "

enum class FutureCriterion {
    EndOfDay, Current
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
    futureCriterion: FutureCriterion,
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
    val collapsedIds = expansionHandler.collapsedIds.collectAsState(initial = null).value

    if (lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.refresh != LoadState.Loading) {
        Text(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentSize(), text = stringResource(id = R.string.no_expenses)
        )
    } else {
        val futureBackgroundColor = colorResource(id = R.color.future_background)
        val showOnlyDelta = headerData.account.isHomeAggregate || headerData.isFiltered
        LazyColumn(modifier = modifier
            .testTag(TEST_TAG_LIST)
            .semantics {
                collectionInfo = CollectionInfo(lazyPagingItems.itemCount, 1)
            }) {

            var lastHeader: Int? = null

            for (index in 0 until lazyPagingItems.itemCount) {
                val item = lazyPagingItems.peek(index)
                val headerId = item?.let { headerData.calculateGroupId(it) }
                val isGroupHidden = collapsedIds?.contains(headerId.toString()) ?: false
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
                                    showSumDetails = showSumDetails,
                                    showOnlyDelta = showOnlyDelta
                                )
                                Divider()
                            }
                    }
                }
                val isLast = index == lazyPagingItems.itemCount - 1
                val futureCriterionDate = when (futureCriterion) {
                    FutureCriterion.Current -> ZonedDateTime.now(ZoneId.systemDefault())
                    FutureCriterion.EndOfDay -> LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault())
                }
                if (!isGroupHidden || isLast) {
                    item(key = item?.id) {
                        lazyPagingItems[index]?.let {
                            if (!isGroupHidden) {
                                renderer.Render(
                                    modifier = Modifier
                                        .animateItemPlacement()
                                        .conditional(it.date >= futureCriterionDate) {
                                            background(futureBackgroundColor)
                                        },
                                    transaction = it,
                                    selectionHandler = selectionHandler,
                                    menuGenerator = menuGenerator
                                )
                            }
                        }
                        if (isLast) {
                            GroupDivider(modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.fab_related_bottom_padding)))
                        } else Divider()
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
    showOnlyDelta: Boolean,
    alignStart: Boolean = false,
) {
    val context = LocalContext.current
    val amountFormatter = LocalCurrencyFormatter.current
    val showSumDetailsState = remember(showSumDetails) { mutableStateOf(showSumDetails) }

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
            if (!showOnlyDelta) {
                Text(amountFormatter.formatMoney(headerRow.previousBalance))
            }
            Text(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clickable {
                        showSumDetailsState.value = !showSumDetailsState.value
                    },
                text = delta
            )
            if (!showOnlyDelta) {
                Text( " = " + amountFormatter.formatMoney(headerRow.interimBalance))
            }
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
    account: PageAccount,
    headerId: Int,
    headerRow: HeaderRow,
    dateInfo: DateInfo2,
    budget: Pair<Long, Long>?,
    isExpanded: Boolean,
    toggle: () -> Unit,
    onBudgetClick: (Long, Int) -> Unit,
    showSumDetails: Boolean,
    showOnlyDelta: Boolean
) {

    Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
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
                HeaderData(account.grouping, headerRow, dateInfo, showSumDetails, showOnlyDelta, alignStart = true)
            }
        } else {
            HeaderData(account.grouping, headerRow, dateInfo, showSumDetails, showOnlyDelta)
        }
    }
}

@Composable
private fun GroupDivider(modifier: Modifier = Modifier) {
    Divider(modifier = modifier, color = colorResource(id = R.color.emphasis))
}

val mainScreenPadding
    @Composable get() = dimensionResource(id = R.dimen.padding_main_screen)

interface SelectionHandler {
    fun toggle(transaction: Transaction2)
    fun isSelected(transaction: Transaction2): Boolean
    val selectionCount: Int
}