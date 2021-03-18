package org.totschnig.myexpenses.model

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.text.format.Time
import com.android.calendar.CalendarContractCompat
import com.android.calendar.EventRecurrenceFormatter
import com.android.calendarcommon2.EventRecurrence
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.temporal.ChronoField
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.service.DailyScheduler
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.localDateTime2EpochMillis
import timber.log.Timber
import java.io.Serializable
import java.text.DateFormat
import java.util.*

/**
 * @author Michael Totschnig
 * holds information about an event in the calendar
 */
class Plan private constructor(id: Long, var dtStart: Long, var rRule: String?, var title: String, var description: String) : Model(), Serializable {

    private constructor(id: Long = 0L, localDate: LocalDate, rRule: String?, title: String, description: String) : this(id, localDateTime2EpochMillis(localDate.atTime(LocalTime.of(12, 0))), rRule, title, description)
    constructor(localDate: LocalDate, rRule: String?, title: String, description: String) : this(0L, localDate, rRule, title, description)
    constructor(id: Long, localDate: LocalDate, recurrence: Recurrence, title: String, description: String) :
            this(id, if (recurrence == Recurrence.LAST_DAY_OF_MONTH) localDate.withDayOfMonth(localDate.lengthOfMonth()) else localDate, recurrence.toRule(localDate), title, description)
    constructor(localDate: LocalDate, recurrence: Recurrence, title: String, description: String) : this(0L, localDate, recurrence, title, description)

    enum class Recurrence {
        NONE, ONETIME, DAILY, WEEKLY, MONTHLY, LAST_DAY_OF_MONTH, YEARLY, CUSTOM;

        fun toRule(localDate: LocalDate): String? {
            val weekStart = calendarDay2String(Utils.getFirstDayOfWeek(Locale.getDefault()))
            return when (this) {
                DAILY -> "FREQ=DAILY;INTERVAL=1;WKST=$weekStart"
                WEEKLY -> "FREQ=WEEKLY;INTERVAL=1;WKST=$weekStart;BYDAY=${calendarDay2String(localDate[ChronoField.DAY_OF_WEEK])}"
                MONTHLY -> {
                    "FREQ=MONTHLY;INTERVAL=1;WKST=$weekStart"
                }
                LAST_DAY_OF_MONTH -> "FREQ=MONTHLY;INTERVAL=1;BYDAY=SU,MO,TU,WE,TH,FR,SA;BYSETPOS=-1;WKST=$weekStart"
                YEARLY -> "FREQ=YEARLY;INTERVAL=1;WKST=$weekStart"
                else -> null
            }
        }

        private fun calendarDay2String(calendarDay: Int): String {
            return EventRecurrence.day2String(EventRecurrence.dayOfWeek2Day(DayOfWeek.of(calendarDay)))
        }
    }

    /**
     * insert a new planing event into the calendar
     *
     * @return the id of the created object
     */
    override fun save(): Uri {
        val uri: Uri
        val values = ContentValues()
        values.put(CalendarContractCompat.Events.TITLE, title)
        values.put(CalendarContractCompat.Events.DESCRIPTION, description)
        if (id == 0L) {
            val isOneTimeEvent = TextUtils.isEmpty(rRule)
            if (!isOneTimeEvent) {
                values.put(CalendarContractCompat.Events.RRULE, rRule)
            }
            values.put(CalendarContractCompat.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            var calendarId: String? = MyApplication.getInstance().checkPlanner()
                    ?: throw CalendarIntegrationNotAvailableException()
            if (MyApplication.INVALID_CALENDAR_ID == calendarId) {
                calendarId = MyApplication.getInstance().createPlanner(true)
                if (calendarId == MyApplication.INVALID_CALENDAR_ID) {
                    throw CalendarIntegrationNotAvailableException()
                }
            }
            values.put(CalendarContractCompat.Events.CALENDAR_ID, calendarId!!.toLong())
            values.put(CalendarContractCompat.Events.DTSTART, dtStart)
            if (isOneTimeEvent) {
                values.put(CalendarContractCompat.Events.DTEND, dtStart)
            } else {
                values.put(CalendarContractCompat.Events.DURATION, "P0S")
            }
            uri = cr().insert(CalendarContractCompat.Events.CONTENT_URI, values)!!
            id = ContentUris.parseId(uri)
            DailyScheduler.updatePlannerAlarms(MyApplication.getInstance(), true, true)
        } else {
            uri = ContentUris.withAppendedId(CalendarContractCompat.Events.CONTENT_URI, id)
            cr().update(uri, values, null, null)
        }
        return uri
    }

    fun updateCustomAppUri(customAppUri: String?) {
        check(id != 0L) { "Can not set custom app uri on unsaved plan" }
        updateCustomAppUri(id, customAppUri)
    }

    class CalendarIntegrationNotAvailableException : IllegalStateException()
    companion object {
        @JvmStatic
        fun getInstanceFromDb(planId: Long): Plan? {
            var plan: Plan? = null
            if (PermissionGroup.CALENDAR.hasPermission(MyApplication.getInstance())) {
                val c = cr().query(
                        ContentUris.withAppendedId(CalendarContractCompat.Events.CONTENT_URI, planId), arrayOf(
                        CalendarContractCompat.Events._ID,
                        CalendarContractCompat.Events.DTSTART,
                        CalendarContractCompat.Events.RRULE,
                        CalendarContractCompat.Events.TITLE),
                        null,
                        null,
                        null)
                if (c != null) {
                    if (c.moveToFirst()) {
                        val eventId = c.getLong(c.getColumnIndexOrThrow(CalendarContractCompat.Events._ID))
                        val dtStart = c.getLong(c.getColumnIndexOrThrow(CalendarContractCompat.Events.DTSTART))
                        val rRule = c.getString(c.getColumnIndexOrThrow(CalendarContractCompat.Events.RRULE))
                        val title = c.getString(c.getColumnIndexOrThrow(CalendarContractCompat.Events.TITLE))
                        plan = Plan(
                                eventId,
                                dtStart,
                                rRule,
                                title,
                                "" // we do not need the description stored in the event
                        )
                    }
                    c.close()
                }
            }
            return plan
        }

        @JvmStatic
        fun delete(id: Long) {
            val calendarId = PrefKey.PLANNER_CALENDAR_ID.getString("-1")
            val eventUri = CalendarContractCompat.Events.CONTENT_URI.buildUpon().appendPath(id.toString()).build()
            val eventCursor = cr().query(
                    eventUri, arrayOf("1 as ignore"),
                    CalendarContractCompat.Events.CALENDAR_ID + " = ?", arrayOf(calendarId),
                    null)
            if (eventCursor != null && eventCursor.count > 0) {
                cr().delete(
                        eventUri,
                        null,
                        null)
            } else {
                Timber.w("Attempt to delete event %d, which does not exist in calendar %s, has been blocked",
                        id, calendarId)
            }
            eventCursor!!.close()
        }

        @JvmStatic
        fun prettyTimeInfo(ctx: Context, rRule: String?, start: Long?): String {
            return if (!TextUtils.isEmpty(rRule)) {
                val eventRecurrence = EventRecurrence()
                try {
                    eventRecurrence.parse(rRule)
                } catch (e: EventRecurrence.InvalidFormatException) {
                    CrashHandler.report(e, "rRule", rRule)
                    return e.message ?: "ERROR"
                }
                val date = Time()
                date.set(start!!)
                eventRecurrence.setStartDate(date)
                EventRecurrenceFormatter.getRepeatString(ctx, ctx.resources, eventRecurrence, true)
            } else {
                DateFormat
                        .getDateInstance(DateFormat.FULL)
                        .format(Date(start!!))
            }
        }

        @JvmStatic
        fun updateCustomAppUri(id: Long?, customAppUri: String?) {
            if (Build.VERSION.SDK_INT >= 16) {
                try {
                    val values = ContentValues()
                    values.put(CalendarContractCompat.Events.CUSTOM_APP_URI, customAppUri)
                    values.put(CalendarContractCompat.Events.CUSTOM_APP_PACKAGE, MyApplication.getInstance().packageName)
                    cr().update(ContentUris.withAppendedId(CalendarContractCompat.Events.CONTENT_URI, id!!), values, null, null)
                } catch (e: SQLiteException) {
                    // we have seen a buggy calendar provider implementation on Symphony phone
                }
            }
        }

        fun updateDescription(id: Long?, description: String?) {
            val values = ContentValues()
            values.put(CalendarContractCompat.Events.DESCRIPTION, description)
            cr().update(ContentUris.withAppendedId(CalendarContractCompat.Events.CONTENT_URI, id!!), values, null, null)
        }
    }

    init {
        this.id  = id
    }
}