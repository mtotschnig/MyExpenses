package org.totschnig.myexpenses.compose

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.MenuEntry.Companion.delete
import org.totschnig.myexpenses.compose.MenuEntry.Companion.edit
import org.totschnig.myexpenses.compose.MenuEntry.Companion.toggle
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.DisplayDebt
import timber.log.Timber
import java.time.LocalDate
import kotlin.math.absoluteValue
import kotlin.math.sign

@Composable
fun DebtCard(
    debt: DisplayDebt,
    transactions: List<DebtViewModel.Transaction>,
    expanded: MutableState<Boolean>,
    onEdit: () -> Unit,
    onDelete: (Int) -> Unit,
    onToggle: () -> Unit,
    onShare: (DebtViewModel.ExportFormat) -> Unit,
    onTransactionClick: (Long) -> Unit
) {
    val cornerSize = 8.dp
    val horizontalPadding = dimensionResource(id = R.dimen.padding_main_screen) - cornerSize
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .animateContentSize()
            .clickable(onClick = { expanded.value = !expanded.value }),
        shape = RoundedCornerShape(cornerSize),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.cardBackground))
    ) {
        DebtRenderer(
            debt,
            transactions,
            expanded.value,
            onEdit,
            onDelete,
            onToggle,
            onShare,
            onTransactionClick
        )
    }
}

@Composable
fun DebtRenderer(
    debt: DisplayDebt,
    transactions: List<DebtViewModel.Transaction>,
    expanded: Boolean,
    onEdit: () -> Unit = {},
    onDelete: (Int) -> Unit = {},
    onToggle: () -> Unit = {},
    onShare: (DebtViewModel.ExportFormat) -> Unit = {},
    onTransactionClick: (Long) -> Unit = {}
) {

    val homeCurrency = LocalHomeCurrency.current
    val showEquivalentAmount =
        rememberSaveable { mutableStateOf((debt.currency.code == homeCurrency.code)) }

    CompositionLocalProvider(
        LocalColors provides LocalColors.current.copy(
            income = colorResource(id = R.color.colorIncomeOnCard),
            expense = colorResource(id = R.color.colorExpenseOnCard)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            Timber.d("rendering Card")
            Column(
                modifier = Modifier
                    .padding(8.dp)
            ) {
                val currency = if (showEquivalentAmount.value) homeCurrency else debt.currency
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expanded) {
                        Initials(
                            name = debt.payeeName,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    if (debt.isSealed) {
                        Icon(
                            modifier = Modifier.padding(end = 4.dp),
                            painter = painterResource(id = R.drawable.ic_lock),
                            contentDescription = stringResource(
                                id = R.string.content_description_closed
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1F)) {
                        Text(
                            style = MaterialTheme.typography.titleLarge,
                            text = debt.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!expanded) {
                            Text(
                                text = debt.title(LocalContext.current),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        debt.description.takeIf { it.isNotEmpty() }?.let {
                            Text(
                                fontStyle = FontStyle.Italic,
                                text = debt.description,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!expanded) {
                        ColoredAmountText(
                            if (showEquivalentAmount.value) debt.currentEquivalentBalance else debt.currentBalance,
                            currency
                        )
                    }
                }
                val start = if (showEquivalentAmount.value) debt.equivalentAmount
                    ?: debt.amount else debt.amount
                if (expanded) {
                    TransactionRenderer(
                        date = epoch2LocalDate(debt.date),
                        amount = 0,
                        runningTotal = start,
                        currency,
                        boldBalance = false,
                        trendIcon = null
                    )
                    val count = transactions.size
                    transactions.forEachIndexed { index, transaction ->

                        val runningTotal = if (showEquivalentAmount.value)
                            transaction.equivalentRunningTotal
                        else
                            transaction.runningTotal

                        val previousBalance = if (index == 0)
                            start
                        else with(transactions[index - 1]) {
                            if (showEquivalentAmount.value) equivalentRunningTotal else runningTotal
                        }

                        val trend = if (previousBalance.sign * runningTotal.sign == -1)
                            0
                        else
                            runningTotal.absoluteValue.compareTo(previousBalance.absoluteValue)

                        val trendIcon = when {
                            runningTotal == 0L -> R.drawable.ic_check
                            trend > 0 -> R.drawable.ic_debt_up
                            trend < 0 -> R.drawable.ic_debt_down
                            else -> R.drawable.ic_swap_vert
                        }

                        TransactionRenderer(
                            date = transaction.date,
                            amount = if (showEquivalentAmount.value) transaction.equivalentAmount else transaction.amount,
                            runningTotal = runningTotal,
                            currency,
                            index == count - 1,
                            onTransactionClick = { onTransactionClick(transaction.id) },
                            trendIcon = trendIcon
                        )
                    }
                }
            }
            if (expanded) {
                OverFlowMenu(
                    modifier = Modifier.align(Alignment.TopEnd),
                    menu = Menu(buildList {
                        if (!debt.isSealed) {
                            add(edit("EDIT_DEBT") { onEdit() })
                        }
                        add(toggle("DEBT", debt.isSealed) { onToggle() })
                        add(delete("DELETE_DEBT") { onDelete(transactions.size) })
                        add(
                            SubMenuEntry(
                                icon = Icons.Filled.Share,
                                label = R.string.share,
                                subMenu = Menu(
                                    DebtViewModel.ExportFormat.entries.map { format ->
                                        MenuEntry(
                                            label = format.resId,
                                            command = "SHARE_DEBT_$format"
                                        ) {
                                            onShare(format)
                                        }
                                    }
                                )
                            ))
                        if (debt.currency.code != homeCurrency.code) {
                            add(
                                CheckableMenuEntry(
                                    label = R.string.menu_equivalent_amount,
                                    command = "DEBT_EQUIVALENT",
                                    showEquivalentAmount.value
                                ) {
                                    showEquivalentAmount.value = !showEquivalentAmount.value
                                }
                            )
                        }
                    })
                )
            }
        }
    }
}

@Composable
fun TransactionRenderer(
    date: LocalDate,
    amount: Long,
    runningTotal: Long,
    currency: CurrencyUnit,
    boldBalance: Boolean,
    @DrawableRes trendIcon: Int? = null,
    onTransactionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.optional(onTransactionClick) {
            clickable(onClick = it)
        },
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = LocalDateFormatter.current.format(date),
            fontWeight = FontWeight.Light
        )

        amount.takeIf { it != 0L }?.let {
            Text(
                modifier = Modifier.weight(1F),
                textAlign = TextAlign.End,
                text = LocalCurrencyFormatter.current.convAmount(it, currency)
            )
        }

        ColoredAmountText(
            amount = runningTotal,
            currency = currency,
            modifier = Modifier.weight(1F),
            fontWeight = if (boldBalance) FontWeight.Bold else null,
            textAlign = TextAlign.End
        )
        if (trendIcon != null) {
            Icon(
                painter = painterResource(id = trendIcon),
                contentDescription = null
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Preview
@Composable
private fun SingleDebtPreview() {
    val debt = DisplayDebt(
        id = 1,
        label = "Debt 1",
        description = "some long, very long, extremely long description",
        payeeId = -1L,
        amount = 4000,
        currency = CurrencyUnit.DebugInstance,
        date = LocalDate.now().toEpoch(),
        payeeName = "Joe Doe"
    )
    val transactions = listOf(
        DebtViewModel.Transaction(
            1, LocalDate.now(), 100, 100, 1
        ),
        DebtViewModel.Transaction(
            1, LocalDate.now(), 3000, 1000, 0
        ),
        DebtViewModel.Transaction(
            1, LocalDate.now(), 10000, 10000, 1
        )
    )
    Column(
        modifier = Modifier
            .width(350.dp)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DebtRenderer(
            debt = debt,
            transactions = transactions,
            expanded = false
        )
        DebtRenderer(
            debt = debt,
            transactions = transactions,
            expanded = true
        )
    }
}