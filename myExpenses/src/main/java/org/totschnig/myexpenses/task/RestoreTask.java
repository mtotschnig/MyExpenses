package org.totschnig.myexpenses.task;

import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.calendar.CalendarContractCompat.Calendars;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionDatabase;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.BackupUtils;
import org.totschnig.myexpenses.util.PictureDirHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.ZipUtils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.io.FileCopyUtils;
import org.totschnig.myexpenses.util.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import timber.log.Timber;

import static org.totschnig.myexpenses.MyApplication.INVALID_CALENDAR_ID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;

public class RestoreTask extends AsyncTask<Void, Result, Result> {
  public static final String KEY_BACKUP_FROM_SYNC = "backupFromSync";
  public static final String KEY_RESTORE_PLAN_STRATEGY = "restorePlanStrategy";
  public static final String KEY_PASSWORD = "passwordEncryption";
  private final TaskExecutionFragment taskExecutionFragment;
  private int restorePlanStrategy;
  private Uri fileUri;
  private String syncAccountName, backupFromSync, password;

  RestoreTask(TaskExecutionFragment taskExecutionFragment, Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.fileUri = b.getParcelable(TaskExecutionFragment.KEY_FILE_PATH);
    if (fileUri == null) {
      this.syncAccountName = b.getString(KEY_SYNC_ACCOUNT_NAME);
      this.backupFromSync = b.getString(KEY_BACKUP_FROM_SYNC);
    }
    this.restorePlanStrategy = b.getInt(KEY_RESTORE_PLAN_STRATEGY);
    this.password = b.getString(KEY_PASSWORD);
  }

  @Override
  protected void onProgressUpdate(Result... values) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onProgressUpdate(values[0]);
    }
  }

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
    final MyApplication application = MyApplication.getInstance();
    ContentResolver cr = application.getContentResolver();
    workingDir = AppDirHelper.getCacheDir();
    if (workingDir == null) {
      return Result.ofFailure(R.string.external_storage_unavailable);
    }
    try {
      InputStream is;
      SyncBackendProvider syncBackendProvider;
      boolean isEncrypted;
      if (syncAccountName != null) {
        android.accounts.Account account = GenericAccountService.getAccount(syncAccountName);
        try {
          syncBackendProvider = SyncBackendProviderFactory.get(MyApplication.getInstance(), account, false)
              .getOrThrow();
        } catch (Throwable throwable) {
          String errorMessage = String.format("Unable to get sync backend provider for %s",
              syncAccountName);
          CrashHandler.report(new Exception(errorMessage, throwable));
          return Result.ofFailure(errorMessage);
        }
        try {
          is = syncBackendProvider.getInputStreamForBackup(backupFromSync);
          isEncrypted = backupFromSync.endsWith("enc");
        } catch (IOException e) {
          return Result.ofFailure(e.getMessage());
        }
      } else {
        is = cr.openInputStream(fileUri);
        isEncrypted = FileUtils.getPath(application, fileUri).endsWith("enc");
      }
      if (is == null) {
        return Result.ofFailure("Unable to open backup file");
      }
      if (isEncrypted) {
        if (TextUtils.isEmpty(password)) {
          return Result.ofFailure(R.string.backup_is_encrypted);
        }
      }
      try {
        ZipUtils.unzip(is, workingDir, isEncrypted ? password : null);
      } catch (IOException e) {
        return e.getCause() instanceof GeneralSecurityException ?
            Result.ofFailure(R.string.backup_wrong_password) :
            Result.ofFailure(R.string.restore_backup_archive_not_valid, fileUri);
      } finally {
        try {
          is.close();
        } catch (IOException e) {
          Timber.e(e);
        }

      }
    } catch (FileNotFoundException | SecurityException | GeneralSecurityException e) {
      Map<String, String> customData = new HashMap<>();
      customData.put("fileUri", fileUri != null ? fileUri.toString() : "null");
      customData.put("syncAccountName", syncAccountName);
      customData.put("backupFromSync", backupFromSync);
      CrashHandler.report(e, customData);
      return Result.ofFailure(
          R.string.parse_error_other_exception,
          e.getMessage());
    }

    File backupFile = BackupUtils.getBackupDbFile(workingDir);
    File backupPrefFile = BackupUtils.getBackupPrefFile(workingDir);
    if (!backupFile.exists()) {
      return Result.ofFailure(
          R.string.restore_backup_file_not_found,
          BackupUtils.BACKUP_DB_FILE_NAME, workingDir);
    }
    if (!backupPrefFile.exists()) {
      return Result.ofFailure(
          R.string.restore_backup_file_not_found,
          BackupUtils.BACKUP_PREF_FILE_NAME, workingDir);
    }

    //peek into file to inspect version
    try {
      SQLiteDatabase db = SQLiteDatabase.openDatabase(
          backupFile.getPath(),
          null,
          SQLiteDatabase.OPEN_READONLY);
      int version = db.getVersion();
      if (version > TransactionDatabase.DATABASE_VERSION) {
        db.close();
        return Result.ofFailure(
            R.string.restore_cannot_downgrade,
            version, TransactionDatabase.DATABASE_VERSION);
      }
      db.close();
    } catch (SQLiteException e) {
      return Result.ofFailure(R.string.restore_db_not_valid);
    }

    //peek into preferences to see if there is a calendar configured
    File internalAppDir = application.getFilesDir().getParentFile();
    File sharedPrefsDir = new File(internalAppDir.getPath() + "/shared_prefs/");
    sharedPrefsDir.mkdir();
    if (!sharedPrefsDir.isDirectory()) {
      CrashHandler.report(
          String.format(Locale.US, "Could not access shared preferences directory at %s",
              sharedPrefsDir.getAbsolutePath()));
      return Result.ofFailure(R.string.restore_preferences_failure);
    }
    File tempPrefFile = new File(sharedPrefsDir, "backup_temp.xml");
    if (!FileCopyUtils.copy(backupPrefFile, tempPrefFile)) {
      CrashHandler.report(
          new Exception("Preferences restore failed"),
          "FAILED_COPY_OPERATION",
          String.format("%s => %s",
              backupPrefFile.getAbsolutePath(),
              tempPrefFile.getAbsolutePath()));
      return Result.ofFailure(R.string.restore_preferences_failure);
    }
    SharedPreferences backupPref =
        application.getSharedPreferences("backup_temp", 0);
    if (restorePlanStrategy == R.id.restore_calendar_handling_configured) {
      currentPlannerId = application.checkPlanner();
      currentPlannerPath = PrefKey.PLANNER_CALENDAR_PATH.getString("");
      if (INVALID_CALENDAR_ID.equals(currentPlannerId)) {
        return Result.ofFailure(R.string.restore_not_possible_local_calendar_missing);
      }
    } else if (restorePlanStrategy == R.id.restore_calendar_handling_backup) {
      boolean found = false;
      String calendarId = backupPref
          .getString(PrefKey.PLANNER_CALENDAR_ID.getKey(), "-1");
      String calendarPath = backupPref
          .getString(PrefKey.PLANNER_CALENDAR_PATH.getKey(), "");
      if (!(calendarId.equals("-1") || calendarPath.equals(""))) {
        Cursor c;
        try {
          c = cr.query(Calendars.CONTENT_URI,
              new String[]{Calendars._ID},
              MyApplication.getCalendarFullPathProjection() + " = ?",
              new String[]{calendarPath},
              null);
        } catch (SecurityException e) {
          return Result.ofFailure(e.getMessage());
        }
        if (c != null) {
          if (c.moveToFirst()) {
            found = true;
          }
          c.close();
        }
      }
      if (!found) {
        return Result.ofFailure(
            R.string.restore_not_possible_target_calendar_missing,
            calendarPath);
      }
    }

    if (DbUtils.restore(backupFile)) {
      publishProgress(Result.ofSuccess(R.string.restore_db_success));

      //since we already started reading settings, we can not just copy the file
      //unless I found a way
      //either to close the shared preferences and read it again
      //or to find out if we are on a new install without reading preferences
      //
      //we open the backup file and read every entry
      //getSharedPreferences does not allow to access file if it not in private data directory
      //hence we copy it there first
      //upon application install does not exist yet

      application.getSettings()
          .unregisterOnSharedPreferenceChangeListener(application);

      Editor edit = application.getSettings().edit();
      for (Map.Entry<String, ?> entry : application.getSettings().getAll().entrySet()) {
        String key = entry.getKey();
        if (!key.equals(PrefKey.NEW_LICENCE.getKey()) && !key.equals(PrefKey.LICENCE_EMAIL.getKey())
            && !key.startsWith("acra") && !key.equals(PrefKey.FIRST_INSTALL_VERSION.getKey())) {
          edit.remove(key);
        }
      }

      for (Map.Entry<String, ?> entry : backupPref.getAll().entrySet()) {
        String key = entry.getKey();
        if (key.equals(PrefKey.LICENCE_LEGACY.getKey()) || key.equals(PrefKey.FIRST_INSTALL_VERSION.getKey())) {
          continue;
        }
        Object val = entry.getValue();
        if (val == null) {
          Timber.i("Found: %s null", key);
          continue;
        }
        if (val.getClass() == Long.class) {
          edit.putLong(key, backupPref.getLong(key, 0));
        } else if (val.getClass() == Integer.class) {
          edit.putInt(key, backupPref.getInt(key, 0));
        } else if (val.getClass() == String.class) {
          edit.putString(key, backupPref.getString(key, ""));
        } else if (val.getClass() == Boolean.class) {
          edit.putBoolean(key, backupPref.getBoolean(key, false));
        } else {
          Timber.i("Found: %s of type %s", key, val.getClass().getName());
        }
      }

      if (restorePlanStrategy == R.id.restore_calendar_handling_configured) {
        edit.putString(PrefKey.PLANNER_CALENDAR_PATH.getKey(), currentPlannerPath);
        edit.putString(PrefKey.PLANNER_CALENDAR_ID.getKey(), currentPlannerId);
      } else if (restorePlanStrategy == R.id.restore_calendar_handling_ignore) {
        edit.remove(PrefKey.PLANNER_CALENDAR_PATH.getKey());
        edit.remove(PrefKey.PLANNER_CALENDAR_ID.getKey());
      }

      edit.apply();
      application.getSettings()
          .registerOnSharedPreferenceChangeListener(application);
      tempPrefFile.delete();
      if (fileUri != null) {
        backupFile.delete();
        backupPrefFile.delete();
      }
      publishProgress(Result.ofSuccess(R.string.restore_preferences_success));
      //if a user restores a backup we do not want past plan instances to flood the database
      PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP
          .putLong(System.currentTimeMillis());
      //now handling plans
      if (restorePlanStrategy == R.id.restore_calendar_handling_ignore) {
        //we remove all links to plans we did not restore
        ContentValues planValues = new ContentValues();
        planValues.putNull(DatabaseConstants.KEY_PLANID);
        cr.update(Template.CONTENT_URI,
            planValues, null, null);
      } else {
        publishProgress(application.restorePlanner());
      }
      Timber.i("now emptying event cache");
      cr.delete(
          TransactionProvider.EVENT_CACHE_URI, null, null);

      //now handling pictures
      //1.stale uris in the backup can be ignored1
      //delete from db
      cr.delete(
          TransactionProvider.STALE_IMAGES_URI, null, null);
      //2. all images that are left over in external and
      //internal picture dir are now stale
      registerAsStale(false);
      registerAsStale(true);

      //3. move pictures home and update uri
      File backupPictureDir = new File(workingDir, ZipUtils.PICTURES);
      Cursor c = cr.query(TransactionProvider.TRANSACTIONS_URI,
          new String[]{KEY_ROWID, DatabaseConstants.KEY_PICTURE_URI, DatabaseConstants.KEY_ACCOUNTID},
          DatabaseConstants.KEY_PICTURE_URI + " IS NOT NULL", null, null);
      if (c == null)
        return Result.ofFailure(R.string.restore_db_failure);
      if (c.moveToFirst()) {
        do {
          ContentValues uriValues = new ContentValues();
          String[] rowId = new String[] {c.getString(0)};
          String[] accountId = new String[] {c.getString(2)};
          Uri fromBackup = Uri.parse(c.getString(1));
          String fileName = fromBackup.getLastPathSegment();
          File backupImage = new File(backupPictureDir, fileName);
          Uri restored = null;
          if (backupImage.exists()) {
            File restoredImage = PictureDirHelper.getOutputMediaFile(
                fileName.substring(0, fileName.lastIndexOf('.')), false, true);
            if (restoredImage == null || !FileCopyUtils.copy(backupImage, restoredImage)) {
              CrashHandler.report(String.format("Could not restore file %s from backup", fromBackup.toString()));
            } else {
              restored = AppDirHelper.getContentUriForFile(restoredImage);
            }
          } else {
            CrashHandler.report(String.format("Could not restore file %s from backup", fromBackup.toString()));
          }
          if (restored != null) {
            uriValues.put(DatabaseConstants.KEY_PICTURE_URI, restored.toString());
          } else {
            uriValues.putNull(DatabaseConstants.KEY_PICTURE_URI);
          }
          ArrayList<ContentProviderOperation> ops = new ArrayList<>();
          try {
            ops.add(ContentProviderOperation.newUpdate(Account.CONTENT_URI).withValue(KEY_SEALED, -1)
                .withSelection(KEY_SEALED + " = 1 AND " + KEY_ROWID + " = ?", accountId).build());
            ops.add(ContentProviderOperation.newUpdate(TransactionProvider.TRANSACTIONS_URI)
                .withValues(uriValues).withSelection(KEY_ROWID + " = ?", rowId)
                .build());
            ops.add(ContentProviderOperation.newUpdate(Account.CONTENT_URI).withValue(KEY_SEALED, 1)
                .withSelection(KEY_SEALED + " = -1 AND " + KEY_ROWID + " = ?", accountId).build());
            cr.applyBatch(TransactionProvider.AUTHORITY, ops);
          } catch (OperationApplicationException | RemoteException e) {
            CrashHandler.report(e);
          }
        } while (c.moveToNext());
      }
      c.close();
      Result restoreSyncStateResult = restoreSyncState();
      if (restoreSyncStateResult != null) {
        publishProgress(restoreSyncStateResult);
      }
      return Result.SUCCESS;
    } else {
      return Result.ofFailure(R.string.restore_db_failure);
    }
  }

  @Nullable
  private Result restoreSyncState() {
    Result result = null;
    MyApplication application = MyApplication.getInstance();
    AccountManager accountManager = AccountManager.get(application);
    List<String> accounts = Arrays.asList(GenericAccountService.getAccountNames(application));
    ContentResolver cr = application.getContentResolver();
    String[] projection = {KEY_ROWID, KEY_SYNC_ACCOUNT_NAME};
    Cursor cursor = cr.query(TransactionProvider.ACCOUNTS_URI, projection,
        KEY_SYNC_ACCOUNT_NAME + " IS NOT null", null, null);
    SharedPreferences sharedPreferences = application.getSettings();
    Editor editor = sharedPreferences.edit();
    if (cursor != null) {
      if (cursor.moveToFirst()) {
        int restored = 0, failed = 0;
        do {
          long accountId = cursor.getLong(0);
          String accountName = cursor.getString(1);
          String localKey = SyncAdapter.KEY_LAST_SYNCED_LOCAL(accountId);
          String remoteKey = SyncAdapter.KEY_LAST_SYNCED_REMOTE(accountId);
          if (accounts.contains(accountName)) {
            android.accounts.Account account = GenericAccountService.getAccount(accountName);
            accountManager.setUserData(account, localKey, sharedPreferences.getString(localKey, null));
            accountManager.setUserData(account, remoteKey, sharedPreferences.getString(remoteKey, null));
            restored++;
          } else {
            failed++;
          }
          editor.remove(localKey);
          editor.remove(remoteKey);
        } while (cursor.moveToNext());
        editor.apply();
        String message = "";
        if (restored > 0) {
          message += application.getString(R.string.sync_state_restored, restored);
        }
        if (failed > 0) {
          message += application.getString(R.string.sync_state_could_not_be_restored, failed);
        }
        result = Result.ofSuccess(message);
        Account.checkSyncAccounts(application);
      }
      cursor.close();
    }
    return result;
  }

  private void registerAsStale(boolean secure) {
    File dir = PictureDirHelper.getPictureDir(secure);
    if (dir == null) return;
    final File[] files = dir.listFiles();
    if (files == null) return;
    ContentValues values = new ContentValues();
    for (File file : files) {
      Uri uri = secure ? FileProvider.getUriForFile(MyApplication.getInstance(),
          "org.totschnig.myexpenses.fileprovider", file) :
          Uri.fromFile(file);
      values.put(DatabaseConstants.KEY_PICTURE_URI, uri.toString());
      MyApplication.getInstance().getContentResolver().insert(
          TransactionProvider.STALE_IMAGES_URI, values);
    }
  }
}
