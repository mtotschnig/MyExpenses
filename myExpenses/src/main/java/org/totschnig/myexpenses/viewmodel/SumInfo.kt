package org.totschnig.myexpenses.viewmodel

import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.getInt

sealed interface SumInfo

data class SumInfoLoaded(
    val mappedCategories: Boolean,
    val mappedPayees: Boolean,
    val mappedMethods: Boolean,
    val hasTransfers: Boolean,
    val hasTags: Boolean
) : SumInfo {
    companion object {
        fun fromCursor(cursor: Cursor) =
            SumInfoLoaded(
                cursor.getInt(DatabaseConstants.KEY_MAPPED_CATEGORIES) > 0,
                cursor.getInt(DatabaseConstants.KEY_MAPPED_PAYEES) > 0,
                cursor.getInt(DatabaseConstants.KEY_MAPPED_METHODS) > 0,
                cursor.getInt(DatabaseConstants.KEY_HAS_TRANSFERS) > 0,
                cursor.getInt(DatabaseConstants.KEY_MAPPED_TAGS) > 0
            )
    }
}

object SumInfoUnknown: SumInfo