package org.totschnig.myexpenses.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import dev.burnoo.compose.rememberpreference.rememberStringSetPreference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.BaseTransactionList
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.Category.Companion.NO_CATEGORY_ASSIGNED_LABEL
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.DateInfo2
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.HeaderRow
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComposeTransactionList(
    pagingSourceFactory: () -> PagingSource<Int, Transaction2>,
    headerData: HeaderData,
    accountId: Long
) {
    val pager = remember {
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
    val itemCount = lazyPagingItems.itemCount
    if (itemCount == 0) {
        Text(modifier = Modifier.fillMaxSize().wrapContentSize(), text = stringResource(id = R.string.no_expenses))
    } else {
        LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {

            var lastHeader: Int? = null

            for (index in 0 until itemCount) {
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
                                    headerData.grouping,
                                    it,
                                    headerData.dateInfo,
                                    isExpanded = !isGroupHidden,
                                ) {
                                    if (isGroupHidden) {
                                        collapsedHeaders.value =
                                            collapsedHeaders.value - headerId.toString()
                                    } else {
                                        collapsedHeaders.value =
                                            collapsedHeaders.value + headerId.toString()
                                    }
                                }
                            }
                    }
                }
                if (!collapsedHeaders.value.contains(headerId.toString())) {
                    item(key = transaction?.id) {
                        // Gets item, triggering page loads if needed
                        lazyPagingItems[index]?.let {
                            TransactionRenderer(it)
                        }
                    }
                }

                lastHeader = headerId
            }
        }
    }
}

@Composable
fun HeaderRenderer(
    grouping: Grouping,
    headerRow: HeaderRow,
    dateInfo: DateInfo2,
    isExpanded: Boolean,
    toggle: () -> Unit
) {
    val context = LocalContext.current
    val amountFormatter = LocalCurrencyFormatter.current
    Box {
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

@Composable
fun TransactionRenderer(transaction: Transaction2) {
    val description = buildAnnotatedString {
        transaction.referenceNumber.takeIf { it.isNotEmpty() }?.let {
            append("($it) ")
        }
        if (transaction.transferPeer != null) {
            append(Transfer.getIndicatorPrefixForLabel(transaction.amount.amountMinor))
        }
        if (transaction.catId == DatabaseConstants.SPLIT_CATID) {
            append(stringResource(id = R.string.split_transaction))
        } else if (transaction.catId == null && transaction.status != DatabaseConstants.STATUS_HELPER) {
            append(NO_CATEGORY_ASSIGNED_LABEL)
        } else {
            transaction.label?.let { append(it) }
        }
        transaction.comment?.takeIf { it.isNotEmpty() }?.let {
            append(BaseTransactionList.COMMENT_SEPARATOR)
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(it)
            }
        }
        transaction.payee?.takeIf { it.isNotEmpty() }?.let {
            append(BaseTransactionList.COMMENT_SEPARATOR)
            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(it)
            }
        }
        transaction.tagList?.takeIf { it.isNotEmpty() }?.let {
            append(BaseTransactionList.COMMENT_SEPARATOR)
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(it)
            }
        }
    }
    Row {
        Text(text = LocalDateFormatter.current.format(transaction.date))
        Text(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f), text = description
        )
        ColoredAmountText(money = transaction.amount)
    }
}