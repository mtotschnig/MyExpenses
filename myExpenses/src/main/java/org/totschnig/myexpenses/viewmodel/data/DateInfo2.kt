package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_MONTH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_WEEK
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_MONTH_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_START

data class DateInfo2(
    val day: Int, val week: Int, val month: Int, val year: Int,
    val yearOfWeekStart: Int, val yearOfMonthStart: Int
) {
    companion object {
        fun fromCursor(cursor: Cursor) = with(cursor) {
            DateInfo2(
                safeGet(KEY_THIS_DAY),
                safeGet(KEY_THIS_WEEK),
                safeGet(KEY_THIS_MONTH),
                safeGet(KEY_THIS_YEAR),
                safeGet(KEY_THIS_YEAR_OF_WEEK_START),
                safeGet(KEY_THIS_YEAR_OF_MONTH_START)
            )
        }
    }
}

fun Cursor.safeGet(key: String) =
    getColumnIndex(key).takeIf { it != -1 }?.let { getInt(it) }
        ?: 0

data class DateInfo3(
    val maxValue: Int, val weekStart: Int, val weekEnd: Int
) {
    companion object {
        fun fromCursor(cursor: Cursor) = with(cursor) {
            DateInfo3(
                safeGet(KEY_MAX_VALUE),
                safeGet(KEY_WEEK_START),
                safeGet(KEY_WEEK_END)
            )
        }
    }
}