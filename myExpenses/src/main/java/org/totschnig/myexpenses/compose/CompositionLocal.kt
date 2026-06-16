package org.totschnig.myexpenses.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.totschnig.myexpenses.di.NoOpTracker
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.DebugCurrencyFormatter
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.tracking.Tracker
import java.time.format.DateTimeFormatter
import java.util.Currency

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

val LocalTracker = staticCompositionLocalOf<Tracker> { NoOpTracker }

val LocalCurrencyContext = staticCompositionLocalOf<CurrencyContext> {
    object : CurrencyContext {
        override fun get(currencyCode: String) = CurrencyUnit(Currency.getInstance(currencyCode))

        override fun getAll(): Flow<List<CurrencyUnit>> = emptyFlow()

        override fun storeCustomFractionDigits(currencyCode: String, fractionDigits: Int?) {}

        override fun storeCustomSymbol(currencyCode: String, symbol: String) {}

        override fun ensureFractionDigitsAreCached(currency: CurrencyUnit) {}

        override fun invalidateHomeCurrency() {}

        override val homeCurrencyString: String
            get() = "EUR"
        override val localCurrency: Currency
            get() = Currency.getInstance(java.util.Locale.ROOT)
    }
}