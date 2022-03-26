package org.totschnig.myexpenses.activity

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
import org.totschnig.myexpenses.compose.*
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.SelectivePieChartRenderer
import org.totschnig.myexpenses.viewmodel.DistributionViewModel
import org.totschnig.myexpenses.viewmodel.data.Category2
import kotlin.math.abs

class DistributionActivity: ProtectedFragmentActivity() {
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
                val category = viewModel.categoryTreeWithSum.collectAsState(initial = Category2.EMPTY)
                val selectionState: MutableState<Category2?> = remember {
                    mutableStateOf(null)
                }
                when (configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        Row {
                            RenderTree(
                                modifier = Modifier.weight(0.5f),
                                category = category.value,
                                selectionState = selectionState
                            )
                            RenderChart(
                                modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight(),
                                category = category,
                                expansionState = selectionState
                            )
                        }
                    }
                    else -> {
                        Column {
                            RenderTree(
                                modifier = Modifier.weight(0.5f),
                                category = category.value,
                                selectionState = selectionState
                            )
                            RenderChart(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .fillMaxSize(),
                                category = category,
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
        modifier: Modifier, category: Category2, selectionState: MutableState<Category2?>
    ) {
        Category(
            modifier = modifier,
            category = category,
            choiceMode = ChoiceMode.SingleChoiceMode(selectionState),
            expansionMode = ExpansionMode.Single(remember { mutableStateOf(null) })
        )
    }

    @Composable
    fun RenderChart(
        modifier: Modifier,
        category: State<Category2>,
        expansionState: MutableState<Category2?>
    ) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
            PieChart(ctx).apply {
                description.isEnabled = false
                setExtraOffsets(20f, 0f, 20f, 0f)
                renderer = SelectivePieChartRenderer(this, object : SelectivePieChartRenderer.Selector {
                    var lastValueGreaterThanOne = true
                    override fun shouldDrawEntry(index: Int, pieEntry: PieEntry, value: Float): Boolean {
                        val greaterThanOne = value > 1f
                        val shouldDraw = greaterThanOne || lastValueGreaterThanOne
                        lastValueGreaterThanOne = greaterThanOne
                        return shouldDraw
                    }
                }).apply {
                    paintEntryLabels.color = textColorSecondary.defaultColor
                    paintEntryLabels.textSize = getTextSizeForAppearance(android.R.attr.textAppearanceSmall).toFloat()
                }
                setCenterTextSizePixels(getTextSizeForAppearance(android.R.attr.textAppearanceMedium).toFloat())
                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry, highlight: Highlight) {
                        val index = highlight.x.toInt()
                        expansionState.value = category.value.children[index]
                        this@apply.setCenterText(index)
                    }

                    override fun onNothingSelected() {
                        expansionState.value = null
                    }
                })
                setUsePercentValues(true)
                legend.isEnabled = false
            }
        }) { pieChart ->
            val categories = category.value.children
            pieChart.data = PieData(PieDataSet(categories.map { PieEntry(abs(it.aggregateSum.toFloat()), it.label) }, "").apply {
                colors = categories.map(Category2::color)
                sliceSpace = 2f
                setDrawValues(false)
                xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart2Length = 0.1f
                valueLineColor = textColorSecondary.defaultColor
            }).apply {
                setValueFormatter(PercentFormatter())
            }
            val position = categories.indexOf(expansionState.value)
            if (position > -1) {
                pieChart.highlightValue(position.toFloat(), 0)
                pieChart.setCenterText(position)
            }
        }
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
            entry, position, null)
        centerText = """
            $description
            $value
            """.trimIndent()
    }
}