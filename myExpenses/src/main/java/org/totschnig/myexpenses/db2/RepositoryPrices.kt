package org.totschnig.myexpenses.db2

import android.content.ContentValues
import org.totschnig.myexpenses.dialog.SelectCategoryMoveTargetDialogFragment.Companion.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import java.time.LocalDate

fun Repository.savePrice(
    base: String,
    commodity: String,
    date: LocalDate,
    source: ExchangeRateSource,
    value: Double,
) {
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
}

fun Repository.deletePrice(
    date: LocalDate,
    source: ExchangeRateSource,
    currency: String,
    commodity: String
) {
    contentResolver.delete(
        TransactionProvider.PRICES_URI,
        "$KEY_DATE = ? AND $KEY_SOURCE = ? AND $KEY_CURRENCY = ? AND $KEY_COMMODITY = ?",
        arrayOf(date.toString(), source.name, currency, commodity)
    )
}