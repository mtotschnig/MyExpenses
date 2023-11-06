package org.totschnig.myexpenses.provider

import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.res.ResourcesCompat
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.service.PlanExecutor
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

const val PLANNER_CALENDAR_NAME = "MyExpensesPlanner"
const val PLANNER_ACCOUNT_NAME = "Local Calendar"
const val INVALID_CALENDAR_ID = "-1"

@Singleton class PlannerUtils @Inject constructor(
    val context: MyApplication,
    val prefHandler: PrefHandler) {

    /**
     * we cache value of planner calendar id, so that we can handle changes in
     * value
     */
    private var plannerCalendarId: String

    init {
            plannerCalendarId = prefHandler.requireString(PrefKey.PLANNER_CALENDAR_ID, INVALID_CALENDAR_ID)
    }

    /**
     * check if we already have a calendar in Account [.PLANNER_ACCOUNT_NAME]
     * of type [CalendarContract.ACCOUNT_TYPE_LOCAL] with name
     * [.PLANNER_ACCOUNT_NAME] if yes use it, otherwise create it
     *
     * @return id if we have configured a useable calendar, or [INVALID_CALENDAR_ID]
     */
    fun createPlanner(persistToSharedPref: Boolean): String {
        val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, PLANNER_ACCOUNT_NAME)
        builder.appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )
        builder.appendQueryParameter(
            CalendarContract.CALLER_IS_SYNCADAPTER,
            "true"
        )
        val calendarUri = builder.build()
        val plannerCalendarId = context.contentResolver.query(
            calendarUri,
            arrayOf(CalendarContract.Calendars._ID),
            CalendarContract.Calendars.NAME + " = ?",
            arrayOf(PLANNER_CALENDAR_NAME),
            null
        )?.use {
            if (it.moveToFirst()) {
                val existing = it.getLong(0).toString()
                Timber.i("found a preexisting calendar %s ", existing)
                existing
            } else {
                val values = ContentValues()
                values.put(CalendarContract.Calendars.ACCOUNT_NAME, PLANNER_ACCOUNT_NAME)
                values.put(
                    CalendarContract.Calendars.ACCOUNT_TYPE,
                    CalendarContract.ACCOUNT_TYPE_LOCAL
                )
                values.put(CalendarContract.Calendars.NAME, PLANNER_CALENDAR_NAME)
                values.put(
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    Utils.getTextWithAppName(context, R.string.plan_calendar_name).toString()
                )
                values.put(
                    CalendarContract.Calendars.CALENDAR_COLOR,
                    ResourcesCompat.getColor(context.resources, R.color.appDefault, null)
                )
                values.put(
                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                    CalendarContract.Calendars.CAL_ACCESS_OWNER
                )
                values.put(CalendarContract.Calendars.OWNER_ACCOUNT, "private")
                values.put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                val uri: Uri? = try {
                    context.contentResolver.insert(calendarUri, values)
                } catch (e: IllegalArgumentException) {
                    CrashHandler.report(e)
                    return INVALID_CALENDAR_ID
                }
                if (uri == null) {
                    CrashHandler.report(Exception("Inserting planner calendar failed, uri is null"))
                    return INVALID_CALENDAR_ID
                }
                val lastPathSegment = uri.lastPathSegment
                if (lastPathSegment == null || lastPathSegment == "0") {
                    CrashHandler.report(
                        Exception("Inserting planner calendar failed, last path segment is $lastPathSegment")
                    )
                    return INVALID_CALENDAR_ID
                }
                Timber.i("successfully set up new calendar: %s", lastPathSegment)
                lastPathSegment
            }
        } ?: run {
            CrashHandler.report(Exception("Searching for planner calendar failed, Calendar app not installed?"))
            return INVALID_CALENDAR_ID
        }
        if (persistToSharedPref) {
            prefHandler.putString(PrefKey.PLANNER_CALENDAR_ID, plannerCalendarId)
        }
        return plannerCalendarId
    }

    /**
     * WARNING this method relies on calendar permissions being granted. It is the callers duty
     * to check if they have been granted
     *
     * @return id of planning calendar if it has been configured and passed checked
     */
    fun checkPlanner(): String? {
        val calendarId =
            prefHandler.requireString(PrefKey.PLANNER_CALENDAR_ID, INVALID_CALENDAR_ID)
        if (calendarId != INVALID_CALENDAR_ID) {
            val checkedId = checkPlannerInternal(calendarId)
            if (INVALID_CALENDAR_ID == checkedId) {
                removePlanner(prefHandler)
            }
            return checkedId
        }
        return INVALID_CALENDAR_ID
    }


    /**
     * verifies if the passed in calendarId exists and is the one stored in [PrefKey.PLANNER_CALENDAR_PATH]
     *
     * @param calendarId id of calendar in system calendar content provider
     * @return the same calendarId if it is safe to use, [.INVALID_CALENDAR_ID] if the calendar
     * is no longer valid, null if verification was not possible
     */
    private fun checkPlannerInternal(calendarId: String?) = context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        arrayOf(
            "$CALENDAR_FULL_PATH_PROJECTION AS path",
            CalendarContract.Calendars.SYNC_EVENTS
        ),
        CalendarContract.Calendars._ID + " = ?",
        arrayOf(calendarId),
        null
    )?.use {
        if (it.moveToFirst()) {
            val found = it.requireString(0)
            val expected = prefHandler.getString(PrefKey.PLANNER_CALENDAR_PATH, "")
            if (found != expected) {
                CrashHandler.report(Exception("found calendar, but path did not match"))
                INVALID_CALENDAR_ID
            } else {
                val syncEvents = it.getInt(1)
                if (syncEvents == 0) {
                    val parts = found.split("/".toRegex(), limit = 3).toTypedArray<String>()
                    if (parts[0] == PLANNER_ACCOUNT_NAME && parts[1] == CalendarContract.ACCOUNT_TYPE_LOCAL) {
                        val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                            .appendEncodedPath(calendarId)
                        builder.appendQueryParameter(
                            CalendarContract.Calendars.ACCOUNT_NAME,
                            PLANNER_ACCOUNT_NAME
                        )
                        builder.appendQueryParameter(
                            CalendarContract.Calendars.ACCOUNT_TYPE,
                            CalendarContract.ACCOUNT_TYPE_LOCAL
                        )
                        builder.appendQueryParameter(
                            CalendarContract.CALLER_IS_SYNCADAPTER,
                            "true"
                        )
                        val values = ContentValues(1)
                        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                        context.contentResolver.update(builder.build(), values, null, null)
                        Timber.i("Fixing sync_events for planning calendar ")
                    }
                }
                calendarId
            }
        } else {
            CrashHandler.report(Exception("configured calendar $calendarId has been deleted"))
            INVALID_CALENDAR_ID
        }
    } ?: run {
        CrashHandler.report(Exception("Received null cursor while checking calendar"))
        null
    }


    fun removePlanner(prefHandler: PrefHandler) {
        prefHandler.remove(PrefKey.PLANNER_CALENDAR_ID)
        prefHandler.remove(PrefKey.PLANNER_CALENDAR_PATH)
        prefHandler.remove(PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP)
    }

    fun onPlannerCalendarIdChanged(newValue: String) {
        val oldValue = plannerCalendarId
        var safeToMovePlans = true
        if (oldValue == newValue) {
            return
        }
        plannerCalendarId = newValue
        if (newValue != INVALID_CALENDAR_ID) {
            val contentResolver = context.contentResolver
            // if we cannot verify that the oldValue has the correct path
            // we will not risk mangling with an unrelated calendar
            if (oldValue != INVALID_CALENDAR_ID && oldValue != checkPlannerInternal(oldValue))
                safeToMovePlans = false
            // we also store the name and account of the calendar,
            // to protect against cases where a user wipes the data of the calendar
            // provider
            // and then accidentally we link to the wrong calendar
            val path = getCalendarPath(contentResolver, plannerCalendarId)
            if (path != null) {
                Timber.i("storing calendar path %s ", path)
                prefHandler.putString(PrefKey.PLANNER_CALENDAR_PATH, path)
            } else {
                CrashHandler.report(
                    IllegalStateException(
                        "could not retrieve configured calendar"
                    )
                )
                plannerCalendarId = INVALID_CALENDAR_ID
                prefHandler.remove(PrefKey.PLANNER_CALENDAR_PATH)
                prefHandler.putString(PrefKey.PLANNER_CALENDAR_ID, INVALID_CALENDAR_ID)
                return
            }
            if (oldValue == INVALID_CALENDAR_ID) {
                PlanExecutor.enqueueSelf(context, prefHandler, true)
            } else if (safeToMovePlans) {
                val eventValues = ContentValues()
                eventValues.put(CalendarContract.Events.CALENDAR_ID, newValue.toLong())
                contentResolver.query(
                    Template.CONTENT_URI, arrayOf(
                        DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PLANID
                    ),
                    DatabaseConstants.KEY_PLANID + " IS NOT null", null, null
                )?.use { plan ->
                    if (plan.moveToFirst()) {
                        do {
                            val templateId = plan.getLong(0)
                            val planId = plan.getLong(1)
                            val eventUri = ContentUris.withAppendedId(
                                CalendarContract.Events.CONTENT_URI,
                                planId
                            )
                            contentResolver.query(
                                eventUri,
                                MyApplication.buildEventProjection(),
                                CalendarContract.Events.CALENDAR_ID + " = ?",
                                arrayOf(oldValue),
                                null
                            )?.use { event ->
                                if (event.moveToFirst()) {
                                    MyApplication.copyEventData(event, eventValues)
                                    if (insertEventAndUpdatePlan(contentResolver, eventValues, templateId)) {
                                        Timber.i("updated plan id in template %d", templateId)
                                        val deleted =
                                            contentResolver.delete(eventUri, null, null)
                                        Timber.i("deleted old event %d", deleted)
                                    }
                                }
                            }
                        } while (plan.moveToNext())
                    }
                }
            }
        } else {
            prefHandler.remove(PrefKey.PLANNER_CALENDAR_PATH)
        }
    }
}