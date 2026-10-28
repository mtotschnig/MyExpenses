package org.totschnig.myexpenses.next.feature.contracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.next.core.ui.LocalAmountFormatter
import org.totschnig.myexpenses.designsystem.Banner
import org.totschnig.myexpenses.designsystem.BannerStyle
import org.totschnig.myexpenses.designsystem.DetailCard
import org.totschnig.myexpenses.designsystem.Dot
import org.totschnig.myexpenses.designsystem.EmptyState
import org.totschnig.myexpenses.designsystem.GroupItem
import org.totschnig.myexpenses.designsystem.GroupPosition
import org.totschnig.myexpenses.designsystem.PageTopBar
import org.totschnig.myexpenses.designsystem.RowDivider
import org.totschnig.myexpenses.designsystem.SectionTitle
import org.totschnig.myexpenses.designsystem.TopBarAction
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.common.NO_NAME
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.rememberDateFormatter
import org.totschnig.myexpenses.next.data.contracts.ContractNews
import org.totschnig.myexpenses.next.data.contracts.labelRes

/**
 * What happened, in one sentence
 */
@Composable
internal fun ContractNews.text(currency: CurrencyInfo): String {
    val formatter = LocalAmountFormatter.current
    val dateFormatter = rememberDateFormatter()
    val amount = formatter.format(amount, currency)
    return when (type) {
        ContractNews.Type.PAYMENT_AFTER_CANCELLATION -> stringResource(
            when {
                isIncome -> R.string.next_news_income_after_cancellation
                isReserve -> R.string.next_news_reserve_after_cancellation
                else -> R.string.next_news_payment_after_cancellation
            },
            amount,
            dateFormatter.format(date),
            cancelledOn?.let { dateFormatter.format(it) } ?: NO_NAME
        )

        ContractNews.Type.PRICE_CHANGE -> {
            val previous = previousAmount ?: 0
            stringResource(
                when {
                    isIncome && this.amount > previous -> R.string.next_news_income_up
                    isIncome -> R.string.next_news_income_down
                    isReserve && this.amount > previous -> R.string.next_news_reserve_up
                    isReserve -> R.string.next_news_reserve_down
                    this.amount > previous -> R.string.next_news_price_up
                    else -> R.string.next_news_price_down
                },
                formatter.format(previous, currency),
                amount
            )
        }

        ContractNews.Type.NEW_CONTRACT -> stringResource(
            when {
                isIncome -> R.string.next_news_new_income
                isReserve -> R.string.next_news_new_reserve
                else -> R.string.next_news_new_contract
            },
            amount,
            interval?.let { stringResource(it.labelRes) } ?: NO_NAME
        )

        ContractNews.Type.PAYMENT_MISSING -> stringResource(
            when {
                isIncome -> R.string.next_news_income_missing
                isReserve -> R.string.next_news_reserve_missing
                else -> R.string.next_news_payment_missing
            },
            dateFormatter.format(date),
            amount
        )

        ContractNews.Type.CONTRACT_STOPPED -> stringResource(
            when {
                isIncome -> R.string.next_news_income_stopped
                isReserve -> R.string.next_news_reserve_stopped
                else -> R.string.next_news_contract_stopped
            },
            dateFormatter.format(date)
        )
    }
}

/**
 * News of a contract at the top of its details
 *
 * @param onCancelAgain for news suggesting that the contract ended, if it is not cancelled
 */
@Composable
internal fun NewsBanner(
    news: List<ContractNews>,
    currency: CurrencyInfo,
    onCancelAgain: (() -> Unit)?,
) {
    // News that suggest the contract has ended, the user confirms with one tap
    val cancelling = news.firstOrNull {
        it.type == ContractNews.Type.PAYMENT_AFTER_CANCELLATION || it.type == ContractNews.Type.CONTRACT_STOPPED
    }
    Banner(
        title = stringResource(R.string.next_news),
        messages = news.map { it.text(currency) },
        style = BannerStyle.Alert,
        actions = if (cancelling != null && onCancelAgain != null) {
            {
                Button(onClick = onCancelAgain) {
                    Text(
                        stringResource(
                            if (cancelling.type == ContractNews.Type.CONTRACT_STOPPED) when {
                                cancelling.isIncome -> R.string.next_income_cancel
                                cancelling.isReserve -> R.string.next_reserve_cancel
                                else -> R.string.next_contracts_cancel
                            } else when {
                                cancelling.isIncome -> R.string.next_news_income_cancel_again
                                cancelling.isReserve -> R.string.next_news_reserve_cancel_again
                                else -> R.string.next_news_cancel_again
                            }
                        )
                    )
                }
            }
        } else null
    )
}

/**
 * All news of the list, newest first, also those read already. Unread ones have a dot.
 *
 * @param onOpen opens the contract of a news, which marks it as read
 */
@Composable
internal fun NewsHistoryScreen(
    news: List<ContractNews>,
    currency: CurrencyInfo,
    onBack: () -> Unit,
    onOpen: (ContractNews) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = rememberDateFormatter()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        PageTopBar(onBack = onBack, title = stringResource(R.string.next_news), actions = {
            if (news.any { !it.isRead }) TopBarAction(
                Icons.Default.DoneAll,
                contentDescription = stringResource(R.string.next_news_mark_all_read),
                onClick = onMarkAllRead
            )
        })
        if (news.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.next_news_history_empty),
                hint = stringResource(R.string.next_news_history_empty_hint)
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            itemsIndexed(news, key = { _, item -> item.id }) { index, item ->
                GroupItem(GroupPosition.of(index, news.size)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(item) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Keeps the texts aligned, whether read or not
                        Box(Modifier.size(8.dp)) { if (!item.isRead) Dot() }
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.contractName.ifEmpty { NO_NAME },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    dateFormatter.format(item.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Text(
                                item.text(currency),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * All news of a contract, in its details
 */
@Composable
internal fun NewsHistorySection(news: List<ContractNews>, currency: CurrencyInfo) {
    val dateFormatter = rememberDateFormatter()
    SectionTitle(stringResource(R.string.next_news_history))
    DetailCard {
        news.forEachIndexed { index, item ->
            if (index > 0) RowDivider()
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    dateFormatter.format(item.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(item.text(currency), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

