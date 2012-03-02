package org.totschnig.myexpenses;

import java.io.File;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class MyApplication extends Application {
    private SharedPreferences settings;
    private String databaseName;
    private ExpensesDbAdapter mDbOpenHelper;
    private static MyApplication mSelf;
    public static final String BACKUP_PREF_PATH = "BACKUP_PREF";

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
      if (mSelf.mDbOpenHelper.backup()) {
        File appDir = Utils.requireAppDir();
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
              //if we found a database to restore, we also try to import corresponding preferences
              File appDir = Utils.requireAppDir();
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
                }
              } else {
                Log.w("MyExpenses","Did not find backup for preferences");
              }
            }
          }
          mSelf.mDbOpenHelper.open();
      }
      return mSelf.mDbOpenHelper;
  }
}