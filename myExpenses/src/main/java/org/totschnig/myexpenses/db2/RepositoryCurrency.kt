package org.totschnig.myexpenses.db2

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_COMMODITY_TYPE
import org.totschnig.myexpenses.provider.KEY_FRACTION_DIGITS
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SYMBOL
import org.totschnig.myexpenses.provider.KEY_VALUES
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.CURRENCIES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DUAL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_UPDATED_ACCOUNTS_COUNT
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_CHECK_CURRENCY_IN_USE
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_UPDATE_CURRENCY
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_OLD_CODE
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_OLD_FRACTION_DIGITS

suspend fun Repository.insertCurrency(
    code: String,
    symbol: String,
    label: String?,
    fractionDigits: Int,
    type: CommodityType
): Uri? = withContext(Dispatchers.IO) {
    contentResolver.insert(
        CURRENCIES_URI,
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
    oldCode: String?,
    commodityType: CommodityType?,
    oldFractionDigits: Int?
): Pair<Int, Int> = withContext(Dispatchers.IO) {
    val values = ContentValues().apply {
        put(KEY_CODE, code)
        put(KEY_SYMBOL, symbol)
        put(KEY_FRACTION_DIGITS, fractionDigits)
        put(KEY_LABEL, label)
        commodityType?.let {
            put(KEY_COMMODITY_TYPE, it.name)
        }
    }
    val extras = Bundle().apply {
        putLong(KEY_ROWID, id)
        putParcelable(KEY_VALUES, values)
        oldCode?.let { putString(QUERY_PARAMETER_OLD_CODE, it) }
        oldFractionDigits?.let { putInt(QUERY_PARAMETER_OLD_FRACTION_DIGITS, it) }
    }
    with(contentResolver.call(
        DUAL_URI,
        METHOD_UPDATE_CURRENCY,
        null,
        extras
    )!!) {
        getInt(KEY_RESULT, 0) to getInt(KEY_UPDATED_ACCOUNTS_COUNT, 0)
    }
}

suspend fun Repository.deleteCurrency(currencyId: Long): Int =
    withContext(Dispatchers.IO) {
        contentResolver.delete(
            CURRENCIES_URI,
            "$KEY_ROWID = ?", arrayOf(currencyId.toString())
        )
    }

suspend fun Repository.isCurrencyUsed(code: String): Boolean = withContext(Dispatchers.IO) {
    val result = contentResolver.call(
        DUAL_URI,
        METHOD_CHECK_CURRENCY_IN_USE,
        code,
        null
    )
    result?.getBoolean(KEY_RESULT, false) ?: false
}
