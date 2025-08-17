package org.totschnig.myexpenses.model

import android.content.ContentValues
import android.database.Cursor
import androidx.core.content.contentValuesOf
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_ASSET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUPPORTS_RECONCILIATION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VISIBLE
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getIntIfExists
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString

const val PREDEFINED_NAME_FAVORITE = "_FAVORITE_"
const val PREDEFINED_NAME_HIDDEN = "_HIDDEN_"

data class AccountFlag(
    val id: Long = 0,
    val label: String,
    val sortKey: Int,
    val icon: String? = null,
    val isVisible: Boolean = true,
    val count: Int? = null
) {

    val asContentValues: ContentValues
        get() = contentValuesOf(
            KEY_LABEL to label,
            KEY_SORT_KEY to sortKey,
            KEY_ICON to icon,
            KEY_VISIBLE to isVisible
        )

    companion object {
        val initialFlags = listOf(
            AccountFlag(
                id = 1,
                label = PREDEFINED_NAME_FAVORITE,
                sortKey = 1,
                icon = "star",
                isVisible = true
            ),
            AccountFlag(
                id = 2,
                label = PREDEFINED_NAME_HIDDEN,
                sortKey = -1,
                icon = "box-archive",
                isVisible = false
            )
        )

        fun fromCursor(cursor: Cursor) = AccountFlag(
            id = cursor.getLong(KEY_ROWID),
            label = cursor.getString(KEY_LABEL),
            sortKey = cursor.getInt(KEY_SORT_KEY),
            icon = cursor.getString(KEY_ICON),
            isVisible = cursor.getBoolean(KEY_VISIBLE),
            count = cursor.getIntIfExists(KEY_COUNT)
        )
    }
}