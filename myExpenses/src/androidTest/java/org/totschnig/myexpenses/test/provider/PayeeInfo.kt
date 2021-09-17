package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.Utils

data class PayeeInfo(val name: String) {
    val contentValues: ContentValues
        get() {
            val contentValues = ContentValues(2)
            contentValues.put(DatabaseConstants.KEY_PAYEE_NAME, name)
            contentValues.put(
                DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED, Utils.normalize(
                    name
                )
            )
            return contentValues
        }
}