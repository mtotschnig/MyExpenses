package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.Colors
import org.totschnig.myexpenses.compose.Initials
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.Navigation
import org.totschnig.myexpenses.compose.OverFlowMenu
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.DebugCurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.getDateTimeFormatter
import org.totschnig.myexpenses.util.localDate2Epoch
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel.Transaction
import org.totschnig.myexpenses.viewmodel.data.Debt
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class DebtOverview : DebtActivity() {
    val viewModel: DebtViewModel by viewModels()

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as MyApplication).appComponent.inject(viewModel)
        viewModel.loadDebts()
        setContent {
            CompositionLocalProvider(
                LocalColors provides Colors(
                    income = colorResource(id = R.color.colorIncome),
                    expense = colorResource(id = R.color.colorExpense)
                )
            ) {
                MdcTheme {
                    val amountFormatter = { amount: Long, currency: String ->
                        currencyFormatter.convAmount(amount, currencyContext[currency])
                    }
                    val debts = viewModel.getDebts().observeAsState(emptyList())
                    Navigation(
                        onNavigation = { finish() },
                        title = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = dimensionResource(id = R.dimen.padding_form)),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(id = R.string.title_activity_debt_overview))
                                ColoredAmountText(
                                    amount = debts.value.sumOf { it.currentBalance },
                                    currency = Utils.getHomeCurrency().code,
                                    amountFormatter = amountFormatter
                                )
                            }
                        }
                    ) {
                        DebtList(
                            modifier = Modifier.padding(paddingValues = it),
                            debts = debts,
                            loadTransactionsForDebt = { debt ->
                                viewModel.loadTransactions(debt).observeAsState(emptyList())
                            },
                            amountFormatter = amountFormatter,
                            dateFormatter = getDateTimeFormatter(this),
                            onEdit = this::editDebt,
                            onDelete = this::deleteDebt
                        )
                    }
                }
            }
        }
    }

    override fun injectDependencies() {
        (applicationContext as MyApplication).appComponent.inject(this)
    }

    override fun deleteDebtDo(debtId: Long) {
        viewModel.deleteDebt(debtId).observe(this) {
            if (!it) {
                lifecycleScope.launchWhenResumed {
                    showSnackbar("ERROR", Snackbar.LENGTH_LONG, null)
                }
            }
        }
    }
}

@Composable
fun DebtList(
    modifier: Modifier = Modifier,
    debts: State<List<Debt>>,
    loadTransactionsForDebt: @Composable (Debt) -> State<List<Transaction>>,
    amountFormatter: ((Long, String) -> String)? = null,
    dateFormatter: DateTimeFormatter? = null,
    onEdit: (Debt) -> Unit = {},
    onDelete: (Debt, Int) -> Unit = { _, _ -> }
) {
    LazyColumn(
        modifier = modifier
            .padding(horizontal = dimensionResource(id = R.dimen.padding_form)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(items = debts.value) { index, item ->
            Timber.d("rendering item $index")
            val expandedState = rememberSaveable { mutableStateOf(false) }
            DebtRenderer(
                debt = item,
                loadTransactionsForDebt(item),
                amountFormatter,
                dateFormatter,
                expandedState,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
fun DebtRenderer(
    debt: Debt,
    transactions: State<List<Transaction>>,
    _amountFormatter: ((Long, String) -> String)? = null,
    _dateFormatter: DateTimeFormatter? = null,
    expanded: MutableState<Boolean>,
    onEdit: (Debt) -> Unit = {},
    onDelete: (Debt, Int) -> Unit = { _, _ -> }
) {
    val amountFormatter = _amountFormatter ?: { amount, currency ->
        DebugCurrencyFormatter.convAmount(amount, CurrencyUnit(currency, "€", 2))
    }
    val dateFormatter = _dateFormatter ?: DateTimeFormatter.BASIC_ISO_DATE
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = colorResource(id = R.color.cardBackground)
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
                        .clickable(onClick = { expanded.value = !expanded.value })
                        .padding(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val signum = debt.amount > 0
                        if (expanded.value) {
                            Initials(
                                name = debt.payeeName!!,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1F)) {
                            Text(
                                style = MaterialTheme.typography.h6,
                                text = debt.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!expanded.value) {
                                Text(
                                    text = stringResource(
                                        id = if (signum) R.string.debt_owes_me else R.string.debt_I_owe,
                                        debt.payeeName!!
                                    ),
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
                        if (!expanded.value) {
                            ColoredAmountText(
                                debt.currentBalance,
                                debt.currency,
                                amountFormatter
                            )
                        }
                    }
                    if (expanded.value) {
                        TransactionRenderer(
                            transaction = Transaction(
                                0, epoch2LocalDate(debt.date), 0, debt.amount
                            ),
                            debt.currency,
                            amountFormatter,
                            dateFormatter,
                            false
                        )
                        val count = transactions.value.size
                        transactions.value.forEachIndexed { index, transaction ->
                            TransactionRenderer(
                                transaction = transaction,
                                debt.currency,
                                amountFormatter,
                                dateFormatter,
                                index == count - 1
                            )
                        }
                    }
                }
                if (expanded.value) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        OverFlowMenu(
                            content = listOf(
                                stringResource(id = R.string.menu_edit) to { onEdit(debt) },
                                stringResource(id = R.string.menu_delete) to {
                                    onDelete(
                                        debt,
                                        transactions.value.size
                                    )
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColoredAmountText(
    amount: Long,
    currency: String,
    amountFormatter: (Long, String) -> String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null
) {
    Text(
        modifier = modifier,
        fontWeight = fontWeight,
        textAlign = textAlign,
        text = amountFormatter(amount, currency),
        color = if (amount > 0) LocalColors.current.income else LocalColors.current.income
    )
}

@Composable
fun TransactionRenderer(
    transaction: Transaction,
    currency: String,
    amountFormatter: (Long, String) -> String,
    dateFormatter: DateTimeFormatter,
    boldBalance: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (transaction.trend != 0) {
            Icon(
                painter = painterResource(
                    id = if (transaction.trend > 0) R.drawable.ic_trending_up else R.drawable.ic_trending_down
                ),
                contentDescription = null
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = dateFormatter.format(transaction.date)
        )
        transaction.amount.takeIf { it != 0L }?.let {
            Text(
                modifier = Modifier.weight(1F),
                textAlign = TextAlign.End,
                text = amountFormatter(it, currency)
            )
        }
        ColoredAmountText(
            amount = transaction.runningTotal,
            currency = currency,
            amountFormatter = amountFormatter,
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
        Transaction(
            1, LocalDate.now(), 100, 100, 1
        ),
        Transaction(
            1, LocalDate.now(), 3000, 1000, 0
        ),
        Transaction(
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
            transactions = remember { mutableStateOf(transactions) },
            expanded = remember { mutableStateOf(false) }
        )
        DebtRenderer(
            debt = debt,
            transactions = remember { mutableStateOf(transactions) },
            expanded = remember { mutableStateOf(true) }
        )
    }
}

@Preview
@Composable
fun DebtListPreview() {
    Surface(modifier = Modifier.padding(8.dp)) {
        DebtList(
            debts = remember {
                mutableStateOf(
                    listOf(
                        Debt(
                            id = 1,
                            label = "Debt 1",
                            description = "some description",
                            payeeId = 1,
                            amount = 4000,
                            currency = "EUR",
                            date = localDate2Epoch(LocalDate.now()),
                            payeeName = "Joe Doe",
                            sum = 3000
                        ),
                        Debt(
                            id = 2,
                            label = "Debt 2",
                            description = "",
                            payeeId = 2,
                            amount = -500,
                            currency = "EUR",
                            date = localDate2Epoch(LocalDate.now()),
                            payeeName = "Klara Masterful",
                            sum = -200
                        )
                    )
                )
            },
            loadTransactionsForDebt = {
                remember {
                    mutableStateOf(emptyList())
                }
            }
        )
    }
}
