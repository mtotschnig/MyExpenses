/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.UriPermission
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.StrictMode
import android.provider.CalendarContract
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.database.getLongOrNull
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.acra.util.StreamReader
import org.totschnig.myexpenses.MyApplication.Companion.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.activity.OnboardingActivity
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.feature.OcrFeature
import org.totschnig.myexpenses.feature.RESTART_ACTION
import org.totschnig.myexpenses.feature.START_ACTION
import org.totschnig.myexpenses.feature.STOP_ACTION
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.CALENDAR_FULL_PATH_PROJECTION
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getCalendarPath
import org.totschnig.myexpenses.provider.requireString
import org.totschnig.myexpenses.service.AutoBackupWorker.Companion.enqueueOrCancel
import org.totschnig.myexpenses.service.PlanExecutor
import org.totschnig.myexpenses.service.PlanExecutor.Companion.enqueueSelf
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.ui.ContextHelper
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import org.totschnig.myexpenses.util.io.isConnectedWifi
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.log.TagFilterFileLoggingTree
import org.totschnig.myexpenses.util.setNightMode
import org.totschnig.myexpenses.viewmodel.WebUiViewModel.Companion.serviceIntent
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET_DATA_ENTRY
import org.totschnig.myexpenses.widget.WidgetObserver.Companion.register
import org.totschnig.myexpenses.widget.onConfigurationChanged
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import java.util.function.Consumer
import javax.inject.Inject

open class MyApplication : Application(), SharedPreferences.OnSharedPreferenceChangeListener,
    DefaultLifecycleObserver {
    lateinit var appComponent: AppComponent

    @Inject
    lateinit var licenceHandler: LicenceHandler

    @Inject
    lateinit var crashHandler: CrashHandler

    @Inject
    lateinit var featureManager: FeatureManager

    @Inject
    lateinit var prefHandler: PrefHandler

    @get:Deprecated("")
    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter
    private var lastPause: Long = 0

    @JvmField
    var isLocked = false
    private var _userPreferredLocale: Locale? = null
    val userPreferredLocale: Locale
        get() = _userPreferredLocale ?: Locale.getDefault()

    /**
     * We would not need this, if [243457462](https://issuetracker.google.com/issues/243457462) were fixed
     */
    fun setUserPreferredLocale(locale: Locale?) {
        _userPreferredLocale = locale
    }
    val wrappedContext: Context
        get() = wrapContext(this)

    fun wrapContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && _userPreferredLocale != null) {
            ContextHelper.wrap(context, _userPreferredLocale)
        } else context
    }

    /**
     * we cache value of planner calendar id, so that we can handle changes in
     * value
     */
    private var mPlannerCalendarId: String = INVALID_CALENDAR_ID
    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        DynamicColors.applyToActivitiesIfAvailable(this)
        super<Application>.onCreate()
        checkAppReplacingState()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setNightMode(prefHandler, this)
        val syncService = isSyncService
        crashHandler.initProcess(this, syncService)
        setupLogging()
        if (!syncService) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            settings.registerOnSharedPreferenceChangeListener(this)
            register(this)
        }
        licenceHandler.init()
        NotificationBuilderWrapper.createChannels(this)
        if (BuildConfig.DEBUG) {
            contentResolver.persistedUriPermissions.forEach(Consumer { uriPermission: UriPermission? ->
                Timber.d(
                    "persisted permissions: %s",
                    uriPermission
                )
            })
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (prefHandler.getBoolean(PrefKey.UI_WEB, false)) {
            if (isConnectedWifi(this)) {
                controlWebUi(START_ACTION)
            } else {
                prefHandler.putBoolean(PrefKey.UI_WEB, false)
            }
        }
    }

    private fun checkAppReplacingState() {
        if (resources == null) {
            Timber.w("app is replacing...kill")
            Process.killProcess(Process.myPid())
        }
    }

    private val isSyncService: Boolean
        get() {
            val processName = currentProcessName
            return processName != null && processName.endsWith(":sync")
        }

    override fun attachBaseContext(base: Context) {
        instance = this
        super.attachBaseContext(base)
        appComponent = buildAppComponent()
        appComponent.inject(this)
        featureManager.initApplication(this)
        crashHandler.onAttachBaseContext(this)
    }

    protected open fun buildAppComponent(): AppComponent {
        return DaggerAppComponent.builder()
            .applicationContext(this)
            .build()
    }
    
    private fun plantTree(tag: String) {
        Timber.plant(TagFilterFileLoggingTree(this, tag))
    }

    private fun setupLogging() {
        Timber.uprootAll()
        if (prefHandler.getBoolean(PrefKey.DEBUG_LOGGING, BuildConfig.DEBUG)) {
            MainScope().launch(Dispatchers.IO) {
                Timber.plant(Timber.DebugTree())
                try {
                    plantTree(PlanExecutor.TAG)
                    plantTree(SyncAdapter.TAG)
                    plantTree(LicenceHandler.TAG)
                    plantTree(BaseTransactionProvider.TAG)
                    plantTree(OcrFeature.TAG)
                    plantTree(BankingFeature.TAG)
                } catch (e: Exception) {
                    report(e)
                }   
            }
        }
        if (prefHandler.getBoolean(PrefKey.CRASHREPORT_ENABLED, true)) {
            crashHandler.setupLogging(this)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onConfigurationChanged(this)
    }

    fun setLastPause(ctx: Activity) {
        if (ctx is OnboardingActivity) return
        if (!isLocked) {
            // if we are dealing with an activity called from widget that allows to
            // bypass password protection, we do not reset last pause
            // otherwise user could gain unprotected access to the app
            val isDataEntryEnabled =
                prefHandler.getBoolean(PrefKey.PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET, false)
            val isStartFromWidget = ctx.intent.getBooleanExtra(
                EXTRA_START_FROM_WIDGET_DATA_ENTRY, false
            )
            if (!isDataEntryEnabled || !isStartFromWidget) {
                lastPause = System.nanoTime()
            }
            Timber.i("setting last pause : %d", lastPause)
        }
    }

    fun resetLastPause() {
        lastPause = 0
    }

    /**
     * @param ctx Activity that should be password protected, can be null if called
     * from widget provider
     * @return true if password protection is set, and we have paused for at least
     * [PrefKey.PROTECTION_DELAY_SECONDS] seconds unless we are called
     * from widget or from an activity called from widget and passwordless
     * data entry from widget is allowed sets isLocked as a side effect
     */
    fun shouldLock(ctx: Activity?): Boolean {
        if (ctx is OnboardingActivity) return false
        val isStartFromWidget = (ctx == null
                || ctx.intent.getBooleanExtra(
            EXTRA_START_FROM_WIDGET_DATA_ENTRY, false
        ))
        val isProtected = isProtected
        val lastPause = lastPause
        Timber.i("reading last pause : %d", lastPause)
        val isPostDelay = System.nanoTime() - lastPause > prefHandler.getInt(
            PrefKey.PROTECTION_DELAY_SECONDS,
            15
        ) * 1000000000L
        val isDataEntryEnabled =
            prefHandler.getBoolean(PrefKey.PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET, false)
        return isProtected && isPostDelay && !(isDataEntryEnabled && isStartFromWidget)
    }

    val isProtected: Boolean
        get() = prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false) ||
                prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)

    /**
     * verifies if the passed in calendarid exists and is the one stored in [PrefKey.PLANNER_CALENDAR_PATH]
     *
     * @param calendarId id of calendar in system calendar content provider
     * @return the same calendarId if it is safe to use, [.INVALID_CALENDAR_ID] if the calendar
     * is no longer valid, null if verification was not possible
     */
    private fun checkPlannerInternal(calendarId: String?) = contentResolver.query(
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
                report(Exception("found calendar, but path did not match"))
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
                        contentResolver.update(builder.build(), values, null, null)
                        Timber.i("Fixing sync_events for planning calendar ")
                    }
                }
                calendarId
            }
        } else {
            report(Exception("configured calendar %s has been deleted: $calendarId"))
            INVALID_CALENDAR_ID
        }
    } ?: run {
        report(Exception("Received null cursor while checking calendar"))
        null
    }

    /**
     * WARNING this method relies on calendar permissions being granted. It is the callers duty
     * to check if they have been granted
     *
     * @return id of planning calendar if it has been configured and passed checked
     */
    fun checkPlanner(): String? {
        mPlannerCalendarId =
            prefHandler.requireString(PrefKey.PLANNER_CALENDAR_ID, INVALID_CALENDAR_ID)
        if (mPlannerCalendarId != INVALID_CALENDAR_ID) {
            val checkedId = checkPlannerInternal(mPlannerCalendarId)
            if (INVALID_CALENDAR_ID == checkedId) {
                removePlanner()
            }
            return checkedId
        }
        return INVALID_CALENDAR_ID
    }

    fun removePlanner() {
        settings.edit()
            .remove(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID))
            .remove(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_PATH))
            .remove(prefHandler.getKey(PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP))
            .apply()
    }

    /**
     * check if we already have a calendar in Account [.PLANNER_ACCOUNT_NAME]
     * of type [CalendarContract.ACCOUNT_TYPE_LOCAL] with name
     * [.PLANNER_ACCOUNT_NAME] if yes use it, otherwise create it
     *
     * @param persistToSharedPref if true id of the created calendar is stored in preferences
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
        val plannerCalendarId = contentResolver.query(
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
                    Utils.getTextWithAppName(this, R.string.plan_calendar_name).toString()
                )
                values.put(
                    CalendarContract.Calendars.CALENDAR_COLOR,
                    resources.getColor(R.color.appDefault)
                )
                values.put(
                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                    CalendarContract.Calendars.CAL_ACCESS_OWNER
                )
                values.put(CalendarContract.Calendars.OWNER_ACCOUNT, "private")
                values.put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                val uri: Uri? = try {
                    contentResolver.insert(calendarUri, values)
                } catch (e: IllegalArgumentException) {
                    report(e)
                    return INVALID_CALENDAR_ID
                }
                if (uri == null) {
                    report(Exception("Inserting planner calendar failed, uri is null"))
                    return INVALID_CALENDAR_ID
                }
                val lastPathSegment = uri.lastPathSegment
                if (lastPathSegment == null || lastPathSegment == "0") {
                    report(
                        Exception("Inserting planner calendar failed, last path segment is $lastPathSegment")
                    )
                    return INVALID_CALENDAR_ID
                }
                Timber.i("successfully set up new calendar: %s", lastPathSegment)
                lastPathSegment
            }
        } ?: run {
            report(Exception("Searching for planner calendar failed, Calendar app not installed?"))
            return INVALID_CALENDAR_ID
        }
        if (persistToSharedPref) {
            // onSharedPreferenceChanged should now trigger DailyScheduler.updatePlannerAlarms
            prefHandler.putString(PrefKey.PLANNER_CALENDAR_ID, plannerCalendarId)
        }
        return plannerCalendarId
    }

    private fun insertEventAndUpdatePlan(
        eventValues: ContentValues,
        templateId: Long
    ): Boolean {
        val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, eventValues)
        val planId = ContentUris.parseId(uri!!)
        Timber.i("event copied with new id %d ", planId)
        val planValues = ContentValues()
        planValues.put(DatabaseConstants.KEY_PLANID, planId)
        val updated = contentResolver.update(
            ContentUris.withAppendedId(Template.CONTENT_URI, templateId),
            planValues, null, null
        )
        return updated > 0
    }

    private fun controlWebUi(action: String) {
        val intent = serviceIntent
        if (intent != null) {
            intent.setAction(action)
            val componentName =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && action == START_ACTION) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            if (componentName == null) {
                report(Exception("Start of Web User Interface failed"))
                prefHandler.putBoolean(PrefKey.UI_WEB, false)
            }
        } else {
            prefHandler.putBoolean(PrefKey.UI_WEB, false)
        }
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String?
    ) {
        if (key != prefHandler.getKey(PrefKey.AUTO_BACKUP_DIRTY)) {
            markDataDirty()
        }
        if (key == prefHandler.getKey(PrefKey.DEBUG_LOGGING)) {
            setupLogging()
        } else if (key == prefHandler.getKey(PrefKey.UI_WEB) || key == prefHandler.getKey(
                PrefKey.WEBUI_PASSWORD
            ) || key == prefHandler.getKey(PrefKey.WEBUI_HTTPS)
        ) {
            val webUiRunning =
                sharedPreferences.getBoolean(prefHandler.getKey(PrefKey.UI_WEB), false)
            //If user configures https or password, while the web ui is not running, there is nothing to do
            if (key == prefHandler.getKey(PrefKey.UI_WEB) || webUiRunning) {
                controlWebUi(if (webUiRunning) RESTART_ACTION else STOP_ACTION)
            }
        } else if (key == prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID)) {
            val oldValue = mPlannerCalendarId
            var safeToMovePlans = true
            val newValue = sharedPreferences.getString(key, INVALID_CALENDAR_ID)!!
            if (oldValue == newValue) {
                return
            }
            mPlannerCalendarId = newValue
            if (newValue != INVALID_CALENDAR_ID) {
                // if we cannot verify that the oldValue has the correct path
                // we will not risk mangling with an unrelated calendar
                if (oldValue != INVALID_CALENDAR_ID && oldValue != checkPlannerInternal(oldValue)) safeToMovePlans =
                    false
                // we also store the name and account of the calendar,
                // to protect against cases where a user wipes the data of the calendar
                // provider
                // and then accidentally we link to the wrong calendar
                val path = getCalendarPath(contentResolver, mPlannerCalendarId)
                if (path != null) {
                    Timber.i("storing calendar path %s ", path)
                    prefHandler.putString(PrefKey.PLANNER_CALENDAR_PATH, path)
                } else {
                    report(
                        IllegalStateException(
                            "could not retrieve configured calendar"
                        )
                    )
                    mPlannerCalendarId = INVALID_CALENDAR_ID
                    prefHandler.remove(PrefKey.PLANNER_CALENDAR_PATH)
                    prefHandler.putString(PrefKey.PLANNER_CALENDAR_ID, INVALID_CALENDAR_ID)
                }
                if (mPlannerCalendarId == INVALID_CALENDAR_ID) {
                    return
                }
                if (oldValue == INVALID_CALENDAR_ID) {
                    enqueueSelf(this, prefHandler, true)
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
                                    buildEventProjection(),
                                    CalendarContract.Events.CALENDAR_ID + " = ?",
                                    arrayOf(oldValue),
                                    null
                                )?.use { event ->
                                    if (event.moveToFirst()) {
                                        copyEventData(event, eventValues)
                                        if (insertEventAndUpdatePlan(eventValues, templateId)) {
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

    val memoryClass: Int
        get() {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            return am.memoryClass
        }
    //TODO move out to helper class
    /**
     * 1.check if a planner is configured. If no, nothing to do 2.check if the
     * configured planner exists on the device 2.1 if yes go through all events
     * and look for them based on UUID added to description recreate events that
     * we did not find (2.2 if no, user should have been asked to select a target
     * calendar where we will store the recreated events)
     *
     * @return number of restored plans
     */
    fun restorePlanner(): Int {
        val calendarId = prefHandler.getString(PrefKey.PLANNER_CALENDAR_ID, INVALID_CALENDAR_ID)
        val calendarPath = prefHandler.getString(PrefKey.PLANNER_CALENDAR_PATH, "")
        Timber.d(
            "restore plans to calendar with id %s and path %s", calendarId,
            calendarPath
        )
        var restoredPlansCount = 0
        if (!(calendarId == INVALID_CALENDAR_ID || calendarPath == "")) {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID),
                CALENDAR_FULL_PATH_PROJECTION
                        + " = ?",
                arrayOf(calendarPath),
                null
            )?.use {
                if (it.moveToFirst()) {
                    mPlannerCalendarId = it.getString(0)
                    Timber.d("restorePlaner: found calendar with id %s", mPlannerCalendarId)
                    prefHandler.putString(PrefKey.PLANNER_CALENDAR_ID, mPlannerCalendarId)
                    val planValues = ContentValues()
                    val eventValues = ContentValues()
                    eventValues.put(
                        CalendarContract.Events.CALENDAR_ID,
                        mPlannerCalendarId.toLong()
                    )
                    contentResolver.query(
                        Template.CONTENT_URI, arrayOf(
                            DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PLANID,
                            DatabaseConstants.KEY_UUID
                        ), DatabaseConstants.KEY_PLANID
                                + " IS NOT null", null, null
                    )?.use { plan ->
                        if (plan.moveToFirst()) {
                            do {
                                val templateId = plan.getLong(0)
                                val oldPlanId = plan.getLong(1)
                                val uuid = plan.getString(2)
                                if (contentResolver.query(
                                        CalendarContract.Events.CONTENT_URI,
                                        arrayOf<String>(CalendarContract.Events._ID),
                                        CalendarContract.Events.CALENDAR_ID + " = ? AND " + CalendarContract.Events.DESCRIPTION
                                                + " LIKE ?",
                                        arrayOf<String>(mPlannerCalendarId, "%$uuid%"),
                                        null
                                    )?.use { event ->
                                        if (event.moveToFirst()) {
                                            val newPlanId = event.getLong(0)
                                            Timber.d(
                                                "Looking for event with uuid %s: found id %d. Original event had id %d",
                                                uuid, newPlanId, oldPlanId
                                            )
                                            if (newPlanId != oldPlanId) {
                                                planValues.put(
                                                    DatabaseConstants.KEY_PLANID,
                                                    newPlanId
                                                )
                                                val updated = contentResolver.update(
                                                    ContentUris.withAppendedId(
                                                        Template.CONTENT_URI, templateId
                                                    ), planValues, null,
                                                    null
                                                )
                                                if (updated > 0) {
                                                    Timber.i(
                                                        "updated plan id in template: %d",
                                                        templateId
                                                    )
                                                    restoredPlansCount++
                                                }
                                            } else {
                                                restoredPlansCount++
                                            }
                                            true
                                        } else false
                                    } == false
                                ) {
                                    Timber.d(
                                        "Looking for event with uuid %s did not find, now reconstructing from cache",
                                        uuid
                                    )
                                    if (contentResolver.query(
                                            TransactionProvider.EVENT_CACHE_URI,
                                            buildEventProjection(),
                                            CalendarContract.Events.DESCRIPTION + " LIKE ?",
                                            arrayOf<String>(
                                                "%$uuid%"
                                            ),
                                            null
                                        )?.use { event ->
                                            if (event.moveToFirst()) {
                                                copyEventData(event, eventValues)
                                                if (insertEventAndUpdatePlan(
                                                        eventValues,
                                                        templateId
                                                    )
                                                ) {
                                                    Timber.i(
                                                        "updated plan id in template %d",
                                                        templateId
                                                    )
                                                    restoredPlansCount++
                                                }
                                                true
                                            } else false
                                        } == true
                                    ) {
                                        //need to set eventId to null
                                        planValues.putNull(DatabaseConstants.KEY_PLANID)
                                        contentResolver.update(
                                            ContentUris.withAppendedId(
                                                Template.CONTENT_URI,
                                                templateId
                                            ),
                                            planValues, null, null
                                        )
                                    }
                                }
                            } while (plan.moveToNext())
                        }
                    }
                }
            }
        }
        return restoredPlansCount
    }

    fun markDataDirty() {
        try {
            prefHandler.putBoolean(PrefKey.AUTO_BACKUP_DIRTY, true)
            enqueueOrCancel(this, prefHandler)
        } catch (e: Exception) {
            report(e)
        }
    }

    protected open fun enableStrictMode() {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .penaltyFlashScreen()
        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            vmPolicyBuilder.detectNonSdkApiUsage()
        }
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }

    fun invalidateHomeCurrency(newValue: String?) {
        currencyContext.invalidateHomeCurrency()
        currencyFormatter.invalidate(
            contentResolver,
            DataBaseAccount.AGGREGATE_HOME_CURRENCY_CODE
        )
        DatabaseConstants.buildProjection(this, newValue)
        contentResolver.notifyChange(TransactionProvider.TRANSACTIONS_URI, null, false)
    }

    companion object {
        const val DEFAULT_LANGUAGE = "default"
        const val PLANNER_CALENDAR_NAME = "MyExpensesPlanner"
        const val PLANNER_ACCOUNT_NAME = "Local Calendar"
        const val INVALID_CALENDAR_ID = "-1"

        @get:Deprecated("")
        lateinit var instance: MyApplication
            private set
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val INVOICES_EMAIL = "billing@myexpenses.mobi"
        private val currentProcessName: String?
            //from ACRA
            get() = try {
                StreamReader("/proc/self/cmdline").read().trim { it <= ' ' }
            } catch (e: IOException) {
                null
            }

        fun buildEventProjection(): Array<String?> {
            val projection = arrayOfNulls<String>(10)
            projection[0] = CalendarContract.Events.DTSTART
            projection[1] = CalendarContract.Events.DTEND
            projection[2] = CalendarContract.Events.RRULE
            projection[3] = CalendarContract.Events.TITLE
            projection[4] = CalendarContract.Events.ALL_DAY
            projection[5] = CalendarContract.Events.EVENT_TIMEZONE
            projection[6] = CalendarContract.Events.DURATION
            projection[7] = CalendarContract.Events.DESCRIPTION
            projection[8] = CalendarContract.Events.CUSTOM_APP_PACKAGE
            projection[9] = CalendarContract.Events.CUSTOM_APP_URI
            return projection
        }

        /**
         * @param eventCursor must have been populated with a projection built by
         * [.buildEventProjection]
         * @param eventValues ContentValues where the extracted data is copied to
         */
        fun copyEventData(eventCursor: Cursor, eventValues: ContentValues) {
            eventValues.put(CalendarContract.Events.DTSTART, eventCursor.getLongOrNull(0))
            //older Android versions have populated both dtend and duration
            //restoring those on newer versions leads to IllegalArgumentException
            val dtEnd = eventCursor.getLongOrNull(1)
            var duration: String? = null
            if (dtEnd == null) {
                duration = eventCursor.getString(6)
                if (duration == null) {
                    duration = "P0S"
                }
            }
            eventValues.put(CalendarContract.Events.DTEND, dtEnd)
            eventValues.put(CalendarContract.Events.RRULE, eventCursor.getString(2))
            eventValues.put(CalendarContract.Events.TITLE, eventCursor.getString(3))
            eventValues.put(CalendarContract.Events.ALL_DAY, eventCursor.getInt(4))
            eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, eventCursor.getString(5))
            eventValues.put(CalendarContract.Events.DURATION, duration)
            eventValues.put(CalendarContract.Events.DESCRIPTION, eventCursor.getString(7))
            eventValues.put(CalendarContract.Events.CUSTOM_APP_PACKAGE, eventCursor.getString(8))
            eventValues.put(CalendarContract.Events.CUSTOM_APP_URI, eventCursor.getString(9))
        }
    }
}
