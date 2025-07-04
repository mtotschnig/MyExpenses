package org.totschnig.myexpenses.model

import android.content.Context
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.ui.asDateTimeFormatter
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.min

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

    fun calculateGroupId(transaction: Transaction2) =
        calculateGroupId(transaction.year, getSecond(transaction))

    fun calculateGroupId(year: Int, second: Int) = if (this == NONE) 1 else groupId(year, second)

    fun getSecond(transaction: Transaction2) = when (this) {
        DAY -> transaction.day
        WEEK -> transaction.week
        MONTH -> transaction.month
        else -> 0
    }

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
        dateInfo: DateInfo,
        weekStart: LocalDate?,
        weekRangeOnly: Boolean = false,
        relativeDay: Boolean = true
    ): String {
        val locale = ctx.resources.configuration.locale
        return try {
            when (this) {
                NONE -> ctx.getString(R.string.menu_aggregates)
                DAY -> {
                    val today = LocalDate.ofYearDay(dateInfo.year, dateInfo.day)
                    val day = LocalDate.ofYearDay(groupYear, groupSecond)
                    val closeReference = if (relativeDay) when (ChronoUnit.DAYS.between(day, today)) {
                        1L -> R.string.yesterday
                        0L -> R.string.today
                        -1L -> R.string.tomorrow
                        else -> null
                    }?.let { ctx.getString(it) } else null
                    val formatStyle =
                        if (closeReference == null) FormatStyle.FULL else FormatStyle.LONG
                    val dateFormatted = DateTimeFormatter.ofLocalizedDate(formatStyle)
                        .withLocale(locale)
                        .format(day)
                    if (closeReference == null) dateFormatted else "$closeReference ($dateFormatted)"
                }

                WEEK -> {
                    val formatter =
                        (Utils.localizedYearLessDateFormat(ctx) as? SimpleDateFormat)?.asDateTimeFormatter
                    val weekEnd = getWeekEndFromStart(weekStart!!)
                    fun format(localDate: LocalDate) =
                        formatter?.format(localDate) ?: localDate.toString()

                    val weekRange = "${format(weekStart)} - ${format(weekEnd)}"

                    if (weekRangeOnly) return weekRange

                    val thisWeek = dateInfo.week
                    val thisYearOfWeekStart = dateInfo.yearOfWeekStart

                    if (groupYear == thisYearOfWeekStart) {
                        if (groupSecond == thisWeek) {
                            return "${ctx.getString(R.string.grouping_this_week)} ($weekRange)"
                        } else if (groupSecond == thisWeek - 1) {
                            return "${ctx.getString(R.string.grouping_last_week)} ($weekRange)"
                        }
                    }

                    val yearPrefix = if (groupYear == thisYearOfWeekStart) "" else "$groupYear, "
                    "$yearPrefix${ctx.getString(R.string.grouping_week)} $groupSecond ($weekRange)"
                }

                MONTH -> {
                    getDisplayTitleForMonth(
                        groupYear,
                        groupSecond,
                        FormatStyle.LONG,
                        locale,
                        ctx.injector.prefHandler().monthStart
                    )
                }

                YEAR -> groupYear.toString()
            }
        } catch (e: Exception) {
            CrashHandler.report(e)
            "Error while generating title: ${e.safeMessage}"
        }
    }

    val queryArgumentForThisSecond: String?
        get() = when (this) {
            DAY -> DatabaseConstants.THIS_DAY
            WEEK -> DatabaseConstants.getThisWeek()
            MONTH -> DatabaseConstants.getThisMonth()
            else -> null
        }

    companion object {

        fun groupId(year: Int, second: Int) = year * 1000 + second

        fun getWeekEndFromStart(weekStart: LocalDate) = weekStart.plusDays(6)

        fun getMonthRange(
            groupYear: Int,
            groupSecond: Int,
            monthStarts: Int
        ): Pair<LocalDate, LocalDate> {
            val startMonth = groupSecond + 1
            val yearMonth = YearMonth.of(groupYear, startMonth)
            return if (monthStarts == 1) {
                yearMonth.atDay(1) to yearMonth.atEndOfMonth()
            } else {
                val nextMonth = yearMonth.plusMonths(1)
                (if (monthStarts > yearMonth.lengthOfMonth())
                    nextMonth.atDay(1) else yearMonth.atDay(monthStarts)) to
                        nextMonth.atDay(min(monthStarts - 1, nextMonth.lengthOfMonth()))
            }
        }

        fun getDisplayTitleForMonth(
            groupYear: Int,
            groupSecond: Int,
            style: FormatStyle,
            userPreferredLocale: Locale,
            monthStarts: Int
        ): String {
            return if (monthStarts == 1) {
                DateTimeFormatter.ofPattern("MMMM y")
                    .format(YearMonth.of(groupYear, groupSecond + 1))
            } else {
                val dateFormat =
                    DateTimeFormatter.ofLocalizedDate(style).withLocale(userPreferredLocale)
                val range = getMonthRange(groupYear, groupSecond, monthStarts)
                "(${dateFormat.format(range.first)} - ${dateFormat.format(range.second)})"
            }
        }

        @JvmField
        val JOIN: String = TextUtils.joinEnum(Grouping::class.java)
    }
}