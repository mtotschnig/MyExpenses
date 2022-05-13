package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.ChipGroup
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.SimpleFormDialog
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.ACTION_MANAGE
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.Budget
import org.totschnig.myexpenses.compose.ExpansionMode
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.addChipsBulk
import org.totschnig.myexpenses.util.buildAmountField
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2
import org.totschnig.myexpenses.viewmodel.data.Category
import java.math.BigDecimal

class BudgetActivity : DistributionBaseActivity<BudgetViewModel2>(), OnDialogResultListener {
    companion object {
        const val EDIT_BUDGET_DIALOG = "EDIT_BUDGET"
        private const val DELETE_BUDGET_DIALOG = "DELETE_BUDGET"
    }

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
                supportActionBar?.title = it.title
            }
        }
        binding.composeView.setContent {
            AppTheme(this) {
                val category =
                    viewModel.categoryTreeForBudget.collectAsState(initial = Category.LOADING).value
                val budget = viewModel.accountInfo.collectAsState(null).value
                val sums = viewModel.sums.collectAsState(initial = 0L to 0L).value
                val sort = viewModel.sortOrder.collectAsState()
                val filterPersistence = viewModel.filterPersistence.collectAsState().value
                Box(modifier = Modifier.fillMaxSize()) {
                    if (category == Category.LOADING || budget == null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(96.dp)
                                .align(Alignment.Center)
                        )
                    } else {
                        Column {
                            AndroidView(
                                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.activity_horizontal_margin)),
                                factory = { ChipGroup(it) },
                                update = { chipGroup ->
                                    chipGroup.addChipsBulk(buildList {
                                        add(budget.label(this@BudgetActivity))
                                        filterPersistence?.whereFilter?.criteria?.map {
                                            it.prettyPrint(this@BudgetActivity)
                                        }?.let { addAll(it) }
                                    })
                                }

                            )
                            Budget(
                                category = category.copy(
                                    budget = budget.amount.amountMinor,
                                    sum = if (viewModel.aggregateTypes) sums.first - sums.second else -sums.second,
                                ).let {
                                    when (sort.value) {
                                        Sort.SPENT -> it.sortChildrenBySumRecursive()
                                        Sort.ALLOCATED -> it.sortChildrenByBudgetRecursive()
                                        else -> it
                                    }
                                },
                                expansionMode = ExpansionMode.DefaultCollapsed(
                                    rememberMutableStateListOf()
                                ),
                                currency = budget.currency,
                                onBudgetEdit = { cat, parent ->
                                    showEditBudgetDialog(
                                        cat,
                                        parent,
                                        budget.currency
                                    )
                                },
                                onShowTransactions = ::showTransactions
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showEditBudgetDialog(
        category: Category,
        parentItem: Category?,
        currencyUnit: CurrencyUnit
    ) {
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
                buildAmountField(
                    amount, max?.let { Money(currencyUnit, it).amountMajor },
                    Money(currencyUnit, min).amountMajor, category.level, this
                )
            )
            .show(this, EDIT_BUDGET_DIALOG)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == OnDialogResultListener.BUTTON_POSITIVE) {
            val budget = viewModel.accountInfo.value
            if (budget != null && dialogTag == EDIT_BUDGET_DIALOG) {
                val amount = Money(
                    budget.currency,
                    (extras.getSerializable(DatabaseConstants.KEY_AMOUNT) as BigDecimal?)!!
                )
                viewModel.updateBudget(
                    budget.id,
                    extras.getLong(DatabaseConstants.KEY_CATID),
                    amount
                )
                return true
            }
            if (budget != null && dialogTag == DELETE_BUDGET_DIALOG) {
                viewModel.deleteBudget(budget.id).observe(this) {
                    if (it) {
                        setResult(Activity.RESULT_FIRST_USER)
                        finish()
                    } else {
                        showDeleteFailureFeedback()
                    }
                }
                return true
            }
        }
        return false
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) {
            true
        } else when (command) {
            R.id.MANAGE_CATEGORIES_COMMAND -> {
                startActivity(Intent(this, ManageCategories::class.java).apply {
                    action = ACTION_MANAGE
                })
                true
            }
            R.id.BUDGET_ALLOCATED_ONLY -> {
                viewModel.accountInfo.value?.let {
                    val value = tag as Boolean
                    viewModel.setAllocatedOnly(value)
                    prefHandler.putBoolean(templateForAllocatedOnlyKey(it.id), value)
                    invalidateOptionsMenu()
                    reset()
                }
                true
            }
            R.id.EDIT_COMMAND -> {
                viewModel.accountInfo.value?.let {
                    startActivity(Intent(this, BudgetEdit::class.java).apply {
                        putExtra(DatabaseConstants.KEY_ROWID, it.id)
                    })
                }
                true
            }
            R.id.DELETE_COMMAND -> {
                viewModel.accountInfo.value?.let {
                    SimpleDialog.build()
                        .title(R.string.dialog_title_warning_delete_budget)
                        .msg(
                            getString(
                                R.string.warning_delete_budget,
                                it.title
                            ) + " " + getString(R.string.continue_confirmation)
                        )
                        .pos(R.string.menu_delete)
                        .neg(android.R.string.cancel)
                        .show(this, DELETE_BUDGET_DIALOG)
                }
                true
            }
            else -> false
        }

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

    override val snackBarContainerId: Int = R.id.compose_container

    override fun onOptionsItemSelected(item: MenuItem) =
        if (sortDelegate.onOptionsItemSelected(item)) {
            invalidateOptionsMenu()
            viewModel.setSortOrder(sortDelegate.currentSortOrder)
            true
        } else super.onOptionsItemSelected(item)
}