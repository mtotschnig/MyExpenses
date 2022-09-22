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

data class DateInfo2(
    val day: Int, val week: Int, val month: Int, val year: Int,
    val yearOfWeekStart: Int, val yearOfMonthStart: Int
) {
    companion object {
        val EMPTY = DateInfo2(0, 0, 0 ,0, 0, 0)
        fun fromCursor(cursor: Cursor) = with(cursor) {
            DateInfo2(
                getIntIfExists(KEY_THIS_DAY),
                getIntIfExists(KEY_THIS_WEEK),
                getIntIfExists(KEY_THIS_MONTH),
                getIntIfExists(KEY_THIS_YEAR),
                getIntIfExists(KEY_THIS_YEAR_OF_WEEK_START),
                getIntIfExists(KEY_THIS_YEAR_OF_MONTH_START)
            )
        }
    }
}

data class DateInfo3(
    val maxValue: Int, val weekStart: Int, val weekEnd: Int
) {
    companion object {
        fun fromCursor(cursor: Cursor) = with(cursor) {
            DateInfo3(
                getIntIfExists(KEY_MAX_VALUE),
                getIntIfExists(KEY_WEEK_START),
                getIntIfExists(KEY_WEEK_END)
            )
        }
    }
}