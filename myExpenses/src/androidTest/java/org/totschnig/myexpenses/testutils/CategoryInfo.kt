package org.totschnig.myexpenses.testutils

import android.content.ContentValues
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_TYPE

internal data class CategoryInfo(
    val label: String,
    val id: Long? = null,
    val parentId: Long? = null,
    val type: Int = 0
) {

    val contentValues: ContentValues
        get() = ContentValues().apply {
            put(KEY_LABEL, label)
            if (parentId != null) {
                put(KEY_PARENTID, parentId)
            }
            put(KEY_TYPE, type)
        }
}