package org.totschnig.myexpenses.testutils

import android.content.ContentValues
import org.totschnig.myexpenses.provider.DatabaseConstants

class CurrencyInfo(val label: String, val code: String) {
    val contentValues = ContentValues().apply {
        put(DatabaseConstants.KEY_LABEL, label)
        put(DatabaseConstants.KEY_CODE, code)
    }
}
