package org.totschnig.myexpenses.model

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.Flow
import java.util.Currency

interface CurrencyContext {
    operator fun get(currencyCode: String): CurrencyUnit
    fun getAll(): Flow<List<CurrencyUnit>>
    fun invalidateHomeCurrency()
    fun invalidate(currencyCode: String)

    val homeCurrencyString: String
    val homeCurrencyUnit: CurrencyUnit
        get() = this[homeCurrencyString]
    val localCurrency: Currency
}
