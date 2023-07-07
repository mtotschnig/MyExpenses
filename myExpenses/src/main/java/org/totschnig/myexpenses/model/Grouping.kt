package org.totschnig.myexpenses.model

import android.content.Context
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * grouping of transactions
 */
enum class Grouping {
    NONE,
    DAY,
    WEEK,
    MONTH {
        override val minValue = 0
    },
    YEAR;

    open val minValue = 1

    fun calculateGroupId(year: Int, second: Int) = if (this == NONE) 1 else groupId(year, second)

    /**
     * @param groupYear           the year of the group to display
     * @param groupSecond         the number of the group in the second dimension (day, week or month)
     * @param dateInfo            information about the current date
     * @return a human readable String representing the group as header or activity title
     */
    fun getDisplayTitle(
        ctx: Context,
        groupYear: Int,
        groupSecond: Int,
        dateInfo: DateInfo
    ): String {
        val locale = ctx.resources.configuration.locale
        return try {
            when (this) {
                NONE -> ctx.getString(R.string.menu_aggregates)
                DAY -> {
                    val today = LocalDate.ofYearDay(dateInfo.thisYear, dateInfo.thisDay)
                    val day = LocalDate.ofYearDay(groupYear, groupSecond)
                    val title = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(day)
                    when(ChronoUnit.DAYS.between(day, today)) {
                        1L -> R.string.yesterday
                        0L -> R.string.today
                        -1L -> R.string.tomorrow
                        else -> null
                    }?.let { ctx.getString(it) + " (" + title + ")" } ?: title
                }

                WEEK -> {
                    val thisWeek = dateInfo.thisWeek
                    val thisYearOfWeekStart = dateInfo.thisYearOfWeekStart
                    val dateFormat = Utils.localizedYearLessDateFormat(ctx)
                    val weekRange = (" (" + Utils.convDateTime(
                        dateInfo.weekStart.toLong(),
                        dateFormat
                    )
                            + " - " + Utils.convDateTime(
                        dateInfo.weekEnd.toLong(),
                        dateFormat
                    ) + " )")
                    val yearPrefix = if (groupYear == thisYearOfWeekStart) {
                        if (groupSecond == thisWeek) return ctx.getString(R.string.grouping_this_week) + weekRange else if (groupSecond == thisWeek - 1) return ctx.getString(
                            R.string.grouping_last_week
                        ) + weekRange
                        ""
                    } else "$groupYear, "
                    yearPrefix + ctx.getString(R.string.grouping_week) + " " + groupSecond + weekRange
                }

                MONTH -> {
                    getDisplayTitleForMonth(
                        groupYear,
                        groupSecond,
                        DateFormat.LONG,
                        locale
                    )
                }

                YEAR -> groupYear.toString()
            }
        } catch (e: Exception) {
            "Error while generating title: ${e.safeMessage}"
        }
    }

    companion object {

        fun groupId(year: Int, second: Int) = year * 1000 + second

        fun getDisplayTitleForMonth(
            groupYear: Int,
            groupSecond: Int,
            style: Int,
            userPreferredLocale: Locale
        ): String {
            val monthStarts = PrefKey.GROUP_MONTH_STARTS.getString("1")!!.toInt()
            var cal = Calendar.getInstance()
            return if (monthStarts == 1) {
                cal[groupYear, groupSecond] = 1
                SimpleDateFormat("MMMM y", userPreferredLocale).format(cal.time)
            } else {
                val dateFormat = DateFormat.getDateInstance(style, userPreferredLocale)
                cal = Calendar.getInstance()
                cal[groupYear, groupSecond] = 1
                if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) < monthStarts) {
                    cal[groupYear, groupSecond + 1] = 1
                } else {
                    cal[Calendar.DATE] = monthStarts
                }
                val startDate = dateFormat.format(cal.time)
                var endYear = groupYear
                var endMonth = groupSecond + 1
                if (endMonth > Calendar.DECEMBER) {
                    endMonth = Calendar.JANUARY
                    endYear++
                }
                cal = Calendar.getInstance()
                cal[endYear, endMonth] = 1
                if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) < monthStarts - 1) {
                    cal[Calendar.DATE] = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                } else {
                    cal[Calendar.DATE] = monthStarts - 1
                }
                val endDate = dateFormat.format(cal.time)
                " ($startDate - $endDate )"
            }
        }

        @JvmField
        val JOIN: String = TextUtils.joinEnum(Grouping::class.java)
    }
}