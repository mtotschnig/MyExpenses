package org.totschnig.myexpenses.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.DebugCurrencyFormatter
import org.totschnig.myexpenses.util.ICurrencyFormatter
import java.time.format.DateTimeFormatter

data class Colors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
)

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
