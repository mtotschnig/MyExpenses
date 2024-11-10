package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.ChipGroup
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.DonutInABox
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.compose.simpleStickyHeader
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.viewmodel.BudgetListViewModel
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
                val (grouping, data) = viewModel.dataGrouped.collectAsStateWithLifecycle()
                    .value
                if (data.isEmpty()) {
                    Box {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.no_budgets)
                        )
                    }
                }
                val nestedScrollInterop = rememberNestedScrollInteropConnection()
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollInterop)
                ) {
                    val breakpoint = 400.dp
                    val narrowScreen = maxWidth < breakpoint
                    LazyColumn(
                        modifier = Modifier
                            .testTag(TEST_TAG_LIST)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        data.forEach { (header, list) ->
                            simpleStickyHeader(header)
                            list.forEachIndexed { index, budget ->
                                item(key = budget.id) {
                                    val currencyUnit = currencyContext[budget.currency]
                                    val allocated = remember { mutableLongStateOf(0) }
                                    val spent = remember { mutableLongStateOf(0) }
                                    val criteria = remember { mutableStateListOf<Criterion<*>>() }
                                    LaunchedEffect(budget.id) {
                                        viewModel.budgetAmounts(budget).collect { (s, a) ->
                                            allocated.longValue = a
                                            spent.longValue = s
                                        }
                                    }
                                    LaunchedEffect(budget.id) {
                                        viewModel.budgetCriteria(budget).collect {
                                            criteria.clear()
                                            criteria.addAll(it)
                                        }
                                    }

                                    BudgetItem(
                                        narrowScreen,
                                        grouping,
                                        Modifier
                                            .padding(
                                                horizontal = dimensionResource(R.dimen.padding_main_screen)
                                            )
                                            .clickable {
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
                                        criteria,
                                        currencyUnit,
                                        allocated.longValue,
                                        spent.longValue
                                    )
                                    if (index < list.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.budgets, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        lifecycleScope.launch {
            menu.findItem(R.id.GROUPING_COMMAND)?.subMenu
                ?.findItem(viewModel.grouping().first().commandId)
                ?.isChecked = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.GROUPING_BUDGETS_ACCOUNT_COMMAND, R.id.GROUPING_BUDGETS_GROUPING_COMMAND -> {
                if (!item.isChecked) {
                    BudgetListViewModel.Grouping.entries.find { it.commandId == item.itemId }?.let {
                        lifecycleScope.launch {
                            viewModel.setGrouping(it)
                            invalidateOptionsMenu()
                        }
                    }
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
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
    grouping: BudgetListViewModel.Grouping,
    modifier: Modifier,
    budget: Budget,
    criteria: List<Criterion<*>>,
    currencyUnit: CurrencyUnit,
    allocated: Long,
    spent: Long,
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = if (grouping == BudgetListViewModel.Grouping.Grouping && budget.grouping != Grouping.NONE)
                budget.title else budget.titleComplete(context)
        )
        ChipGroup(
            budget = budget.takeIf { grouping != BudgetListViewModel.Grouping.Account },
            criteria = criteria
        )
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
        BudgetListViewModel.Grouping.Account,
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
        BudgetListViewModel.Grouping.Account,
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

