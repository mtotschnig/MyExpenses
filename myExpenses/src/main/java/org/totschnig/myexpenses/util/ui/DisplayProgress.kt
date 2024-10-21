package org.totschnig.myexpenses.util.ui

import app.futured.donut.DonutSection

data class DisplayProgress(val displayValue: Float, val displayExcess: Float) {
    fun forViewSystem(valueColor: Int, excessColor: Int) = listOf(
        DonutSection("excess", excessColor, displayExcess),
        DonutSection("progress", valueColor, displayValue)
    )

    fun forCompose(
        valueColor: androidx.compose.ui.graphics.Color,
        excessColor: androidx.compose.ui.graphics.Color,
    ) = listOf(
        app.futured.donut.compose.data.DonutSection(displayExcess, excessColor),
        app.futured.donut.compose.data.DonutSection(displayValue, valueColor)
    )
    companion object {
        fun calcProgressVisualRepresentation(progress: Float) = when {

            progress > 200 -> DisplayProgress(0f,100f)

            progress > 100 -> DisplayProgress(200f - progress, progress - 100)

            progress >= 0 -> DisplayProgress(progress, 0f)

            else -> throw IllegalArgumentException()
        }
    }
}