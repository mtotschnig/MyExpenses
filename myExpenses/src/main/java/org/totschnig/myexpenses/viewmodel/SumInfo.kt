package org.totschnig.myexpenses.viewmodel

import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getInt

sealed interface SumInfo

data class SumInfoLoaded(
    val hasItems: Boolean,
    val mappedCategories: Boolean,
    val mappedPayees: Boolean,
    val mappedMethods: Boolean,
    val hasTransfers: Boolean,
    val hasTags: Boolean
) : SumInfo {
    companion object {
        fun fromCursor(cursor: Cursor) =
            SumInfoLoaded(
                cursor.getBoolean(DatabaseConstants.KEY_COUNT),
                cursor.getBoolean(DatabaseConstants.KEY_MAPPED_CATEGORIES),
                cursor.getBoolean(DatabaseConstants.KEY_MAPPED_PAYEES),
                cursor.getBoolean(DatabaseConstants.KEY_MAPPED_METHODS),
                cursor.getBoolean(DatabaseConstants.KEY_HAS_TRANSFERS),
                cursor.getBoolean(DatabaseConstants.KEY_MAPPED_TAGS)
            )
    }
}

object SumInfoUnknown: SumInfo