package org.totschnig.myexpenses.activity

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.SparseArray
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Menu
import android.widget.CompoundButton
import androidx.activity.viewModels
import androidx.appcompat.widget.SwitchCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.color.SimpleColorDialog
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.*
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.ui.SelectivePieChartRenderer
import org.totschnig.myexpenses.util.ColorUtils
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.DistributionViewModel
import org.totschnig.myexpenses.viewmodel.DistributionViewModelBase
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo
import kotlin.math.abs

class DistributionActivity : DistributionBaseActivity<DistributionViewModel>(), OnDialogResultListener {
    override val viewModel: DistributionViewModel by viewModels()
    private lateinit var chart: PieChart
    override val prefKey = PrefKey.DISTRIBUTION_AGGREGATE_TYPES
    private val showChart = mutableStateOf(false)
    private var mDetector: GestureDetector? = null

    private val subColorMap = SparseArray<List<Int>>()
    private fun getSubColors(color: Int, isDark: Boolean): List<Int> {
        return subColorMap.get(color)
            ?: (if (isDark) ColorUtils.getTints(color) else ColorUtils.getShades(color)).also {
                subColorMap.put(color, it)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.distribution, menu)
        menuInflater.inflate(R.menu.grouping, menu)
        (menu.findItem(R.id.switchId).actionView?.findViewById(R.id.TaType) as? SwitchCompat)?.let {
            it.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                prefHandler.putBoolean(prefKey, isChecked)
                viewModel.setIncomeType(isChecked)
                reset()
            }
        }
        super.onCreateOptionsMenu(menu)
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
        menu.findItem(R.id.TOGGLE_CHART_COMMAND)?.let {
            it.isChecked = showChart.value
        }
        val item = menu.findItem(R.id.switchId)
        Utils.menuItemSetEnabledAndVisible(item, !viewModel.aggregateTypes)
        if (!viewModel.aggregateTypes) {
            (item.actionView?.findViewById<View>(R.id.TaType) as? SwitchCompat)?.isChecked =
                viewModel.incomeType
        }
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
            R.id.TOGGLE_CHART_COMMAND -> {
                showChart.value = tag as Boolean
                prefHandler.putBoolean(PrefKey.DISTRIBUTION_SHOW_CHART, showChart.value)
                invalidateOptionsMenu()
                true
            }
            else -> false
        }

    private fun setChartData(categories: List<Category>) {
        if ((::chart.isInitialized)) {
            chart.data = PieData(PieDataSet(categories.map {
                PieEntry(
                    abs(it.aggregateSum.toFloat()),
                    it.label
                )
            }, "").apply {
                colors = categories.map { it.color ?: 0 }
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
        val binding = setupView()
        showChart.value = prefHandler.getBoolean(PrefKey.DISTRIBUTION_SHOW_CHART, true)
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
        }
        viewModel.initWithAccount(intent.getLongExtra(DatabaseConstants.KEY_ACCOUNTID, 0))
        if (savedInstanceState == null) {
            val grouping = enumValueOrDefault(intent.getStringExtra(KEY_GROUPING), Grouping.NONE)
            val groupingYear = intent.getIntExtra(DatabaseConstants.KEY_YEAR, 0)
            val groupingSecond = intent.getIntExtra(DatabaseConstants.KEY_SECOND_GROUP, 0)
            if (groupingYear == 0 && groupingSecond == 0) {
                viewModel.setGrouping(grouping)
            } else {
                viewModel.groupingInfo =
                    DistributionViewModelBase.GroupingInfo(grouping, groupingYear, groupingSecond)
            }
        }

        lifecycleScope.launch {
            viewModel.accountInfo.filterNotNull().collect {
                supportActionBar?.title = it.label(this@DistributionActivity)
            }
        }

        binding.composeView.setContent {
            AppTheme(this) {
                val isDark = isSystemInDarkTheme()
                val configuration = LocalConfiguration.current
                val categoryTree =
                    viewModel.categoryTreeForDistribution.collectAsState(initial = Category.LOADING).value.let { category ->
                        if (showChart.value) category.withSubColors {
                            getSubColors(it, isDark)
                        } else {
                            category.copy(children = category.children.map { it.copy(color = null) })
                        }
                    }

                val chartCategoryTree = derivedStateOf {
                    //expansionState does not reflect updates to the data, that is why we just use it
                    //to walk down the updated tree and find the expanded category
                    var result = categoryTree
                    expansionState.forEach { expanded ->
                        result = result.children.find { it.id == expanded.id } ?: result
                    }
                    result
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
                    override fun toggle(category: Category) {
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
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        categoryTree == Category.LOADING -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(96.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        categoryTree.children.isEmpty() -> {
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = stringResource(id = R.string.no_mapped_transactions),
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            val sums = viewModel.sums.collectAsState(initial = 0L to 0L).value
                            when (configuration.orientation) {
                                Configuration.ORIENTATION_LANDSCAPE -> {
                                    Column {
                                        Row(modifier = Modifier.weight(1f)) {
                                            RenderTree(
                                                modifier = Modifier.weight(0.5f),
                                                category = categoryTree,
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
                                            category = categoryTree,
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
        modifier: Modifier,
        category: Category,
        choiceMode: ChoiceMode,
        expansionMode: ExpansionMode,
        accountInfo: DistributionAccountInfo?
    ) {
        Category(
            modifier = modifier,
            category = category,
            choiceMode = choiceMode,
            expansionMode = expansionMode,
            sumCurrency = accountInfo?.currency,
            menuGenerator = {
                org.totschnig.myexpenses.compose.Menu(
                    buildList {
                        if (accountInfo != null) {
                            add(
                                MenuEntry(
                                    Icons.Filled.List,
                                    stringResource(id = R.string.menu_show_transactions),
                                    ::showTransactions
                                )
                            )
                        }
                        if (it.level == 1)
                            add(
                                MenuEntry(
                                    Icons.Filled.Palette,
                                    stringResource(id = R.string.color)
                                ) { category -> category.color?.let { editCategoryColor(category.id, it) } }
                            )
                    }
                )
            }
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
            Divider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colors.onSurface,
                thickness = 1.dp
            )
            Row(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.activity_horizontal_margin))) {
                CompositionLocalProvider(
                    LocalTextStyle provides TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = TEXT_SIZE_MEDIUM_SP.sp
                    )
                ) {
                    Text("∑ :")
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "+" + accountFormatter(sums.first, it.currency),
                        textAlign = TextAlign.End
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "-" + accountFormatter(sums.second, it.currency),
                        textAlign = TextAlign.End
                    )
                }
            }
            Divider(color = Color(it.color), thickness = 4.dp)
        }
    }

    @Composable
    fun RenderChart(
        modifier: Modifier,
        categories: State<Category>
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
                                UiUtils.sp2Px(TEXT_SIZE_SMALL_SP, resources).toFloat()
                        }
                        setCenterTextSizePixels(
                            UiUtils.sp2Px(TEXT_SIZE_MEDIUM_SP, resources).toFloat()
                        )
                        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry, highlight: Highlight) {
                                val index = highlight.x.toInt()
                                selectionState.value = categories.value.children.getOrNull(index)
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

    private fun editCategoryColor(id: Long, color: Int) {
        SimpleColorDialog.build()
            .allowCustom(true)
            .cancelable(false)
            .neut()
            .extra(Bundle().apply {
                putLong(KEY_ROWID, id)
            })
            .colorPreset(color)
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