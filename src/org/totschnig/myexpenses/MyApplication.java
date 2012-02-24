package org.totschnig.myexpenses;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MyApplication extends Application {
    private SharedPreferences settings;
    private String databaseName;
    private ExpensesDbAdapter mDbOpenHelper;
    private static MyApplication mSelf;

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
      if(mSelf.mDbOpenHelper != null)
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
    public static ExpensesDbAdapter db() {
      if(mSelf.mDbOpenHelper == null) {
          mSelf.mDbOpenHelper = new ExpensesDbAdapter(mSelf);
          mSelf.mDbOpenHelper.open();
      }
      return mSelf.mDbOpenHelper;
  }
}