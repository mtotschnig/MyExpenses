package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE

data class Template(val id: Long, val title: String) {
    companion object {
        fun fromCursor(cursor: Cursor) = Template(
                cursor.getLong(cursor.getColumnIndex(KEY_ROWID)),
                cursor.getString(cursor.getColumnIndex(KEY_TITLE)))
    }
}