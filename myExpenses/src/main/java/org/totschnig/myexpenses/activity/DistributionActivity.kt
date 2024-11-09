package org.totschnig.myexpenses.activity

import android.content.Context
import android.os.Bundle
import android.util.SparseArray
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.PieRadarChartBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.color.SimpleColorDialog
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.compose.ChoiceMode
import org.totschnig.myexpenses.compose.ExpansionMode
import org.totschnig.myexpenses.compose.FilterCard
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.ui.SelectivePieChartRenderer
import org.totschnig.myexpenses.util.ColorUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.getLocale
import org.totschnig.myexpenses.util.ui.UiUtils
import org.totschnig.myexpenses.viewmodel.DistributionViewModel
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo
import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign

class DistributionActivity : DistributionBaseActivity<DistributionViewModel>(),
    OnDialogResultListener {
    override val viewModel: DistributionViewModel by viewModels()
    private lateinit var chart: PieChart
    private lateinit var innerChart: PieChart
    private var mDetector: GestureDetector? = null

    override val showChartPrefKey = PrefKey.DISTRIBUTION_SHOW_CHART

    override val showChartDefault = true

    private val subColorMap = SparseArray<List<Int>>()

    private fun getSubColors(color: Int, isDark: Boolean): List<Int> {
        return subColorMap.get(color)
            ?: (if (isDark) ColorUtils.getTints(color) else ColorUtils.getShades(color)).also {
                subColorMap.put(color, it)
            }
    }

    private val localizedPercentFormat: NumberFormat by lazy {
        NumberFormat.getPercentInstance(getLocale()).also {
            it.setMinimumFractionDigits(1)
        }
    }

    private val percentFormatter = IValueFormatter { value, _, _, _ ->
        localizedPercentFormat.format(value / 100)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.distribution, menu)
        menuInflater.inflate(R.menu.grouping, menu.findItem(R.id.GROUPING_COMMAND).subMenu)
        return true
    }

    val selectionState
        get() = viewModel.selectionState

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        Utils.configureGroupingMenu(
            menu.findItem(R.id.GROUPING_COMMAND).subMenu,
            viewModel.grouping
        )
        lifecycleScope.launch {
            val typeFlags = viewModel.typeFlags.first()
            with(menu.findItem(R.id.TYPE_FILTER_COMMAND).subMenu!!) {
                findItem(R.id.FILTER_INCOME_COMMAND).isChecked = typeFlags.first
                findItem(R.id.FILTER_EXPENSE_COMMAND).isChecked = typeFlags.second
                with(findItem(R.id.AGGREGATE_COMMAND)) {
                    isEnabled = !(typeFlags.first && typeFlags.second)
                    isChecked = viewModel.aggregateNeutral.first()
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when {
        handleGrouping(item) -> true
        item.itemId == R.id.FILTER_INCOME_COMMAND || item.itemId == R.id.FILTER_EXPENSE_COMMAND -> {
            lifecycleScope.launch {
                viewModel.toggleTypeFlag(item.itemId == R.id.FILTER_INCOME_COMMAND)
                invalidateOptionsMenu()
            }
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun handleGrouping(item: MenuItem): Boolean {
        val newGrouping = Utils.getGroupingFromMenuItemId(item.itemId)
        if (newGrouping != null) {
            viewModel.persistGrouping(newGrouping)
            reset()
            return true
        }
        return false
    }

    private fun setChartData(chart: PieChart, categories: List<Category>) {
        with(chart) {
            data = PieData(PieDataSet(categories.map { category ->
                PieEntry(
                    abs(category.aggregateSum.toFloat()),
                    category.label
                )
            }, "").apply {
                colors = categories.map { it.color ?: 0 }
                sliceSpace = 2f
                setDrawValues(false)
                xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart2Length = 0.1f
                valueLineColor = textColorSecondary.defaultColor
            }).apply {
                setValueFormatter(percentFormatter)
            }
            invalidate()
        }
        selectionState.value = categories.firstOrNull()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = setupView()
        injector.inject(viewModel)

        val whereFilter = intent.getParcelableArrayListExtra<Criterion<*>>(KEY_FILTER)?.let {
            WhereFilter(it)
        }
        if (savedInstanceState == null) {
            viewModel.initWithAccount(
                intent.getLongExtra(DatabaseConstants.KEY_ACCOUNTID, 0),
                intent.getSerializableExtra(KEY_GROUPING) as? Grouping ?: Grouping.NONE,
                whereFilter
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accountInfo.filterNotNull().collect {
                    supportActionBar?.title = it.label(this@DistributionActivity)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.groupingInfoFlow.collect {
                    invalidateOptionsMenu()
                }
            }
        }

        binding.composeView.setContent {

            AppTheme {
                fun clearFilter() { viewModel.clearFilter() }
                val typeFlags = viewModel.typeFlags.collectAsState(initial = false to true)
                val (showIncome, showExpense) = typeFlags.value
                when {
                    showIncome && showExpense -> {
                        RenderCombined(viewModel.whereFilter.collectAsState().value, ::clearFilter)
                    }

                    !showIncome && !showExpense -> throw IllegalStateException()
                    else -> RenderSingle(showIncome, viewModel.whereFilter.collectAsState().value, ::clearFilter)
                }
            }
        }
        setupGestureDetector()
    }

    private fun Category.prepareColors(showChart: Boolean, isDark: Boolean) =
        when {
            showChart -> withSubColors { getSubColors(it, isDark) }
            else -> copy(children = children.map { it.copy(color = null) })
        }

    @Composable
    private fun RenderCombined(whereFilter: WhereFilter, clearFilter: () -> Unit) {
        val isDark = isSystemInDarkTheme()
        val categoryState =
            viewModel.combinedCategoryTree.collectAsState(initial = Category.LOADING)
        val categoryTree = remember {
            derivedStateOf {
                categoryState.value.copy(
                    children = categoryState.value.children.map {
                        it.prepareColors(
                            showChart.value,
                            isDark
                        )
                    }
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            val accountInfo = viewModel.accountInfo.collectAsState(null).value
            if (categoryState.value === Category.LOADING || accountInfo == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.Center)
                )
            } else {
                val incomeTree = remember {
                    derivedStateOf {
                        categoryTree.value.children.first().filterChildren(true)
                    }
                }.value
                val expenseTree = remember {
                    derivedStateOf {
                        categoryTree.value.children[1].filterChildren(false)
                    }
                }.value
                LaunchedEffect(categoryTree.value) {
                    if (::chart.isInitialized) {
                        setChartData(chart, incomeTree.children)
                    }
                    if (::innerChart.isInitialized) {
                        setChartData(innerChart, expenseTree.children)
                    }
                }
                LaunchedEffect(categoryTree.value, selectionState.value?.id) {
                    if (::chart.isInitialized) {
                        onSelectionChanged(chart, incomeTree)
                    }
                    if (::innerChart.isInitialized) {
                        onSelectionChanged(innerChart, expenseTree)
                    }
                }
                val choiceMode =
                    ChoiceMode.SingleChoiceMode(selectionState, mainOnly = true, selectTree = true)
                val expansionMode = ExpansionMode.DefaultCollapsed(
                    rememberMutableStateListOf()
                )
                if (incomeTree.children.isEmpty() && expenseTree.children.isEmpty()) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(id = R.string.no_mapped_transactions),
                        textAlign = TextAlign.Center
                    )
                } else {
                    val sums = viewModel.sums.collectAsState(initial = 0L to 0L).value
                    Column {
                        if (!whereFilter.isEmpty) {
                            FilterCard(whereFilter, clearFilter)
                        }
                        LayoutHelper(
                            data = {
                                RenderTree(
                                    modifier = it,
                                    tree = categoryTree.value,
                                    choiceMode = choiceMode,
                                    expansionMode = expansionMode,
                                    accountInfo = accountInfo
                                )
                            }, chart = {
                                val ratio = if (sums.first > 0L && sums.second < 0L)
                                    sums.first.toFloat() / -sums.second else 1f
                                val angles = if (ratio > 1f) 360f to 360f / ratio else
                                    360f * ratio to 360f
                                Timber.d("ratio: %f", ratio)
                                Timber.d("angles: %s", angles)
                                Box(modifier = it) {
                                    RenderChart(
                                        modifier = Modifier
                                            .fillMaxSize(0.95f)
                                            .align(Alignment.Center),
                                        false,
                                        categories = incomeTree,
                                        angle = angles.first,
                                        isCombined = true
                                    )
                                    RenderChart(
                                        modifier = Modifier
                                            .fillMaxSize(0.75f)
                                            .align(Alignment.Center),
                                        true,
                                        categories = expenseTree,
                                        angle = angles.second,
                                        isCombined = true
                                    )
                                }
                            }
                        )
                        RenderSumLine(accountInfo, sums)
                    }
                }
            }
        }
    }

    private fun onSelectionChanged(chart: PieChart, tree: Category) {
        val position = tree.children.indexOf(selectionState.value)
        if (position > -1) {
            chart.highlightValue(position.toFloat(), 0)
        } else {
            chart.highlightValue(null)
        }
    }

    private fun Category.filterChildren(showIncome: Boolean) = copy(
        children = children.filter { category ->
            when (category.aggregateSum.sign) {
                1 -> true
                else -> false
            } == showIncome
        })

    @Composable
    private fun RenderSingle(showIncome: Boolean, whereFilter: WhereFilter, clearFilter: () -> Unit) {
        val isDark = isSystemInDarkTheme()
        val categoryState =
            (if (showIncome) viewModel.categoryTreeForIncome else viewModel.categoryTreeForExpenses)
                .collectAsState(initial = Category.LOADING)

        val categoryTree = remember(whereFilter) {
            derivedStateOf {
                categoryState.value.prepareColors(showChart.value, isDark)
            }
        }

        val chartCategoryTree = remember(showIncome) {
            derivedStateOf {
                //expansionState does not reflect updates to the data, that is why we just use it
                //to walk down the updated tree and find the expanded category
                var result = categoryTree.value
                expansionState.forEach { expanded ->
                    result = result.children.find { it.id == expanded.id } ?: result
                }
                result.filterChildren(showIncome)

            }
        }
        LaunchedEffect(chartCategoryTree.value) {
            if (::chart.isInitialized) {
                setChartData(chart, chartCategoryTree.value.children)
            }
        }

        LaunchedEffect(chartCategoryTree.value, selectionState.value?.id) {
            if (::chart.isInitialized) {
                onSelectionChanged(chart, chartCategoryTree.value)
            }
        }
        val choiceMode = ChoiceMode.SingleChoiceMode(selectionState)
        val expansionMode = object : ExpansionMode.Single(expansionState) {
            override fun toggle(category: Category) {
                super.toggle(category)
                // when we collapse a category, we want it to be selected;
                // when we expand, the first child should be selected
                if (isExpanded(category.id)) {
                    selectionState.value = category.children.firstOrNull()
                } else {
                    selectionState.value = category
                }
            }
        }
        val accountInfo = viewModel.accountInfo.collectAsState(null).value
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                categoryTree.value === Category.LOADING || accountInfo == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(96.dp)
                            .align(Alignment.Center)
                    )
                }

                categoryTree.value.children.isEmpty() -> {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(id = R.string.no_mapped_transactions),
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    val sums = viewModel.sums.collectAsState(initial = 0L to 0L).value
                    Column {
                        if (!whereFilter.isEmpty) {
                            FilterCard(whereFilter, clearFilter)
                        }
                        LayoutHelper(
                            data = {
                                RenderTree(
                                    modifier = it,
                                    tree = categoryTree.value,
                                    choiceMode = choiceMode,
                                    expansionMode = expansionMode,
                                    accountInfo = accountInfo
                                )
                            }, chart = {
                                Box(modifier = it) {
                                    RenderChart(
                                        modifier = Modifier
                                            .fillMaxSize(0.85f)
                                            .align(Alignment.Center),
                                        false,
                                        categories = chartCategoryTree.value
                                    )
                                }
                            }
                        )
                        RenderSumLine(accountInfo, sums)
                    }
                }
            }
        }
    }

    private fun setupGestureDetector() {
        val dm = resources.displayMetrics

        val minDistance =
            (SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f).toInt()
        val maxOffPath =
            (SWIPE_MAX_OFF_PATH * dm.densityDpi / 160.0f).toInt()
        val thresholdVelocity =
            (SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f).toInt()
        mDetector = GestureDetector(this,
            object : SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent,
                    velocityX: Float, velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    if (abs(e1.y - e2.y) > maxOffPath) return false
                    if (e1.x - e2.x > minDistance
                        && abs(velocityX) > thresholdVelocity
                    ) {
                        viewModel.forward()
                        return true
                    } else if (e2.x - e1.x > minDistance
                        && abs(velocityX) > thresholdVelocity
                    ) {
                        viewModel.backward()
                        return true
                    }
                    return false
                }
            })
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        mDetector?.let {
            if (viewModel.grouping != Grouping.NONE && it.onTouchEvent(event)) {
                return true
            }
        }
        return super.dispatchTouchEvent(event)
    }

    @Composable
    fun RenderTree(
        modifier: Modifier = Modifier,
        tree: Category,
        choiceMode: ChoiceMode,
        expansionMode: ExpansionMode,
        accountInfo: DistributionAccountInfo
    ) {
        val nestedScrollInterop = rememberNestedScrollInteropConnection()
        Category(
            modifier = modifier.nestedScroll(nestedScrollInterop),
            category = tree,
            choiceMode = choiceMode,
            expansionMode = expansionMode,
            sumCurrency = currencyContext[accountInfo.currency],
            menuGenerator = remember {
                { category ->
                    org.totschnig.myexpenses.compose.Menu(
                        buildList {
                            add(
                                MenuEntry(
                                    Icons.AutoMirrored.Filled.List,
                                    R.string.menu_show_transactions,
                                    "SHOW_TRANSACTIONS"
                                ) {
                                    lifecycleScope.launch {
                                        showTransactions(
                                            category,
                                            category.id.sign > 0
                                        )
                                    }
                                }
                            )
                            if (category.level == 1 && category.color != null)
                                add(
                                    MenuEntry(
                                        Icons.Filled.Palette,
                                        R.string.color,
                                        "COLOR"
                                    ) {
                                        editCategoryColor(category.id, category.color)
                                    }
                                )
                        }
                    )
                }
            },
            withTypeColors = false
        )
    }

    private fun requireChart(context: Context, inner: Boolean) = PieChart(context).also {
        if (inner) {
            innerChart = it
        } else {
            chart = it
        }
    }

    @Composable
    fun RenderSumLine(
        accountInfo: DistributionAccountInfo,
        sums: Pair<Long, Long>
    ) {
        val sumLineBehaviour = viewModel.sumLineBehaviour.collectAsState(initial = DistributionViewModel.SumLineBehaviour.WithoutTotal)
        val accountFormatter = LocalCurrencyFormatter.current
        val currencyUnit = currencyContext[accountInfo.currency]
        val income = Money(currencyUnit, sums.first)
        val expense = Money(currencyUnit, sums.second)
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(modifier = Modifier
            .padding(horizontal = dimensionResource(id = R.dimen.padding_main_screen))
            .clickable {
                lifecycleScope.launch {
                    viewModel.cycleSumLineBehaviour()
                }
            }) {
            CompositionLocalProvider(
                LocalTextStyle provides TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = TEXT_SIZE_MEDIUM_SP.sp
                )
            ) {
                Text("∑ :")
                if (sumLineBehaviour.value == DistributionViewModel.SumLineBehaviour.WithoutTotal) {
                    val configure: (DecimalFormat) -> Unit = {
                        it.positivePrefix = "+"
                        it.negativePrefix = "-"
                    }
                    Text(
                        modifier = Modifier.weight(1f),
                        text = accountFormatter.formatCurrency(
                            income.amountMajor,
                            currencyUnit,
                            configure
                        ),
                        textAlign = TextAlign.End
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = accountFormatter.formatCurrency(
                            expense.amountMajor,
                            currencyUnit,
                            configure
                        ),
                        textAlign = TextAlign.End
                    )
                } else {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = buildString {
                            val configure: (DecimalFormat) -> Unit = {
                                it.positivePrefix = "+ "
                                it.negativePrefix = "- "
                            }
                            append(
                                accountFormatter.formatCurrency(
                                    income.amountMajor,
                                    currencyUnit,
                                    configure
                                )
                            )
                            append(" ")
                            append(
                                accountFormatter.formatCurrency(
                                    expense.amountMajor,
                                    currencyUnit,
                                    configure
                                )
                            )
                            if (sumLineBehaviour.value == DistributionViewModel.SumLineBehaviour.PercentageExpense
                                && sums.first != 0L && sums.second != 0L
                                ) {
                                append(" (${localizedPercentFormat.format(sums.second / sums.first.toFloat())})")
                            }
                            append(" = ")
                            val delta = sums.first + sums.second
                            append(
                                accountFormatter.formatMoney(
                                    Money(currencyUnit, delta),
                                    configure
                                )
                            )
                            if (sumLineBehaviour.value == DistributionViewModel.SumLineBehaviour.PercentageTotal
                                && delta != 0L && sums.first != 0L) {
                                append(" (${localizedPercentFormat.format(delta.absoluteValue / sums.first.toFloat())})")
                            }
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        HorizontalDivider(thickness = 4.dp, color = Color(accountInfo.color))
    }

    @Composable
    fun RenderChart(
        modifier: Modifier,
        inner: Boolean,
        categories: Category,
        angle: Float = 360f,
        isCombined: Boolean = false
    ) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                requireChart(ctx, inner).apply {
                    isRotationEnabled = PieRadarChartBase.ROTATION_INSIDE_ONLY
                    description.isEnabled = false
                    renderer = SelectivePieChartRenderer(
                        this,
                        object : SelectivePieChartRenderer.Selector {
                            var lastValueGreaterThanOne = true
                            override fun shouldDrawEntry(
                                index: Int,
                                pieEntry: PieEntry,
                                value: Float
                            ): Boolean {
                                val greaterThanOne = value > 1f
                                val shouldDraw = greaterThanOne || lastValueGreaterThanOne
                                lastValueGreaterThanOne = greaterThanOne
                                return shouldDraw
                            }
                        }).apply {
                        paintEntryLabels.color = textColorSecondary.defaultColor
                        paintEntryLabels.textSize =
                            UiUtils.sp2Px(TEXT_SIZE_SMALL_SP, resources).toFloat()
                    }
                    setCenterTextSizePixels(
                        UiUtils.sp2Px(TEXT_SIZE_MEDIUM_SP, resources).toFloat()
                    )
                    setCenterTextColor(textColorSecondary.defaultColor)
                    setUsePercentValues(true)
                    holeRadius = if (inner) 75f else 85f
                    setHoleColor(android.graphics.Color.TRANSPARENT)
                    legend.isEnabled = false
                    description.isEnabled = false
                }
            }) {
            it.maxAngle = angle
            it.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry, highlight: Highlight) {
                    val index = highlight.x.toInt()
                    selectionState.value = categories.children.getOrNull(index)
                    it.setCenterText(index, if (isCombined) innerChart else chart)
                }

                override fun onNothingSelected() {
                    selectionState.value = null
                }
            })
            it.notifyDataSetChanged()
            it.invalidate()
        }
    }

    private fun PieChart.setCenterText(position: Int, target: PieChart) {
        val entry = data.dataSet.getEntryForIndex(position)
        val description = entry.label
        val value = data.dataSet.valueFormatter.getFormattedValue(
            entry.value / data.yValueSum * 100f,
            entry, position, null
        )

        target.centerText = """
            $description
            $value
            """.trimIndent()
    }

    private fun editCategoryColor(id: Long, color: Int?) {
        SimpleColorDialog.build()
            .allowCustom(true)
            .cancelable(false)
            .neut()
            .extra(Bundle().apply {
                putLong(KEY_ROWID, id.absoluteValue)
            }).apply {
                color?.let { colorPreset(it) }
            }
            .show(this, EDIT_COLOR_DIALOG)
    }

    companion object {
        private const val SWIPE_MIN_DISTANCE = 120
        private const val SWIPE_MAX_OFF_PATH = 250
        private const val SWIPE_THRESHOLD_VELOCITY = 100
        private const val TEXT_SIZE_SMALL_SP = 14F
        private const val TEXT_SIZE_MEDIUM_SP = 18F
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle) =
        if (EDIT_COLOR_DIALOG == dialogTag && which == OnDialogResultListener.BUTTON_POSITIVE) {
            viewModel.updateColor(extras.getLong(KEY_ROWID), extras.getInt(SimpleColorDialog.COLOR))
            true
        } else false
}