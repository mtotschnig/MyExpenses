package org.totschnig.myexpenses.model

import androidx.annotation.VisibleForTesting
import java.io.Serializable
import java.util.*

data class CurrencyUnit(val code: String, val symbol: String, val fractionDigits: Int, val description: String) : Serializable {
    companion object {
        @VisibleForTesting
        @JvmStatic
        fun create(currency: Currency) = CurrencyUnit(currency.currencyCode, currency.symbol, currency.defaultFractionDigits,
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) currency.getDisplayName() else currency.currencyCode)

        @JvmStatic
        fun create(code: String, symbol: String, fractionDigits: Int) =
                CurrencyUnit(code, symbol, fractionDigits, code)
    }
}