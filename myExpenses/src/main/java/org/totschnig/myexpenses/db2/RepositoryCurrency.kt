package org.totschnig.myexpenses.db2

import android.content.ContentValues
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_COMMODITY_TYPE
import org.totschnig.myexpenses.provider.KEY_FRACTION_DIGITS
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_SYMBOL
import org.totschnig.myexpenses.provider.TransactionProvider

fun Repository.createCurrency(
    code: String,
    symbol: String,
    label: String?,
    fractionDigits: Int,
    type: CommodityType
) {
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