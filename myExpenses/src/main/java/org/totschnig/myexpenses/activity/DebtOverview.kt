package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.DebtCard
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.localDate2Epoch
import org.totschnig.myexpenses.viewmodel.DebtOverViewViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel.Transaction
import org.totschnig.myexpenses.viewmodel.data.Debt
import timber.log.Timber
import java.time.LocalDate

class DebtOverview : DebtActivity() {
    override val debtViewModel: DebtOverViewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        binding.composeView.setContent {
            AppTheme {
                val debts = debtViewModel.loadDebts().collectAsState(initial = emptyList())
                LaunchedEffect(debts.value) {
                    val total = debts.value.sumOf { it.currentBalance }
                    toolbar.subtitle = currencyFormatter.formatMoney(
                        Money(Utils.getHomeCurrency(), total)
                    )
                    setSignedToolbarColor(total)
                }
                DebtList(
                    debts = debts,
                    loadTransactionsForDebt = { debt ->
                        debtViewModel.loadTransactions(debt)
                            .observeAsState(emptyList()).value
                    },
                    onEdit = this::editDebt,
                    onDelete = this::deleteDebt,
                    onToggle = this::toggleDebt,
                    onShare = { debt, exportFormat -> this.shareDebt(debt, exportFormat, null) },
                    onTransactionClick = {
                        showDetails(it)
                    }
                )
            }
        }
    }

    override fun injectDependencies() {
        (applicationContext as MyApplication).appComponent.inject(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.debt_overview, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.SHOW_ALL_COMMAND).let {
            lifecycleScope.launch {
                it.isChecked = debtViewModel.showAll().first()
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.SHOW_ALL_COMMAND) {
            lifecycleScope.launch {
                debtViewModel.persistShowAll(!item.isChecked)
                invalidateOptionsMenu()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
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
    onShare: (Debt, DebtViewModel.ExportFormat) -> Unit = { _, _ -> },
    onTransactionClick: (Long) -> Unit = {}
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
                onShare = onShare,
                onTransactionClick = onTransactionClick
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
            }
        )
    }
}
