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

data class DateInfo(
    val thisDay: Int, val thisWeek: Int, val thisMonth: Int, val thisYear: Int,
    val thisYearOfWeekStart: Int, val thisYearOfMonthStart: Int,
    val maxValue: Int, val weekStart: Int, val weekEnd: Int
) {
    companion object {
        @JvmStatic
        fun fromCursor(cursor: Cursor) = with(cursor) {
            DateInfo(
                safeGet(KEY_THIS_DAY),
                safeGet(KEY_THIS_WEEK),
                safeGet(KEY_THIS_MONTH),
                safeGet(KEY_THIS_YEAR),
                safeGet(KEY_THIS_YEAR_OF_WEEK_START),
                safeGet(KEY_THIS_YEAR_OF_MONTH_START),
                safeGet(KEY_MAX_VALUE),
                safeGet(KEY_WEEK_START),
                safeGet(KEY_WEEK_END)
            )
        }

        private fun Cursor.safeGet(key: String) =
            getColumnIndex(key).takeIf { it != -1 }?.let { getInt(it) }
                ?: 0
    }
}