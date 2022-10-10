package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_MONTH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_WEEK
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_MONTH_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_START
import org.totschnig.myexpenses.provider.getIntIfExistsOr0

data class DateInfo(
    val thisDay: Int, val thisWeek: Int, val thisMonth: Int, val thisYear: Int,
    val thisYearOfWeekStart: Int, val thisYearOfMonthStart: Int,
    val weekStart: Int, val weekEnd: Int
) {
    companion object {
        @JvmStatic
        fun fromCursor(cursor: Cursor) = with(cursor) {
            DateInfo(
                getIntIfExistsOr0(KEY_THIS_DAY),
                getIntIfExistsOr0(KEY_THIS_WEEK),
                getIntIfExistsOr0(KEY_THIS_MONTH),
                getIntIfExistsOr0(KEY_THIS_YEAR),
                getIntIfExistsOr0(KEY_THIS_YEAR_OF_WEEK_START),
                getIntIfExistsOr0(KEY_THIS_YEAR_OF_MONTH_START),
                getIntIfExistsOr0(KEY_WEEK_START),
                getIntIfExistsOr0(KEY_WEEK_END)
            )
        }
    }
}