package org.totschnig.myexpenses.model

import androidx.annotation.VisibleForTesting
import java.io.Serializable
import java.util.*

data class CurrencyUnit(val code: String, val symbol: String, val fractionDigits: Int, val description: String) : Serializable {
    @VisibleForTesting constructor(currency: Currency) : this(currency.currencyCode, currency.symbol, currency.defaultFractionDigits,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) currency.displayName else currency.currencyCode)
    constructor(code: String, symbol: String, fractionDigits: Int) : this(code, symbol, fractionDigits, code)
}