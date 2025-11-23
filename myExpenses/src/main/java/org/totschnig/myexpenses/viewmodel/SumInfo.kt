package org.totschnig.myexpenses.viewmodel

import android.database.Cursor
import org.totschnig.myexpenses.provider.KEY_COUNT
import org.totschnig.myexpenses.provider.KEY_HAS_TRANSFERS
import org.totschnig.myexpenses.provider.KEY_MAPPED_CATEGORIES
import org.totschnig.myexpenses.provider.KEY_MAPPED_METHODS
import org.totschnig.myexpenses.provider.KEY_MAPPED_PAYEES
import org.totschnig.myexpenses.provider.KEY_MAPPED_TAGS
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
                cursor.getBoolean(KEY_COUNT),
                cursor.getBoolean(KEY_MAPPED_CATEGORIES),
                cursor.getBoolean(KEY_MAPPED_PAYEES),
                cursor.getBoolean(KEY_MAPPED_METHODS),
                cursor.getBoolean(KEY_HAS_TRANSFERS),
                cursor.getBoolean(KEY_MAPPED_TAGS)
            )
        val EMPTY = SumInfo(
            hasItems = false,
            mappedCategories = false,
            mappedPayees = false,
            mappedMethods = false,
            hasTransfers = false,
            hasTags = false
        )
    }
}