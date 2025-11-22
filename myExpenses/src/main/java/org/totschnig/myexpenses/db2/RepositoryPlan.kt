package org.totschnig.myexpenses.db2

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import androidx.annotation.RequiresPermission
import org.totschnig.myexpenses.db2.entities.Plan
import org.totschnig.myexpenses.db2.entities.Recurrence
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.templateUri
import org.totschnig.myexpenses.provider.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.useAndMapToOne
import org.totschnig.myexpenses.util.toEpochMillis
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

class CalendarIntegrationNotAvailableException : IllegalStateException()

@RequiresPermission(allOf = [Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR])
fun Repository.createPlan(
    title: String,
    description: String,
    date: LocalDate,
    recurrence: Recurrence,
): Plan {
    val calendarId =
        (plannerUtils.checkPlanner() ?: throw CalendarIntegrationNotAvailableException())
            .takeIf { it != INVALID_CALENDAR_ID }
            ?: plannerUtils.createPlanner(true)
                .takeIf { it != INVALID_CALENDAR_ID }
            ?: throw CalendarIntegrationNotAvailableException()
    val dtStart = date.atTime(LocalTime.of(12, 0)).toEpochMillis()

    val rule = recurrence.toRule(date)
    val values = ContentValues().apply {
        put(Events.TITLE, title)
        put(Events.DESCRIPTION, description)
        put(Events.DTSTART, dtStart)
        if (recurrence == Recurrence.NONE) {
            put(Events.DTEND, dtStart)
        } else {
            put(Events.RRULE, rule)
            put(Events.DURATION, "P0S")
        }
        put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        put(Events.CALENDAR_ID, calendarId.toLong())
    }
    return Plan(
        id = ContentUris.parseId(contentResolver.insert(Events.CONTENT_URI, values)!!),
        dtStart = dtStart,
        rRule = rule,
        title = title,
        description = description
    )
}

fun Repository.updatePlan(
    planId: Long,
    title: String?,
    description: String,
) {
    val values = ContentValues().apply {
        title?.let { put(Events.TITLE, title) }
        put(Events.DESCRIPTION, description)
    }
    contentResolver.update(
        ContentUris.withAppendedId(Events.CONTENT_URI, planId),
        values,
        null,
        null
    )
}

@RequiresPermission(allOf = [Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR])
fun Repository.loadPlan(planId: Long): Plan? {
    return contentResolver.query(
        ContentUris.withAppendedId(Events.CONTENT_URI, planId),
        arrayOf(
            Events._ID,
            Events.DTSTART,
            Events.RRULE,
            Events.TITLE
        ),
        null,
        null,
        null
    )?.useAndMapToOne {
        Plan(
            it.getLong(Events._ID),
            it.getLong(Events.DTSTART),
            it.getString(Events.RRULE),
            it.getString(Events.TITLE),
            "" // we do not need the description stored in the event
        )
    }
}

@RequiresPermission(allOf = [Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR])
fun Repository.deletePlan(id: Long) {
    val calendarId = prefHandler.getString(PrefKey.PLANNER_CALENDAR_ID, "-1")
    val eventUri =
        Events.CONTENT_URI.buildUpon().appendPath(id.toString()).build()
    contentResolver.query(
        eventUri, arrayOf("1 as ignore"),
        Events.CALENDAR_ID + " = ?", arrayOf(calendarId),
        null
    )?.use {
        if (it.count > 0) {
            contentResolver.delete(
                eventUri,
                null,
                null
            )
        } else {
            Timber.w(
                "Attempt to delete event %d, which does not exist in calendar %s, has been blocked",
                id, calendarId
            )
        }
    }
}

fun Repository.updateCustomAppUri(planId: Long, templateId: Long) {
    updateCustomAppUri(context, planId, templateId)
}

fun updateCustomAppUri(
    context: Context,
    planId: Long,
    templateId: Long
) {
    try {
        context.contentResolver.update(
            ContentUris.withAppendedId(
                Events.CONTENT_URI,
                planId
            ), ContentValues().apply {
                put(Events.CUSTOM_APP_URI, templateUri(templateId).toString())
                put(Events.CUSTOM_APP_PACKAGE, context.packageName)
            }, null, null
        )
    } catch (_: SQLiteException) {
        // we have seen a buggy calendar provider implementation on Symphony phone
    }
}