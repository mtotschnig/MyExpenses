package org.totschnig.myexpenses.db2

import android.content.ContentValues
import org.totschnig.myexpenses.db2.name
import org.totschnig.myexpenses.dialog.SelectCategoryMoveTargetDialogFragment.Companion.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import java.time.LocalDate

const val USER_AS_SOURCE = "user"

private val ExchangeRateSource?.name
    get() = this?.name ?: USER_AS_SOURCE

fun Repository.savePrice(
    base: String,
    commodity: String,
    date: LocalDate,
    source: ExchangeRateSource?,
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

fun Repository.deletePrice(date: LocalDate, source: ExchangeRateSource?) {
    contentResolver.delete(
        TransactionProvider.PRICES_URI,
        "$KEY_DATE = ? AND ${DatabaseConstants.KEY_SOURCE} = ?",
        arrayOf(date.toString(), source.name)
    )
}