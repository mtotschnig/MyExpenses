package org.totschnig.myexpenses.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.core.content.contentValuesOf
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VISIBLE
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getIntIfExists
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull

const val PREDEFINED_NAME_DEFAULT = "_DEFAULT_"
const val PREDEFINED_NAME_FAVORITE = "_FAVORITE_"
const val PREDEFINED_NAME_INACTIVE = "_INACTIVE_"
const val DEFAULT_FLAG_ID = 0L

data class AccountFlag(
    val id: Long = 0,
    val label: String,
    val sortKey: Int = 0,
    val icon: String? = null,
    val isVisible: Boolean = true,
    val count: Int? = null
) {

    val asContentValues: ContentValues
        get() = contentValuesOf(
            KEY_FLAG_LABEL to label,
            KEY_FLAG_SORT_KEY to sortKey,
            KEY_FLAG_ICON to icon,
            KEY_VISIBLE to isVisible
        ).apply {
            if (label == "_DEFAULT_") {
                put(KEY_ROWID, DEFAULT_FLAG_ID)
            }
        }

    fun localizedLabel(context: Context) = when (label) {
        PREDEFINED_NAME_DEFAULT -> R.string.flag_unflagged
        PREDEFINED_NAME_FAVORITE -> R.string.flag_favorite
        PREDEFINED_NAME_INACTIVE -> R.string.flag_inactive
        else -> 0
    }.takeIf { it != 0 }?.let { context.getString(it) } ?: label

    companion object {
        val DEFAULT = AccountFlag(
            label = PREDEFINED_NAME_DEFAULT,
            sortKey = 0,
            isVisible = true
        )
        val initialFlags = listOf(
           DEFAULT,
            AccountFlag(
                label = PREDEFINED_NAME_FAVORITE,
                sortKey = 1,
                icon = "star",
                isVisible = true
            ),
            AccountFlag(
                label = PREDEFINED_NAME_INACTIVE,
                sortKey = -1,
                icon = "box-archive",
                isVisible = false
            )
        )

        fun fromCursor(cursor: Cursor) = AccountFlag(
            id = cursor.getLong(KEY_ROWID),
            label = cursor.getString(KEY_FLAG_LABEL),
            sortKey = cursor.getInt(KEY_FLAG_SORT_KEY),
            icon = cursor.getStringOrNull(KEY_FLAG_ICON),
            isVisible = cursor.getBoolean(KEY_VISIBLE),
            count = cursor.getIntIfExists(KEY_COUNT)
        )

        fun fromAccountCursor(cursor: Cursor) = AccountFlag(
            id = cursor.getLong(KEY_FLAG),
            label = cursor.getString(KEY_FLAG_LABEL),
            sortKey = cursor.getInt(KEY_FLAG_SORT_KEY),
            icon = cursor.getStringOrNull(KEY_FLAG_ICON),
            isVisible = cursor.getBoolean(KEY_VISIBLE)
        )

        fun isReservedName(name: String) = name.startsWith("_") && name.endsWith("_")
    }
}