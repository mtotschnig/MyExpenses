package org.totschnig.myexpenses.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import dev.burnoo.compose.rememberpreference.rememberStringSetPreference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.*
import org.totschnig.myexpenses.viewmodel.data.Category.Companion.NO_CATEGORY_ASSIGNED_LABEL
import kotlin.math.absoluteValue

const val COMMENT_SEPARATOR = " / "

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComposeTransactionList(
    pagingSourceFactory: () -> PagingSource<Int, Transaction2>,
    headerData: HeaderData,
    accountId: Long,
    selectionHandler: SelectionHandler,
    menuGenerator: (Transaction2) -> Menu<Transaction2>? = { null },
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
    val collapsedHeaders = rememberStringSetPreference(
        keyName = "collapsedHeaders_${accountId}_${headerData.grouping}",
        initialValue = emptySet(),
        defaultValue = emptySet()
    )

    if (lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.refresh != LoadState.Loading) {
        Text(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(), text = stringResource(id = R.string.no_expenses)
        )
    } else {
        LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {

            var lastHeader: Int? = null

            for (index in 0 until lazyPagingItems.itemCount) {
                // Gets item without notifying Paging of the item access,
                // which would otherwise trigger page loads
                val transaction = lazyPagingItems.peek(index)
                val headerId = transaction?.let { headerData.calculateGroupId(it) }

                if (transaction !== null && headerId != lastHeader) {
                    val isGroupHidden = collapsedHeaders.value.contains(headerId.toString())
                    stickyHeader(key = headerId) {
                        headerData.groups[headerId]
                            ?.let {
                                HeaderRenderer(
                                    //modifier = Modifier.animateItemPlacement(),
                                    headerData.grouping,
                                    it,
                                    headerData.dateInfo,
                                    isExpanded = !isGroupHidden,
                                ) {
                                    collapsedHeaders.value = if (isGroupHidden) {
                                        collapsedHeaders.value - headerId.toString()
                                    } else {
                                        collapsedHeaders.value + headerId.toString()
                                    }
                                }
                            }
                    }
                }
                // Gets item, triggering page loads if needed
                lazyPagingItems[index]?.let {
                    if (!collapsedHeaders.value.contains(headerId.toString())) {
                        item(key = transaction?.id) {
                            TransactionRenderer(
                                modifier = Modifier.animateItemPlacement(),
                                transaction = it,
                                selectionHandler = selectionHandler,
                                menuGenerator = menuGenerator
                            )
                        }
                    }
                }

                lastHeader = headerId
            }
        }
    }
}

//currently we are not using animateItemPlacement modifier on Headers
//due to bug https://issuetracker.google.com/issues/209947592
@Composable
fun HeaderRenderer(
    //modifier: Modifier,
    grouping: Grouping,
    headerRow: HeaderRow,
    dateInfo: DateInfo2,
    isExpanded: Boolean,
    toggle: () -> Unit
) {
    val context = LocalContext.current
    val amountFormatter = LocalCurrencyFormatter.current
    Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
        if (grouping != Grouping.NONE) {
            ExpansionHandle(
                modifier = Modifier.align(Alignment.TopEnd),
                isExpanded = isExpanded,
                toggle = toggle
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
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
                if (headerRow.delta.amountMinor > -1) " + " else " - " + amountFormatter.formatMoney(
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
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    "⊕ " + amountFormatter.formatMoney(headerRow.incomeSum),
                    color = LocalColors.current.income
                )
                Text(
                    "⊖ " + amountFormatter.formatMoney(headerRow.expenseSum),
                    color = LocalColors.current.expense
                )
                Text(Transfer.BI_ARROW + " " + amountFormatter.formatMoney(headerRow.transferSum))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRenderer(
    modifier: Modifier = Modifier,
    transaction: Transaction2,
    selectionHandler: SelectionHandler,
    menuGenerator: (Transaction2) -> Menu<Transaction2>?
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
    Row(modifier = modifier
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
    ) {
        transaction.color?.let {
            Divider(
                color = Color(it),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
            )
        }
        Text(text = LocalDateFormatter.current.format(transaction.date))
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

interface SelectionHandler {
    fun toggle(transaction: Transaction2)
    fun isSelected(transaction: Transaction2): Boolean
    val selectionCount: Int
}