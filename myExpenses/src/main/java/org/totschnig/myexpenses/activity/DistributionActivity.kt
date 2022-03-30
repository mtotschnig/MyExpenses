package org.totschnig.myexpenses.activity

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Menu
import android.widget.CompoundButton
import androidx.activity.viewModels
import androidx.appcompat.widget.SwitchCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.*
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.SelectivePieChartRenderer
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.DistributionViewModel
import org.totschnig.myexpenses.viewmodel.data.Category2
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo
import kotlin.math.abs

class DistributionActivity : ProtectedFragmentActivity() {
    private lateinit var chart: PieChart
    private lateinit var binding: ActivityComposeBinding
    val viewModel: DistributionViewModel by viewModels()
    val prefKey = PrefKey.DISTRIBUTION_AGGREGATE_TYPES
    private val showChart = mutableStateOf(false)
    private var mDetector: GestureDetector? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.distribution, menu)
        menuInflater.inflate(R.menu.grouping, menu)
        val typeButton: SwitchCompat =
            menu.findItem(R.id.switchId).actionView.findViewById(R.id.TaType)
        typeButton.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            prefHandler.putBoolean(prefKey, isChecked)
            viewModel.setIncomeType(
                isChecked
            )
            reset()
        }
        super.onCreateOptionsMenu(menu)
        return true
    }

    val expansionState
        get() = viewModel.expansionState
    val selectionState
        get() = viewModel.selectionState

    private fun reset() {
        expansionState.clear()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        Utils.configureGroupingMenu(
            menu.findItem(R.id.GROUPING_COMMAND).subMenu,
            viewModel.grouping
        )
        menu.findItem(R.id.TOGGLE_CHART_COMMAND)?.let {
            it.isChecked = showChart.value
        }
        menu.findItem(R.id.TOGGLE_AGGREGATE_TYPES)?.let {
            it.isChecked = viewModel.aggregateTypes
        }
        val item = menu.findItem(R.id.switchId)
        Utils.menuItemSetEnabledAndVisible(item, !viewModel.aggregateTypes)
        if (!viewModel.aggregateTypes) {
            (item.actionView.findViewById<View>(R.id.TaType) as SwitchCompat).isChecked =
                viewModel.incomeType
        }
        val grouped = viewModel.grouping != Grouping.NONE
        Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.FORWARD_COMMAND), grouped)
        Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.BACK_COMMAND), grouped)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (handleGrouping(item)) return true
        return super.onOptionsItemSelected(item)
    }

    private fun handleGrouping(item: MenuItem): Boolean {
        val newGrouping = Utils.getGroupingFromMenuItemId(item.itemId)
        if (newGrouping != null) {
            viewModel.setGrouping(newGrouping)
            invalidateOptionsMenu()
            reset()
            return true
        }
        return false
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) {
            true
        } else when (command) {
            R.id.TOGGLE_AGGREGATE_TYPES -> {
                val value = tag as Boolean
                viewModel.setAggregateTypes(value)
                if (value) {
                    prefHandler.remove(prefKey)
                } else {
                    prefHandler.putBoolean(prefKey, viewModel.incomeType)
                }
                invalidateOptionsMenu()
                reset()
                true
            }
            R.id.TOGGLE_CHART_COMMAND -> {
                showChart.value = tag as Boolean
                prefHandler.putBoolean(PrefKey.DISTRIBUTION_SHOW_CHART, showChart.value)
                invalidateOptionsMenu()
                true
            }
            R.id.BACK_COMMAND -> {
                viewModel.backward()
                true
            }
            R.id.FORWARD_COMMAND -> {
                viewModel.forward()
                true
            }
            else -> false
        }

    private fun setChartData(categories: List<Category2>) {
        if ((::chart.isInitialized)) {
            chart.data = PieData(PieDataSet(categories.map {
                PieEntry(
                    abs(it.aggregateSum.toFloat()),
                    it.label
                )
            }, "").apply {
                colors = categories.map(Category2::color)
                sliceSpace = 2f
                setDrawValues(false)
                xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart2Length = 0.1f
                valueLineColor = textColorSecondary.defaultColor
            }).apply {
                setValueFormatter(PercentFormatter())
            }
            chart.invalidate()
        }
        selectionState.value = categories.firstOrNull()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(true)
        showChart.value = prefHandler.getBoolean(PrefKey.DISTRIBUTION_SHOW_CHART, true)
        val aggregateTypesFromPreference =
            if (prefHandler.isSet(prefKey)) prefHandler.getBoolean(prefKey, false) else null
        viewModel.setAggregateTypes(aggregateTypesFromPreference == null)
        if (aggregateTypesFromPreference != null) {
            viewModel.setIncomeType(aggregateTypesFromPreference)
        }
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
        }
        viewModel.initWithAccount(intent.getLongExtra(DatabaseConstants.KEY_ACCOUNTID, 0))
        lifecycleScope.launch {
            viewModel.accountInfo.filterNotNull().collect {
                supportActionBar?.title = it.label
            }
        }
        lifecycleScope.launch {
            viewModel.displayTitle.collect {
                supportActionBar?.subtitle = it
            }
        }

        binding.composeView.setContent {
            AppTheme(this) {
                val configuration = LocalConfiguration.current
                val categoryTree =
                    viewModel.categoryTreeWithSum.collectAsState(initial = Category2.EMPTY)

                val chartCategoryTree = derivedStateOf {
                    (expansionState.lastOrNull() ?: categoryTree.value)
                }
                LaunchedEffect(chartCategoryTree.value) {
                    setChartData(chartCategoryTree.value.children)
                }

                LaunchedEffect(selectionState.value) {
                    if (::chart.isInitialized) {
                        val position =
                            chartCategoryTree.value.children.indexOf(selectionState.value)
                        if (position > -1) {
                            chart.highlightValue(position.toFloat(), 0)
                            chart.setCenterText(position)
                        }
                    }
                }
                val choiceMode =
                    ChoiceMode.SingleChoiceMode(selectionState) { id -> chartCategoryTree.value.children.any { it.id == id } }
                val expansionMode = object : ExpansionMode.Single(expansionState) {
                    override fun toggle(category: Category2) {
                        super.toggle(category)
                        //when we collapse a category, we want it to be selected, when expand the first child should be selected
                        if (isExpanded(category.id)) {
                            selectionState.value = category.children.firstOrNull()
                        } else {
                            selectionState.value = category
                        }
                    }
                }
                val accountInfo = viewModel.accountInfo.collectAsState(null)
                if (categoryTree.value.children.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(id = R.string.no_mapped_transactions),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val sums = viewModel.sums.collectAsState(initial = 0L to 0L).value
                    when (configuration.orientation) {
                        Configuration.ORIENTATION_LANDSCAPE -> {
                            Column {
                                Row(modifier = Modifier.weight(1f)) {
                                    RenderTree(
                                        modifier = Modifier.weight(0.5f),
                                        category = categoryTree.value,
                                        choiceMode = choiceMode,
                                        expansionMode = expansionMode,
                                        accountInfo = accountInfo.value
                                    )
                                    RenderChart(
                                        modifier = Modifier
                                            .weight(0.5f)
                                            .fillMaxHeight(),
                                        categories = chartCategoryTree
                                    )
                                }
                                RenderSumLine(accountInfo.value, sums)
                            }
                        }
                        else -> {
                            Column {
                                RenderTree(
                                    modifier = Modifier.weight(0.5f),
                                    category = categoryTree.value,
                                    choiceMode = choiceMode,
                                    expansionMode = expansionMode,
                                    accountInfo = accountInfo.value
                                )
                                RenderChart(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .fillMaxSize(),
                                    categories = chartCategoryTree
                                )
                                RenderSumLine(accountInfo.value, sums)
                            }
                        }
                    }
                }
            }
        }
        setupGestureDetector()
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
                    e1: MotionEvent, e2: MotionEvent,
                    velocityX: Float, velocityY: Float
                ): Boolean {
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

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        mDetector?.let {
            if (viewModel.grouping != Grouping.NONE && it.onTouchEvent(event)) {
                return true
            }
        }
        return super.dispatchTouchEvent(event)
    }

    @Composable
    fun RenderTree(
        modifier: Modifier,
        category: Category2,
        choiceMode: ChoiceMode,
        expansionMode: ExpansionMode,
        accountInfo: DistributionAccountInfo?
    ) {
        Category(
            modifier = modifier,
            category = category,
            choiceMode = choiceMode,
            expansionMode = expansionMode,
            sumCurrency = accountInfo?.currency
        )
    }

    private fun requireChart(context: Context) {
        chart = PieChart(context)
    }

    @Composable
    fun RenderSumLine(
        accountInfo: DistributionAccountInfo?,
        sums: Pair<Long, Long>
    ) {
        val accountFormatter = LocalAmountFormatter.current
        accountInfo?.let {
            Row {
                CompositionLocalProvider(LocalTextStyle provides TextStyle(fontWeight = FontWeight.Bold)) {
                    Text("∑ :")
                    Text(modifier = Modifier.weight(1f), text= "+" + accountFormatter(sums.first, it.currency), textAlign = TextAlign.End)
                    Text(modifier = Modifier.weight(1f), text= "-" + accountFormatter(sums.second, it.currency), textAlign = TextAlign.End)
                }
            }
            Divider(color = Color(it.color), thickness = 4.dp)
        }
    }

    @Composable
    fun RenderChart(
        modifier: Modifier,
        categories: State<Category2>
    ) {
        if (showChart.value)
            AndroidView(
                modifier = modifier,
                factory = { ctx ->
                    requireChart(ctx)
                    chart.apply {
                        description.isEnabled = false
                        setExtraOffsets(20f, 0f, 20f, 0f)
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
                                getTextSizeForAppearance(android.R.attr.textAppearanceSmall).toFloat()
                        }
                        setCenterTextSizePixels(getTextSizeForAppearance(android.R.attr.textAppearanceMedium).toFloat())
                        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry, highlight: Highlight) {
                                val index = highlight.x.toInt()
                                selectionState.value = categories.value.children[index]
                                this@apply.setCenterText(index)
                            }

                            override fun onNothingSelected() {
                                selectionState.value = null
                            }
                        })
                        setUsePercentValues(true)
                        legend.isEnabled = false
                        setChartData(categories.value.children)
                    }
                })
    }

    private fun getTextSizeForAppearance(appearance: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(appearance, typedValue, true)
        val textSizeAttr = intArrayOf(android.R.attr.textSize)
        val indexOfAttrTextSize = 0
        val a = obtainStyledAttributes(typedValue.data, textSizeAttr)
        val textSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1)
        a.recycle()
        return textSize
    }

    private fun PieChart.setCenterText(position: Int) {
        val entry = data.dataSet.getEntryForIndex(position)
        val description = entry.label
        val value = data.dataSet.valueFormatter.getFormattedValue(
            entry.value / data.yValueSum * 100f,
            entry, position, null
        )
        centerText = """
            $description
            $value
            """.trimIndent()
    }

    companion object {
        private const val SWIPE_MIN_DISTANCE = 120
        private const val SWIPE_MAX_OFF_PATH = 250
        private const val SWIPE_THRESHOLD_VELOCITY = 100
    }
}