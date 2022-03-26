package org.totschnig.myexpenses.activity

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.compose.ChoiceMode
import org.totschnig.myexpenses.compose.ExpansionMode
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.SelectivePieChartRenderer
import org.totschnig.myexpenses.util.ColorUtils
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.viewmodel.DistributionViewModel
import org.totschnig.myexpenses.viewmodel.data.Category2
import kotlin.math.abs

class DistributionActivity : ProtectedFragmentActivity() {
    private lateinit var chart: PieChart
    private lateinit var binding: ActivityComposeBinding
    val viewModel: DistributionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(true)
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
        }
        viewModel.initWithAccount(intent.getLongExtra(DatabaseConstants.KEY_ACCOUNTID, 0))
        binding.composeView.setContent {
            AppTheme(this) {
                val configuration = LocalConfiguration.current
                val categoryTree =
                    viewModel.categoryTreeWithSum.collectAsState(initial = Category2.EMPTY)
                val selectionState: MutableState<Category2?> = remember {
                    mutableStateOf(null)
                }
                val expansionState: MutableState<Category2?> = remember {
                    mutableStateOf(null)
                }
                val chartCategoryTree = derivedStateOf {
                    (expansionState.value ?: categoryTree.value)
                }
                LaunchedEffect(chartCategoryTree.value) {
                    if (::chart.isInitialized) {
                        val categories = chartCategoryTree.value.children
                        chart.data = PieData(PieDataSet(categories.map { PieEntry(abs(it.aggregateSum.toFloat()), it.label) }, "").apply {
                            colors = chartCategoryTree.value.takeIf { it.id == 0L }?.children?.map(Category2::color) ?: getSubColors(chartCategoryTree.value.color!!)
                            sliceSpace = 2f
                            setDrawValues(false)
                            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                            valueLinePart2Length = 0.1f
                            valueLineColor = textColorSecondary.defaultColor
                        }).apply {
                            setValueFormatter(PercentFormatter())
                        }
                        chart.highlightValues(null)
                        selectionState.value = categories.firstOrNull()
                        chart.invalidate()
                    }
                }

                LaunchedEffect(selectionState.value) {
                    if (::chart.isInitialized) {

                        val position = chartCategoryTree.value.children.indexOf(selectionState.value)
                        if (position > -1) {
                            chart.highlightValue(position.toFloat(), 0)
                            chart.setCenterText(position)
                        }
                    }
                }
                val choiceMode = ChoiceMode.SingleChoiceMode(selectionState) { id -> chartCategoryTree.value.children.any { it.id == id } }
                when (configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        Row {
                            RenderTree(
                                modifier = Modifier.weight(0.5f),
                                category = categoryTree.value,
                                choiceMode = choiceMode,
                                expansionState = expansionState
                            )
                            RenderChart(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxHeight(),
                                categories = chartCategoryTree,
                                expansionState = selectionState
                            )
                        }
                    }
                    else -> {
                        Column {
                            RenderTree(
                                modifier = Modifier.weight(0.5f),
                                category = categoryTree.value,
                                choiceMode = choiceMode,
                                expansionState = expansionState
                            )
                            RenderChart(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxSize(),
                                categories = chartCategoryTree,
                                expansionState = selectionState
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun RenderTree(
        modifier: Modifier,
        category: Category2,
        choiceMode: ChoiceMode,
        expansionState: MutableState<Category2?>
    ) {
        Category(
            modifier = modifier,
            category = category,
            choiceMode = choiceMode,
            expansionMode = ExpansionMode.Single(expansionState)
        )
    }

    private fun requireChart(context: Context) {
        chart = PieChart(context)
    }

    @Composable
    fun RenderChart(
        modifier: Modifier,
        categories: State<Category2>,
        expansionState: MutableState<Category2?>
    ) {
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
                            expansionState.value = categories.value.children[index]
                            this@apply.setCenterText(index)
                        }

                        override fun onNothingSelected() {
                            expansionState.value = null
                        }
                    })
                    setUsePercentValues(true)
                    legend.isEnabled = false
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

    fun getSubColors(color: Int): List<Int?>? {
        val isLight = UiUtils.themeBoolAttr(this, R.attr.isLightTheme)
        return if (isLight) ColorUtils.getShades(color) else ColorUtils.getTints(color)
        /*var result: List<Int?>? = subColorMap.get(color)
        if (result == null) {
            result = if (isLight) ColorUtils.getShades(color) else ColorUtils.getTints(color)
            subColorMap.put(color, result)
        }
        return result*/
    }
}