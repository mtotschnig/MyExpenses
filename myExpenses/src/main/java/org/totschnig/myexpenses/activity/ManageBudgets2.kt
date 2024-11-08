package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.DonutInABox
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.viewmodel.BudgetListViewModel
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget
import kotlin.getValue

class ManageBudgets2 : ProtectedFragmentActivity() {

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
                LazyColumn(
                    modifier = Modifier
                        .padding(
                            horizontal = dimensionResource(R.dimen.padding_main_screen)
                        )
                        .fillMaxWidth()
                ) {
                    data.forEach { budget ->
                        item(budget.id) {
                            val allocated = remember { mutableLongStateOf(0) }
                            val spent = remember { mutableLongStateOf(0) }
                            LaunchedEffect(budget.id) {
                                viewModel.budgetAmounts(budget).collect { (s, a) ->
                                    allocated.longValue = a
                                    spent.longValue = s
                                }
                            }

                            BudgetItem(
                                Modifier.clickable {

                                    // TODO lastClickedPosition = holder.bindingAdapterPosition
                                    startActivityForResult(
                                        Intent(
                                            this@ManageBudgets2,
                                            BudgetActivity::class.java
                                        ).apply {
                                            this.putExtra(
                                                KEY_ROWID,
                                                budget.id
                                            )
                                            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        }, 0
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
                                allocated.longValue,
                                spent.longValue
                            )
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
    modifier: Modifier,
    budget: Budget,
    criteria: List<Criterion<*>>,
    allocated: Long,
    spent: Long,
) {

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = budget.titleComplete(LocalContext.current)
        )
        ChipGroup(budget, criteria)
        Row {
            DonutInABox(
                modifier = Modifier.size(42.dp),
                progress = 55f,
                fontSize = 12.sp,
                color = Color(budget.color),
                excessColor = LocalColors.current.expense
            )
            Column {
                Text(stringResource(R.string.budget_table_header_budgeted))
                Text(stringResource(R.string.budget_table_header_spent))
                Text(stringResource(R.string.available))
            }
            Column {
                Text(allocated.toString())
                Text(spent.toString())
                Text((allocated + spent).toString())

            }
        }

    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(budget: Budget, criteria: List<Criterion<*>>) {
    val context = LocalContext.current
    FlowRow {
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
fun BudgetItemPreview() {
    BudgetItem(
        Modifier,
        Budget(
            1, 1, "Budget", "Daily Expenses", "EUR", Grouping.MONTH, 0,
            null as String?, null as String?, "Wallet", false
        ),
        emptyList(),
        1000,
        500
    )
}

