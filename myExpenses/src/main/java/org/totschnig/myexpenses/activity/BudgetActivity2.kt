package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.Budget
import org.totschnig.myexpenses.compose.ExpansionMode
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2
import org.totschnig.myexpenses.viewmodel.data.Category2

class BudgetActivity2 : DistributionBaseActivity() {
    override val viewModel: BudgetViewModel2 by viewModels()
    override val prefKey = PrefKey.BUDGET_AGGREGATE_TYPES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(true)
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
        }
        val budgetId: Long = intent.getLongExtra(DatabaseConstants.KEY_ROWID, 0)
        viewModel.initWithBudget(budgetId)
        lifecycleScope.launch {
            viewModel.accountInfo.filterNotNull().collect {
                with(it.budget!!) {
                    supportActionBar?.title = title
                    viewModel.setGrouping(grouping)
                }
            }
        }
        binding.composeView.setContent {
            AppTheme(this) {
                val category = viewModel.categoryTreeForBudget.collectAsState(initial = Category2.EMPTY).value
                val account = viewModel.accountInfo.collectAsState(null).value
                if (category != Category2.EMPTY && account != null) {
                    Budget(
                        category = category.copy(budget = account.budget!!.amount.amountMinor),
                        expansionMode = ExpansionMode.DefaultCollapsed(rememberMutableStateListOf()),
                        currency = account.currency,
                    )
                }
            }
        }
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) {
            true
        } else viewModel.accountInfo.value?.let {
            when (command) {
                R.id.BUDGET_ALLOCATED_ONLY -> {
                    val value = tag as Boolean
                    viewModel.setAllocatedOnly(value)
                    prefHandler.putBoolean(templateForAllocatedOnlyKey(it.budget!!.id), value)
                    invalidateOptionsMenu()
                    reset()
                    true
                }
                else -> false
            }
        } ?: false

    private fun templateForAllocatedOnlyKey(budgetId: Long) = "allocatedOnly_$budgetId"


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.budget, menu)
        super.onCreateOptionsMenu(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.BUDGET_ALLOCATED_ONLY)?.let {
            it.isChecked = viewModel.allocatedOnly
        }
        return true
    }
}