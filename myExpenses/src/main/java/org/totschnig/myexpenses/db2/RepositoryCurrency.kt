package org.totschnig.myexpenses.db2

import android.content.ContentValues
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_COMMODITY_TYPE
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_FRACTION_DIGITS
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SYMBOL
import org.totschnig.myexpenses.provider.TransactionProvider

suspend fun Repository.insertCurrency(
    code: String,
    symbol: String,
    label: String?,
    fractionDigits: Int,
    type: CommodityType
): Uri? = withContext(Dispatchers.IO) {
    contentResolver.insert(
        TransactionProvider.CURRENCIES_URI,
        ContentValues().apply {
            put(KEY_CODE, code)
            put(KEY_SYMBOL, symbol)
            put(KEY_FRACTION_DIGITS, fractionDigits)
            put(KEY_LABEL, label)
            put(KEY_COMMODITY_TYPE, type.name)
        }
    )
}

suspend fun Repository.updateCurrency(
    id: Long,
    code: String,
    symbol: String,
    label: String?,
    fractionDigits: Int,
    oldCode: String?
): Int = withContext(Dispatchers.IO) {
    val values = ContentValues().apply {
        put(KEY_CODE, code)
        put(KEY_SYMBOL, symbol)
        put(KEY_FRACTION_DIGITS, fractionDigits)
        put(KEY_LABEL, label)
    }
    val result = contentResolver.update(
        TransactionProvider.CURRENCIES_URI,
        values, "$KEY_ROWID = ?", arrayOf(id.toString())
    )
    if (result > 0 && oldCode != null && oldCode != code) {
        val accountValues = ContentValues(1).apply {
            put(KEY_CURRENCY, code)
        }
        contentResolver.update(
            TransactionProvider.ACCOUNTS_URI,
            accountValues,
            "$KEY_CURRENCY = ?",
            arrayOf(oldCode)
        )
    }
    result
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

suspend fun Repository.deleteCurrency(currencyId: Long): Int =
    withContext(Dispatchers.IO) {
        contentResolver.delete(
            TransactionProvider.CURRENCIES_URI,
            "$KEY_ROWID = ?", arrayOf(currencyId.toString())
        )
    }
