package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_COUNT
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID
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
            id = cursor.getLong(KEY_ROWID),
            label = cursor.getString(KEY_LABEL),
            color = cursor.getIntOrNull(KEY_COLOR),
            count = cursor.getIntIfExists(KEY_COUNT) ?: 0
        )
    }
}