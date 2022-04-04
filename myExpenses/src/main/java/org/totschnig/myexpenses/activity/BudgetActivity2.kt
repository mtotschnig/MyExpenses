package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.*
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
        binding.composeView.setContent {
            AppTheme(this) {
                Category(
                    category = viewModel.categoryTreeForBudget.collectAsState(initial = Category2.EMPTY).value,
                    expansionMode = ExpansionMode.DefaultCollapsed(rememberMutableStateListOf()),
                    choiceMode = ChoiceMode.NoChoice,
                    sumCurrency = viewModel.accountInfo.collectAsState(null).value?.currency,
                )
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
                    prefHandler.putBoolean(templateForAllocatedOnlyKey(it.budgetId!!), value)
                    invalidateOptionsMenu()
                    reset()
                    true
                }
                else -> false
            }
        } ?: false

    private fun templateForAllocatedOnlyKey(budgetId: String) = "allocatedOnly_$budgetId"


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