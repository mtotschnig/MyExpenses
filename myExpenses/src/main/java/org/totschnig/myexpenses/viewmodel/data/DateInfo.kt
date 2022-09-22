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
import org.totschnig.myexpenses.provider.getIntIfExists

data class DateInfo(
    val thisDay: Int, val thisWeek: Int, val thisMonth: Int, val thisYear: Int,
    val thisYearOfWeekStart: Int, val thisYearOfMonthStart: Int,
    val weekStart: Int, val weekEnd: Int
) {
    companion object {
        @JvmStatic
        fun fromCursor(cursor: Cursor) = with(cursor) {
            DateInfo(
                getIntIfExists(KEY_THIS_DAY),
                getIntIfExists(KEY_THIS_WEEK),
                getIntIfExists(KEY_THIS_MONTH),
                getIntIfExists(KEY_THIS_YEAR),
                getIntIfExists(KEY_THIS_YEAR_OF_WEEK_START),
                getIntIfExists(KEY_THIS_YEAR_OF_MONTH_START),
                getIntIfExists(KEY_WEEK_START),
                getIntIfExists(KEY_WEEK_END)
            )
        }
    }
}