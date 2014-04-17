package org.totschnig.myexpenses.task;

import java.io.File;
import java.util.Map;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;

public class RestoreTask extends AsyncTask<String, Integer, Result> {
  private final TaskExecutionFragment taskExecutionFragment;
  public RestoreTask(TaskExecutionFragment taskExecutionFragment) {
    this.taskExecutionFragment = taskExecutionFragment;
  }
  /*
   * (non-Javadoc) shows toast about success or failure
   * 
   * @see android.os.AsyncTask#onProgressUpdate(Progress[])
   */
  protected void onProgressUpdate(Integer... values) {
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
      this.taskExecutionFragment.mCallbacks.onPostExecute(TaskExecutionFragment.TASK_RESTORE, result);
    }
  }
  @Override
  protected Result doInBackground(String... params) {
    File backupDir = new File(Utils.requireAppDir(),params[0]);
    File backupFile = MyApplication.getBackupDbFile(backupDir);
    File backupPrefFile = MyApplication.getBackupPrefFile(backupDir);
    if (backupFile == null || !backupFile.exists()) {
      return new Result(false,R.string.restore_backup_file_not_found,MyApplication.BACKUP_DB_FILE_NAME,backupDir);
    }
    if (backupPrefFile == null || !backupPrefFile.exists()) {
      return new Result(false,R.string.restore_backup_file_not_found,MyApplication.BACKUP_PREF_FILE_NAME,backupDir);
    }
    if (DbUtils.restore(backupFile)) {
      publishProgress(R.string.restore_db_success);
      
      //since we already started reading settings, we can not just copy the file
      //unless I found a way
      //either to close the shared preferences and read it again
      //or to find out if we are on a new install without reading preferences
      //
      //we open the backup file and read every entry
      //getSharedPreferences does not allow to access file if it not in private data directory
      //hence we copy it there first
      File sharedPrefsDir = new File("/data/data/" + MyApplication.getInstance().getPackageName()
          + "/shared_prefs/");
      //upon application install does not exist yet
      sharedPrefsDir.mkdir();
      File tempPrefFile = new File(sharedPrefsDir,"backup_temp.xml");
      if (Utils.copy(backupPrefFile,tempPrefFile)) {
        SharedPreferences backupPref = MyApplication.getInstance().getSharedPreferences("backup_temp",0);
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
            Log.i(MyApplication.TAG,"Found: "+key+ " of type "+val.getClass().getName());
          }
        }
        SharedPreferencesCompat.apply(edit);
        backupPref = null;
        tempPrefFile.delete();
        return new Result(true,R.string.restore_preferences_success);
      } else {
        return new Result(false,R.string.restore_preferences_failure);
      }
    } else {
      return new Result(false,R.string.restore_db_failure);
    }
  }
}
