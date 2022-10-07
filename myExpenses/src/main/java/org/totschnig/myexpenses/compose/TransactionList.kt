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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
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
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.*
import org.totschnig.myexpenses.viewmodel.data.Category.Companion.NO_CATEGORY_ASSIGNED_LABEL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

const val COMMENT_SEPARATOR = " / "

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    pagingSourceFactory: () -> PagingSource<Int, Transaction2>,
    headerData: HeaderData,
    budgetData: State<BudgetData?>,
    selectionHandler: SelectionHandler,
    menuGenerator: (Transaction2) -> Menu<Transaction2>? = { null },
    onToggleCrStatus: ((Long) -> Unit)?,
    dateTimeFormatter: DateTimeFormatter?,
    futureCriterion: ZonedDateTime,
    expansionHandler: ExpansionHandler,
    onBudgetClick: (Long, Int) -> Unit
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
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(), text = stringResource(id = R.string.no_expenses)
        )
    } else {
        LazyColumn {

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
                                    onBudgetClick = onBudgetClick
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
                                TransactionRenderer(
                                    modifier = Modifier.animateItemPlacement(),
                                    transaction = it,
                                    selectionHandler = selectionHandler,
                                    menuGenerator = menuGenerator,
                                    onToggleCrStatus = onToggleCrStatus,
                                    dateTimeFormatter = dateTimeFormatter,
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
    alignStart: Boolean = false
) {
    val context = LocalContext.current
    val amountFormatter = LocalCurrencyFormatter.current
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
        Text(
            text = amountFormatter.formatMoney(headerRow.previousBalance) + " " + delta + " = " + amountFormatter.formatMoney(
                headerRow.interimBalance
            ),
            style = MaterialTheme.typography.subtitle1
        )
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
            Text(Transfer.BI_ARROW + " " + amountFormatter.formatMoney(headerRow.transferSum))
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
    onBudgetClick: (Long, Int) -> Unit
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
                HeaderData(account.grouping, headerRow, dateInfo, alignStart = true)
            }
        } else {
            HeaderData(account.grouping, headerRow, dateInfo)
        }
    }
}

@Composable
private fun GroupDivider() {
    Divider(color = colorResource(id = R.color.emphasis))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRenderer(
    modifier: Modifier = Modifier,
    transaction: Transaction2,
    selectionHandler: SelectionHandler,
    menuGenerator: (Transaction2) -> Menu<Transaction2>?,
    onToggleCrStatus: ((Long) -> Unit)?,
    dateTimeFormatter: DateTimeFormatter?,
    futureCriterion: ZonedDateTime
) {
    val showMenu = remember { mutableStateOf(false) }
    val description = buildAnnotatedString {
        transaction.referenceNumber?.takeIf { it.isNotEmpty() }?.let {
            append("($it) ")
        }
        if (transaction.transferPeer != null) {
            transaction.accountLabel?.let { append("$it ") }
            append(Transfer.getIndicatorPrefixForLabel(transaction.amount.amountMinor))
            transaction.label?.let { append(it) }
        } else if (transaction.isSplit) {
            append(stringResource(id = R.string.split_transaction))
        } else if (transaction.catId == null && transaction.status != DatabaseConstants.STATUS_HELPER) {
            append(NO_CATEGORY_ASSIGNED_LABEL)
        } else {
            transaction.label?.let { append(it) }
        }
        transaction.comment?.takeIf { it.isNotEmpty() }?.let {
            append(COMMENT_SEPARATOR)
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(it)
            }
        }
        transaction.payee?.takeIf { it.isNotEmpty() }?.let {
            append(COMMENT_SEPARATOR)
            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(it)
            }
        }
        transaction.tagList?.takeIf { it.isNotEmpty() }?.let {
            append(COMMENT_SEPARATOR)
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(it)
            }
        }
    }
    val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)
    val voidMarkerHeight = with(LocalDensity.current) { 2.dp.toPx() }
    val futureBackgroundColor = colorResource(id = R.color.future_background)
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
        }
        .padding(horizontal = mainScreenPadding, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        transaction.color?.let {
            Divider(
                color = Color(it),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
            )
        }
        dateTimeFormatter?.let {
            Text(text = dateTimeFormatter.format(transaction.date))
        }

        if (onToggleCrStatus != null && transaction.crStatus != CrStatus.VOID) {
            Box(modifier = Modifier
                .size(32.dp)
                .clickable { onToggleCrStatus(transaction.id) }
                .padding(8.dp)
                .background(color = Color(transaction.crStatus.color)))
        }
        Text(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f), text = description
        )
        ColoredAmountText(money = transaction.amount)
        if (showMenu.value) {
            remember { menuGenerator(transaction) }?.let {
                HierarchicalMenu(showMenu, it, transaction)
            }
        }
    }
}

val mainScreenPadding
    @Composable get() = dimensionResource(id = R.dimen.padding_main_screen)

interface SelectionHandler {
    fun toggle(transaction: Transaction2)
    fun isSelected(transaction: Transaction2): Boolean
    val selectionCount: Int
}