package org.totschnig.myexpenses.compose.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.compose.emToDp
import org.totschnig.myexpenses.compose.size
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.ui.asDateTimeFormatter
import org.totschnig.myexpenses.viewmodel.data.Trade
import org.totschnig.myexpenses.viewmodel.data.TradeType
import org.totschnig.myexpenses.viewmodel.data.getIndicatorCharForLabel
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class TradeEvent {
    Edit,
    Delete
}

fun TradeType.icon(): ImageVector = when (this) {
    TradeType.AssetTrade.BUY -> Icons.Default.ArrowUpward
    TradeType.AssetTrade.SELL -> Icons.Default.ArrowDownward
    TradeType.CashMovement.DEPOSIT -> Icons.Default.Add
    TradeType.CashMovement.WITHDRAW -> Icons.Default.Remove
    is TradeType.Transfer -> Icons.AutoMirrored.Filled.TrendingFlat
}

@Composable
fun TradeList(
    trades: LazyPagingItems<Trade>,
    modifier: Modifier = Modifier,
    renderType: RenderType = RenderType.New,
    onEvent: (TradeEvent, Trade) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.fab_related_bottom_padding))
    ) {
        items(trades.itemCount) { index ->
            trades[index]?.let { trade ->
                TradeRow(trade = trade, renderType = renderType) {
                    onEvent(it, trade)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun TradeRow(
    trade: Trade,
    renderType: RenderType = RenderType.New,
    onEvent: (TradeEvent) -> Unit
) {
    val showMenu = rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu.value = true }
            .conditional(renderType == RenderType.New) {
                heightIn(min = 48.dp)
            }
            .padding(
                horizontal = 16.dp,
                vertical = 3.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (renderType == RenderType.Legacy) {
            val context = LocalContext.current
            val dateFormatter = remember(context) {
                (Utils.ensureDateFormatWithShortYear(context) as SimpleDateFormat).asDateTimeFormatter
            }
            Text(
                modifier = Modifier.width(emToDp(4f)),
                text = trade.date.format(dateFormatter),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        // Column 1: Icon (Matching standard transaction list)
        Box(
            modifier = Modifier.size(30.sp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = trade.type.icon(),
                contentDescription = null,
                tint = if (trade.type.isIncoming) LocalColors.current.income else LocalColors.current.expense
            )
        }

        // Column 2: Details (Primary and Secondary info)
        Column(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .weight(1f)
        ) {
            if (renderType == RenderType.New && trade.type !is TradeType.CashMovement) {
                Text(trade.quantity.currencyUnit.description)
            }

            val headline = buildAnnotatedString {
                if (renderType == RenderType.Legacy && trade.type !is TradeType.CashMovement) {
                    append(trade.quantity.currencyUnit.description)
                    append(" ")
                }
                append(stringResource(trade.type.label))
                if (trade.type !is TradeType.CashMovement) {
                    val currencyFormatter = LocalCurrencyFormatter.current
                    val quantityFormatted = currencyFormatter.formatMoney(
                        trade.quantity
                    ) {
                        it.decimalFormatSymbols = it.decimalFormatSymbols.apply {
                            currencySymbol = ""
                        }
                    }
                    append(" $quantityFormatted x ")
                    append(
                        currencyFormatter.formatCurrency(
                            trade.price, trade.principal.currencyUnit
                        )
                    )
                }
                trade.peerAccount?.second?.let {
                    append(" ")
                    append(getIndicatorCharForLabel(!trade.type.isIncoming))
                    append(" ")
                    append(it)
                }

                if (renderType == RenderType.Legacy) {
                    trade.comment?.takeIf { it.isNotEmpty() }?.let {
                        append(COMMENT_SEPARATOR)
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(it)
                        }
                    }
                }
            }

            Text(
                text = headline,
                style = if (renderType == RenderType.Legacy) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (renderType == RenderType.New) {
                trade.comment?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Column 3: Amount and Date
        Column(horizontalAlignment = Alignment.End) {
            val amountColor = when (trade.type) {
                TradeType.AssetTrade.BUY, TradeType.CashMovement.WITHDRAW -> LocalColors.current.expense
                TradeType.AssetTrade.SELL, TradeType.CashMovement.DEPOSIT -> LocalColors.current.income
                is TradeType.Transfer -> LocalColors.current.transfer
            }

            AmountText(
                amount = trade.principal.amountMinor,
                currency = trade.principal.currencyUnit,
                color = amountColor,
                fontWeight = if (renderType == RenderType.Legacy) FontWeight.Normal else FontWeight.SemiBold,
                fontSize = if (renderType == RenderType.Legacy) 14.sp else 16.sp
            )

            if (renderType == RenderType.New) {
                Text(
                    text = trade.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        HierarchicalMenu(showMenu, tradeMenu(trade, onEvent))
    }
}

private fun tradeMenu(trade: Trade, onEvent: (TradeEvent) -> Unit): Menu = buildList {
    add(MenuEntry.edit("EDIT_TRADE") { onEvent(TradeEvent.Edit) })
    add(MenuEntry.delete("DELETE_TRADE") { onEvent(TradeEvent.Delete) })
}
