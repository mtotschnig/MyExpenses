package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.ChipGroup
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.Check
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
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.addChipsBulk
import org.totschnig.myexpenses.util.buildAmountField
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2
import org.totschnig.myexpenses.viewmodel.data.Category
import java.math.BigDecimal

class BudgetActivity : DistributionBaseActivity<BudgetViewModel2>(), OnDialogResultListener {
    companion object {
        private const val EDIT_BUDGET_DIALOG = "EDIT_BUDGET"
        private const val DELETE_BUDGET_DIALOG = "DELETE_BUDGET"
        private const val DELETE_ROLLOVER_DIALOG = "DELETE_ROLLOVER"
    }

    override val viewModel: BudgetViewModel2 by viewModels()
    private lateinit var sortDelegate: SortDelegate
    override val prefKey = PrefKey.BUDGET_AGGREGATE_TYPES
    private var hasRollovers: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = setupView()
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
        val groupingYear = intent.getIntExtra(DatabaseConstants.KEY_YEAR, 0)
        val groupingSecond = intent.getIntExtra(DatabaseConstants.KEY_SECOND_GROUP, 0)
        viewModel.initWithBudget(budgetId, groupingYear, groupingSecond)

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
                        hasRollovers = category.hasRolloverNext
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
                                modifier = Modifier.weight(1f),
                                category = category.copy(
                                    sum = viewModel.sum.collectAsState(initial = 0L).value,
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
                                        budget.currency,
                                        budget.grouping != Grouping.NONE
                                    )
                                },
                                onShowTransactions = ::showTransactions,
                                hasRolloverNext = category.hasRolloverNext,
                                editRollOver = if (viewModel.duringRollOverEdit) {
                                    viewModel.editRollOverMap
                                } else null
                            )
                            val editRollOverInValid = viewModel.editRollOverInValid
                            LaunchedEffect(editRollOverInValid) {
                                invalidateOptionsMenu()
                            }
                            if (editRollOverInValid) {
                                Snackbar {
                                    Text(text = stringResource(R.string.rollover_edit_invalid))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showEditBudgetDialog(
        category: Category,
        parentItem: Category?,
        currencyUnit: CurrencyUnit,
        withOneTimeCheck: Boolean
    ) {
        val simpleFormDialog = SimpleFormDialog.build()
            .title(if (category.level > 0) category.label else getString(R.string.dialog_title_edit_budget))
            .neg()
        val amount = Money(currencyUnit, category.budget.budget)
        //The rollOver reduces the amount we need to allocate specific for this period
        val min = category.children.sumOf { it.budget.totalAllocated } - category.budget.rollOverPrevious
        val max = if (category.level > 0) {
            val bundle = Bundle(1).apply {
                putLong(DatabaseConstants.KEY_CATID, category.id)
            }
            simpleFormDialog.extra(bundle)
            val allocated: Long = parentItem?.children?.sumOf { it.budget.totalAllocated } ?: category.budget.totalAllocated
            val allocatable = parentItem?.budget?.totalAllocated?.minus(allocated)
            val maxLong = allocatable?.plus(category.budget.totalAllocated)
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
                *buildList {
                    add(buildAmountField(
                        amount, max?.let { Money(currencyUnit, it).amountMajor },
                        Money(currencyUnit, min).amountMajor, category.level, this@BudgetActivity
                    ))
                    if (withOneTimeCheck)
                        add(Check.box(DatabaseConstants.KEY_ONE_TIME)
                            .label(getString(R.string.budget_only_current_period, supportActionBar?.subtitle))
                            .check(category.budget.oneTime)
                        )
                }.toTypedArray()
            )
            .show(this, EDIT_BUDGET_DIALOG)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == OnDialogResultListener.BUTTON_POSITIVE) {
            val budget = viewModel.accountInfo.value ?: return false
            when (dialogTag) {
                EDIT_BUDGET_DIALOG -> {
                    val amount = Money(
                        budget.currency,
                        (extras.getSerializable(DatabaseConstants.KEY_AMOUNT) as BigDecimal?)!!
                    )
                    viewModel.updateBudget(
                        budget.id,
                        extras.getLong(DatabaseConstants.KEY_CATID),
                        amount,
                        extras.getBoolean(DatabaseConstants.KEY_ONE_TIME)
                    )
                    return true
                }
                DELETE_BUDGET_DIALOG -> {
                    viewModel.deleteBudget(
                        budgetId = budget.id,
                        if (budget.default) BudgetViewModel.prefNameForDefaultBudget(budget.accountId, budget.grouping) else null
                    ).observe(this) {
                        if (it) {
                            setResult(Activity.RESULT_FIRST_USER)
                            finish()
                        } else {
                            showDeleteFailureFeedback()
                        }
                    }
                    return true
                }
                DELETE_ROLLOVER_DIALOG -> {
                    viewModel.rollOverClear()
                    return true
                }
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
            R.id.ROLLOVER_TOTAL -> {
                viewModel.rollOverTotal()
                true
            }
            R.id.ROLLOVER_CLEAR -> {
                SimpleDialog.build()
                    .title(supportActionBar?.subtitle?.toString())
                    .msg(
                        getString(R.string.dialog_confirm_rollover_delete) + " " +
                                getString(R.string.continue_confirmation)
                    )
                    .pos(R.string.menu_delete)
                    .neg(android.R.string.cancel)
                    .show(this, DELETE_ROLLOVER_DIALOG)
                true
            }
            R.id.ROLLOVER_CATEGORIES -> {
                viewModel.rollOverCategories()
                true
            }
            R.id.ROLLOVER_EDIT -> {
                if (viewModel.startRollOverEdit()) {
                    invalidateOptionsMenu()
                } else {
                    Toast.makeText(this, "RollOver Save still ongoing. Try again later", Toast.LENGTH_LONG).show()
                }
                true
            }
            R.id.ROLLOVER_EDIT_CANCEL -> {
                viewModel.stopRollOverEdit()
                invalidateOptionsMenu()
                true
            }
            R.id.ROLLOVER_EDIT_SAVE -> {
                viewModel.stopRollOverEdit()
                invalidateOptionsMenu()
                viewModel.rollOverSave()
                true
            }
            else -> false
        }

    private fun templateForAllocatedOnlyKey(budgetId: Long) = "allocatedOnly_$budgetId"


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (viewModel.duringRollOverEdit) {
            menuInflater.inflate(R.menu.budget_rollover_edit, menu)
        } else {
            menuInflater.inflate(R.menu.budget, menu)
            super.onCreateOptionsMenu(menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (viewModel.duringRollOverEdit) {
            menu.findItem(R.id.ROLLOVER_EDIT_SAVE).isEnabled = !viewModel.editRollOverInValid
        } else {
            sortDelegate.onPrepareOptionsMenu(menu)
            super.onPrepareOptionsMenu(menu)
            menu.findItem(R.id.BUDGET_ALLOCATED_ONLY)?.let {
                it.isChecked = viewModel.allocatedOnly
            }
            val grouped = viewModel.grouping != Grouping.NONE
            Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.ROLLOVER_COMMAND), grouped)
            if (grouped) {
                Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.ROLLOVER_TOTAL), hasRollovers == false)
                Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.ROLLOVER_CATEGORIES), hasRollovers == false)
                Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.ROLLOVER_CLEAR), hasRollovers == true)
            }
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