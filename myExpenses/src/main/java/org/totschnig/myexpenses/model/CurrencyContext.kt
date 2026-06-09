package org.totschnig.myexpenses.model

import kotlinx.coroutines.flow.Flow
import java.util.Currency

interface CurrencyContext {
    operator fun get(currencyCode: String): CurrencyUnit
    fun getAll(): Flow<List<CurrencyUnit>>
    /**
     * if fractionDigits is null, custom value is reset
     */
    fun storeCustomFractionDigits(currencyCode: String, fractionDigits: Int?)
    fun storeCustomSymbol(currencyCode: String, symbol: String)
    fun ensureFractionDigitsAreCached(currency: CurrencyUnit)
    fun invalidateHomeCurrency()

    val homeCurrencyString: String
    val homeCurrencyUnit: CurrencyUnit
        get() = this[homeCurrencyString]
    val localCurrency: Currency
}
