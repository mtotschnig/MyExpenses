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
    private SharedPreferences settings;
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
    public static String PREFKEY_EXPORT_FORMAT;
    public static String PREFKEY_SEND_FEEDBACK;
    public static String PREFKEY_MORE_INFO_DIALOG;
    public static String PREFKEY_SHORTCUT_ACCOUNT_LIST;
    public static String PREFKEY_PLANER_CALENDAR_ID;
    public static final String PREFKEY_CURRENT_VERSION = "currentversion";
    public static final String PREFKEY_CURRENT_ACCOUNT = "current_account";
    public static final String PREFKEY_PLANER_LAST_EXECUTION_TIMESTAMP = "planer_last_execution_timestamp";
    public static final String BACKUP_DB_PATH = "BACKUP";
    public static String BUILD_DATE = "";
    public static String CONTRIB_SECRET = "RANDOM_SECRET";
    public static String MARKET_PREFIX = "market://details?id=";
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
      MyApplication.passwordCheckDelayNanoSeconds = mSelf.settings.getInt(PREFKEY_PROTECTION_DELAY_SECONDS, 15) * 1000000000L;
    }

    public boolean isLocked;
    public static final String FEEDBACK_EMAIL = "myexpenses@totschnig.org";
//    public static int BACKDOOR_KEY = KeyEvent.KEYCODE_CAMERA;
    public static final String HOST = "myexpenses.totschnig.org";
    
    /**
     * we cache value of planer calendar id, so that we can handle changes in value
     */
    private String mPlanerCalendarId;

    @Override
    public void onCreate() {
      super.onCreate();
      mSelf = this;
      if (settings == null)
      {
          settings = PreferenceManager.getDefaultSharedPreferences(this);
      }
      settings.registerOnSharedPreferenceChangeListener(this);
      PREFKEY_CATEGORIES_SORT_BY_USAGES = getString(R.string.pref_categories_sort_by_usages_key);
      PREFKEY_PERFORM_SHARE = getString(R.string.pref_perform_share_key);
      PREFKEY_SHARE_TARGET = getString(R.string.pref_share_target_key);
      PREFKEY_QIF_EXPORT_FILE_ENCODING = getString(R.string.pref_qif_export_file_encoding_key);
      PREFKEY_UI_THEME_KEY = getString(R.string.pref_ui_theme_key);
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
      PREFKEY_EXPORT_FORMAT = getString(R.string.pref_export_format_key);
      PREFKEY_SEND_FEEDBACK = getString(R.string.pref_send_feedback_key);
      PREFKEY_MORE_INFO_DIALOG = getString(R.string.pref_more_info_dialog_key);
      PREFKEY_SHORTCUT_ACCOUNT_LIST = getString(R.string.pref_shortcut_account_list_key);
      PREFKEY_PLANER_CALENDAR_ID = getString(R.string.pref_planer_calendar_id_key);
      setPasswordCheckDelayNanoSeconds();
      try {
        InputStream rawResource = getResources().openRawResource(R.raw.app);
        Properties properties = new Properties();
        properties.load(rawResource);
        BUILD_DATE = properties.getProperty("build.date");
        CONTRIB_SECRET = properties.getProperty("contrib.secret");
      } catch (NotFoundException e) {
        Log.w(TAG,"Did not find raw resource");
      } catch (IOException e) {
        Log.w(TAG,"Failed to open property file");
      }
      refreshContribEnabled();
      mPlanerCalendarId = settings.getString(PREFKEY_PLANER_CALENDAR_ID, "-1");
      initPlaner();
    }

    public boolean refreshContribEnabled() {
      isContribEnabled = Utils.verifyLicenceKey(settings.getString(MyApplication.PREFKEY_ENTER_LICENCE, ""));
      return isContribEnabled;
    }
    public static MyApplication getInstance() {
      return mSelf;
    }

    public SharedPreferences getSettings()
    {
        return settings;
    }
    public void setSettings(SharedPreferences s)
    {
        settings = s;
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
    public static int getThemeId()
    {
      return mSelf.settings.getString(MyApplication.PREFKEY_UI_THEME_KEY,"dark").equals("light") ?
          R.style.ThemeLight : R.style.ThemeDark;
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
            Editor edit = mSelf.settings.edit().clear();
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
      if (settings.getBoolean(PREFKEY_PERFORM_PROTECTION, false) && System.nanoTime() - getmLastPause() > passwordCheckDelayNanoSeconds) {
        isLocked = true;
        return true;
      }
      return false;
    }
    public String requirePlaner() {
      String planerCalendarId = mPlanerCalendarId;
      ContentResolver cr = getContentResolver();
      Cursor c;
      if (!planerCalendarId.equals("-1")) {
        c = cr.query(Calendars.CONTENT_URI,
            new String[]{"1 as ignore"},
            Calendars._ID + " = ?",
            new String[] {planerCalendarId},
            null);
        if (c==null)
          planerCalendarId = "-1";
        else {
          if (c.getCount() == 0) {
            Log.i("DEBUG","configured calendar has been deleted: "+ planerCalendarId);
            planerCalendarId = "-1";
          }
          c.close();
        }
        if (planerCalendarId.equals("-1")) {
          settings.edit().remove(PREFKEY_PLANER_CALENDAR_ID).commit();
        }
        return planerCalendarId;
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        //on API below 16 a local calendar leads to crashes of the com.android.calendar app
        //hence we require users to select a different calendar in the settings
        String accountName = "Local Calendar";
        String calendarName = "MyExpensesPlaner";
        //first we check if our calendar exists already
        Uri.Builder builder = Calendars.CONTENT_URI.buildUpon();
        builder.appendQueryParameter(
            Calendars.ACCOUNT_NAME,
            accountName);
        builder.appendQueryParameter(
            Calendars.ACCOUNT_TYPE,
            CalendarContractCompat.ACCOUNT_TYPE_LOCAL);
        builder.appendQueryParameter(
            CalendarContractCompat.CALLER_IS_SYNCADAPTER,
            "true");
        Uri calendarUri = builder.build();
        c = cr.query(
            calendarUri,
            new String[] {Calendars._ID},
              Calendars.NAME +  " = ?",
            new String[]{calendarName}, null);
        if (c.moveToFirst()) {
          planerCalendarId = c.getString(0);
          Log.i("DEBUG","found preexisting calendar: "+ planerCalendarId);
          c.close();
        } else  {
          c.close();
          ContentValues values = new ContentValues();
          values.put(
              Calendars.ACCOUNT_NAME,
              accountName);
          values.put(
              Calendars.ACCOUNT_TYPE,
              CalendarContractCompat.ACCOUNT_TYPE_LOCAL);
          values.put(
              Calendars.NAME,
              calendarName);
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
            uri = cr.insert(builder.build(), values);
          } catch (IllegalArgumentException e) {
            Log.i("DEBUG","Inserting planer calendar failed, Calendar app not installed?");
            return "-1";
          }
          planerCalendarId = uri.getLastPathSegment();
          if (planerCalendarId == null) {
            Log.i("DEBUG","Inserting planer calendar failed, last path segment is null");
            return "-1";
          }
          Log.i("DEBUG","successfully set up new calendar: "+ planerCalendarId);
        }
        settings.edit().putString(PREFKEY_PLANER_CALENDAR_ID, planerCalendarId).commit();
        return planerCalendarId;
        }
      return "-1";
    }
    /**
     * call PlanExecutor, which will 
     * 1) set up the planer calendar
     * 2) execute plans
     * 3) reschedule execution through alarm 
     */
    public void initPlaner() {
      Log.i("DEBUG","Inside init planer");
      Intent service = new Intent(this, PlanExecutor.class);
      startService(service);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
        String key) {
      if (key.equals(PREFKEY_PLANER_CALENDAR_ID)) {
        Log.i("DEBUG","onSharedPreferenceChanged fired in MyApplication");
        String oldValue = mPlanerCalendarId;
        String newValue = sharedPreferences.getString(PREFKEY_PLANER_CALENDAR_ID, "-1");
        mPlanerCalendarId = newValue;
        if (oldValue == "-1" && newValue != "-1") {
         initPlaner();
        } else if (newValue != "-1") {
          ContentResolver cr = getContentResolver();
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
                  null,
                  null,
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
                Uri uri = cr.insert(Events.CONTENT_URI, eventValues);
                planId = ContentUris.parseId(uri);
                Log.i("DEBUG","copied event from old to new" + planId);
                planValues.put(DatabaseConstants.KEY_PLANID, planId);
                int updated = cr.update(ContentUris.withAppendedId(Template.CONTENT_URI, templateId), planValues, null, null);
                Log.i("DEBUG","updated plan id in template:" + updated);
                int deleted = cr.delete(eventUri, null, null);
                Log.i("DEBUG","deleted old event: " + deleted);
              }
              eventCursor.close();
            } while (planCursor.moveToNext());
          }
          planCursor.close();
        }
      }
    }
}