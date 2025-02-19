package org.totschnig.myexpenses.db2

import android.content.ContentValues
import org.totschnig.myexpenses.dialog.SelectCategoryMoveTargetDialogFragment.Companion.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import java.time.LocalDate

fun Repository.savePrice(
    base: String,
    commodity: String,
    date: LocalDate,
    source: String,
    value: Double,
) {
    contentResolver.insert(
        TransactionProvider.PRICES_URI,
        ContentValues().apply {
            put(KEY_CURRENCY, base)
            put(KEY_COMMODITY, commodity)
            put(KEY_DATE, date.toString())
            put(KEY_SOURCE, source)
            put(KEY_VALUE, value)
        })
}