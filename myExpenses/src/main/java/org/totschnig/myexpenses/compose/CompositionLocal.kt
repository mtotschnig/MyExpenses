package org.totschnig.myexpenses.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.DebugCurrencyFormatter
import org.totschnig.myexpenses.util.convAmount
import java.time.format.DateTimeFormatter

class Colors(
    val income: Color,
    val expense: Color
)

val LocalColors =
    compositionLocalOf<Colors> { throw IllegalStateException("Colors not initialized") }

val LocalAmountFormatter = staticCompositionLocalOf<AmountFormatter> {
    { amount, currency ->
        DebugCurrencyFormatter.convAmount(
            amount,
            CurrencyUnit(currency, currency, 2)
        )
    }
}

val LocalDateFormatter = staticCompositionLocalOf<DateTimeFormatter> { DateTimeFormatter.BASIC_ISO_DATE }
