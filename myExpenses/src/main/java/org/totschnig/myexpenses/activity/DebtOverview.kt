package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.DebtCard
import org.totschnig.myexpenses.compose.LocalHomeCurrency
import org.totschnig.myexpenses.compose.simpleStickyHeader
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.viewmodel.DebtOverViewViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel.Transaction
import org.totschnig.myexpenses.viewmodel.data.DisplayDebt
import java.time.LocalDate

class DebtOverview : DebtActivity() {
    override val debtViewModel: DebtOverViewViewModel by viewModels()

    val debts by lazy { debtViewModel.debts }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        binding.composeView.setContent {
            val debtsInfo = debts.collectAsState()

            AppTheme {
                val homeCurrency = LocalHomeCurrency.current
                val (sort, debts) = debtsInfo.value

                LaunchedEffect(debts) {
                    val total = debts.sumOf { it.currentEquivalentBalance }
                    toolbar.subtitle = currencyFormatter.formatMoney(
                        Money(homeCurrency, total)
                    )
                    setSignedToolbarColor(total)
                }
                val grouped = if (sort == Sort.PAYEE_NAME) {
                    debts.groupBy { it.payeeId }
                        .takeIf { it.values.any { group -> group.size > 1 } }
                } else null
                val nestedScrollInterop = rememberNestedScrollInteropConnection()
                if (grouped != null)
                    GroupedDebtList(
                        modifier = Modifier.nestedScroll(nestedScrollInterop),
                        debts = grouped,
                        loadTransactionsForDebt = { debt ->
                            debtViewModel.loadTransactions(debt)
                                .observeAsState(emptyList())
                        },
                        onEdit = this::editDebt,
                        onDelete = this::deleteDebt,
                        onToggle = this::toggleDebt,
                        onShare = { debt, exportFormat ->
                            this.shareDebt(
                                debt,
                                exportFormat,
                                null
                            )
                        },
                        onTransactionClick = {
                            showDetails(it)
                        }
                    )
                else
                    DebtList(
                        modifier = Modifier.nestedScroll(nestedScrollInterop),
                        debts = debts,
                        loadTransactionsForDebt = { debt ->
                            debtViewModel.loadTransactions(debt)
                                .observeAsState(emptyList())
                        },
                        onEdit = this::editDebt,
                        onDelete = this::deleteDebt,
                        onToggle = this::toggleDebt,
                        onShare = { debt, exportFormat ->
                            this.shareDebt(
                                debt,
                                exportFormat,
                                null
                            )
                        }
                    ) {
                        showDetails(it)
                    }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.debt_overview, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        lifecycleScope.launch {
            menu.findItem(R.id.SHOW_ALL_COMMAND).isChecked = debtViewModel.showAll().first()
            menu.findItem(R.id.SORT_MENU)?.subMenu
                ?.findItem(debtViewModel.sortOrder().first().commandId)
                ?.isChecked = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.SHOW_ALL_COMMAND -> {
                lifecycleScope.launch {
                    debtViewModel.persistShowAll(!item.isChecked)
                    invalidateOptionsMenu()
                }
                true
            }

            R.id.SORT_LABEL_COMMAND, R.id.SORT_AMOUNT_COMMAND, R.id.SORT_PAYEE_NAME_COMMAND -> {
                if (!item.isChecked) {
                    Sort.fromCommandId(item.itemId)?.let {
                        lifecycleScope.launch {
                            debtViewModel.persistSortOrder(it)
                            invalidateOptionsMenu()
                        }
                    }
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}

@Composable
fun GroupedDebtList(
    modifier: Modifier = Modifier,
    debts: Map<Long, List<DisplayDebt>>,
    loadTransactionsForDebt: @Composable (DisplayDebt) -> State<List<Transaction>>,
    onEdit: (DisplayDebt) -> Unit = {},
    onDelete: (DisplayDebt, Int) -> Unit = { _, _ -> },
    onToggle: (DisplayDebt) -> Unit = {},
    onShare: (DisplayDebt, DebtViewModel.ExportFormat) -> Unit = { _, _ -> },
    onTransactionClick: (Long) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        debts.forEach { item ->
            val list = item.value
            val currencies = list.map { it.currency }.distinct()
            simpleStickyHeader {
                Row(
                    modifier = it.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(list.first().payeeName)
                    if (currencies.size == 1) {
                        ColoredAmountText(
                            amount = list.sumOf { it.currentBalance },
                            currency = currencies.first()
                        )
                    } else {
                        ColoredAmountText(
                            amount = list.sumOf { it.currentEquivalentBalance },
                            currency = LocalHomeCurrency.current
                        )
                    }
                }
            }
            items(item.value) {
                val expandedState = rememberSaveable { mutableStateOf(false) }
                val transactions =
                    if (expandedState.value) loadTransactionsForDebt(it).value else emptyList()
                DebtCard(
                    debt = it,
                    transactions = transactions,
                    expanded = expandedState,
                    onEdit = { onEdit(it) },
                    onDelete = { count -> onDelete(it, count) },
                    onToggle = { onToggle(it) },
                    onShare = { format -> onShare(it, format) },
                    onTransactionClick = onTransactionClick
                )
            }
        }
    }
}

@Composable
fun DebtList(
    modifier: Modifier = Modifier,
    debts: List<DisplayDebt>,
    loadTransactionsForDebt: @Composable (DisplayDebt) -> State<List<Transaction>>,
    onEdit: (DisplayDebt) -> Unit = {},
    onDelete: (DisplayDebt, Int) -> Unit = { _, _ -> },
    onToggle: (DisplayDebt) -> Unit = {},
    onShare: (DisplayDebt, DebtViewModel.ExportFormat) -> Unit = { _, _ -> },
    onTransactionClick: (Long) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = debts) {
            val expandedState = rememberSaveable { mutableStateOf(false) }
            val transactions =
                if (expandedState.value) loadTransactionsForDebt(it).value else emptyList()
            DebtCard(
                debt = it,
                transactions = transactions,
                expanded = expandedState,
                onEdit = { onEdit(it) },
                onDelete = { count -> onDelete(it, count) },
                onToggle = { onToggle(it) },
                onShare = { format -> onShare(it, format) },
                onTransactionClick = onTransactionClick
            )
        }
    }
}

@Preview
@Composable
private fun GroupedDebtListPreview() {
    Surface(modifier = Modifier.padding(8.dp)) {
        GroupedDebtList(
            debts = listOf(
                DisplayDebt(
                    id = 1,
                    label = "Debt 1",
                    description = "some description",
                    payeeId = 1,
                    amount = 4000,
                    currency = CurrencyUnit.DebugInstance,
                    date = LocalDate.now().toEpoch(),
                    payeeName = "Joe Doe",
                    sum = 3000
                ),
                DisplayDebt(
                    id = 2,
                    label = "Debt 2",
                    description = "",
                    payeeId = 1,
                    amount = -500,
                    currency = CurrencyUnit.DebugInstance,
                    date = LocalDate.now().toEpoch(),
                    payeeName = "Joe Doe",
                    sum = -200
                )
            ).groupBy { it.payeeId},
            loadTransactionsForDebt = {
                remember { mutableStateOf(emptyList()) }
            }
        )
    }
}


@Preview
@Composable
private fun DebtListPreview() {
    Surface(modifier = Modifier.padding(8.dp)) {
        DebtList(
            debts = listOf(
                DisplayDebt(
                    id = 1,
                    label = "Debt 1",
                    description = "some description",
                    payeeId = 1,
                    amount = 4000,
                    currency = CurrencyUnit.DebugInstance,
                    date = LocalDate.now().toEpoch(),
                    payeeName = "Joe Doe",
                    sum = 3000
                ),
                DisplayDebt(
                    id = 2,
                    label = "Debt 2",
                    description = "",
                    payeeId = 2,
                    amount = -500,
                    currency = CurrencyUnit.DebugInstance,
                    date = LocalDate.now().toEpoch(),
                    payeeName = "Klara Masterful",
                    sum = -200
                )
            ),
            loadTransactionsForDebt = {
                remember { mutableStateOf(emptyList()) }
            }
        )
    }
}
