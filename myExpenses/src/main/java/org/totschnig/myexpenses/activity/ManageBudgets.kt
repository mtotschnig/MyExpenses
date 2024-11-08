package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.DonutInABox
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.viewmodel.BudgetListViewModel
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget
import kotlin.getValue

class ManageBudgets : ProtectedFragmentActivity() {

    private lateinit var binding: ActivityComposeBinding
    private val viewModel: BudgetListViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        floatingActionButton = binding.fab.CREATECOMMAND.also {
            it.isVisible = true
        }
        setContentView(binding.root)
        setupToolbar(true)
        injector.inject(viewModel)
        setTitle(R.string.menu_budget)
        binding.composeView.setContent {
            AppTheme {
                val data = viewModel.data.collectAsStateWithLifecycle(emptyList()).value
                if (data.isEmpty()) {
                    Box {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.no_budgets)
                        )
                    }
                }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val breakpoint = 400.dp
                    val narrowScreen = maxWidth < breakpoint
                    LazyColumn(
                        modifier = Modifier
                            .padding(
                                horizontal = dimensionResource(R.dimen.padding_main_screen)
                            )
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        itemsIndexed(data) { index, budget ->
                            val currencyUnit = currencyContext[budget.currency]
                            val allocated = remember { mutableLongStateOf(0) }
                            val spent = remember { mutableLongStateOf(0) }
                            LaunchedEffect(budget.id) {
                                viewModel.budgetAmounts(budget).collect { (s, a) ->
                                    allocated.longValue = a
                                    spent.longValue = s
                                }
                            }

                            BudgetItem(
                                narrowScreen,
                                Modifier.clickable {
                                    startActivity(
                                        Intent(
                                            this@ManageBudgets,
                                            BudgetActivity::class.java
                                        ).apply {
                                            this.putExtra(KEY_ROWID, budget.id)
                                            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        }
                                    )
                                },
                                budget,
                                FilterPersistence(
                                    prefHandler,
                                    BudgetViewModel.prefNameForCriteria(budget.id),
                                    null,
                                    immediatePersist = false,
                                    restoreFromPreferences = true
                                ).whereFilter.criteria,
                                currencyUnit,
                                allocated.longValue,
                                spent.longValue
                            )
                            if (index < data.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    override val fabDescription = R.string.menu_create_budget

    override val fabActionName = "CREATE_BUDGET"

    override fun onFabClicked() {
        super.onFabClicked()
        startActivity(Intent(this, BudgetEdit::class.java))
    }
}

@Composable
fun BudgetItem(
    narrowScreen: Boolean,
    modifier: Modifier,
    budget: Budget,
    criteria: List<Criterion<*>>,
    currencyUnit: CurrencyUnit,
    allocated: Long,
    spent: Long,
) {

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = budget.titleComplete(LocalContext.current)
        )
        ChipGroup(budget, criteria)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val progress = if (allocated == 0L) 100f else (-spent * 100f / allocated)
            DonutInABox(
                modifier = Modifier
                    .size(42.dp),
                progress = progress,
                fontSize = 12.sp,
                color = Color(budget.color),
                excessColor = LocalColors.current.expense
            )
            val available = allocated + spent

            Column(Modifier.weight(1f)) {
                if (narrowScreen) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.budget_table_header_budgeted))
                        AmountText(
                            textAlign = TextAlign.End,
                            amount = allocated,
                            currency = currencyUnit
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.budget_table_header_spent))
                        AmountText(
                            textAlign = TextAlign.End,
                            amount = spent,
                            currency = currencyUnit
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.available))
                        ColoredAmountText(
                            textAlign = TextAlign.End,
                            amount = available,
                            currency = currencyUnit,
                            withBorder = true
                        )
                    }
                } else {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.budget_table_header_budgeted)
                        )
                        Text(
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.budget_table_header_spent)
                        )
                        Text(
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.available)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AmountText(
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                            amount = allocated, currency = currencyUnit
                        )
                        AmountText(
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                            amount = spent, currency = currencyUnit
                        )
                        Box(Modifier.weight(1f)) {
                            ColoredAmountText(
                                textAlign = TextAlign.End,
                                modifier = Modifier.align(Alignment.CenterEnd),
                                amount = available,
                                currency = currencyUnit,
                                withBorder = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(budget: Budget, criteria: List<Criterion<*>>) {
    val context = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterItem(budget.label(context))
        criteria.forEach {
            FilterItem(it.prettyPrint(context))
        }
    }
}

@Composable
fun FilterItem(filter: String) {
    FilterChip(
        selected = false,
        label = {
            Text(filter)
        },
        onClick = {}
    )
}

@Preview(widthDp = 320, showBackground = true)
@Composable
fun BudgetItemNarrow() {
    BudgetItem(
        true,
        Modifier,
        Budget(
            1, 1, "Budget", "Daily Expenses", "EUR", Grouping.MONTH, android.graphics.Color.CYAN,
            null as String?, null as String?, "Wallet", false
        ),
        emptyList(),
        CurrencyUnit.DebugInstance,
        1000,
        -500
    )
}

@Preview(widthDp = 320, showBackground = true)
@Composable
fun BudgetItemWide() {
    BudgetItem(
        false,
        Modifier,
        Budget(
            1, 1, "Budget", "Daily Expenses", "EUR", Grouping.MONTH, android.graphics.Color.CYAN,
            null as String?, null as String?, "Wallet", false
        ),
        emptyList(),
        CurrencyUnit.DebugInstance,
        1000,
        -500
    )
}

