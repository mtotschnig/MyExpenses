package org.totschnig.myexpenses.dialog.select

import android.database.Cursor
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString

@Parcelize
data class DataHolder(val id: Long, val label: String): Parcelable {
    companion object {
        @JvmStatic
        fun fromCursor(cursor: Cursor, labelColumn: String) = DataHolder(
                cursor.getLong(KEY_ROWID),
                cursor.getString(labelColumn)
        )
    }
}