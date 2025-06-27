package org.totschnig.myexpenses.viewmodel.data

import android.content.ContentResolver
import android.database.Cursor
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_MONTH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_WEEK
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_MONTH_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_START
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getIntIfExistsOr0
import org.totschnig.myexpenses.provider.getLocalDateIfExists
import org.totschnig.myexpenses.util.GroupingInfo
import java.time.LocalDate

@Immutable
data class DateInfo(
    val day: Int, val week: Int, val month: Int, val year: Int,
    val yearOfWeekStart: Int, val yearOfMonthStart: Int
) {
    companion object {
        val EMPTY = DateInfo(0, 0, 0, 0, 0, 0)

        fun load(contentResolver: ContentResolver) =
            contentResolver.query(
                TransactionProvider.DUAL_URI,
                arrayOf(
                    "${DatabaseConstants.getThisYearOfWeekStart()} AS $KEY_THIS_YEAR_OF_WEEK_START",
                    "${DatabaseConstants.getThisYearOfMonthStart()} AS $KEY_THIS_YEAR_OF_MONTH_START",
                    "${DatabaseConstants.THIS_YEAR} AS $KEY_THIS_YEAR",
                    "${DatabaseConstants.getThisMonth()} AS $KEY_THIS_MONTH",
                    "${DatabaseConstants.getThisWeek()} AS $KEY_THIS_WEEK",
                    "${DatabaseConstants.THIS_DAY} AS $KEY_THIS_DAY"
                ),
                null, null, null, null
            )!!.use {
                it.moveToFirst()
                fromCursor(it)
            }

        fun fromCursor(cursor: Cursor) = with(cursor) {
            DateInfo(
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

@Immutable
data class DateInfoExtra(
    val maxValue: Int, val weekStart: LocalDate?,
) {
    companion object {

        suspend fun load(contentResolver: ContentResolver, groupingInfo: GroupingInfo) =
            when (groupingInfo.grouping) {
                Grouping.DAY, Grouping.WEEK -> {
                    val maxYearToLookUp =
                        if (groupingInfo.second <= groupingInfo.grouping.minValue) groupingInfo.year - 1 else groupingInfo.year
                    val maxValueExpression = if (groupingInfo.grouping == Grouping.DAY)
                        "strftime('%j','$maxYearToLookUp-12-31')" else DbUtils.maximumWeekExpression(
                        maxYearToLookUp
                    )
                    val projectionList = buildList {
                        this.add("$maxValueExpression AS $KEY_MAX_VALUE")
                        if (groupingInfo.grouping == Grouping.WEEK) {
                            //we want to find out the week range when we are given a week number
                            //we find out the first day in the year, which is the beginning of week "0" and then
                            //add (weekNumber)*7 days to get at the beginning of the week
                            this.add(
                                DbUtils.weekStartFromGroupSqlExpression(
                                    groupingInfo.year,
                                    groupingInfo.second
                                )
                            )
                        }
                    }
                    withContext(Dispatchers.IO) {
                        contentResolver.query(
                            TransactionProvider.DUAL_URI,
                            projectionList.toTypedArray(),
                            null,
                            null,
                            null
                        )!!.use { cursor ->
                            cursor.moveToFirst()
                            fromCursor(cursor)
                        }
                    }
                }
                Grouping.MONTH -> DateInfoExtra(11, null)
                else -> DateInfoExtra(0, null)
            }

        fun fromCursor(cursor: Cursor) = with(cursor) {
            DateInfoExtra(
                getIntIfExistsOr0(KEY_MAX_VALUE),
                getLocalDateIfExists(KEY_WEEK_START)
            )
        }
    }
}