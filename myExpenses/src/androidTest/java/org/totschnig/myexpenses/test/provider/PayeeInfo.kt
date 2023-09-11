package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED
import org.totschnig.myexpenses.util.Utils


data class PayeeInfo @JvmOverloads constructor(val name: String, val parentId: Long? = null) {
    val contentValues: ContentValues
        get() = ContentValues(2).apply {
            put(KEY_PAYEE_NAME, name)
            put(KEY_PAYEE_NAME_NORMALIZED, Utils.normalize(name))
            put(KEY_PARENTID, parentId)
        }
}