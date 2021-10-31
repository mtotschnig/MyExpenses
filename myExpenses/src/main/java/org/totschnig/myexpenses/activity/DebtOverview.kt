package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.DebugCurrencyFormatter
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.localDate2Epoch
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.time.LocalDate
import javax.inject.Inject

class DebtOverview : ProtectedFragmentActivity() {
    val viewModel: DebtViewModel by viewModels()

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadDebts()
        setContent {
            MdcTheme {
                DebtList(
                    viewModel.getDebts().observeAsState(emptyList())
                ) {
                    currencyFormatter.convAmount(it.amount, currencyContext[it.currency])
                }
            }
        }
    }

    override fun injectDependencies() {
        (applicationContext as MyApplication).appComponent.inject(this)
    }
}


@Composable
fun DebtList(
    debts: State<List<Debt>>,
    amountFormatter: ((Debt) -> String)? = null
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.title_activity_debt_overview)) }) }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .padding(horizontal = dimensionResource(id = R.dimen.padding_form)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)

        ) {
            items(items = debts.value) { item ->
                ItemRenderer(debt = item, amountFormatter)
            }
        }
    }
}

@Composable
fun ItemRenderer(
    debt: Debt,
    _amountFormatter: ((Debt) -> String)? = null
) {
    val amountFormatter = _amountFormatter ?: { DebugCurrencyFormatter.convAmount(it.amount, CurrencyUnit("EUR", "€", 2)) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = colorResource(id = R.color.cardBackground)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    fontWeight = FontWeight.Bold,
                    text = debt.label
                )
                Text(
                    text = stringResource(
                        id = if (debt.amount > 0) R.string.debt_owes_me else R.string.debt_I_owe,
                        debt.payeeName!!
                    )
                )
                debt.description.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        fontStyle = FontStyle.Italic,
                        text = debt.description
                    )
                }
            }
            Text(
                text = amountFormatter(debt),
                color = colorResource(id = if (debt.amount > 0) R.color.colorIncome else R.color.colorExpense)
            )
        }
    }
}

@Preview
@Composable
fun SingleDebtPreview() {
    Surface(modifier = Modifier.padding(8.dp)) {
        ItemRenderer(
            Debt(
                id = 1,
                label = "Debt 1",
                description = "some description",
                payeeId = -1L,
                amount = 4000,
                currency = "EUR",
                date = localDate2Epoch(LocalDate.now()),
                payeeName = "Joe Doe"
            )
        )
    }
}

@Preview
@Composable
fun DebtListPreview() {
    Surface(modifier = Modifier.padding(8.dp)) {
        DebtList(
            debts = mutableStateOf(
                listOf(
                    Debt(
                        id = 1,
                        label = "Debt 1",
                        description = "some description",
                        payeeId = 1,
                        amount = 4000,
                        currency = "EUR",
                        date = localDate2Epoch(LocalDate.now()),
                        payeeName = "Joe Doe"
                    ),
                    Debt(
                        id = 2,
                        label = "Debt 2",
                        description = "",
                        payeeId = 2,
                        amount = -500,
                        currency = "EUR",
                        date = localDate2Epoch(LocalDate.now()),
                        payeeName = "Klara Masterful"
                    )
                )
            )
        )
    }
}