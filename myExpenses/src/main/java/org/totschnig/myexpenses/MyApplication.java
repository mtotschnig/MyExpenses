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

package org.totschnig.myexpenses;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.os.StrictMode;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Calendars;
import com.android.calendar.CalendarContractCompat.Events;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.totschnig.myexpenses.activity.OnboardingActivity;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.di.DaggerAppComponent;
import org.totschnig.myexpenses.di.SecurityProvider;
import org.totschnig.myexpenses.feature.FeatureManager;
import org.totschnig.myexpenses.feature.OcrFeature;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.service.DailyScheduler;
import org.totschnig.myexpenses.service.PlanExecutor;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.ui.ContextHelper;
import org.totschnig.myexpenses.util.MoreUiUtilsKt;
import org.totschnig.myexpenses.util.NotificationBuilderWrapper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.crypt.PRNGFixes;
import org.totschnig.myexpenses.util.io.NetworkUtilsKt;
import org.totschnig.myexpenses.util.io.StreamReader;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.locale.UserLocaleProvider;
import org.totschnig.myexpenses.util.log.TagFilterFileLoggingTree;
import org.totschnig.myexpenses.viewmodel.WebUiViewModel;
import org.totschnig.myexpenses.widget.AbstractWidgetKt;
import org.totschnig.myexpenses.widget.WidgetObserver;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;
import androidx.preference.PreferenceManager;
import timber.log.Timber;

import static org.totschnig.myexpenses.feature.WebUiFeatureKt.START_ACTION;
import static org.totschnig.myexpenses.feature.WebUiFeatureKt.STOP_ACTION;
import static org.totschnig.myexpenses.preference.PrefKey.DEBUG_LOGGING;
import static org.totschnig.myexpenses.preference.PrefKey.UI_WEB;

public class MyApplication extends Application implements
    OnSharedPreferenceChangeListener {

  public static final String DEFAULT_LANGUAGE = "default";
  private AppComponent appComponent;
  @Inject
  LicenceHandler licenceHandler;
  @Inject
  CrashHandler crashHandler;
  @Inject
  FeatureManager featureManager;
  @Inject
  PrefHandler prefHandler;
  @Inject
  UserLocaleProvider userLocaleProvider;
  @Inject
  SharedPreferences mSettings;

  public static final String PLANNER_CALENDAR_NAME = "MyExpensesPlanner";
  public static final String PLANNER_ACCOUNT_NAME = "Local Calendar";
  public static final String INVALID_CALENDAR_ID = "-1";

  private static MyApplication mSelf;

  public static final String KEY_NOTIFICATION_ID = "notification_id";

  public static final String CONTRIB_SECRET = "RANDOM_SECRET";

  private long mLastPause = 0;

  private boolean isLocked;

  public static String getCalendarFullPathProjection() {
    return "ifnull("
        + Calendars.ACCOUNT_NAME + ",'') || '/' ||" + "ifnull("
        + Calendars.ACCOUNT_TYPE + ",'') || '/' ||" + "ifnull(" + Calendars.NAME
        + ",'')";
  }

  public AppComponent getAppComponent() {
    return appComponent;
  }

  public boolean isLocked() {
    return isLocked;
  }

  public void setLocked(boolean isLocked) {
    this.isLocked = isLocked;
  }

  public static final String FEEDBACK_EMAIL = "support@myexpenses.mobi";
  public static final String INVOICES_EMAIL = "billing@myexpenses.mobi";
  // public static int BACKDOOR_KEY = KeyEvent.KEYCODE_CAMERA;

  /**
   * we cache value of planner calendar id, so that we can handle changes in
   * value
   */
  private String mPlannerCalendarId = INVALID_CALENDAR_ID;

  @Override
  public void onCreate() {
    if (BuildConfig.DEBUG) {
      ///TODO disable in test
      enableStrictMode();
    }
    super.onCreate();
    checkAppReplacingState();
    initThreeTen();
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    MoreUiUtilsKt.setNightMode(prefHandler, this);
    final boolean syncService = isSyncService();
    crashHandler.initProcess(this, syncService);
    setupLogging();
    if (!syncService) {
      // sets up mSettings
      if (prefHandler.getBoolean(UI_WEB, false)) {
        if (NetworkUtilsKt.isNetworkConnected(this)) {
          controlWebUi(true);
        } else {
          prefHandler.putBoolean(UI_WEB, false);
        }
      }
      mSettings.registerOnSharedPreferenceChangeListener(this);
      DailyScheduler.updatePlannerAlarms(this, false, false);
      WidgetObserver.Companion.register(this);
    }
    licenceHandler.init();
    NotificationBuilderWrapper.createChannels(this);
    PRNGFixes.apply();
    SecurityProvider.init(this);
  }

  private void initThreeTen() {
    if ("Asia/Hanoi".equals(TimeZone.getDefault().getID())) {
      TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
    AndroidThreeTen.init(this);
  }

  private void checkAppReplacingState() {
    if (getResources() == null) {
      Timber.w("app is replacing...kill");
      Process.killProcess(Process.myPid());
    }
  }

  private boolean isSyncService() {
    final String processName = getCurrentProcessName();
    return processName != null && processName.endsWith(":sync");
  }

  //from ACRA
  @Nullable
  private static String getCurrentProcessName() {
    try {
      return new StreamReader("/proc/self/cmdline").read().trim();
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  protected void attachBaseContext(Context base) {
    mSelf = this;
    //we cannot use the standard way of reading preferences, since this works only after base context
    //has been attached
    final Locale systemLocale = Locale.getDefault();
    super.attachBaseContext(base);
    MultiDex.install(this);
    appComponent = buildAppComponent(systemLocale);
    appComponent.inject(this);
    featureManager.initApplication(this);
    crashHandler.onAttachBaseContext(this);
    DatabaseConstants.buildLocalized(userLocaleProvider.getUserPreferredLocale());
    final Context wrapped = ContextHelper.wrap(base, UserLocaleProvider.Companion.resolveLocale(
        PreferenceManager.getDefaultSharedPreferences(base).getString("ui_language", DEFAULT_LANGUAGE), systemLocale));
    Transaction.buildProjection(wrapped);
  }

  @NonNull
  protected AppComponent buildAppComponent(Locale systemLocale) {
    return DaggerAppComponent.builder()
        .systemLocale(systemLocale)
        .applicationContext(this)
        .build();
  }

  public void setupLogging() {
    Timber.uprootAll();
    if (prefHandler.getBoolean(PrefKey.DEBUG_LOGGING, BuildConfig.DEBUG)) {
      Timber.plant(new Timber.DebugTree());
      Timber.plant(new TagFilterFileLoggingTree(this, PlanExecutor.TAG));
      Timber.plant(new TagFilterFileLoggingTree(this, SyncAdapter.TAG));
      Timber.plant(new TagFilterFileLoggingTree(this, LicenceHandler.TAG));
      Timber.plant(new TagFilterFileLoggingTree(this, TransactionProvider.TAG));
      Timber.plant(new TagFilterFileLoggingTree(this, OcrFeature.TAG));
    }
    crashHandler.setupLogging(this);
  }

  @Deprecated
  public static MyApplication getInstance() {
    return mSelf;
  }

  public Locale getSystemLocale() {
    return userLocaleProvider.getSystemLocale();
  }

  @Deprecated
  public SharedPreferences getSettings() {
    return mSettings;
  }

  public LicenceHandler getLicenceHandler() {
    return licenceHandler;
  }

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    userLocaleProvider.setSystemLocale(newConfig.locale);
    AbstractWidgetKt.onConfigurationChanged(this);
  }

  public long getLastPause() {
    return mLastPause;
  }

  public void setLastPause(ProtectedFragmentActivity ctx) {
    if (ctx instanceof OnboardingActivity) return;
    if (!isLocked()) {
      // if we are dealing with an activity called from widget that allows to
      // bypass password protection, we do not reset last pause
      // otherwise user could gain unprotected access to the app
      boolean isDataEntryEnabled = prefHandler.getBoolean(PrefKey.PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET, false);
      boolean isStartFromWidget = ctx.getIntent().getBooleanExtra(
          AbstractWidgetKt.EXTRA_START_FROM_WIDGET_DATA_ENTRY, false);
      if (!isDataEntryEnabled || !isStartFromWidget) {
        this.mLastPause = System.nanoTime();
      }
      Timber.i("setting last pause : %d", mLastPause);
    }
  }

  public void resetLastPause() {
    this.mLastPause = 0;
  }

  /**
   * @param ctx Activity that should be password protected, can be null if called
   *            from widget provider
   * @return true if password protection is set, and we have paused for at least
   * {@link PrefKey#PROTECTION_DELAY_SECONDS} seconds unless we are called
   * from widget or from an activity called from widget and passwordless
   * data entry from widget is allowed sets isLocked as a side effect
   */
  public boolean shouldLock(ProtectedFragmentActivity ctx) {
    if (ctx instanceof OnboardingActivity) return false;
    boolean isStartFromWidget = ctx == null
        || ctx.getIntent().getBooleanExtra(
        AbstractWidgetKt.EXTRA_START_FROM_WIDGET_DATA_ENTRY, false);
    boolean isProtected = isProtected();
    long lastPause = getLastPause();
    Timber.i("reading last pause : %d", lastPause);
    boolean isPostDelay = System.nanoTime() - lastPause > (prefHandler.getInt(PrefKey.PROTECTION_DELAY_SECONDS, 15) * 1000000000L);
    boolean isDataEntryEnabled = prefHandler.getBoolean(PrefKey.PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET, false);
    return isProtected && isPostDelay && !(isDataEntryEnabled && isStartFromWidget);
  }

  public boolean isProtected() {
    return prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false) ||
        prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false);
  }

  /**
   * verifies if the passed in calendarid exists and is the one stored in {@link PrefKey#PLANNER_CALENDAR_PATH}
   *
   * @param calendarId id of calendar in system calendar content provider
   * @return the same calendarId if it is safe to use, {@link #INVALID_CALENDAR_ID} if the calendar
   * is no longer valid, null if verification was not possible
   */
  @Nullable
  private String checkPlannerInternal(String calendarId) {
    ContentResolver cr = getContentResolver();
    Cursor c = cr.query(Calendars.CONTENT_URI,
        new String[]{getCalendarFullPathProjection() + " AS path", Calendars.SYNC_EVENTS},
        Calendars._ID + " = ?", new String[]{calendarId}, null);
    boolean result = true;
    if (c == null) {
      CrashHandler.report("Received null cursor while checking calendar");
      return null;
    } else {
      if (c.moveToFirst()) {
        String found = DbUtils.getString(c, 0);
        String expected = prefHandler.getString(PrefKey.PLANNER_CALENDAR_PATH, "");
        if (!found.equals(expected)) {
          CrashHandler.report("found calendar, but path did not match");
          result = false;
        } else {
          int syncEvents = c.getInt(1);
          if (syncEvents == 0) {
            String[] parts = found.split("/", 3);
            if (parts[0].equals(PLANNER_ACCOUNT_NAME) && parts[1].equals(CalendarContractCompat.ACCOUNT_TYPE_LOCAL)) {
              Uri.Builder builder = Calendars.CONTENT_URI.buildUpon().appendEncodedPath(calendarId);
              builder.appendQueryParameter(CalendarContractCompat.Calendars.ACCOUNT_NAME, PLANNER_ACCOUNT_NAME);
              builder.appendQueryParameter(CalendarContractCompat.Calendars.ACCOUNT_TYPE,
                  CalendarContractCompat.ACCOUNT_TYPE_LOCAL);
              builder.appendQueryParameter(CalendarContractCompat.CALLER_IS_SYNCADAPTER,
                  "true");
              ContentValues values = new ContentValues(1);
              values.put(CalendarContractCompat.Calendars.SYNC_EVENTS, 1);
              getContentResolver().update(builder.build(), values, null, null);
              Timber.i("Fixing sync_events for planning calendar ");
            }
          }
        }
      } else {
        CrashHandler.report(String.format("configured calendar %s has been deleted: ", calendarId));
        result = false;
      }
      c.close();
    }
    return result ? calendarId : INVALID_CALENDAR_ID;
  }

  /**
   * WARNING this method relies on calendar permissions being granted. It is the callers duty
   * to check if they have been granted
   *
   * @return id of planning calendar if it has been configured and passed checked
   */
  @Nullable
  public String checkPlanner() {
    mPlannerCalendarId = prefHandler.getString(PrefKey.PLANNER_CALENDAR_ID, INVALID_CALENDAR_ID);
    if (!mPlannerCalendarId.equals(INVALID_CALENDAR_ID)) {
      final String checkedId = checkPlannerInternal(mPlannerCalendarId);
      if (INVALID_CALENDAR_ID.equals(checkedId)) {
        removePlanner();
      }
      return checkedId;
    }
    return INVALID_CALENDAR_ID;
  }

  public void removePlanner() {
    mSettings.edit()
        .remove(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID))
        .remove(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_PATH))
        .remove(prefHandler.getKey(PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP))
        .apply();
  }

  /**
   * check if we already have a calendar in Account {@link #PLANNER_ACCOUNT_NAME}
   * of type {@link CalendarContractCompat#ACCOUNT_TYPE_LOCAL} with name
   * {@link #PLANNER_ACCOUNT_NAME} if yes use it, otherwise create it
   *
   * @param persistToSharedPref if true id of the created calendar is stored in preferences
   * @return id if we have configured a useable calendar, or {@link INVALID_CALENDAR_ID}
   */
  public String createPlanner(boolean persistToSharedPref) {
    Uri.Builder builder = Calendars.CONTENT_URI.buildUpon();
    String plannerCalendarId;
    builder.appendQueryParameter(Calendars.ACCOUNT_NAME, PLANNER_ACCOUNT_NAME);
    builder.appendQueryParameter(Calendars.ACCOUNT_TYPE,
        CalendarContractCompat.ACCOUNT_TYPE_LOCAL);
    builder.appendQueryParameter(CalendarContractCompat.CALLER_IS_SYNCADAPTER,
        "true");
    Uri calendarUri = builder.build();
    Cursor c = getContentResolver().query(calendarUri,
        new String[]{Calendars._ID}, Calendars.NAME + " = ?",
        new String[]{PLANNER_CALENDAR_NAME}, null);
    if (c == null) {
      CrashHandler.report("Searching for planner calendar failed, Calendar app not installed?");
      return INVALID_CALENDAR_ID;
    }
    if (c.moveToFirst()) {
      plannerCalendarId = String.valueOf(c.getLong(0));
      Timber.i("found a preexisting calendar %s ", plannerCalendarId);
      c.close();
    } else {
      c.close();
      ContentValues values = new ContentValues();
      values.put(Calendars.ACCOUNT_NAME, PLANNER_ACCOUNT_NAME);
      values.put(Calendars.ACCOUNT_TYPE,
          CalendarContractCompat.ACCOUNT_TYPE_LOCAL);
      values.put(Calendars.NAME, PLANNER_CALENDAR_NAME);
      values.put(Calendars.CALENDAR_DISPLAY_NAME,
          Utils.getTextWithAppName(this, R.string.plan_calendar_name).toString());
      values.put(Calendars.CALENDAR_COLOR,
          getResources().getColor(R.color.appDefault));
      values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
      values.put(Calendars.OWNER_ACCOUNT, "private");
      values.put(Calendars.SYNC_EVENTS, 1);
      Uri uri;
      try {
        uri = getContentResolver().insert(calendarUri, values);
      } catch (IllegalArgumentException e) {
        CrashHandler.report(e);
        return INVALID_CALENDAR_ID;
      }
      if (uri == null) {
        CrashHandler.report("Inserting planner calendar failed, uri is null");
        return INVALID_CALENDAR_ID;
      }
      plannerCalendarId = uri.getLastPathSegment();
      if (plannerCalendarId == null || plannerCalendarId.equals("0")) {
        CrashHandler.report(String.format(Locale.US,
            "Inserting planner calendar failed, last path segment is %s", plannerCalendarId));
        return INVALID_CALENDAR_ID;
      }
      Timber.i("successfully set up new calendar: %s", plannerCalendarId);
    }
    if (persistToSharedPref) {
      // onSharedPreferenceChanged should now trigger initPlanner
      prefHandler.putString(PrefKey.PLANNER_CALENDAR_ID, plannerCalendarId);
    }
    return plannerCalendarId;
  }

  public static String[] buildEventProjection() {
    String[] projection = new String[android.os.Build.VERSION.SDK_INT >= 16 ? 10
        : 8];
    projection[0] = Events.DTSTART;
    projection[1] = Events.DTEND;
    projection[2] = Events.RRULE;
    projection[3] = Events.TITLE;
    projection[4] = Events.ALL_DAY;
    projection[5] = Events.EVENT_TIMEZONE;
    projection[6] = Events.DURATION;
    projection[7] = Events.DESCRIPTION;
    if (android.os.Build.VERSION.SDK_INT >= 16) {
      projection[8] = Events.CUSTOM_APP_PACKAGE;
      projection[9] = Events.CUSTOM_APP_URI;
    }
    return projection;
  }

  /**
   * @param eventCursor must have been populated with a projection built by
   *                    {@link #buildEventProjection()}
   * @param eventValues ContentValues where the extracted data is copied to
   */
  public static void copyEventData(Cursor eventCursor, ContentValues eventValues) {
    eventValues.put(Events.DTSTART, DbUtils.getLongOrNull(eventCursor, 0));
    //older Android versions have populated both dtend and duration
    //restoring those on newer versions leads to IllegalArgumentException
    Long dtEnd = DbUtils.getLongOrNull(eventCursor, 1);
    String duration = null;
    if (dtEnd == null) {
      duration = eventCursor.getString(6);
      if (duration == null) {
        duration = "P0S";
      }
    }
    eventValues.put(Events.DTEND, dtEnd);
    eventValues.put(Events.RRULE, eventCursor.getString(2));
    eventValues.put(Events.TITLE, eventCursor.getString(3));
    eventValues.put(Events.ALL_DAY, eventCursor.getInt(4));
    eventValues.put(Events.EVENT_TIMEZONE, eventCursor.getString(5));
    eventValues.put(Events.DURATION, duration);
    eventValues.put(Events.DESCRIPTION, eventCursor.getString(7));
    if (android.os.Build.VERSION.SDK_INT >= 16) {
      eventValues.put(Events.CUSTOM_APP_PACKAGE, eventCursor.getString(8));
      eventValues.put(Events.CUSTOM_APP_URI, eventCursor.getString(9));
    }
  }

  private boolean insertEventAndUpdatePlan(ContentValues eventValues,
                                           long templateId) {
    Uri uri = getContentResolver().insert(Events.CONTENT_URI, eventValues);
    long planId = ContentUris.parseId(uri);
    Timber.i("event copied with new id %d ", planId);
    ContentValues planValues = new ContentValues();
    planValues.put(DatabaseConstants.KEY_PLANID, planId);
    int updated = getContentResolver().update(
        ContentUris.withAppendedId(Template.CONTENT_URI, templateId),
        planValues, null, null);
    return updated > 0;
  }

  private void controlWebUi(boolean start) {
    final Intent intent = WebUiViewModel.Companion.getServiceIntent();
    intent.setAction(start ? START_ACTION : STOP_ACTION);
    ComponentName componentName;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && start) {
      componentName = startForegroundService(intent);
    } else {
      componentName = startService(intent);
    }
    if (componentName == null) {
      CrashHandler.report("Start of Web User Interface failed");
    }
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                        String key) {
    if (!key.equals(prefHandler.getKey(PrefKey.AUTO_BACKUP_DIRTY))) {
      markDataDirty();
    }
    if (key.equals(prefHandler.getKey(DEBUG_LOGGING))) {
      setupLogging();
    }
    else if (key.equals(prefHandler.getKey(UI_WEB))) {
      controlWebUi(sharedPreferences.getBoolean(key, false));
    }
    // TODO: move to TaskExecutionFragment
    else if (key.equals(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID))) {

      String oldValue = mPlannerCalendarId;
      boolean safeToMovePlans = true;
      String newValue = sharedPreferences.getString(key, INVALID_CALENDAR_ID);
      if (oldValue.equals(newValue)) {
        return;
      }
      mPlannerCalendarId = newValue;
      if (!newValue.equals(INVALID_CALENDAR_ID)) {
        // if we cannot verify that the oldValue has the correct path
        // we will not risk mangling with an unrelated calendar
        if (!oldValue.equals(INVALID_CALENDAR_ID) && !oldValue.equals(checkPlannerInternal(oldValue)))
          safeToMovePlans = false;
        ContentResolver cr = getContentResolver();
        // we also store the name and account of the calendar,
        // to protect against cases where a user wipes the data of the calendar
        // provider
        // and then accidentally we link to the wrong calendar
        Uri uri = ContentUris.withAppendedId(Calendars.CONTENT_URI,
            Long.parseLong(mPlannerCalendarId));
        Cursor c = cr.query(uri, new String[]{getCalendarFullPathProjection()
            + " AS path"}, null, null, null);
        if (c != null && c.moveToFirst()) {
          String path = c.getString(0);
          Timber.i("storing calendar path %s ", path);
          prefHandler.putString(PrefKey.PLANNER_CALENDAR_PATH, path);
        } else {
          CrashHandler.report(new IllegalStateException(
              "could not retrieve configured calendar"));
          mPlannerCalendarId = INVALID_CALENDAR_ID;
          prefHandler.remove(PrefKey.PLANNER_CALENDAR_PATH);
          prefHandler.putString(PrefKey.PLANNER_CALENDAR_ID, INVALID_CALENDAR_ID);
        }
        if (c != null) {
          c.close();
        }
        if (mPlannerCalendarId.equals(INVALID_CALENDAR_ID)) {
          return;
        }
        if (oldValue.equals(INVALID_CALENDAR_ID)) {
          DailyScheduler.updatePlannerAlarms(this, false, true);
        } else if (safeToMovePlans) {
          ContentValues eventValues = new ContentValues();
          eventValues.put(Events.CALENDAR_ID, Long.parseLong(newValue));
          Cursor planCursor = cr.query(Template.CONTENT_URI, new String[]{
                  DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PLANID},
              DatabaseConstants.KEY_PLANID + " IS NOT null", null, null);
          if (planCursor != null) {
            if (planCursor.moveToFirst()) {
              do {
                long templateId = planCursor.getLong(0);
                long planId = planCursor.getLong(1);
                Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI,
                    planId);

                Cursor eventCursor = cr.query(eventUri, buildEventProjection(),
                    Events.CALENDAR_ID + " = ?", new String[]{oldValue}, null);
                if (eventCursor != null) {
                  if (eventCursor.moveToFirst()) {
                    copyEventData(eventCursor, eventValues);
                    if (insertEventAndUpdatePlan(eventValues, templateId)) {
                      Timber.i("updated plan id in template %d", templateId);
                      int deleted = cr.delete(eventUri, null, null);
                      Timber.i("deleted old event %d", deleted);
                    }
                  }
                  eventCursor.close();
                }
              } while (planCursor.moveToNext());
            }
            planCursor.close();
          }
        }
      } else {
        prefHandler.remove(PrefKey.PLANNER_CALENDAR_PATH);
      }
    }
  }

  public int getMemoryClass() {
    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    return am.getMemoryClass();
  }

  //TODO move out to helper class

  /**
   * 1.check if a planner is configured. If no, nothing to do 2.check if the
   * configured planner exists on the device 2.1 if yes go through all events
   * and look for them based on UUID added to description recreate events that
   * we did not find (2.2 if no, user should have been asked to select a target
   * calendar where we will store the recreated events)
   *
   * @return Result with success true
   */
  public Result restorePlanner() {
    ContentResolver cr = getContentResolver();
    String calendarId = prefHandler.getString(PrefKey.PLANNER_CALENDAR_ID, INVALID_CALENDAR_ID);
    String calendarPath = prefHandler.getString(PrefKey.PLANNER_CALENDAR_PATH, "");
    Timber.d("restore plans to calendar with id %s and path %s", calendarId,
        calendarPath);
    int restoredPlansCount = 0;
    if (!(calendarId.equals(INVALID_CALENDAR_ID) || calendarPath.equals(""))) {
      Cursor c = cr.query(Calendars.CONTENT_URI,
          new String[]{Calendars._ID}, getCalendarFullPathProjection()
              + " = ?", new String[]{calendarPath}, null);
      if (c != null) {
        if (c.moveToFirst()) {
          mPlannerCalendarId = c.getString(0);
          Timber.d("restorePlaner: found calendar with id %s", mPlannerCalendarId);
          prefHandler.putString(PrefKey.PLANNER_CALENDAR_ID, mPlannerCalendarId);
          ContentValues planValues = new ContentValues(), eventValues = new ContentValues();
          eventValues.put(Events.CALENDAR_ID,
              Long.parseLong(mPlannerCalendarId));
          Cursor planCursor = cr.query(Template.CONTENT_URI, new String[]{
              DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PLANID,
              DatabaseConstants.KEY_UUID}, DatabaseConstants.KEY_PLANID
              + " IS NOT null", null, null);
          if (planCursor != null) {
            if (planCursor.moveToFirst()) {
              do {
                long templateId = planCursor.getLong(0);
                long oldPlanId = planCursor.getLong(1);
                String uuid = planCursor.getString(2);
                Cursor eventCursor = cr
                    .query(Events.CONTENT_URI, new String[]{Events._ID},
                        Events.CALENDAR_ID + " = ? AND " + Events.DESCRIPTION
                            + " LIKE ?", new String[]{mPlannerCalendarId,
                            "%" + uuid + "%"}, null);
                if (eventCursor != null) {
                  if (eventCursor.moveToFirst()) {
                    long newPlanId = eventCursor.getLong(0);
                    Timber.d("Looking for event with uuid %s: found id %d. Original event had id %d",
                        uuid, newPlanId, oldPlanId);
                    if (newPlanId != oldPlanId) {
                      planValues.put(DatabaseConstants.KEY_PLANID, newPlanId);
                      int updated = cr.update(ContentUris.withAppendedId(
                          Template.CONTENT_URI, templateId), planValues, null,
                          null);
                      if (updated > 0) {
                        Timber.i("updated plan id in template: %d", templateId);
                        restoredPlansCount++;
                      }
                    } else {
                      restoredPlansCount++;
                    }
                    continue;
                  }
                  eventCursor.close();
                }
                Timber.d("Looking for event with uuid %s did not find, now reconstructing from cache",
                    uuid);
                eventCursor = cr.query(TransactionProvider.EVENT_CACHE_URI,
                    buildEventProjection(), Events.DESCRIPTION + " LIKE ?",
                    new String[]{"%" + uuid + "%"}, null);
                boolean found = false;
                if (eventCursor != null) {
                  if (eventCursor.moveToFirst()) {
                    found = true;
                    copyEventData(eventCursor, eventValues);
                    if (insertEventAndUpdatePlan(eventValues, templateId)) {
                      Timber.i("updated plan id in template %d", templateId);
                      restoredPlansCount++;
                    }
                  }
                  eventCursor.close();
                }
                if (!found) {
                  //need to set eventId to null
                  planValues.putNull(DatabaseConstants.KEY_PLANID);
                  getContentResolver().update(
                      ContentUris.withAppendedId(Template.CONTENT_URI, templateId),
                      planValues, null, null);
                }
              } while (planCursor.moveToNext());
            }
            planCursor.close();
          }
        }
        c.close();
      }
    }
    return Result.ofSuccess(R.string.restore_calendar_success, null, restoredPlansCount);
  }

  public void markDataDirty() {
    prefHandler.putBoolean(PrefKey.AUTO_BACKUP_DIRTY, true);
    DailyScheduler.updateAutoBackupAlarms(this);
  }

  private void enableStrictMode() {
    StrictMode.ThreadPolicy.Builder threadPolicyBuilder = new StrictMode.ThreadPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .penaltyFlashScreen();
    StrictMode.setThreadPolicy(threadPolicyBuilder.build());
    StrictMode.VmPolicy.Builder vmPolicyBuilder = new StrictMode.VmPolicy.Builder()
        .detectAll()
        .penaltyLog();
    StrictMode.setVmPolicy(vmPolicyBuilder.build());
  }
}
