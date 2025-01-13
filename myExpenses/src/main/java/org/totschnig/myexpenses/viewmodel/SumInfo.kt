package org.totschnig.myexpenses.viewmodel

import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.getBoolean

data class SumInfo(
    val hasItems: Boolean,
    val mappedCategories: Boolean,
    val mappedPayees: Boolean,
    val mappedMethods: Boolean,
    val hasTransfers: Boolean,
    val hasTags: Boolean
)  {
    companion object {
        fun fromCursor(cursor: Cursor) =
            SumInfo(
                cursor.getBoolean(DatabaseConstants.KEY_COUNT),
                cursor.getBoolean(DatabaseConstants.KEY_MAPPED_CATEGORIES),
                cursor.getBoolean(DatabaseConstants.KEY_MAPPED_PAYEES),
                cursor.getBoolean(DatabaseConstants.KEY_MAPPED_METHODS),
                cursor.getBoolean(DatabaseConstants.KEY_HAS_TRANSFERS),
                cursor.getBoolean(DatabaseConstants.KEY_MAPPED_TAGS)
            )
        val EMPTY = SumInfo(false, false, false, false, false, false)
    }
}