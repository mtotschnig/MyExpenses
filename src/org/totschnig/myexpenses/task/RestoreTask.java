package org.totschnig.myexpenses.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.MyApplication.PrefKey;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionDatabase;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.ZipUtils;

import com.android.calendar.CalendarContractCompat.Calendars;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class RestoreTask extends AsyncTask<Void, Result, Result> {
  public static final String KEY_DIR_NAME_LEGACY = "dirNameLegacy";
  private final TaskExecutionFragment taskExecutionFragment;
  private int restorePlanStrategy;
  Uri fileUri;
  String dirNameLegacy;
  public RestoreTask(TaskExecutionFragment taskExecutionFragment,Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.fileUri = b.getParcelable(TaskExecutionFragment.KEY_FILE_PATH);
    if (fileUri == null) {
      this.dirNameLegacy = b.getString(KEY_DIR_NAME_LEGACY);
    }
    this.restorePlanStrategy = b.getInt(
        BackupRestoreActivity.KEY_RESTORE_PLAN_STRATEGY);
  }
  /*
   * (non-Javadoc) shows toast about success or failure
   * 
   * @see android.os.AsyncTask#onProgressUpdate(Progress[])
   */
  @Override
  protected void onProgressUpdate(Result... values) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onProgressUpdate(values[0]);
    }
  }

  /*
   * (non-Javadoc) reports on success triggering restart if needed
   * 
   * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
   */
  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_RESTORE, result);
    }
  }
  @Override
  protected Result doInBackground(Void... ignored) {
    File workingDir;
    String currentPlannerId = null, currentPlannerPath = null;
    ContentResolver cr = MyApplication.getInstance().getContentResolver();
    if (fileUri != null) {
      workingDir = Utils.getCacheDir();
      if (workingDir == null) {
        return new Result(false,R.string.external_storage_unavailable);
      }
      workingDir.mkdir();
      try {
        InputStream is = cr.openInputStream(fileUri);
        boolean zipResult = ZipUtils.unzip(is,workingDir);
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        if (!zipResult) {
          return new Result(
              false,
              R.string.restore_backup_archive_not_valid,
              fileUri.getPath());
        }
      } catch (FileNotFoundException e) {
        return new Result(
            false,
            R.string.restore_backup_archive_not_valid,
            fileUri.getPath());
      }
    } else {
      workingDir = new File(Utils.requireAppDir(),dirNameLegacy);
    }
    File backupFile = MyApplication.getBackupDbFile(workingDir);
    File backupPrefFile = MyApplication.getBackupPrefFile(workingDir);
    if (backupFile == null || !backupFile.exists()) {
      return new Result(
          false,
          R.string.restore_backup_file_not_found,
          MyApplication.BACKUP_DB_FILE_NAME,workingDir);
    }
    if (backupPrefFile == null || !backupPrefFile.exists()) {
      return new Result(
          false,
          R.string.restore_backup_file_not_found,
          MyApplication.BACKUP_PREF_FILE_NAME,
          workingDir);
    }

    //peek into file to inspect version
    try {
      SQLiteDatabase db = SQLiteDatabase.openDatabase(
          backupFile.getPath(),
          null,
          SQLiteDatabase.OPEN_READONLY);
      int version = db.getVersion();
      if (version>TransactionDatabase.DATABASE_VERSION) {
        db.close();
        return new Result(
            false,
            R.string.restore_cannot_downgrade,
            version,TransactionDatabase.DATABASE_VERSION);
      }
      db.close();
    } catch (SQLiteException e) {
      return new Result(false,R.string.restore_db_not_valid);
    }

    //peek into preferences to see if there is a calendar configured
    File sharedPrefsDir = new File(
        "/data/data/" + MyApplication.getInstance().getPackageName()
        + "/shared_prefs/");
    sharedPrefsDir.mkdir();
    File tempPrefFile = new File(sharedPrefsDir,"backup_temp.xml");
    if (!Utils.copy(backupPrefFile,tempPrefFile)) {
      return new Result(false,R.string.restore_preferences_failure);
    }
    SharedPreferences backupPref =
        MyApplication.getInstance().getSharedPreferences("backup_temp",0);
    if (restorePlanStrategy == R.id.restore_calendar_handling_configured) {
      currentPlannerId = MyApplication.getInstance().checkPlanner();
      currentPlannerPath = PrefKey.PLANNER_CALENDAR_PATH.getString("");
      if (currentPlannerId.equals("-1")) {
      return new Result(
          false,
          R.string.restore_not_possible_local_calendar_missing);
      }
    } else if (restorePlanStrategy == R.id.restore_calendar_handling_backup) {
      boolean found =false;
      String calendarId = backupPref
          .getString(PrefKey.PLANNER_CALENDAR_ID.getKey(),"-1");
      String calendarPath = backupPref
          .getString(PrefKey.PLANNER_CALENDAR_PATH.getKey(),"");
      if (!(calendarId.equals("-1") || calendarPath.equals(""))) {
        Cursor c = cr
            .query(
                Calendars.CONTENT_URI,
                new String[]{Calendars._ID},
                MyApplication.CALENDAR_FULL_PATH_PROJECTION  + " = ?",
                new String[] {calendarPath},
                null);
        if (c!=null && c.moveToFirst()) {
          found = true;
        }
      }
      if (!found) {
        return new Result(
            false,
            R.string.restore_not_possible_target_calendar_missing,
            calendarPath);
      }
    }
    
    if (DbUtils.restore(backupFile)) {
      publishProgress(new Result(true,R.string.restore_db_success));
      
      //since we already started reading settings, we can not just copy the file
      //unless I found a way
      //either to close the shared preferences and read it again
      //or to find out if we are on a new install without reading preferences
      //
      //we open the backup file and read every entry
      //getSharedPreferences does not allow to access file if it not in private data directory
      //hence we copy it there first
      //upon application install does not exist yet
      String oldLicenceKey = PrefKey.ENTER_LICENCE.getString("");

      MyApplication.getInstance().getSettings()
          .unregisterOnSharedPreferenceChangeListener(MyApplication.getInstance());
      Editor edit = MyApplication.getInstance().getSettings().edit().clear();
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
          Log.i(MyApplication.TAG,
              "Found: "+key+ " of type "+val.getClass().getName());
        }
      }
      if (!oldLicenceKey.equals("")) {
        edit.putString(PrefKey.ENTER_LICENCE.getKey(), oldLicenceKey);
      }
      if (restorePlanStrategy == R.id.restore_calendar_handling_configured) {
        edit.putString(PrefKey.PLANNER_CALENDAR_PATH.getKey(), currentPlannerPath);
        edit.putString(PrefKey.PLANNER_CALENDAR_ID.getKey(),currentPlannerId);
      }
      
      SharedPreferencesCompat.apply(edit);
      MyApplication.getInstance().getSettings()
        .registerOnSharedPreferenceChangeListener(MyApplication.getInstance());
      backupPref = null;
      tempPrefFile.delete();
      if (fileUri != null) {
        backupFile.delete();
        backupPrefFile.delete();
      }
      publishProgress(new Result(true,R.string.restore_preferences_success));
      //if a user restores a backup we do not want past plan instances to flood the database
      MyApplication.PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP
          .putLong(System.currentTimeMillis());
      //now handling plans
      if (restorePlanStrategy!=R.id.restore_calendar_handling_ignore) {
        publishProgress(MyApplication.getInstance().restorePlanner());
      } else {
        //we remove all links to plans we did not restore
        ContentValues planValues = new ContentValues();
        planValues.putNull(DatabaseConstants.KEY_PLANID);
        cr.update(Template.CONTENT_URI,
            planValues, null, null);
      }
      Log.i(MyApplication.TAG,"now emptying event cache");
      cr.delete(
          TransactionProvider.EVENT_CACHE_URI, null, null);
      
      //now handling pictures
      //1.step move all existing pictures to backup
      File pictureDir = Utils.getPictureDir();
      Utils.moveToBackup(pictureDir);
      //2.delete now empty dir
      pictureDir.delete();
      //3.move backup picture dir to picturedir
      new File(workingDir,Environment.DIRECTORY_PICTURES).renameTo(pictureDir);
      return new Result(true);
    } else {
      return new Result(false,R.string.restore_db_failure);
    }
  }
}
