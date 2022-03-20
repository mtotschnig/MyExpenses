package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import org.totschnig.myexpenses.provider.DatabaseConstants

internal data class CategoryInfo(
    val label: String,
    val parentId: Long?
) {

    val contentValues: ContentValues
        get() = ContentValues().apply {
            put(DatabaseConstants.KEY_LABEL, label)
            if (parentId != null) put(DatabaseConstants.KEY_PARENTID, parentId)
        }
}