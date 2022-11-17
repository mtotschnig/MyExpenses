package org.totschnig.myexpenses.dialog.select

import android.database.Cursor
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.totschnig.myexpenses.provider.DatabaseConstants

@Parcelize
data class DataHolder(val id: Long, val label: String): Parcelable {
    companion object {
        @JvmStatic
        fun fromCursor(cursor: Cursor, labelColumn: String?) = DataHolder(
                cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_ROWID)),
                cursor.getString(cursor.getColumnIndexOrThrow(labelColumn)))
    }

}