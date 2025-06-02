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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
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
import org.totschnig.myexpenses.compose.scrollbar.LazyColumnWithScrollbarAndBottomPadding
import org.totschnig.myexpenses.compose.simpleStickyHeader
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.populateWithSync
import org.totschnig.myexpenses.viewmodel.BudgetListViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget

class ManageBudgets : ProtectedFragmentActivity() {

    private lateinit var binding: ActivityComposeBinding
    private val viewModel: BudgetListViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
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
                            text = stringResource(R.string.no_budgets),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                viewModel.importInfo.collectAsStateWithLifecycle().value?.second?.let { remotes ->
                    val importableBudgets =
                        remotes.filterIsInstance<BudgetListViewModel.Importable>()
                    val notImportableBudgets =
                        remotes.filterIsInstance<BudgetListViewModel.NotImportable>()
                    val selectedBudgets = remember { mutableStateListOf<String>() }
                    AlertDialog(
                        onDismissRequest = {
                            selectedBudgets.clear()
                            viewModel.importDialogDismissed()
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (selectedBudgets.isNotEmpty()) {
                                    viewModel.importBudgetsDo(selectedBudgets.toList())
                                } else {
                                    viewModel.importDialogDismissed()
                                }
                            }) {
                                Text(stringResource(if (selectedBudgets.isNotEmpty()) R.string.menu_import else R.string.menu_close))
                            }
                        },
                        text = {
                            if (remotes.isNotEmpty()) {
                                Column {
                                    if (importableBudgets.isNotEmpty()) {
                                        Text(stringResource(R.string.budget_import_can_be_imported))
                                        importableBudgets.forEach { (uuid, budget) ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = selectedBudgets.contains(uuid),
                                                    onCheckedChange = {
                                                        if (it) {
                                                            selectedBudgets.add(uuid)
                                                        } else {
                                                            selectedBudgets.remove(uuid)
                                                        }
                                                    }
                                                )
                                                Text(budget.title)
                                            }
                                        }
                                    }
                                    if (notImportableBudgets.isNotEmpty()) {
                                        Text(stringResource(R.string.budget_import_cannot_be_imported))
                                        notImportableBudgets.forEach { (uuid, title) ->
                                            Row {
                                                Text("$title ($uuid)")
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(stringResource(R.string.budget_import_none_found))
                            }
                        }
                    )
                }
                val nestedScrollInterop = rememberNestedScrollInteropConnection()
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollInterop)
                ) {
                    val breakpoint = 400.dp * LocalConfiguration.current.fontScale
                    val narrowScreen = maxWidth < breakpoint
                    LazyColumnWithScrollbarAndBottomPadding(
                        modifier = Modifier
                            .fillMaxWidth(),
                        itemsAvailable = data.sumOf { it.second.size },
                        groupCount = data.size
                    ) {
                        data.forEach { (header, list) ->
                            simpleStickyHeader(header)
                            list.forEachIndexed { index, budget ->
                                item(key = budget.id) {
                                    val currencyUnit = currencyContext[budget.currency]
                                    val allocated = remember { mutableLongStateOf(0) }
                                    val spent = remember { mutableLongStateOf(0) }
                                    val criteria =
                                        remember { mutableStateListOf<SimpleCriterion<*>>() }
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
                                        narrowScreen = narrowScreen,
                                        grouping = grouping,
                                        budget = budget,
                                        criteria = criteria,
                                        currencyUnit = currencyUnit,
                                        allocated = allocated.longValue,
                                        spent = spent.longValue
                                    ) {
                                        startActivity(
                                            Intent(
                                                this@ManageBudgets,
                                                BudgetActivity::class.java
                                            ).apply {
                                                this.putExtra(KEY_ROWID, budget.id)
                                                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            }
                                        )
                                    }
                                    if (index < list.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp))
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
        menu.findItem(R.id.SYNC_COMMAND_IMPORT)?.populateWithSync(
            GenericAccountService.getAccountNames(this)
        )
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

            Menu.NONE -> {
                if (item.groupId == R.id.SYNC_COMMAND_IMPORT) {
                    viewModel.importBudgets(item.title.toString())
                    true
                } else false
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
    budget: Budget,
    criteria: List<SimpleCriterion<*>>,
    currencyUnit: CurrencyUnit,
    allocated: Long,
    spent: Long,
    onClick: () -> Unit = {},
) {
    val available = allocated + spent
    val padding = Modifier
        .padding(
            horizontal = dimensionResource(R.dimen.padding_main_screen)
        )

    val amountPadding = if (narrowScreen)
        Modifier.padding(end = 8.dp) else Modifier.padding(vertical = 8.dp)

    @Composable
    fun Budgeted(headerModifier: Modifier = Modifier) {
        Text(stringResource(R.string.budget_table_header_budgeted), headerModifier)
        AmountText(
            modifier = amountPadding,
            textAlign = TextAlign.End,
            amount = allocated,
            currency = currencyUnit
        )
    }

    @Composable
    fun Spent(headerModifier: Modifier = Modifier) {
        Text(stringResource(R.string.budget_table_header_spent), headerModifier)
        AmountText(
            modifier = amountPadding,
            textAlign = TextAlign.End,
            amount = spent,
            currency = currencyUnit
        )
    }

    @Composable
    fun Available(headerModifier: Modifier = Modifier) {
        Text(stringResource(R.string.available), headerModifier)
        ColoredAmountText(
            textAlign = TextAlign.End,
            amount = available,
            currency = currencyUnit,
            withBorder = true
        )
    }

    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .clickable(onClick = onClick)) {
        Text(
            modifier = padding
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp),
            text = if (grouping == BudgetListViewModel.Grouping.Grouping && budget.grouping != Grouping.NONE)
                budget.title else budget.titleComplete(context)
        )
        ChipGroup(
            modifier = padding,
            budget = budget.takeIf { grouping != BudgetListViewModel.Grouping.Account },
            criteria = criteria
        )
        Row(
            padding
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(bottom = 4.dp),
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


            if (narrowScreen) {
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Budgeted()
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spent()
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Available()
                    }
                }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.End
                ) {
                    Budgeted(headerModifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.End
                ) {
                    Spent(headerModifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.End
                ) {
                    Available(headerModifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Preview(widthDp = 320, showBackground = true)
@Composable
fun BudgetItemNarrow() {
    BudgetItem(
        narrowScreen = true,
        grouping = BudgetListViewModel.Grouping.Grouping,
        budget = Budget(
            1, 1, "Budget", "Daily Expenses", "EUR", Grouping.MONTH, android.graphics.Color.CYAN,
            null as String?, null as String?, "Wallet with a long name", false
        ),
        criteria = listOf(
            PayeeCriterion("Payee 1"), TagCriterion("Tags"),
            MethodCriterion("Method")
        ),
        currencyUnit = CurrencyUnit.DebugInstance,
        allocated = 1000,
        spent = -500
    )
}

@Preview(widthDp = 480, showBackground = true)
@Composable
fun BudgetItemWide() {
    BudgetItem(
        narrowScreen = false,
        grouping = BudgetListViewModel.Grouping.Account,
        budget = Budget(
            1, 1, "Budget", "Daily Expenses", "EUR", Grouping.MONTH, android.graphics.Color.CYAN,
            null as String?, null as String?, "Wallet", false
        ),
        criteria = listOf(
            PayeeCriterion("Payee 1, Payee 2Payee 2Payee 2Payee 2Payee 2Payee 2Payee 2"),
            TagCriterion("Tags"),
            MethodCriterion("Method")
        ),
        currencyUnit = CurrencyUnit.DebugInstance,
        allocated = 1000,
        spent = -500
    )
}

