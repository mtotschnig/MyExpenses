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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

import org.acra.*;
import org.acra.annotation.*;

import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.service.UnlockHandler;
import org.totschnig.myexpenses.service.PlanExecutor;
import org.totschnig.myexpenses.util.Distrib;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.widget.*;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Calendars;
import com.android.calendar.CalendarContractCompat.Events;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources.NotFoundException;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

@ReportsCrashes(
    formKey = "",
    formUri = "https://mtotschnig.cloudant.com/acra-myexpenses/_design/acra-storage/_update/report",
    reportType = org.acra.sender.HttpSender.Type.JSON,
    httpMethod = org.acra.sender.HttpSender.Method.PUT,
    formUriBasicAuthLogin="thapponcedonventseliance",
    formUriBasicAuthPassword="8xVV4Rw5SVpkhHFahqF1W3ww",
    logcatArguments = { "-t", "250", "-v", "long", "ActivityManager:I", "MyExpenses:V", "*:S" },
    excludeMatchingSharedPreferencesKeys={"planner_calendar_path","password"}
    )
public class MyApplication extends Application implements OnSharedPreferenceChangeListener {
    public static final String PLANNER_CALENDAR_NAME = "MyExpensesPlanner";
    public static final String PLANNER_ACCOUNT_NAME = "Local Calendar";
    private SharedPreferences mSettings;
    private static MyApplication mSelf;
    public static final String BACKUP_DB_FILE_NAME = "BACKUP";
    public static final String BACKUP_PREF_FILE_NAME = "BACKUP_PREF";
    //the following keys are stored as string resources, so that
    //they can be referenced from preferences.xml, and thus we
    //can guarantee the referential integrity
    public static String PREFKEY_CATEGORIES_SORT_BY_USAGES;
    public static String PREFKEY_PERFORM_SHARE;
    public static String PREFKEY_SHARE_TARGET;
    public static String PREFKEY_QIF_EXPORT_FILE_ENCODING;
    public static String PREFKEY_UI_THEME_KEY;
    public static String PREFKEY_UI_FONTSIZE;
    public static String PREFKEY_BACKUP;
    public static String PREFKEY_RESTORE;
    public static String PREFKEY_CONTRIB_INSTALL;
    public static String PREFKEY_REQUEST_LICENCE;
    public static String PREFKEY_ENTER_LICENCE;
    public static String PREFKEY_PERFORM_PROTECTION;
    public static String PREFKEY_SET_PASSWORD;
    public static String PREFKEY_SECURITY_ANSWER;
    public static String PREFKEY_SECURITY_QUESTION;
    public static String PREFKEY_PROTECTION_DELAY_SECONDS;
    public static String PREFKEY_PROTECTION_ENABLE_ACCOUNT_WIDGET;
    public static String PREFKEY_PROTECTION_ENABLE_TEMPLATE_WIDGET;
    public static String PREFKEY_PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET;
    public static String PREFKEY_EXPORT_FORMAT;
    public static String PREFKEY_SEND_FEEDBACK;
    public static String PREFKEY_MORE_INFO_DIALOG;
    public static final String PREFKEY_LICENSE_STATUS = "licenseStatus";
    public static final String PREFKEY_LICENSE_RETRY_COUNT = "retryCount";
    public static String PREFKEY_CATEGORY_CONTRIB;
    //public static String PREFKEY_SHORTCUT_ACCOUNT_LIST;
    public static String PREFKEY_PLANNER_CALENDAR_ID;
    private static final String PREFKEY_PLANNER_CALENDAR_PATH = "planner_calendar_path";
    public static final String PREFKEY_CURRENT_VERSION = "currentversion";
    public static final String PREFKEY_CURRENT_ACCOUNT = "current_account";
    public static final String PREFKEY_PLANNER_LAST_EXECUTION_TIMESTAMP = "planner_last_execution_timestamp";
    public static String PREFKEY_APP_DIR;
    public static String PREFKEY_RATE;
    public static String PREFKEY_UI_LANGUAGE;

    public static final String KEY_NOTIFICATION_ID = "notification_id";
    public static final String KEY_OPERATION_TYPE = "operationType";

    public static String BUILD_DATE = "";
    public static String CONTRIB_SECRET = "onqn1bKZzM";
    public static String MARKET_PREFIX = "market://details?id=";
    public static String CALENDAR_FULL_PATH_PROJECTION = 
        "ifnull(" + Calendars.ACCOUNT_NAME + ",'') || '/' ||" +
        "ifnull(" + Calendars.ACCOUNT_TYPE + ",'') || '/' ||" +
        "ifnull(" + Calendars.NAME + ",'') AS path";
    //public static String MARKET_PREFIX = "amzn://apps/android?p=";

    public static final boolean debug = false;
    public boolean isContribEnabled = false,
        showImportantUpgradeInfo = false,
        showContribRetryLimitReachedInfo = false;
    private ServiceConnection mConnection;
    private long mLastPause = 0;
    public static String TAG = "MyExpenses";
    /**
     * how many nanoseconds should we wait before prompting for the password
     */
    public static long passwordCheckDelayNanoSeconds;
    public static void setPasswordCheckDelayNanoSeconds() {
      MyApplication.passwordCheckDelayNanoSeconds = mSelf.mSettings.getInt(PREFKEY_PROTECTION_DELAY_SECONDS, 15) * 1000000000L;
    }

    private boolean isLocked;
    public boolean isLocked() {
      return isLocked;
    }

    public void setLocked(boolean isLocked) {
      this.isLocked = isLocked;
    }

    protected Messenger mService;

    public static final String FEEDBACK_EMAIL = "support@myexpenses.mobi";
//    public static int BACKDOOR_KEY = KeyEvent.KEYCODE_CAMERA;
    
    /**
     * we cache value of planner calendar id, so that we can handle changes in value
     */
    private String mPlannerCalendarId;
    /**
     * we store the systemLocale if the user wants to come back to it
     * after having tried a different locale;
     */
    private Locale systemLocale = Locale.getDefault();

    private WidgetObserver mTemplateObserver,mAccountObserver;

    @Override
    public void onCreate() {
      super.onCreate();
      ACRA.init(this);
      //ACRA.getErrorReporter().putCustomData("Distribution", "Google Play");
      mSelf = this;
      //sets up mSettings
      getSettings().registerOnSharedPreferenceChangeListener(this);
      PREFKEY_CATEGORIES_SORT_BY_USAGES = getString(R.string.pref_categories_sort_by_usages_key);
      PREFKEY_PERFORM_SHARE = getString(R.string.pref_perform_share_key);
      PREFKEY_SHARE_TARGET = getString(R.string.pref_share_target_key);
      PREFKEY_QIF_EXPORT_FILE_ENCODING = getString(R.string.pref_qif_export_file_encoding_key);
      PREFKEY_UI_THEME_KEY = getString(R.string.pref_ui_theme_key);
      PREFKEY_UI_FONTSIZE = getString(R.string.pref_ui_fontsize_key);
      PREFKEY_BACKUP = getString(R.string.pref_backup_key);
      PREFKEY_RESTORE = getString(R.string.pref_restore_key);
      PREFKEY_CONTRIB_INSTALL = getString(R.string.pref_contrib_install_key);
      PREFKEY_REQUEST_LICENCE = getString(R.string.pref_request_licence_key);
      PREFKEY_ENTER_LICENCE = getString(R.string.pref_enter_licence_key);
      PREFKEY_PERFORM_PROTECTION = getString(R.string.pref_perform_protection_key);
      PREFKEY_SET_PASSWORD = getString(R.string.pref_set_password_key);
      PREFKEY_SECURITY_ANSWER = getString(R.string.pref_security_answer_key);
      PREFKEY_SECURITY_QUESTION = getString(R.string.pref_security_question_key);
      PREFKEY_PROTECTION_DELAY_SECONDS = getString(R.string.pref_protection_delay_seconds_key);
      PREFKEY_PROTECTION_ENABLE_ACCOUNT_WIDGET = getString(R.string.pref_protection_enable_account_widget_key);
      PREFKEY_PROTECTION_ENABLE_TEMPLATE_WIDGET = getString(R.string.pref_protection_enable_template_widget_key);
      PREFKEY_PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET = getString(R.string.pref_protection_enable_data_entry_from_widget_key);
      PREFKEY_EXPORT_FORMAT = getString(R.string.pref_export_format_key);
      PREFKEY_SEND_FEEDBACK = getString(R.string.pref_send_feedback_key);
      PREFKEY_MORE_INFO_DIALOG = getString(R.string.pref_more_info_dialog_key);
      //PREFKEY_SHORTCUT_ACCOUNT_LIST = getString(R.string.pref_shortcut_account_list_key);
      PREFKEY_PLANNER_CALENDAR_ID = getString(R.string.pref_planner_calendar_id_key);
      PREFKEY_RATE = getString(R.string.pref_rate_key);
      PREFKEY_UI_LANGUAGE = getString(R.string.pref_ui_language_key);
      PREFKEY_APP_DIR = getString(R.string.pref_app_dir_key);
      PREFKEY_CATEGORY_CONTRIB = getString(R.string.pref_category_contrib_key);
      setPasswordCheckDelayNanoSeconds();
      try {
        InputStream rawResource = getResources().openRawResource(R.raw.app);
        Properties properties = new Properties();
        properties.load(rawResource);
        BUILD_DATE = properties.getProperty("build.date");
      } catch (NotFoundException e) {
        Log.w(TAG,"Did not find raw resource");
      } catch (IOException e) {
        Log.w(TAG,"Failed to open property file");
      }
      initContribEnabled();
      mPlannerCalendarId = mSettings.getString(PREFKEY_PLANNER_CALENDAR_ID, "-1");
      initPlanner();
      registerWidgetObservers();
    }

    private void registerWidgetObservers() {
      final ContentResolver r = getContentResolver();
      mTemplateObserver = new WidgetObserver(TemplateWidget.class);
      for (Uri uri: TemplateWidget.OBSERVED_URIS) {
        r.registerContentObserver(uri, true, mTemplateObserver);
      }
      mAccountObserver = new WidgetObserver(AccountWidget.class);
      for (Uri uri: AccountWidget.OBSERVED_URIS) {
        r.registerContentObserver(uri, true, mAccountObserver);
      }
    }

    public boolean initContribEnabled() {
      //TODO profile time taken in this function
      int contribStatusInfo = Distrib.getContribStatusInfo(this);
      isContribEnabled = contribStatusInfo == -1 ||Utils.verifyLicenceKey(mSettings.getString(MyApplication.PREFKEY_ENTER_LICENCE, ""));
      //we call MyExpensesContrib to check status
      if (!isContribEnabled) {
        Log.i(TAG,"contribStatusInfo: " + contribStatusInfo);
        if (contribStatusInfo < 5) {
          try {
            final Messenger mMessenger = new Messenger(new UnlockHandler() {
              public void handleMessage(Message msg) {
                super.handleMessage(msg);
                try {
                  unbindService(mConnection);
                  Log.i(TAG,"having handled message; unbinding from service");
                } catch (IllegalArgumentException e) {
                  Log.i(TAG,"unbind service during handleMessage lead to IllegalArgumentException");
                }
              }
            });
            mConnection = new ServiceConnection() {
              public void onServiceConnected(ComponentName className, IBinder service) {
                  // This is called when the connection with the service has been
                  // established, giving us the object we can use to
                  // interact with the service.  We are communicating with the
                  // service using a Messenger, so here we get a client-side
                  // representation of that from the raw IBinder object.
                  mService = new Messenger(service);
                  try {
                    Message msg = Message.obtain();
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                  } catch (RemoteException e) {
                    Log.w(TAG,"Could not communicate with licence verification service");
                  }
              }
  
              public void onServiceDisconnected(ComponentName className) {
                  // This is called when the connection with the service has been
                  // unexpectedly disconnected -- that is, its process crashed.
                  mService = null;
              }
            };
            if (!bindService(new Intent("org.totschnig.myexpenses.contrib.MyService"), mConnection,
                Context.BIND_AUTO_CREATE)) {
              //showImportantUpgradeInfo = Utils.doesPackageExist(this, "org.totschnig.myexpenses.contrib");
              try {
                //prevent ServiceConnectionLeaked warning
                unbindService(mConnection);
              } catch (Throwable t) {}
            }
            //TODO implement dialog showing contribupgradeinfo
          } catch (SecurityException e) {
            Log.w(TAG,"Could not bind to licence verification service");
          }
        }
        else
          showContribRetryLimitReachedInfo = true;
      } else
        Log.i(TAG,"Contrib status enabled");
      return isContribEnabled;
    }
    public static MyApplication getInstance() {
      return mSelf;
    }

    public SharedPreferences getSettings() {
      if (mSettings == null) {
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
      }
      return mSettings;
    }
    public void setSettings(SharedPreferences s) {
        mSettings = s;
    }
    public boolean backup(File backupDir) {
      File backupPrefFile, sharedPrefFile;
      if (DbUtils.backup(backupDir)) {
        backupPrefFile = new File(backupDir, BACKUP_PREF_FILE_NAME);
        //Samsung has special path on some devices
        //http://stackoverflow.com/questions/5531289/copy-the-shared-preferences-xml-file-from-data-on-samsung-device-failed
        String sharedPrefFileCommon = getPackageName() 
            + "/shared_prefs/" + getPackageName() + "_preferences.xml";
        sharedPrefFile = new File("/dbdata/databases/" + sharedPrefFileCommon);
        if (!sharedPrefFile.exists()) {
          sharedPrefFile = new File("/data/data/" + sharedPrefFileCommon);
          if (!sharedPrefFile.exists()) {
            Log.e(TAG,"Unable to determine path to shared preference file");
            return false;
          }
        }
        return Utils.copy(sharedPrefFile, backupPrefFile);
      } else
        return false;
    }
    public static int getThemeId() {
      return getThemeId(false);
    }
    public enum ThemeType {
      DARK,
      LIGHT
    }
    public static ThemeType getThemeType() {
      return mSelf.mSettings.getString(MyApplication.PREFKEY_UI_THEME_KEY,"dark").equals("light") ?
          ThemeType.LIGHT : ThemeType.DARK;
    }
    public static int getThemeId(boolean legacyPreferenceActivity) {
      int fontScale;
      try {
        fontScale = mSelf.mSettings.getInt(PREFKEY_UI_FONTSIZE, 0);
      } catch (Exception e) {
        //in a previous version, the same key was holding an integer
        fontScale = 0;
        SharedPreferencesCompat.apply(
            mSelf.mSettings.edit().remove(PREFKEY_UI_FONTSIZE));
      }
      int resId;
      String suffix = legacyPreferenceActivity ? ".LegacyPreferenceActivity" : "";
      if (getThemeType() == ThemeType.LIGHT) {
        if (fontScale < 1 || fontScale > 3)
          return legacyPreferenceActivity ? R.style.ThemeLight_LegacyPreferenceActivity : R.style.ThemeLight;
        else
          resId = mSelf.getResources().getIdentifier("ThemeLight.s"+fontScale+suffix, "style", mSelf.getPackageName());
      } else{
        if (fontScale < 1 || fontScale > 3)
          return legacyPreferenceActivity ? R.style.ThemeDark_LegacyPreferenceActivity : R.style.ThemeDark;
        else
          resId = mSelf.getResources().getIdentifier("ThemeDark.s"+fontScale+suffix, "style", mSelf.getPackageName());
      }
      return resId;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        systemLocale = newConfig.locale;
    }

    public void setLanguage() {
      String language = mSettings.getString(MyApplication.PREFKEY_UI_LANGUAGE, "default");
      Locale l;
      if (language.equals("default")) {
        l = systemLocale;
      } else if (language.contains("-")) {
        String[] parts = language.split("-");
        l = new Locale(parts[0],parts[1]);
      } else {
        l = new Locale(language);
      }
      setLanguage(l);
    }
    public void setLanguage(Locale locale) {
      if (!Locale.getDefault().equals(locale)) {
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        config.fontScale = getResources().getConfiguration().fontScale;
        getResources().updateConfiguration(config,
            getResources().getDisplayMetrics());
        //in order to the following statement to be effective the cursor loader would need to be restarted
        //DatabaseConstants.buildLocalized();
      }
    }
    public static File requireBackupDir() {
      File appDir = Utils.requireAppDir();
      if (appDir == null)
        return null;
      File dir = Utils.timeStampedFile(appDir,"backup");
      return dir;
    }
    public static File getBackupDbFile(File backupDir) {
      return new File(backupDir, BACKUP_DB_FILE_NAME);
    }
    public static File getBackupPrefFile(File backupDir) {
      return new File(backupDir, BACKUP_PREF_FILE_NAME);
    }

    public long getLastPause() {
      return mLastPause;
    }
    public void setLastPause(Activity ctx) {
      if (!isLocked()) {
        //if we are dealing with an activity called from widget that allows to 
        //bypass password protection, we do not reset last pause
        //otherwise user could gain unprotected access to the app
        boolean isDataEntryEnabled = mSettings.getBoolean(PREFKEY_PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET, false);
        boolean isStartFromWidget = ctx.getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, false);
        if (!isDataEntryEnabled || !isStartFromWidget) {
          this.mLastPause = System.nanoTime();
        }
      }
    }
    public void resetLastPause() {
      this.mLastPause = 0;
    }
    /**
     * @param ctx Activity that should be password protected, can be null if called from widget provider
     * @return true if password protection is set, and
     * we have paused for at least {@link #passwordCheckDelayNanoSeconds} seconds
     * unless we are called from widget or from an activity called from widget and passwordless data entry from widget is allowed
     * sets isLocked as a side effect
     */
    public boolean shouldLock(Activity ctx) {
      boolean isStartFromWidget =
          ctx == null ||
          ctx.getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, false);
      boolean isProtected = isProtected();
      long lastPause = getLastPause();
      boolean isPostDelay = System.nanoTime() - lastPause > passwordCheckDelayNanoSeconds;
      boolean isDataEntryEnabled = mSettings.getBoolean(PREFKEY_PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET, false);
      if (
          isProtected && isPostDelay && (!isDataEntryEnabled || !isStartFromWidget)
      ) {
        setLocked(true);
        return true;
      }
      return false;
    }
    public boolean isProtected() {
      return mSettings.getBoolean(PREFKEY_PERFORM_PROTECTION, false);
    }
    /**
     * @param calendarId
     * @return verifies if the passed in calendarid exists and
     * is the one stored in {@link PREFKEY_PLANNER_CALENDAR_PATH}
     */
    private boolean checkPlannerInternal(String calendarId) {
      ContentResolver cr = getContentResolver();
      Cursor c = cr.query(Calendars.CONTENT_URI,
            new String[]{CALENDAR_FULL_PATH_PROJECTION},
            Calendars._ID + " = ?",
            new String[] {calendarId},
            null);
      boolean result = true;
      if (c==null)
        return false;
      else {
        if (c.moveToFirst()) {
          String found = DbUtils.getString(c,0);
          String expected = mSettings.getString(PREFKEY_PLANNER_CALENDAR_PATH,"");
          if (!found.equals(expected)) {
            Log.w(TAG,String.format(
                "found calendar, but path did not match; expected %s ; got %s",
                expected,found));
            result = false;
          }
        } else {
          Log.i(TAG,"configured calendar has been deleted: "+ calendarId);
          result = false;
        }
        c.close();
        return result;
      }
    }
    public String checkPlanner() {
      if (!mPlannerCalendarId.equals("-1")) {
        if (!checkPlannerInternal(mPlannerCalendarId)) {
          SharedPreferencesCompat.apply(
              mSettings.edit()
                .remove(PREFKEY_PLANNER_CALENDAR_ID)
                .remove(PREFKEY_PLANNER_CALENDAR_PATH)
                .remove(PREFKEY_PLANNER_LAST_EXECUTION_TIMESTAMP));
          return "-1";
        }
      }
      return mPlannerCalendarId;
    }
    /**
     * check if we already have a calendar in Account {@link PLANNER_ACCOUNT_NAME} of
     * type {@link CalendarContractCompat.ACCOUNT_TYPE_LOCAL} with name {@link PLANNER_ACCOUNT_NAME}
     * if yes use it, otherwise create it
     * @return true if we have configured a useable calendar
     */
    public boolean createPlanner() {
      Uri.Builder builder = Calendars.CONTENT_URI.buildUpon();
      String plannerCalendarId;
      builder.appendQueryParameter(
          Calendars.ACCOUNT_NAME,
          PLANNER_ACCOUNT_NAME);
      builder.appendQueryParameter(
          Calendars.ACCOUNT_TYPE,
          CalendarContractCompat.ACCOUNT_TYPE_LOCAL);
      builder.appendQueryParameter(
          CalendarContractCompat.CALLER_IS_SYNCADAPTER,
          "true");
      Uri calendarUri = builder.build();
      Cursor c = getContentResolver().query(
          calendarUri,
          new String[] {Calendars._ID},
            Calendars.NAME +  " = ?",
          new String[]{PLANNER_CALENDAR_NAME}, null);
      if (c == null) {
        Log.w(TAG,"Searching for planner calendar failed, Calendar app not installed?");
        return false;
      }
      if (c.moveToFirst()) {
        plannerCalendarId = String.valueOf(c.getLong(0));
        Log.i(TAG,"found a preexisting calendar: "+ plannerCalendarId);
        c.close();
      } else {
        c.close();
        ContentValues values = new ContentValues();
        values.put(
            Calendars.ACCOUNT_NAME,
            PLANNER_ACCOUNT_NAME);
        values.put(
            Calendars.ACCOUNT_TYPE,
            CalendarContractCompat.ACCOUNT_TYPE_LOCAL);
        values.put(
            Calendars.NAME,
            PLANNER_CALENDAR_NAME);
        values.put(
            Calendars.CALENDAR_DISPLAY_NAME,
            getString(R.string.plan_calendar_name));
        values.put(
            Calendars.CALENDAR_COLOR,
            getResources().getColor(R.color.appDefault));
        values.put(
            Calendars.CALENDAR_ACCESS_LEVEL,
            Calendars.CAL_ACCESS_OWNER);
        values.put(
            Calendars.OWNER_ACCOUNT,
            "private");
        Uri uri;
        try {
          uri = getContentResolver().insert(calendarUri, values);
        } catch (IllegalArgumentException e) {
          Log.w(TAG,"Inserting planner calendar failed, Calendar app not installed?");
          return false;
        }
        plannerCalendarId = uri.getLastPathSegment();
        if (plannerCalendarId == null || plannerCalendarId.equals("0")) {
          Log.w(TAG,"Inserting planner calendar failed, last path segment is null or 0");
          return false;
        }
        Log.i(TAG,"successfully set up new calendar: "+ plannerCalendarId);
      }
      //onSharedPreferenceChanged should now trigger initPlanner
      SharedPreferencesCompat.apply(
          mSettings.edit().putString(PREFKEY_PLANNER_CALENDAR_ID, plannerCalendarId));
      return true;
    }
    /**
     * call PlanExecutor, which will 
     * 1) set up the planner calendar
     * 2) execute plans
     * 3) reschedule execution through alarm 
     */
    public void initPlanner() {
      Log.i(TAG,"initPlanner called");
      Intent service = new Intent(this, PlanExecutor.class);
      startService(service);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
        String key) {
      //TODO: move to TaskExecutionFragment
      if (key.equals(PREFKEY_PLANNER_CALENDAR_ID)) {
        String oldValue = mPlannerCalendarId;
        boolean safeToMovePlans = true;
        String newValue = sharedPreferences.getString(PREFKEY_PLANNER_CALENDAR_ID, "-1");
        if (oldValue.equals(newValue)) {
          return;
        }
        mPlannerCalendarId = newValue;
        if (!newValue.equals("-1")) {
          //if we cannot verify that the oldValue has the correct path
          //we will not risk mangling with an unrelated calendar
          if (!oldValue.equals("-1") && !checkPlannerInternal(oldValue))
            safeToMovePlans = false;
          ContentResolver cr = getContentResolver();
          //we also store the name and account of the calendar,
          //to protect against cases where a user wipes the data of the calendar provider
          //and then accidentally we link to the wrong calendar
          Uri uri= ContentUris.withAppendedId(Calendars.CONTENT_URI, Long.parseLong(mPlannerCalendarId));
          Cursor c = cr.query(uri,
              new String[]{CALENDAR_FULL_PATH_PROJECTION},
                  null, null, null);
          if (c != null && c.moveToFirst()) {
            String path = c.getString(0);
            Log.i(TAG,"storing calendar path : "+ path);
            SharedPreferencesCompat.apply(sharedPreferences.edit().putString(
                PREFKEY_PLANNER_CALENDAR_PATH, path));
          } else {
            Log.e("TAG","could not retrieve configured calendar");
            mPlannerCalendarId = "-1";
            SharedPreferencesCompat.apply(sharedPreferences.edit().putString(
                PREFKEY_PLANNER_CALENDAR_ID, "-1"));
          }
          if (c != null)
            c.close();
          if (oldValue.equals("-1")) {
            initPlanner();
          } else if (safeToMovePlans) {
            ContentValues eventValues = new ContentValues(),
                planValues = new ContentValues();
            eventValues.put(Events.CALENDAR_ID, Long.parseLong(newValue));
            Cursor planCursor = cr.query(
                Template.CONTENT_URI,
                new String[] {DatabaseConstants.KEY_ROWID,DatabaseConstants.KEY_PLANID},
                DatabaseConstants.KEY_PLANID + " IS NOT null",
                null,
                null);
            if (planCursor!=null && planCursor.moveToFirst()) {
              do {
                long templateId = planCursor.getLong(0);
                long planId = planCursor.getLong(1);
                Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, planId);
                Cursor eventCursor = cr.query(
                    eventUri,
                    new String[]{
                        Events.DTSTART,
                        Events.DTEND,
                        Events.RRULE,
                        Events.TITLE,
                        Events.ALL_DAY,
                        Events.EVENT_TIMEZONE,
                        Events.DURATION,
                        Events.DESCRIPTION},
                    Events.CALENDAR_ID + " = ?",
                    new String[] {oldValue},
                    null);
                if (eventCursor != null && eventCursor.moveToFirst()) {
                  //Log.i("DEBUG", DatabaseUtils.dumpCursorToString(eventCursor));
                  eventValues.put(Events.DTSTART, DbUtils.getLongOrNull(eventCursor,0));
                  eventValues.put(Events.DTEND, DbUtils.getLongOrNull(eventCursor,1));
                  eventValues.put(Events.RRULE, eventCursor.getString(2));
                  eventValues.put(Events.TITLE, eventCursor.getString(3));
                  eventValues.put(Events.ALL_DAY,eventCursor.getInt(4));
                  eventValues.put(Events.EVENT_TIMEZONE, eventCursor.getString(5));
                  eventValues.put(Events.DURATION, eventCursor.getString(6));
                  eventValues.put(Events.DESCRIPTION, eventCursor.getString(7));
                  uri = cr.insert(Events.CONTENT_URI, eventValues);
                  planId = ContentUris.parseId(uri);
                  Log.i(TAG,"copied event from old to new" + planId);
                  planValues.put(DatabaseConstants.KEY_PLANID, planId);
                  int updated = cr.update(ContentUris.withAppendedId(Template.CONTENT_URI, templateId), planValues, null, null);
                  Log.i(TAG,"updated plan id in template:" + updated);
                  int deleted = cr.delete(eventUri,
                      null,
                      null);
                  Log.i(TAG,"deleted old event: " + deleted);
                }
                eventCursor.close();
              } while (planCursor.moveToNext());
            }
            planCursor.close();
          }
        } else {
          SharedPreferencesCompat.apply(
              sharedPreferences.edit().remove(PREFKEY_PLANNER_CALENDAR_PATH));
        }
      }
    }
    class WidgetObserver extends ContentObserver {
      /**
       * 
       */
      private Class<? extends AbstractWidget<?>> mProvider;

      WidgetObserver(Class<? extends AbstractWidget<?>> provider) {
          super(null);
          mProvider = provider;
      }

      @Override
      public void onChange(boolean selfChange) {
          super.onChange(selfChange);
          AbstractWidget.updateWidgets(mSelf,mProvider);
      }
    }
}