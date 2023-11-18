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
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.UriPermission
import android.content.res.Configuration
import android.database.Cursor
import android.os.Build
import android.os.Process
import android.os.StrictMode
import android.provider.CalendarContract
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.database.getLongOrNull
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.acra.util.StreamReader
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
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.service.AutoBackupWorker.Companion.enqueueOrCancel
import org.totschnig.myexpenses.service.PlanExecutor
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.ui.ContextHelper
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
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

    @Inject
    lateinit var plannerUtils: PlannerUtils

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

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
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
        MainScope().launch(Dispatchers.IO) {
            Timber.uprootAll()
            if (prefHandler.getBoolean(PrefKey.DEBUG_LOGGING, BuildConfig.DEBUG)) {

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
            if (prefHandler.getBoolean(PrefKey.CRASHREPORT_ENABLED, true)) {
                crashHandler.setupLogging(this@MyApplication)
            }
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
            plannerUtils.onPlannerCalendarIdChanged(
                sharedPreferences.getString(key, INVALID_CALENDAR_ID)!!
            )
        }
    }

    val memoryClass: Int
        get() {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            return am.memoryClass
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
