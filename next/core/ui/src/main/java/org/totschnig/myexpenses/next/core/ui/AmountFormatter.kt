package org.totschnig.myexpenses.next.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import java.text.NumberFormat
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo

/**
 * Formats an amount in minor units of a currency, e.g. 123456 cents as "1.234,56 €". The app hands
 * in its formatter with [LocalAmountFormatter], so that amounts look as in the rest of the app.
 */
fun interface AmountFormatter {
    fun format(amount: Long, currency: CurrencyInfo): String
}

/** Used where the app provides none, e.g. in previews */
val SimpleAmountFormatter = AmountFormatter { amount, currency ->
    val format = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = currency.fractionDigits
        maximumFractionDigits = currency.fractionDigits
    }
    "${format.format(amount / Math.pow(10.0, currency.fractionDigits.toDouble()))} ${currency.symbol}"
}

val LocalAmountFormatter = staticCompositionLocalOf { SimpleAmountFormatter }
