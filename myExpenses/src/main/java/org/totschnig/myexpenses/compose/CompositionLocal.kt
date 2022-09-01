package org.totschnig.myexpenses.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.DebugCurrencyFormatter
import org.totschnig.myexpenses.util.convAmount
import java.time.format.DateTimeFormatter

data class Colors(
    val income: Color,
    val expense: Color,
    val iconTint: Color
)

val LocalColors = compositionLocalOf { Colors(
        income = Color.Red,
        expense = Color.Green,
        iconTint = Color.DarkGray
    ) }

val LocalAmountFormatter = staticCompositionLocalOf<AmountFormatter> {
    { amount, currency ->
        DebugCurrencyFormatter.convAmount(
            amount,
            currency
        )
    }
}

val LocalDateFormatter = staticCompositionLocalOf<DateTimeFormatter> { DateTimeFormatter.BASIC_ISO_DATE }
