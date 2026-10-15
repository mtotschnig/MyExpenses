package org.totschnig.myexpenses.provider

import android.content.ContentValues
import org.totschnig.myexpenses.util.Utils

class PayeeInfo @JvmOverloads constructor(val name: String?, val parentId: Long? = null) {
    val contentValues = ContentValues(2).apply {
        put(KEY_PAYEE_NAME, name)
        put(
            KEY_PAYEE_NAME_NORMALIZED,
            Utils.normalize(name)
        )
        put(KEY_PARENTID, parentId)
    }
}
