package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.DebtCard
import org.totschnig.myexpenses.compose.Navigation
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.localDate2Epoch
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel.Transaction
import org.totschnig.myexpenses.viewmodel.data.Debt
import timber.log.Timber
import java.time.LocalDate

class DebtOverview : DebtActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        debtViewModel.loadDebts()
        setContent {
            AppTheme(this) {
                val debts = debtViewModel.getDebts().observeAsState(emptyList())
                Navigation(
                    onNavigation = { finish() },
                    title = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = dimensionResource(id = R.dimen.padding_form)),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(id = R.string.title_activity_debt_overview),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.h6
                            )
                            ColoredAmountText(
                                amount = debts.value.sumOf { it.currentBalance },
                                currency = Utils.getHomeCurrency(),
                            )
                        }
                    }
                ) {
                    DebtList(
                        modifier = Modifier.padding(paddingValues = it),
                        debts = debts,
                        loadTransactionsForDebt = { debt ->
                            debtViewModel.loadTransactions(debt)
                                .observeAsState(emptyList()).value
                        },
                        onEdit = this::editDebt,
                        onDelete = this::deleteDebt,
                        onToggle = this::toggleDebt,
                        onShare = this::shareDebt
                    )
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
    modifier: Modifier = Modifier,
    debts: State<List<Debt>>,
    loadTransactionsForDebt: @Composable (Debt) -> List<Transaction>,
    onEdit: (Debt) -> Unit = {},
    onDelete: (Debt, Int) -> Unit = { _, _ -> },
    onToggle: (Debt) -> Unit = {},
    onShare: (Debt, DebtViewModel.ExportFormat) -> Unit = { _, _ -> }
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
            DebtCard(
                debt = item,
                transactions = loadTransactionsForDebt(item),
                expanded = expandedState,
                onEdit = onEdit,
                onDelete = onDelete,
                onToggle = onToggle,
                onShare = onShare
            )
        }
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
                            currency = CurrencyUnit.DebugInstance,
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
                            currency = CurrencyUnit.DebugInstance,
                            date = localDate2Epoch(LocalDate.now()),
                            payeeName = "Klara Masterful",
                            sum = -200
                        )
                    )
                )
            },
            loadTransactionsForDebt = {
                emptyList()
            },
        )
    }
}
