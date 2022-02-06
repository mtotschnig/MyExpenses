package org.totschnig.myexpenses.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import arrow.core.Either
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.localDate2Epoch
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.Debt
import timber.log.Timber
import java.time.LocalDate

typealias AmountFormatter = ((Long, String) -> String)

@Composable
fun DebtCard(
    debt: Debt,
    transactions: List<DebtViewModel.Transaction>,
    expanded: MutableState<Boolean>,
    onEdit: (Debt) -> Unit,
    onDelete: (Debt, Int) -> Unit,
    onToggle: (Debt) -> Unit,
    onShare: (Debt, DebtViewModel.ExportFormat) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = { expanded.value = !expanded.value }),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = colorResource(id = R.color.cardBackground)
    ) {
        DebtRenderer(
            debt,
            transactions,
            expanded.value,
            onEdit,
            onDelete,
            onToggle,
            onShare
        )
    }
}

@Composable
fun DebtRenderer(
    debt: Debt,
    transactions: List<DebtViewModel.Transaction>,
    expanded: Boolean,
    onEdit: (Debt) -> Unit = {},
    onDelete: (Debt, Int) -> Unit = { _, _ -> },
    onToggle: (Debt) -> Unit = {},
    onShare: (Debt, DebtViewModel.ExportFormat) -> Unit = {_,_ -> }
) {
    CompositionLocalProvider(
        LocalColors provides Colors(
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
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expanded) {
                        Initials(
                            name = debt.payeeName!!,
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
                            style = MaterialTheme.typography.h6,
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
                            debt.currentBalance,
                            debt.currency
                        )
                    }
                }
                if (expanded) {
                    TransactionRenderer(
                        transaction = DebtViewModel.Transaction(
                            0, epoch2LocalDate(debt.date), 0, debt.amount
                        ),
                        debt.currency,
                        false
                    )
                    val count = transactions.size
                    transactions.forEachIndexed { index, transaction ->
                        TransactionRenderer(
                            transaction = transaction,
                            debt.currency,
                            index == count - 1
                        )
                    }
                }
            }
            if (expanded) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    OverFlowMenu(
                        menu = Menu(buildList {
                            if(!debt.isSealed) {
                                add(MenuEntry(stringResource(id = R.string.menu_edit),  Either.Left {
                                    onEdit(debt)
                                }))
                            }
                            add(MenuEntry(stringResource(id = if (debt.isSealed) R.string.menu_reopen else R.string.menu_close),  Either.Left {
                                onToggle(debt)
                            }))
                            add(MenuEntry(stringResource(id = R.string.menu_delete),  Either.Left {
                                onDelete(debt, transactions.size)
                            }))
                            add(
                                MenuEntry(stringResource(id = R.string.button_label_share_file), Either.Right(
                                Menu(
                                    DebtViewModel.ExportFormat.values().map {
                                        MenuEntry(it.name, Either.Left {
                                            onShare(debt, it)
                                        })
                                    }
                                )
                            )))
                        })
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRenderer(
    transaction: DebtViewModel.Transaction,
    currency: String,
    boldBalance: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {

        when {
            transaction.runningTotal == 0L -> R.drawable.ic_check
            transaction.trend > 0 -> R.drawable.ic_trending_up
            transaction.trend < 0 -> R.drawable.ic_trending_down
            else -> null
        }?.also {
            Icon(
                painter = painterResource(
                    id = when {
                        transaction.runningTotal == 0L -> R.drawable.ic_check
                        transaction.trend > 0 -> R.drawable.ic_trending_up
                        transaction.trend < 0 -> R.drawable.ic_trending_down
                        else -> throw IllegalStateException()
                    }
                ),
                contentDescription = null
            )
        } ?: run {
            Spacer(modifier = Modifier.size(24.dp))
        }

        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = LocalDateFormatter.current.format(transaction.date)
        )

        transaction.amount.takeIf { it != 0L }?.let {
            Text(
                modifier = Modifier.weight(1F),
                textAlign = TextAlign.End,
                text = LocalAmountFormatter.current(it, currency)
            )
        }

        ColoredAmountText(
            amount = transaction.runningTotal,
            currency = currency,
            modifier = Modifier.weight(1F),
            fontWeight = if (boldBalance) FontWeight.Bold else null,
            textAlign = TextAlign.End
        )
    }
}

@Preview
@Composable
fun SingleDebtPreview() {
    val debt = Debt(
        id = 1,
        label = "Debt 1",
        description = "some long, very long, extremely long description",
        payeeId = -1L,
        amount = 4000,
        currency = "EUR",
        date = localDate2Epoch(LocalDate.now()),
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