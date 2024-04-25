package org.totschnig.myexpenses.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Grouping.DAY
import org.totschnig.myexpenses.model.Grouping.MONTH
import org.totschnig.myexpenses.model.Grouping.NONE
import org.totschnig.myexpenses.model.Grouping.WEEK
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.DateInfoExtra

@Parcelize
data class GroupingInfo(
    val grouping: Grouping = NONE,
    val year: Int = -1,
    val second: Int = -1
) : Parcelable

object GroupingNavigator {
    fun current(grouping: Grouping, dateInfo: DateInfo) = with(dateInfo) {
        GroupingInfo(
            grouping = grouping,
            year = when (grouping) {
                WEEK -> yearOfWeekStart
                MONTH -> yearOfMonthStart
                else -> year
            },
            second = when (grouping) {
                DAY -> day
                WEEK -> week
                MONTH -> month
                else -> 0
            }
        )
    }

    fun nextYear(groupingInfo: GroupingInfo) = groupingInfo.copy(year = groupingInfo.year + 1)

    fun next(groupingInfo: GroupingInfo, dateInfo: DateInfoExtra): GroupingInfo {
        val nextSecond = groupingInfo.second + 1
        val overflow = nextSecond > dateInfo.maxValue
        return groupingInfo.copy(
            year = if (overflow) groupingInfo.year + 1 else groupingInfo.year,
            second = if (overflow) groupingInfo.grouping.minValue else nextSecond
        )
    }

    fun previousYear(groupingInfo: GroupingInfo) = groupingInfo.copy(year = groupingInfo.year - 1)

    fun previous(groupingInfo: GroupingInfo, dateInfo: DateInfoExtra): GroupingInfo {
        val nextSecond = groupingInfo.second - 1
        val underflow = nextSecond < groupingInfo.grouping.minValue
        return groupingInfo.copy(
            year = if (underflow) groupingInfo.year - 1 else groupingInfo.year,
            second = if (underflow) dateInfo.maxValue else nextSecond
        )
    }
}