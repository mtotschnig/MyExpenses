package org.totschnig.myexpenses.model

import java.util.Currency

interface CurrencyContext {
    operator fun get(currencyCode: String): CurrencyUnit
    fun storeCustomFractionDigits(currencyCode: String, fractionDigits: Int)
    fun storeCustomSymbol(currencyCode: String, symbol: String)
    fun ensureFractionDigitsAreCached(currency: CurrencyUnit)
    fun invalidateHomeCurrency()

    val homeCurrencyString: String
    val homeCurrencyUnit: CurrencyUnit
        get() = this[homeCurrencyString]
    val localCurrency: Currency
}
