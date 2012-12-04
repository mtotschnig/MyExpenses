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
import java.util.Map;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
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
    public static String PREFKEY_CURRENT_VERSION = "currentversion";
    public static String PREFKEY_CURRENT_ACCOUNT = "current_account";
    public static String PREFKEY_LAST_ACCOUNT = "last_account";
//    public static int BACKDOOR_KEY = KeyEvent.KEYCODE_CAMERA;

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
    }
    
    @Override
    public void onTerminate() {
      if(mDbOpenHelper != null)
        mDbOpenHelper.close();
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
    
    public static ExpensesDbAdapter db() {
      if(mSelf.mDbOpenHelper == null) {
          mSelf.mDbOpenHelper = new ExpensesDbAdapter(mSelf);
          if (mSelf.settings.getInt("currentversion", -1) == -1) {
            if (mSelf.mDbOpenHelper.maybeRestore()) {
              Toast.makeText(mSelf, mSelf.getString(R.string.restore_db_success), Toast.LENGTH_LONG).show();
              //if we found a database to restore, we also try to import corresponding preferences
              File appDir = Utils.requireAppDir();
              if (appDir != null) {
                File backupPrefFile = new File(appDir, BACKUP_PREF_PATH);
                if (backupPrefFile.exists()) {
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
                  }
                  else {
                    Log.w("MyExpenses","Could not copy backup to private data directory");
                  }
                } else {
                  Log.w("MyExpenses","Did not find backup for preferences");
                }
              } else {
                Log.w("MyExpenses","Exernal storage not available");
              }
            }
          }
          mSelf.mDbOpenHelper.open();
      }
      return mSelf.mDbOpenHelper;
  }
}