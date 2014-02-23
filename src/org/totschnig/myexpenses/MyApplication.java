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
import java.util.Map;
import java.util.Properties;

import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.service.PlanExecutor;
import org.totschnig.myexpenses.util.Utils;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Calendars;
import com.android.calendar.CalendarContractCompat.Events;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
//import android.view.KeyEvent;
import android.widget.Toast;

public class MyApplication extends Application implements OnSharedPreferenceChangeListener {
    public static final String PLANNER_CALENDAR_NAME = "MyExpensesPlanner";
    public static final String PLANNER_ACCOUNT_NAME = "Local Calendar";
    private SharedPreferences mSettings;
    private static MyApplication mSelf;
    public static final String BACKUP_PREF_PATH = "BACKUP_PREF";
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
    public static String PREFKEY_CONTRIB_DONATE;
    public static String PREFKEY_REQUEST_LICENCE;
    public static String PREFKEY_ENTER_LICENCE;
    public static String PREFKEY_PERFORM_PROTECTION;
    public static String PREFKEY_SET_PASSWORD;
    public static String PREFKEY_SECURITY_ANSWER;
    public static String PREFKEY_SECURITY_QUESTION;
    public static String PREFKEY_PROTECTION_DELAY_SECONDS;
    public static String PREFKEY_EXPORT_FORMAT;
    public static String PREFKEY_SEND_FEEDBACK;
    public static String PREFKEY_MORE_INFO_DIALOG;
    //public static String PREFKEY_SHORTCUT_ACCOUNT_LIST;
    public static String PREFKEY_PLANNER_CALENDAR_ID;
    private static final String PREFKEY_PLANNER_CALENDAR_PATH = "planner_calendar_path";
    public static final String PREFKEY_CURRENT_VERSION = "currentversion";
    public static final String PREFKEY_CURRENT_ACCOUNT = "current_account";
    public static final String PREFKEY_PLANNER_LAST_EXECUTION_TIMESTAMP = "planner_last_execution_timestamp";
    public static String PREFKEY_RATE;
    public static String PREFKEY_UI_LANGUAGE;
    public static final String BACKUP_DB_PATH = "BACKUP";
    public static String BUILD_DATE = "";
    public static String CONTRIB_SECRET = "RANDOM_SECRET";
    public static String MARKET_PREFIX = "market://details?id=";
    public static String CALENDAR_FULL_PATH_PROJECTION = 
        "ifnull(" + Calendars.ACCOUNT_NAME + ",'') || '/' ||" +
        "ifnull(" + Calendars.ACCOUNT_TYPE + ",'') || '/' ||" +
        "ifnull(" + Calendars.NAME + ",'') AS path";
    //public static String MARKET_PREFIX = "amzn://apps/android?p=";

    public static final boolean debug = false;
    public boolean isContribEnabled,
      showImportantUpgradeInfo = false;
    private long mLastPause = 0;
    public static String TAG = "MyExpenses";
    /**
     * how many nanoseconds should we wait before prompting for the password
     */
    public static long passwordCheckDelayNanoSeconds;
    public static void setPasswordCheckDelayNanoSeconds() {
      MyApplication.passwordCheckDelayNanoSeconds = mSelf.mSettings.getInt(PREFKEY_PROTECTION_DELAY_SECONDS, 15) * 1000000000L;
    }

    public boolean isLocked;
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

    @Override
    public void onCreate() {
      super.onCreate();
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
      PREFKEY_CONTRIB_DONATE = getString(R.string.pref_contrib_donate_key);
      PREFKEY_REQUEST_LICENCE = getString(R.string.pref_request_licence_key);
      PREFKEY_ENTER_LICENCE = getString(R.string.pref_enter_licence_key);
      PREFKEY_PERFORM_PROTECTION = getString(R.string.pref_perform_protection_key);
      PREFKEY_SET_PASSWORD = getString(R.string.pref_set_password_key);
      PREFKEY_SECURITY_ANSWER = getString(R.string.pref_security_answer_key);
      PREFKEY_SECURITY_QUESTION = getString(R.string.pref_security_question_key);
      PREFKEY_PROTECTION_DELAY_SECONDS = getString(R.string.pref_protection_delay_seconds_key);
      PREFKEY_EXPORT_FORMAT = getString(R.string.pref_export_format_key);
      PREFKEY_SEND_FEEDBACK = getString(R.string.pref_send_feedback_key);
      PREFKEY_MORE_INFO_DIALOG = getString(R.string.pref_more_info_dialog_key);
      //PREFKEY_SHORTCUT_ACCOUNT_LIST = getString(R.string.pref_shortcut_account_list_key);
      PREFKEY_PLANNER_CALENDAR_ID = getString(R.string.pref_planner_calendar_id_key);
      PREFKEY_RATE = getString(R.string.pref_rate_key);
      PREFKEY_UI_LANGUAGE = getString(R.string.pref_ui_language_key);
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
      refreshContribEnabled();
      mPlannerCalendarId = mSettings.getString(PREFKEY_PLANNER_CALENDAR_ID, "-1");
      initPlanner();
    }

    public boolean refreshContribEnabled() {
      isContribEnabled = Utils.verifyLicenceKey(mSettings.getString(MyApplication.PREFKEY_ENTER_LICENCE, ""));
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
    public boolean backup() {
      File appDir, backupPrefFile, sharedPrefFile;
      appDir = Utils.requireAppDir();
      if (appDir == null)
         return false;
      if (DbUtils.backup()) {
        backupPrefFile = new File(appDir, BACKUP_PREF_PATH);
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
      if (mSelf.mSettings.getString(MyApplication.PREFKEY_UI_THEME_KEY,"dark").equals("light")) {
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
    public static File getBackupDbFile() {
      File appDir = Utils.requireAppDir();
      if (appDir == null)
        return null;
      return new File(appDir, BACKUP_DB_PATH);
    }
    public static File getBackupPrefFile() {
      File appDir = Utils.requireAppDir();
      if (appDir == null)
        return null;
      return new File(appDir, BACKUP_PREF_PATH);
    }
    /**
     * is a backup avaiblable ?
     * @return
     */
    public static boolean backupExists() {
        File backupDb = getBackupDbFile();
        if (backupDb == null)
          return false;
        return backupDb.exists();
    }
    /**
     * as restores database and preferences, as a side effect calls shouldLock to set password protection
     * if necessary
     * @return true if backup successful
     */
    public static boolean backupRestore() {
      if (DbUtils.restore()) {
        Toast.makeText(mSelf, mSelf.getString(R.string.restore_db_success), Toast.LENGTH_LONG).show();
        //if we found a database to restore, we also try to import corresponding preferences
        File backupPrefFile = getBackupPrefFile();
        if (backupPrefFile != null && backupPrefFile.exists()) {
          //since we already started reading settings, we can not just copy the file
          //unless I found a way
          //either to close the shared preferences and read it again
          //or to find out if we are on a new install without reading preferences
          //
          //we open the backup file and read every entry
          //getSharedPreferences does not allow to access file if it not in private data directory
          //hence we copy it there first
          File sharedPrefsDir = new File("/data/data/" + mSelf.getPackageName()
              + "/shared_prefs/");
          //upon application install does not exist yet
          sharedPrefsDir.mkdir();
          File tempPrefFile = new File(sharedPrefsDir,"backup_temp.xml");
          if (Utils.copy(backupPrefFile,tempPrefFile)) {
            SharedPreferences backupPref = mSelf.getSharedPreferences("backup_temp",0);
            Editor edit = mSelf.mSettings.edit().clear();
            String key;
            Object val;
            for (Map.Entry<String, ?> entry : backupPref.getAll().entrySet()) {
              key = entry.getKey();
              val = entry.getValue();
              if (val.getClass() == Long.class) {
                edit.putLong(key,backupPref.getLong(key,0));
              } else if (val.getClass() == Integer.class) {
                edit.putInt(key,backupPref.getInt(key,0));
              } else if (val.getClass() == String.class) {
                edit.putString(key, backupPref.getString(key,""));
              } else if (val.getClass() == Boolean.class) {
                edit.putBoolean(key,backupPref.getBoolean(key,false));
              } else {
                Log.i(TAG,"Found: "+key+ " of type "+val.getClass().getName());
              }
            }
            SharedPreferencesCompat.apply(edit);
            backupPref = null;
            tempPrefFile.delete();
            mSelf.refreshContribEnabled();
            Toast.makeText(mSelf, mSelf.getString(R.string.restore_preferences_success), Toast.LENGTH_LONG).show();
            //if the backup is password protected, we want to force the password check
            //is it not enough to set mLastPause to zero, since it would be overwritten by the callings activity onpause
            //hence we need to set isLocked if necessary
            mSelf.mLastPause = 0;
            mSelf.shouldLock();
            return true;
          }
          else {
            Log.w(TAG,"Could not copy backup to private data directory");
          }
        } else {
          Log.w(TAG,"Did not find backup for preferences");
        }
        Toast.makeText(mSelf, mSelf.getString(R.string.restore_preferences_failure), Toast.LENGTH_LONG).show();
      }
      else {
        Toast.makeText(mSelf, mSelf.getString(R.string.restore_db_failure), Toast.LENGTH_LONG).show();
      }
      return false;
    }

    public long getmLastPause() {
      return mLastPause;
    }
    public void setmLastPause() {
      if (!isLocked)
        this.mLastPause = System.nanoTime();
    }
    /**
     * @return true if password protection is set, and
     * we have paused for at least {@link #passwordCheckDelayNanoSeconds} seconds
     * sets isLocked as a side effect
     */
    public boolean shouldLock() {
      if (mSettings.getBoolean(PREFKEY_PERFORM_PROTECTION, false) && System.nanoTime() - getmLastPause() > passwordCheckDelayNanoSeconds) {
        isLocked = true;
        return true;
      }
      return false;
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
      if (key.equals(PREFKEY_PLANNER_CALENDAR_ID)) {
        String oldValue = mPlannerCalendarId;
        boolean safeToMovePlans = true;
        String newValue = sharedPreferences.getString(PREFKEY_PLANNER_CALENDAR_ID, "-1");
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
}