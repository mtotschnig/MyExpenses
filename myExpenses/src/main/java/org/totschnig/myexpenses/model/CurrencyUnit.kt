package org.totschnig.myexpenses.model

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import java.io.Serializable
import java.util.*

@Immutable
data class CurrencyUnit(
    val code: String,
    val symbol: String,
    val fractionDigits: Int,
    val description: String
) : Serializable {
    @VisibleForTesting
    constructor(currency: Currency) : this(
        currency.currencyCode, currency.symbol, currency.defaultFractionDigits,
        currency.displayName
    )

    constructor(code: String, symbol: String, fractionDigits: Int) : this(
        code,
        symbol,
        fractionDigits,
        code
    )

    companion object {
        val DebugInstance by lazy { CurrencyUnit(Currency.getInstance("EUR")) }
    }
}