package org.totschnig.myexpenses.compose

import android.content.Context
import android.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.NoOpUpdate
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.PieRadarChartBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import org.totschnig.myexpenses.ui.SelectivePieChartRenderer
import org.totschnig.myexpenses.util.ui.UiUtils
import java.text.NumberFormat

const val TEXT_SIZE_SMALL_SP = 14F
const val TEXT_SIZE_MEDIUM_SP = 18F

@Composable
fun PieChartCompose(
    modifier: Modifier,
    factory: (Context) -> PieChart,
    angle: Float = 360f,
    holeRadius: Float = 85f,
    onValueSelected: (Int?) -> Unit,
    data: List<PieEntry>,
    colors: List<Int>,
    update: (PieChart) -> Unit = NoOpUpdate
) {
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            factory(ctx).apply {
                isRotationEnabled = PieRadarChartBase.ROTATION_INSIDE_ONLY
                description.isEnabled = false
                renderer = SelectivePieChartRenderer(
                    this,
                    object : SelectivePieChartRenderer.Selector {
                        var lastValueGreaterThanOne = true
                        override fun shouldDrawEntry(
                            index: Int,
                            pieEntry: PieEntry,
                            value: Float,
                        ): Boolean {
                            val greaterThanOne = value > 1f
                            val shouldDraw = greaterThanOne || lastValueGreaterThanOne
                            lastValueGreaterThanOne = greaterThanOne
                            return shouldDraw
                        }
                    }).apply {
                    paintEntryLabels.color = color
                    paintEntryLabels.textSize =
                        UiUtils.sp2Px(TEXT_SIZE_SMALL_SP, resources).toFloat()
                }
                setCenterTextSizePixels(
                    UiUtils.sp2Px(TEXT_SIZE_MEDIUM_SP, resources).toFloat()
                )
                setCenterTextColor(color)
                setUsePercentValues(true)
                this.holeRadius = holeRadius
                setHoleColor(Color.TRANSPARENT)
                legend.isEnabled = false
                description.isEnabled = false
            }
        }) {
        it.maxAngle = angle
        it.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, highlight: Highlight) {
                val index = highlight.x.toInt()
                onValueSelected(index)
                it.setCenterText(index)
            }

            override fun onNothingSelected() {
                onValueSelected(null)
                it.centerText = null
            }
        })
        it.setChartData(data, colors, color)
        update(it)
        it.invalidate()
    }
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

fun PieChart.setChartData(
    data: List<PieEntry>,
    colors: List<Int>,
    valueLineColor: Int
) {
    this.data = PieData(PieDataSet(data, "").apply {
        this.colors =  colors
        sliceSpace = 2f
        setDrawValues(false)
        xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        valueLinePart2Length = 0.1f
        this.valueLineColor = valueLineColor
    }).apply {
        setValueFormatter(percentFormatter)
    }
    invalidate()
}

val localizedPercentFormat: NumberFormat by lazy {
    NumberFormat.getPercentInstance().also {
        it.setMinimumFractionDigits(1)
    }
}

private val percentFormatter = IValueFormatter { value, _, _, _ ->
    localizedPercentFormat.format(value / 100)
}