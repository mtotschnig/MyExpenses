package org.totschnig.myexpenses.testutils

import android.content.ContentValues
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_LABEL

class CurrencyInfo(val label: String, val code: String) {
    val contentValues = ContentValues().apply {
        put(KEY_LABEL, label)
        put(KEY_CODE, code)
    }
}
