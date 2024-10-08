package org.totschnig.myexpenses.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.totschnig.myexpenses.di.NoOpTracker
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.DebugCurrencyFormatter
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.tracking.Tracker
import java.time.format.DateTimeFormatter

data class Colors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
) {
    fun amountColor(sign: Int) = when {
        sign > 0 -> income
        sign < 0 -> expense
        else -> Color.Unspecified
    }
}

val LocalColors = compositionLocalOf {
    Colors(
        income = Color.Green,
        expense = Color.Red,
        transfer = Color.Unspecified,
    )
}

val LocalCurrencyFormatter = staticCompositionLocalOf<ICurrencyFormatter> { DebugCurrencyFormatter }

val LocalDateFormatter = staticCompositionLocalOf { DateTimeFormatter.BASIC_ISO_DATE }

val LocalHomeCurrency = staticCompositionLocalOf { CurrencyUnit.DebugInstance }

val LocalTracker = staticCompositionLocalOf<Tracker> { NoOpTracker }
