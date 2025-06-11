package org.totschnig.myexpenses.db2

import android.content.ContentValues
import android.os.Bundle
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.priceSort
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.util.calculateRawExchangeRate
import org.totschnig.myexpenses.util.calculateRealExchangeRate
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Stores the value of 1 major unit of commodity expressed in base currency
 */
fun Repository.savePrice(
    base: CurrencyUnit,
    commodity: CurrencyUnit,
    date: LocalDate,
    source: ExchangeRateSource,
    value: BigDecimal,
) = savePrice(base.code, commodity.code, date, source,
    calculateRawExchangeRate(value, commodity, base))

/**
 * Stores the value of 1 minor unit of commodity expressed in base currency
 */
fun Repository.savePrice(
    base: String,
    commodity: String,
    date: LocalDate,
    source: ExchangeRateSource,
    value: Double,
    updateEquivalentAmount: Boolean = prefHandler.getBoolean(PrefKey.EQUIVALENT_AMOUNTS_AUTOMATIC_UPDATE, true)
): Int {
    require(value > 0)
    contentResolver.insert(
        TransactionProvider.PRICES_URI,
        ContentValues().apply {
            put(KEY_CURRENCY, base)
            put(KEY_COMMODITY, commodity)
            put(KEY_DATE, date.toString())
            put(KEY_SOURCE, source.name)
            put(KEY_VALUE, value)
        })

    return if (updateEquivalentAmount) {
        contentResolver.call(
            TransactionProvider.DUAL_URI,
            TransactionProvider.METHOD_RECALCULATE_EQUIVALENT_AMOUNTS_FOR_DATE, null,
            Bundle(1).apply {
                putString(KEY_CURRENCY,  commodity)
                putSerializable(KEY_DATE, date)
            }
        )!!.getInt(TransactionProvider.KEY_RESULT)
    } else 0
}

fun Repository.deletePrice(
    date: LocalDate,
    source: ExchangeRateSource,
    currency: String,
    commodity: String
) = contentResolver.delete(
    TransactionProvider.PRICES_URI,
    "$KEY_DATE = ? AND $KEY_SOURCE = ? AND $KEY_CURRENCY = ? AND $KEY_COMMODITY = ?",
    arrayOf(date.toString(), source.name, currency, commodity)
)

fun Repository.loadPrice(
    base: CurrencyUnit,
    commodity: CurrencyUnit,
    date: LocalDate,
    source: ExchangeRateApi?,
): BigDecimal? = contentResolver.query(
    TransactionProvider.PRICES_URI,
    arrayOf(KEY_VALUE),
    listOfNotNull(
        KEY_CURRENCY, KEY_COMMODITY, KEY_DATE, if (source != null) KEY_SOURCE else null
    ).joinToString(" AND ") { "$it = ?" },
    if (source == null)
        arrayOf(base.code, commodity.code, date.toString())
    else
        arrayOf(base.code, commodity.code, date.toString(), source.name), priceSort(), null
)?.use {
    if (it.moveToFirst()) it.getDouble(0) else null
}?.let {
    calculateRealExchangeRate(it, commodity, base)
}