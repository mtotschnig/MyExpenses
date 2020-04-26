package org.totschnig.myexpenses.dialog.select

import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants

internal data class DataHolder(val id: Long, val label: String) {
    override fun toString(): String {
        return label
    }

    companion object {
        @JvmStatic
        fun fromCursor(cursor: Cursor, labelColumn: String?) = DataHolder(
                cursor.getLong(cursor.getColumnIndex(DatabaseConstants.KEY_ROWID)),
                cursor.getString(cursor.getColumnIndex(labelColumn)))
    }

}