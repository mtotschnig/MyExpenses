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

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class MyApplication extends Application {
    private SharedPreferences settings;
    private String databaseName;
    private ExpensesDbAdapter mDbOpenHelper;
    private static MyApplication mSelf;
    public static final String BACKUP_PREF_PATH = "BACKUP_PREF";
    public static final String CURRENCY_USE_MINOR_UNIT = "x";
    public static String PREFKEY_CATEGORIES_SORT_BY_USAGES;
    public static String PREFKEY_USE_STANDARD_MENU;
    public static String PREFKEY_PERFORM_SHARE;
    public static String PREFKEY_SHARE_TARGET;
    public static String PREFKEY_CURRENCY_DECIMAL_SEPARATOR;
    public static String PREFKEY_CURRENT_VERSION = "currentversion";
    public static String PREFKEY_CURRENT_ACCOUNT = "current_account";
    public static String PREFKEY_LAST_ACCOUNT = "last_account";

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
      File appDir = Utils.requireAppDir();
      if (appDir == null)
         return false;
      if (mSelf.mDbOpenHelper.backup()) {
        File backupPrefFile = new File(appDir, BACKUP_PREF_PATH);
        File sharedPrefFile = new File("/data/data/" + getPackageName() 
            + "/shared_prefs/" + getPackageName() + "_preferences.xml");
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
                    edit.putInt("currentversion",backupPref.getInt("currentversion", -1));
                    edit.putLong("current_account",backupPref.getLong("current_account", 0));
                    edit.putString("share_target",backupPref.getString("share_target",""));
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