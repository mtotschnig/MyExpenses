package org.totschnig.myexpenses.db2.entities

import android.content.Context
import android.text.format.Time
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.calendar.EventRecurrence
import org.totschnig.myexpenses.calendar.EventRecurrenceFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.toEpochMillis
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoField
import java.util.Date
import java.util.Locale

data class Plan(
    val id: Long,
    val dtStart: Long,
    val rRule: String?,
    val title: String,
    val description: String,
) {
    constructor(localDate: LocalDate, rRule: String?, title: String, description: String) : this(
        0L,
        localDate.atTime(LocalTime.of(12, 0)).toEpochMillis(),
        rRule,
        title,
        description
    )

    constructor(
        localDate: LocalDate,
        recurrence: Recurrence,
        title: String,
        description: String,
    ) : this(
        if (recurrence == Recurrence.LAST_DAY_OF_MONTH) localDate.withDayOfMonth(localDate.lengthOfMonth()) else localDate,
        recurrence.toRule(localDate),
        title,
        description
    )
}

enum class Recurrence {
    NONE, ONETIME, DAILY, WEEKLY, MONTHLY, LAST_DAY_OF_MONTH, YEARLY, CUSTOM;

    fun toRule(localDate: LocalDate): String? {
        val weekStart = calendarDay2String(Utils.getFirstDayOfWeek(Locale.getDefault()))
        return when (this) {
            DAILY -> "FREQ=DAILY;INTERVAL=1;WKST=$weekStart"
            WEEKLY -> "FREQ=WEEKLY;INTERVAL=1;WKST=$weekStart;BYDAY=${
                calendarDay2String(
                    localDate[ChronoField.DAY_OF_WEEK]
                )
            }"

            MONTHLY -> {
                "FREQ=MONTHLY;INTERVAL=1;WKST=$weekStart"
            }

            LAST_DAY_OF_MONTH -> "FREQ=MONTHLY;INTERVAL=1;BYDAY=SU,MO,TU,WE,TH,FR,SA;BYSETPOS=-1;WKST=$weekStart"
            YEARLY -> "FREQ=YEARLY;INTERVAL=1;WKST=$weekStart"
            else -> null
        }
    }

    private fun calendarDay2String(calendarDay: Int): String {
        return EventRecurrence.day2String(
            EventRecurrence.dayOfWeek2Day(DayOfWeek.of(calendarDay))
        )
    }

    fun label(context: Context) = when (this) {
        ONETIME -> context.getString(R.string.does_not_repeat)
        DAILY -> context.getString(R.string.daily_plain)
        WEEKLY -> context.getString(R.string.weekly_plain)
        MONTHLY -> context.getString(R.string.monthly_plain)
        YEARLY -> context.getString(R.string.yearly_plain)
        CUSTOM -> context.getString(R.string.pref_sort_order_custom)
        else -> "- - - -"
    }
}

fun prettyTimeInfo(ctx: Context, rRule: String?, start: Long): String {
    return  rRule?.takeIf { it.isNotEmpty() }?.let {
        val eventRecurrence =
            EventRecurrence()
        try {
            eventRecurrence.parse(rRule)
        } catch (e: EventRecurrence.InvalidFormatException) {
            CrashHandler.report(e, "rRule", rRule)
            return e.safeMessage
        }
        val date = Time()
        date.set(start)
        eventRecurrence.setStartDate(date)
        EventRecurrenceFormatter.getRepeatString(ctx, ctx.resources, eventRecurrence, true)
    } ?:  DateFormat
        .getDateInstance(DateFormat.FULL)
        .format(Date(start))
}