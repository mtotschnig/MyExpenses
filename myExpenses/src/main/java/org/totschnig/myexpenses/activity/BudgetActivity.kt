package org.totschnig.myexpenses.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.AmountInputHostDialog
import eltos.simpledialogfragment.form.Check
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.Budget
import org.totschnig.myexpenses.compose.ChipGroup
import org.totschnig.myexpenses.compose.ExpansionMode
import org.totschnig.myexpenses.compose.TEST_TAG_BUDGET_ROOT
import org.totschnig.myexpenses.compose.breakPoint
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.filter.asSimpleList
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.buildAmountField
import org.totschnig.myexpenses.util.populateWithSync
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.viewmodel.BudgetViewModel2
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Category
import timber.log.Timber
import java.math.BigDecimal
import kotlin.math.pow


class BudgetActivity : DistributionBaseActivity<BudgetViewModel2>(), OnDialogResultListener {
    companion object {
        private const val EDIT_BUDGET_DIALOG = "EDIT_BUDGET"
        private const val DELETE_BUDGET_DIALOG = "DELETE_BUDGET"
        private const val DELETE_ROLLOVER_DIALOG = "DELETE_ROLLOVER"
    }

    override val viewModel: BudgetViewModel2 by viewModels()
    private lateinit var sortDelegate: SortDelegate
    private var hasRollovers: Boolean? = null
    private lateinit var chart: RadarChart

    override val showChartPrefKey = PrefKey.BUDGEt_SHOW_CHART

    override val showChartDefault = false

    private val showFilter = mutableStateOf(true)

    private val showImportConfirmation = mutableStateOf(false)

    val budgetId: Long
        get() = intent.getLongExtra(KEY_ROWID, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!licenceHandler.hasTrialAccessTo(ContribFeature.BUDGET)) {
            contribFeatureRequested(ContribFeature.BUDGET)
            finish()
            return
        }
        val binding = setupView()
        injector.inject(viewModel)
        sortDelegate = SortDelegate(
            defaultSortOrder = Sort.ALLOCATED,
            prefKey = PrefKey.SORT_ORDER_BUDGET_CATEGORIES,
            options = arrayOf(Sort.LABEL, Sort.ALLOCATED, Sort.SPENT, Sort.AVAILABLE),
            prefHandler = prefHandler,
            collate = collate
        )
        viewModel.setSortOrder(sortDelegate.currentSortOrder)
        val groupingYear = intent.getIntExtra(KEY_YEAR, 0)
        val groupingSecond = intent.getIntExtra(KEY_SECOND_GROUP, 0)
        viewModel.initWithBudget(budgetId, groupingYear, groupingSecond)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accountInfo.filterNotNull().collect {
                    supportActionBar?.title = it.title
                }
            }
        }
        observeSyncResult()
        binding.composeView.setContent {
            AppTheme {
                val sort = viewModel.sortOrder.collectAsState()
                val category =
                    viewModel.categoryTreeForBudget.collectAsState(initial = Category.LOADING)
                val budget = viewModel.accountInfo.collectAsState(null).value
                LaunchedEffect(category.value) {
                    if (category.value != Category.LOADING) {
                        invalidateOptionsMenu()
                    }
                }

                if (showImportConfirmation.value) {
                    AlertDialog(
                        onDismissRequest = {
                            showImportConfirmation.value = false
                        },
                        dismissButton = {
                            Button(onClick = { showImportConfirmation.value = false }) {
                                Text(stringResource(id = android.R.string.cancel))
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                showImportConfirmation.value = false
                                viewModel.importBudget()
                            }) {
                                Text(stringResource(R.string.menu_import))
                            }
                        },
                        text = {
                            Text(
                                stringResource(R.string.warning_budget_sync_import) + " " + stringResource(
                                    R.string.continue_confirmation
                                )
                            )
                        }
                    )
                }

                Box(modifier = Modifier
                    .fillMaxSize()
                ) {
                    if (category.value === Category.LOADING || budget == null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(96.dp)
                                .align(Alignment.Center)
                        )
                    } else {
                        val currencyUnit = currencyContext[budget.currency]
                        val sortedData = remember {
                            derivedStateOf {
                                when (sort.value) {
                                    Sort.SPENT -> category.value.sortChildrenBySumRecursive()
                                    Sort.ALLOCATED -> category.value.sortChildrenByBudgetRecursive()
                                    Sort.AVAILABLE -> category.value.sortChildrenByAvailableRecursive()
                                    else -> category.value
                                }
                            }
                        }
                        LaunchedEffect(sortedData.value, showChart.value) {
                            setChartData(sortedData.value, currencyUnit.fractionDigits)
                        }
                        hasRollovers = category.value.hasRolloverNext
                        Column(verticalArrangement = Arrangement.Center) {
                            RenderFilters(budget)
                            LayoutHelper(
                                data = { modifier, withContentPadding ->
                                    RenderBudget(
                                        modifier = modifier,
                                        category = sortedData.value,
                                        budget = budget,
                                        currencyUnit = currencyUnit,
                                        sort = sort.value,
                                        withContentPadding = withContentPadding
                                    )
                                }, chart = {
                                    RenderChart(
                                        it,
                                        sortedData.value
                                    )
                                })
                        }
                    }
                }
            }
        }
        viewModel.setAllocatedOnly(
            prefHandler.getBoolean(
                templateForAllocatedOnlyKey(budgetId),
                false
            )
        )
    }

    override fun onToggleFullScreen(fullScreen: Boolean) {
        super.onToggleFullScreen(fullScreen)
        showFilter.value = !fullScreen
    }

    private fun requireChart(context: Context) = RadarChart(context).also { chart = it }

    @Composable
    fun RenderFilters(budget: Budget) {
        if (showFilter.value) {
            val whereFilter = viewModel.whereFilter.collectAsState().value
            ChipGroup(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_main_screen)),
                budget = budget,
                criteria = whereFilter.asSimpleList
            )
        }
    }

    @Composable
    fun RenderBudget(
        modifier: Modifier,
        category: Category,
        budget: Budget,
        currencyUnit: CurrencyUnit,
        sort: Sort,
        withContentPadding: Boolean,
    ) {
        BoxWithConstraints(modifier = modifier.testTag(TEST_TAG_BUDGET_ROOT)) {
            val narrowScreen = this.maxWidth.value < breakPoint.value
            Timber.d("narrowScreen : %b (%f)", narrowScreen, maxWidth.value)

            Budget(
                category = category.copy(
                    sum = viewModel.sum.collectAsState().value,
                ),
                expansionMode = ExpansionMode.DefaultCollapsed(
                    rememberMutableStateListOf()
                ),
                currency = currencyUnit,
                onBudgetEdit = { cat, parent ->
                    showEditBudgetDialog(
                        cat,
                        parent,
                        currencyUnit,
                        budget.grouping != Grouping.NONE
                    )
                },
                onShowTransactions = {
                    lifecycleScope.launch {
                        showTransactions(it)
                    }
                },
                hasRolloverNext = category.hasRolloverNext,
                editRollOver = if (viewModel.duringRollOverEdit) {
                    viewModel.editRollOverMap
                } else null,
                narrowScreen = narrowScreen,
                showChart = showChart.value,
                currentSort = sort,
                onChangeSort = {
                    prefHandler.putString(sortDelegate.prefKey, it.name)
                    viewModel.setSortOrder(it)
                },
                contentPadding = if (withContentPadding)
                    WindowInsets.navigationBars.asPaddingValues() else PaddingValues(0.dp)
            )
        }
    }

    @Composable
    fun RenderChart(modifier: Modifier, category: Category) {
        if (category.children.count { it.budget.totalAllocated > 0 } >= 3)
            AndroidView(
                modifier = modifier,
                factory = { ctx ->
                    requireChart(ctx).apply {

                        description.isEnabled = false

                        webLineWidth = 1f
                        webColor = Color.LTGRAY
                        webLineWidthInner = 1f
                        webColorInner = Color.LTGRAY

                        animateXY(1400, 1400, Easing.EaseInOutQuad)

                        with(xAxis) {
                            setTextSize(9f)
                            yOffset = 0f
                            xOffset = 0f
                            textColor = textColorSecondary.defaultColor
                        }


                        with(yAxis) {
                            setLabelCount(5, false)
                            setTextSize(9f)
                            textColor = textColorSecondary.defaultColor
                            setAxisMinimum(0f)
                        }
                        legend.isEnabled = false
                    }
                }
            )
        else
            Text(
                stringResource(R.string.not_enough_data),
                modifier.wrapContentHeight(),
                textAlign = TextAlign.Center
            )
    }

    private fun showEditBudgetDialog(
        category: Category,
        parentItem: Category?,
        currencyUnit: CurrencyUnit,
        withOneTimeCheck: Boolean,
    ) {
        val simpleFormDialog = AmountInputHostDialog.build()
            .title(if (category.level > 0) category.label else getString(R.string.dialog_title_edit_budget))
            .neg()
        val amount = Money(currencyUnit, category.budget.budget)
        //The rollOver reduces the amount we need to allocate specific for this period
        val min =
            category.children.sumOf { it.budget.budget } - category.budget.rollOverPrevious
        val max = if (category.level > 0) {
            val bundle = Bundle(1).apply {
                putLong(KEY_CATID, category.id)
            }
            simpleFormDialog.extra(bundle)
            val allocated: Long = parentItem?.children?.sumOf { it.budget.totalAllocated }
                ?: category.budget.totalAllocated
            val allocatable = parentItem?.budget?.totalAllocated?.minus(allocated)
            val maxLong = allocatable?.plus(category.budget.totalAllocated)
            if (maxLong != null && maxLong <= 0) {
                showSnackBar(
                    concatResStrings(
                        this,
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
                    add(
                        buildAmountField(
                            amount,
                            max?.let { Money(currencyUnit, it).amountMajor },
                            Money(currencyUnit, min).amountMajor,
                            category.level,
                            this@BudgetActivity
                        )
                    )
                    if (withOneTimeCheck)
                        add(
                            Check.box(KEY_ONE_TIME)
                                .label(
                                    getString(
                                        R.string.budget_only_current_period,
                                        supportActionBar?.subtitle
                                    )
                                )
                                .check(category.budget.oneTime)
                        )
                }.toTypedArray()
            )
            .show(this, EDIT_BUDGET_DIALOG)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == BUTTON_POSITIVE) {
            val budget = viewModel.accountInfo.value ?: return false

            when (dialogTag) {
                EDIT_BUDGET_DIALOG -> {
                    BundleCompat.getSerializable(extras, KEY_AMOUNT, BigDecimal::class.java)?.let {
                        viewModel.updateBudget(
                            budget.id,
                            extras.getLong(KEY_CATID),
                            Money(this.currencyContext[budget.currency], it),
                            extras.getBoolean(KEY_ONE_TIME)
                        )
                    }
                    return true
                }

                DELETE_BUDGET_DIALOG -> {
                    viewModel.deleteBudget(
                        budgetId = budget.id
                    ).observe(this) {
                        if (it) {
                            setResult(RESULT_FIRST_USER)
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
                    action = Action.MANAGE.name
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
                        putExtra(KEY_ROWID, it.id)
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
                    Toast.makeText(
                        this,
                        "RollOver Save still ongoing. Try again later",
                        Toast.LENGTH_LONG
                    ).show()
                }
                true
            }

            R.id.ROLLOVER_EDIT_CANCEL -> {
                viewModel.stopRollOverEdit()
                invalidateOptionsMenu()
                viewModel.editRollOverMap.clear()
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

    private val dismissCallback = object : Snackbar.Callback() {
        override fun onDismissed(
            transientBottomBar: Snackbar,
            event: Int,
        ) {
            if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION || event == DISMISS_EVENT_TIMEOUT)
                viewModel.messageShown()
        }
    }

    private fun observeSyncResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncResult.collect {
                    showSnackBar(it, callback = dismissCallback)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when {
        item.itemId == Menu.NONE && item.groupId == R.id.SYNC_COMMAND -> {
            viewModel.exportBudget(item.title.toString())
            true
        }

        item.itemId == R.id.SYNC_COMMAND_EXPORT -> {
            viewModel.exportBudget()
            true
        }

        item.itemId == R.id.SYNC_COMMAND_IMPORT -> {
            showImportConfirmation.value = true
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

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
        if (!viewModel.duringRollOverEdit) {
            super.onPrepareOptionsMenu(menu)
            menu.findItem(R.id.BUDGET_ALLOCATED_ONLY)?.let {
                it.isChecked = viewModel.allocatedOnly
            }
            val grouped = viewModel.grouping != Grouping.NONE
            menu.findItem(R.id.ROLLOVER_COMMAND).setEnabledAndVisible(grouped)
            if (grouped) {
                menu.findItem(R.id.ROLLOVER_TOTAL).setEnabledAndVisible(hasRollovers == false)
                menu.findItem(R.id.ROLLOVER_CATEGORIES).setEnabledAndVisible(hasRollovers == false)
                menu.findItem(R.id.ROLLOVER_CLEAR).setEnabledAndVisible(hasRollovers == true)
            }
            lifecycleScope.launch {
                menu.findItem(R.id.AGGREGATE_COMMAND).isChecked = viewModel.aggregateNeutral.first()
            }
            val isSynced = viewModel.isSynced()
            menu.findItem(R.id.SYNC_COMMAND)?.let { menuItem ->
                if (isSynced) {
                    menuItem.subMenu?.let {
                        it.clear()
                        it.add(
                            Menu.NONE,
                            R.id.SYNC_COMMAND_EXPORT,
                            Menu.NONE,
                            getString(R.string.menu_export)
                        )
                        it.add(
                            Menu.NONE,
                            R.id.SYNC_COMMAND_IMPORT,
                            Menu.NONE,
                            getString(R.string.menu_import)
                        )
                    }
                } else {
                    viewModel.accountInfo.value?.let {
                        if (it.accountId > 0 && it.syncAccountName == null) {
                            //a budget for an account that is not synced can not be synced
                            menuItem.setEnabledAndVisible(false)
                        } else {
                            menuItem.populateWithSync(
                                //if the account is synced with a backend, we offer to sync with the same backend
                                //budgets for aggregate accounts can be synced with any backend
                                if (it.syncAccountName != null) arrayOf(it.syncAccountName) else
                                    GenericAccountService.getAccountNames(this)
                            )
                        }
                    }
                }
            }
        }
        return true
    }

    private fun setChartData(category: Category, fractionDigits: Int) {
        if ((::chart.isInitialized)) {
            val factor = 10.0.pow(fractionDigits).toFloat()
            val labels = mutableListOf<String>()
            val allocated = mutableListOf<RadarEntry>()
            val spent = mutableListOf<RadarEntry>()
            val categories = category.children.filter {
                it.budget.totalAllocated > 0
            }
            if (categories.size < 3) return
            categories.forEach {
                labels.add(it.label)
                allocated.add(RadarEntry(it.budget.totalAllocated / factor))
                spent.add(RadarEntry(-it.aggregateSum.toFloat() / factor))
            }
            with(chart) {
                xAxis.valueFormatter = IAxisValueFormatter { value, _ ->
                    labels[value.toInt() % labels.size]
                }
                setData(RadarData(buildList<RadarDataSet> {
                    add(
                        RadarDataSet(
                            allocated,
                            getString(R.string.budget_table_header_allocated)
                        ).apply {
                            setColor(ResourcesCompat.getColor(resources, R.color.colorIncome, null))
                            setLineWidth(2f)
                            isDrawHighlightCircleEnabled = true
                            setDrawHighlightIndicators(false)
                        })
                    add(RadarDataSet(spent, getString(R.string.budget_table_header_spent)).apply {
                        setColor(ResourcesCompat.getColor(resources, R.color.colorExpense, null))
                        setLineWidth(2f)
                        isDrawHighlightCircleEnabled = true
                        setDrawHighlightIndicators(false)
                    })
                }).apply {
                    setValueTextSize(8f)
                    setDrawValues(false)
                    setValueTextColor(Color.WHITE)
                })
                invalidate()
            }
        }
    }
}

/*
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RadarChart() {
    Mdc3Theme {
        Surface(modifier = Modifier.size(200.dp)) {
            RenderChart(modifier = Modifier)
        }
    }
}
*/
