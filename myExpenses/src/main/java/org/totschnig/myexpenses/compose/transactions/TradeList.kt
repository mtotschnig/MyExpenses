package org.totschnig.myexpenses.compose.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.size
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.Trade
import org.totschnig.myexpenses.viewmodel.data.TradeType
import org.totschnig.myexpenses.viewmodel.data.getIndicatorCharForLabel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class TradeEvent {
    Edit,
    Delete
}

@Composable
fun TradeList(
    trades: LazyPagingItems<Trade>,
    modifier: Modifier = Modifier,
    onEvent: (TradeEvent, Trade) -> Unit,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(trades.itemCount) { index ->
            trades[index]?.let { trade ->
                TradeRow(trade = trade, onEvent = { onEvent(it, trade) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun TradeRow(
    trade: Trade,
    onEvent: (TradeEvent) -> Unit,
) {
    val showMenu = rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu.value = true }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Icon (Matching standard transaction list)
        Box(modifier = Modifier.size(30.sp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when (trade.type) {
                    TradeType.AssetTrade.BUY -> Icons.Default.ArrowUpward
                    TradeType.AssetTrade.SELL -> Icons.Default.ArrowDownward
                    TradeType.CashMovement.DEPOSIT -> Icons.Default.Add
                    TradeType.CashMovement.WITHDRAW -> Icons.Default.Remove
                },
                contentDescription = null,
                tint = if (trade.type is TradeType.AssetTrade.BUY || trade.type is TradeType.CashMovement.DEPOSIT)
                    LocalColors.current.income else LocalColors.current.expense
            )
        }

        // Column 2: Details (Primary and Secondary info)
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f)
        ) {
            if (trade.type is TradeType.AssetTrade) {
                Text(trade.quantity.currencyUnit.description)
            }
            val headline = stringResource(trade.type.label) +
                    if (trade.type is TradeType.AssetTrade) {
                        val currencyFormatter = LocalCurrencyFormatter.current
                        val quantityFormatted = currencyFormatter.formatMoney(
                            trade.quantity
                        ) {
                            it.decimalFormatSymbols = it.decimalFormatSymbols.apply {
                                currencySymbol = ""
                            }
                        }
                        " $quantityFormatted x " + currencyFormatter.formatCurrency(
                            trade.price, trade.principal.currencyUnit
                        )
                    } else trade.fundingAccount?.second?.let {
                        " " + getIndicatorCharForLabel(trade.type == TradeType.CashMovement.WITHDRAW) + " " + it
                    } ?: ""

            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            trade.comment?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Column 3: Amount and Date
        Column(horizontalAlignment = Alignment.End) {
            val amountColor =
                if (trade.type is TradeType.AssetTrade.BUY || trade.type is TradeType.CashMovement.WITHDRAW)
                    LocalColors.current.expense else LocalColors.current.income

            AmountText(
                amount = trade.principal.amountMinor,
                currency = trade.principal.currencyUnit,
                color = amountColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Text(
                text = trade.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                style = MaterialTheme.typography.bodySmall
            )
        }
        HierarchicalMenu(showMenu, tradeMenu(trade, onEvent))
    }
}

private fun tradeMenu(trade: Trade, onEvent: (TradeEvent) -> Unit): Menu = buildList {
    add(MenuEntry.edit("EDIT_TRADE") { onEvent(TradeEvent.Edit) })
    add(MenuEntry.delete("DELETE_TRADE") { onEvent(TradeEvent.Delete) })
}
