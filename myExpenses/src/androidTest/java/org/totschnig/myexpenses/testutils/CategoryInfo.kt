package org.totschnig.myexpenses.testutils

import android.content.ContentValues
import org.totschnig.myexpenses.provider.DatabaseConstants

internal data class CategoryInfo(
    val label: String,
    val id: Long? = null,
    val parentId: Long? = null,
    val type: Int = 0
) {

    val contentValues: ContentValues
        get() = ContentValues().apply {
            put(DatabaseConstants.KEY_LABEL, label)
            if (parentId != null) {
                put(DatabaseConstants.KEY_PARENTID, parentId)
            }
            put(DatabaseConstants.KEY_TYPE, type)
        }
}