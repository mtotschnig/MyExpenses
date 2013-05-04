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

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources.NotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
//import android.view.KeyEvent;
import android.widget.Toast;

public class MyApplication extends Application {
    private SharedPreferences settings;
    private String databaseName;
    private ExpensesDbAdapter mDbOpenHelper;
    private static MyApplication mSelf;
    public static final String BACKUP_PREF_PATH = "BACKUP_PREF";
    public static String PREFKEY_CATEGORIES_SORT_BY_USAGES;
    public static String PREFKEY_USE_STANDARD_MENU;
    public static String PREFKEY_PERFORM_SHARE;
    public static String PREFKEY_SHARE_TARGET;
    public static String PREFKEY_QIF_EXPORT_FILE_ENCODING;
    public static String PREFKEY_CURRENCY_DECIMAL_SEPARATOR;
    public static String PREFKEY_ACCOUNT_BUTTON_BEHAVIOUR;
    public static String PREFKEY_UI_THEME_KEY;
    public static String PREFKEY_CURRENT_VERSION = "currentversion";
    public static String PREFKEY_CURRENT_ACCOUNT = "current_account";
    public static String PREFKEY_LAST_ACCOUNT = "last_account";
    public static String PREFKEY_BACKUP;
    public static String PREFKEY_RESTORE;
    public static String PREFKEY_CONTRIB_INSTALL;
    public static String PREFKEY_REQUEST_LICENCE;
    public static String PREFKEY_ENTER_LICENCE;
    public static String PREFKEY_PERFORM_PROTECTION;
    public static String PREFKEY_SET_PASSWORD;
    public static final String BACKUP_DB_PATH = "BACKUP";
    public static int currentAccountColor;
    public static String BUILD_DATE = "";
    protected static String CONTRIB_SECRET = "RANDOM_SECRET";
    public static String CONTRIB_FEATURE_EDIT_TEMPLATE = "edit_template";
    public static String CONTRIB_FEATURE_RESTORE= "restore";
    public static String CONTRIB_FEATURE_AGGREGATE = "aggregate";
    private CharSequence mVersionInfo = "";
    public boolean isContribEnabled;
    private long mLastPause = 0;
    /**
     * how many nanoseconds should we wait before prompting for the password
     */
    public static long PASSWORD_CHECK_DELAY_NANO_SECONDS = 10000000000L;
    public boolean isLocked;
//    public static int BACKDOOR_KEY = KeyEvent.KEYCODE_CAMERA;
    protected String passwordHash;

    public CharSequence getVersionInfo() {
      return mVersionInfo;
    }
    public void setVersionInfo(CharSequence mVersionInfo) {
      this.mVersionInfo = mVersionInfo;
    }
    @Override
    public void onCreate()
    {
        super.onCreate();
        mSelf = this;
        if (settings == null)
        {
            settings = PreferenceManager.getDefaultSharedPreferences(this);
        }
        if (databaseName == null) {
          databaseName = "data";
        }

        PREFKEY_CATEGORIES_SORT_BY_USAGES = getString(R.string.pref_categories_sort_by_usages_key);
        PREFKEY_USE_STANDARD_MENU = getString(R.string.pref_use_standard_menu_key);
        PREFKEY_PERFORM_SHARE = getString(R.string.pref_perform_share_key);
        PREFKEY_SHARE_TARGET = getString(R.string.pref_share_target_key);
        PREFKEY_CURRENCY_DECIMAL_SEPARATOR = getString(R.string.pref_currency_decimal_separator_key);
        PREFKEY_ACCOUNT_BUTTON_BEHAVIOUR = getString(R.string.pref_account_button_behaviour_key);
        PREFKEY_QIF_EXPORT_FILE_ENCODING = getString(R.string.pref_qif_export_file_encoding_key);
        PREFKEY_UI_THEME_KEY = getString(R.string.pref_ui_theme_key);
        PREFKEY_BACKUP = getString(R.string.pref_backup_key);
        PREFKEY_RESTORE = getString(R.string.pref_restore_key);
        PREFKEY_CONTRIB_INSTALL = getString(R.string.pref_contrib_install_key);
        PREFKEY_REQUEST_LICENCE = getString(R.string.pref_request_licence_key);
        PREFKEY_ENTER_LICENCE = getString(R.string.pref_enter_licence_key);
        PREFKEY_PERFORM_PROTECTION = getString(R.string.pref_perform_protection_key);
        PREFKEY_SET_PASSWORD = getString(R.string.pref_set_password_key);
        passwordHash = settings.getString(PREFKEY_SET_PASSWORD,"");
        //mDbOpenHelper = db();
        try {
          InputStream rawResource = getResources().openRawResource(R.raw.app);
          Properties properties = new Properties();
          properties.load(rawResource);
          BUILD_DATE = properties.getProperty("build.date");
          CONTRIB_SECRET = properties.getProperty("contrib.secret");
        } catch (NotFoundException e) {
          Log.w("MyExpenses","Did not find raw resource");
        } catch (IOException e) {
          Log.w("MyExpenses","Failed to open property file");
        }
        refreshContribEnabled();
    }
    public boolean refreshContribEnabled() {
      isContribEnabled = Utils.doesPackageExist(this, "org.totschnig.myexpenses.contrib") ||
          Utils.verifyLicenceKey(settings.getString(MyApplication.PREFKEY_ENTER_LICENCE, ""));
      return isContribEnabled;
    }
    public static void setCurrentAccountColor(int currentAccountColor) {
      MyApplication.currentAccountColor = currentAccountColor;
    }
    public static int getCurrentAccountColor() {
      return currentAccountColor;
    }
    public static void updateUIWithAppColor(Activity ctx) {
      updateUIWithColor(ctx,ctx.getResources().getColor(R.color.appDefault));
    }
    public static void updateUIWithAccountColor(Activity ctx) {
      updateUIWithColor(ctx,currentAccountColor);
    }
    public static void updateUIWithColor(Activity ctx,int color) {
      int textColor = Utils.getTextColorForBackground(color);
      View heading = ctx.getWindow().findViewById(android.R.id.title);
      ((TextView) heading).setTextColor(textColor);
      ((View) heading.getParent()).setBackgroundColor(color);
      View divider = ctx.findViewById(R.id.ButtonBarDividerTop);
      if (divider != null) {
        divider.setBackgroundColor(color);
        ctx.findViewById(R.id.ButtonBarDividerBottom).setBackgroundColor(color);
      }
    }
    @Override
    public void onTerminate() {
      if(mDbOpenHelper != null)
        mDbOpenHelper.close();
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
    public String getDatabaseName() {
      return databaseName;
    }
    public void setDatabaseName(String s) {
      databaseName = s;
    }
    public boolean backup() {
      File appDir, backupPrefFile, sharedPrefFile;
      appDir = Utils.requireAppDir();
      if (appDir == null)
         return false;
      if (mSelf.mDbOpenHelper.backup()) {
        backupPrefFile = new File(appDir, BACKUP_PREF_PATH);
        //Samsung has special path on some devices
        //http://stackoverflow.com/questions/5531289/copy-the-shared-preferences-xml-file-from-data-on-samsung-device-failed
        String sharedPrefFileCommon = getPackageName() 
            + "/shared_prefs/" + getPackageName() + "_preferences.xml";
        sharedPrefFile = new File("/dbdata/databases/" + sharedPrefFileCommon);
        if (!sharedPrefFile.exists()) {
          sharedPrefFile = new File("/data/data/" + sharedPrefFileCommon);
          if (!sharedPrefFile.exists()) {
            Log.e("MyExpenses","Unable to determine path to shared preference file");
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

    public static int getThemeIdNoTitle() {
      return mSelf.settings.getString(MyApplication.PREFKEY_UI_THEME_KEY,"dark").equals("light") ?
          R.style.ThemeLight_NoTitle : R.style.ThemeDark_NoTitle;
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
    public boolean restoreDb() {
      boolean result = false;
      try {
        Account.accounts.clear();
        PaymentMethod.methods.clear();
        File dataDir = new File("/data/data/"+ getPackageName()+ "/databases/");
        dataDir.mkdir();
        File backupDb = getBackupDbFile();
        if (backupDb == null)
          return false;
        //line below gives app_databases instead of databases ???
        //File currentDb = new File(mCtx.getDir("databases", 0),mDatabaseName);
        File currentDb = new File(dataDir,databaseName);

        if (backupDb.exists()) {
          mDbOpenHelper.close();
          result = Utils.copy(backupDb,currentDb);
          mDbOpenHelper.open();
        }
      } catch (Exception e) {
        Log.e("MyExpenses",e.getLocalizedMessage());
      }
      return result;
    }
    public static void backupRestore() {
      if (mSelf.restoreDb()) {
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
            Editor edit = mSelf.settings.edit();
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
                Log.i("MyExpenses","Found: "+key+ " of type "+val.getClass().getName());
              }
            }
            edit.commit();
            backupPref = null;
            tempPrefFile.delete();
            Toast.makeText(mSelf, mSelf.getString(R.string.restore_preferences_success), Toast.LENGTH_LONG).show();
            return;
          }
          else {
            Log.w("MyExpenses","Could not copy backup to private data directory");
          }
        } else {
          Log.w("MyExpenses","Did not find backup for preferences");
        }
        Toast.makeText(mSelf, mSelf.getString(R.string.restore_preferences_failure), Toast.LENGTH_LONG).show();
      }
      else {
        Toast.makeText(mSelf, mSelf.getString(R.string.restore_db_failure), Toast.LENGTH_LONG).show();
      }
    }
    /**
     * @return the opened DB Adapter, if we find a backup, we return null 
     * to give the activity the chance to prompt the user for confirmation of restore
     */
    public static ExpensesDbAdapter db() {
      if(mSelf.mDbOpenHelper == null) {
        mSelf.mDbOpenHelper = new ExpensesDbAdapter(mSelf);
        mSelf.mDbOpenHelper.open();
      }
      return mSelf.mDbOpenHelper;
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
     * we have paused for at least {@link #PASSWORD_CHECK_DELAY_NANO_SECONDS} seconds
     * sets isLocked as a side effect
     */
    public boolean shouldLock() {
      if (settings.getBoolean(getString(R.string.pref_perform_protection_key), false) && System.nanoTime() - getmLastPause() > PASSWORD_CHECK_DELAY_NANO_SECONDS) {
        isLocked = true;
        return true;
      }
      return false;
    }
}