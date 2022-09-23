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
import org.totschnig.myexpenses.provider.getIntIfExistsOr0

data class DateInfo2(
    val day: Int, val week: Int, val month: Int, val year: Int,
    val yearOfWeekStart: Int, val yearOfMonthStart: Int
) {
    companion object {
        val EMPTY = DateInfo2(0, 0, 0 ,0, 0, 0)
        fun fromCursor(cursor: Cursor) = with(cursor) {
            DateInfo2(
                getIntIfExistsOr0(KEY_THIS_DAY),
                getIntIfExistsOr0(KEY_THIS_WEEK),
                getIntIfExistsOr0(KEY_THIS_MONTH),
                getIntIfExistsOr0(KEY_THIS_YEAR),
                getIntIfExistsOr0(KEY_THIS_YEAR_OF_WEEK_START),
                getIntIfExistsOr0(KEY_THIS_YEAR_OF_MONTH_START)
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
                getIntIfExistsOr0(KEY_MAX_VALUE),
                getIntIfExistsOr0(KEY_WEEK_START),
                getIntIfExistsOr0(KEY_WEEK_END)
            )
        }
    }
}