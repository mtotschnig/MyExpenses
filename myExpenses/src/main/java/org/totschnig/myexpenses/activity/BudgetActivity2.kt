package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.SimpleFormDialog
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.Budget
import org.totschnig.myexpenses.compose.ExpansionMode
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.fragment.BudgetFragment
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2
import org.totschnig.myexpenses.viewmodel.data.Category2
import java.math.BigDecimal

class BudgetActivity2 : DistributionBaseActivity(), OnDialogResultListener {
    override val viewModel: BudgetViewModel2 by viewModels()
    private lateinit var sortDelegate: SortDelegate
    override val prefKey = PrefKey.BUDGET_AGGREGATE_TYPES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(true)
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
        }
        sortDelegate = SortDelegate(
            defaultSortOrder = Sort.ALLOCATED,
            prefKey = PrefKey.SORT_ORDER_BUDGET_CATEGORIES,
            options = arrayOf(Sort.LABEL, Sort.ALLOCATED, Sort.SPENT),
            prefHandler = prefHandler
        )
        viewModel.setSortOrder(sortDelegate.currentSortOrder)
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
                val sums = viewModel.sums.collectAsState(initial = 0L to 0L).value
                val sort = viewModel.sortOrder.collectAsState()
                if (category != Category2.EMPTY && account != null) {
                    Budget(
                        category = category.copy(budget = account.budget!!.amount.amountMinor,
                            sum = if (viewModel.aggregateTypes) sums.first - sums.second else -sums.second).let {
                            when(sort.value) {
                                Sort.SPENT -> it.sortChildrenBySum()
                                Sort.ALLOCATED -> it.sortChildrenByBudget()
                                else -> it
                            }
                        },
                        expansionMode = ExpansionMode.DefaultCollapsed(rememberMutableStateListOf()),
                        currency = account.currency,
                        onBudgetEdit = { cat, parent -> showEditBudgetDialog(cat, parent, account.currency) }
                    )
                }
            }
        }
    }

    private fun showEditBudgetDialog(category: Category2, parentItem: Category2?, currencyUnit: CurrencyUnit) {
        val simpleFormDialog = SimpleFormDialog.build()
            .title(if (category.level > 0) category.label else getString(R.string.dialog_title_edit_budget))
            .neg()
        val amount = Money(currencyUnit, category.budget)
        val min = category.children.sumOf { it.budget }
        val max = if (category.level > 0) {
            val bundle = Bundle(1).apply {
                putLong(DatabaseConstants.KEY_CATID, category.id)
            }
            simpleFormDialog.extra(bundle)
            val allocated = parentItem?.children?.sumOf { it.budget } ?: category.budget
            val allocatable = parentItem?.budget?.minus(allocated)
            val maxLong = allocatable?.plus(category.budget)
            if (maxLong != null && maxLong <= 0) {
                showSnackBar(
                    concatResStrings(
                        this, " ",
                        if (category.level == 1) R.string.budget_exceeded_error_1_2 else R.string.sub_budget_exceeded_error_1_2,
                        if (category.level == 1) R.string.budget_exceeded_error_2 else R.string.sub_budget_exceeded_error_2
                    )
                )
                return
            }
            maxLong
        } else null
        simpleFormDialog
            .fields(
                BudgetFragment.buildAmountField(
                    amount, max?.let { Money(currencyUnit, it).amountMajor },
                    Money(currencyUnit, min).amountMajor, category.level, this)
            )
            .show(this, BudgetFragment.EDIT_BUDGET_DIALOG)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == OnDialogResultListener.BUTTON_POSITIVE) {
            val accountInfo = viewModel.accountInfo.value
            if (accountInfo != null && dialogTag == BudgetFragment.EDIT_BUDGET_DIALOG) {
                val amount = Money(
                    accountInfo.currency,
                    (extras.getSerializable(DatabaseConstants.KEY_AMOUNT) as BigDecimal?)!!
                )
                viewModel.updateBudget(
                    accountInfo.budget!!.id,
                    extras.getLong(DatabaseConstants.KEY_CATID),
                    amount
                )
                return true
            }
/*            if (dialogTag == BudgetFragment.DELETE_BUDGET_DIALOG) {
                viewModel.deleteBudget(budget.id)
                return true
            }*/
        }
        return false
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
        sortDelegate.onPrepareOptionsMenu(menu)
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.BUDGET_ALLOCATED_ONLY)?.let {
            it.isChecked = viewModel.allocatedOnly
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (sortDelegate.onOptionsItemSelected(item)) {
            invalidateOptionsMenu()
            viewModel.setSortOrder(sortDelegate.currentSortOrder)
            true
        } else super.onOptionsItemSelected(item)
}