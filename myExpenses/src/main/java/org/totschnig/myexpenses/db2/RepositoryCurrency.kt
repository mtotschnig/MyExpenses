package org.totschnig.myexpenses.db2

import android.content.ContentValues
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_COMMODITY_TYPE
import org.totschnig.myexpenses.provider.KEY_FRACTION_DIGITS
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_SYMBOL
import org.totschnig.myexpenses.provider.TransactionProvider

suspend fun Repository.insertCurrency(
    code: String,
    symbol: String,
    label: String?,
    fractionDigits: Int,
    type: CommodityType
): Uri? = withContext(Dispatchers.IO) {
    contentResolver.insert(TransactionProvider.CURRENCIES_URI,
        ContentValues().apply {
            put(KEY_CODE, code)
            put(KEY_SYMBOL, symbol)
            put(KEY_FRACTION_DIGITS, fractionDigits)
            put(KEY_LABEL, label)
            put(KEY_COMMODITY_TYPE, type.name)
        }
    )
}

suspend fun Repository.updateFractionDigits(currency: String, fractionDigits: Int): Int =
    withContext(Dispatchers.IO) {
        contentResolver.update(
            TransactionProvider.CURRENCIES_URI.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_CHANGE_FRACTION_DIGITS)
                .appendPath(currency)
                .appendPath(fractionDigits.toString())
                .build(), null, null, null
        )
    }

suspend fun Repository.updateCurrencyLabel(currency: String, label: String): Int =
    withContext(Dispatchers.IO) {
        val contentValues = ContentValues(1).apply {
            put(KEY_LABEL, label)
        }
        contentResolver.update(
            TransactionProvider.CURRENCIES_URI.buildUpon().appendPath(currency).build(),
            contentValues, null, null
        )
    }



suspend fun Repository.deleteCurrency(currency: String): Int =
    withContext(Dispatchers.IO) {
        contentResolver.delete(
            TransactionProvider.CURRENCIES_URI.buildUpon().appendPath(currency).build(),
            null, null
        )
    }
