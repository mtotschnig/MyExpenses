package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.getIntIfExists
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.util.ui.Itag

@Parcelize
data class Tag(
    val id: Long,
    override val label: String,
    @ColorInt override val color: Int? = null,
    val count: Int = 0
) :
    Parcelable, Itag {
    init {
        check(id > 0)
    }

    override fun toString() = label

    companion object {
        fun fromCursor(cursor: Cursor) = Tag(
            id = cursor.getLong(DatabaseConstants.KEY_ROWID),
            label = cursor.getString(DatabaseConstants.KEY_LABEL),
            color = cursor.getIntOrNull(DatabaseConstants.KEY_COLOR),
            count = cursor.getIntIfExists(DatabaseConstants.KEY_COUNT) ?: 0
        )
    }
}