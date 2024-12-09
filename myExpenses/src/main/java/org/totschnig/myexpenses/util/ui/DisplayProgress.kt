package org.totschnig.myexpenses.util.ui

import android.content.Context
import android.icu.text.MessageFormat
import android.os.Build
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastRoundToInt
import app.futured.donut.DonutSection
import org.totschnig.myexpenses.R

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

        fun contentDescription(context: Context, progress: Float) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                MessageFormat.format(
                    context.getString(R.string.percent_long),
                    mapOf("value" to progress.fastRoundToInt())
                )
            } else progress.fastRoundToInt().toString()
    }
}