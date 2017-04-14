package org.totschnig.myexpenses.util;

import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DbUtils;

import java.io.File;
import java.io.IOException;

public class BackupUtils {
  public static final String BACKUP_DB_FILE_NAME = "BACKUP";
  public static final String BACKUP_PREF_FILE_NAME = "BACKUP_PREF";

  @NonNull
  public static Result doBackup() {
    MyApplication application = MyApplication.getInstance();
    if (!AppDirHelper.isExternalStorageAvailable()) {
      return new Result(false, R.string.external_storage_unavailable);
    }
    DocumentFile appDir = AppDirHelper.getAppDir(application);
    if (appDir == null) {
      return new Result(false, R.string.io_error_appdir_null);
    }
    if (!AppDirHelper.existsAndIsWritable(appDir)) {
      return new Result(false, R.string.app_dir_not_accessible,
          FileUtils.getPath(application, appDir.getUri()));
    }
    DocumentFile backupFile = requireBackupFile(appDir);
    if (backupFile == null) {
      return new Result(false, R.string.io_error_backupdir_null);
    }
    File cacheDir = AppDirHelper.getCacheDir();
    if (cacheDir == null) {
      AcraHelper.report(new Exception(
          application.getString(R.string.io_error_cachedir_null)));
      return new Result(false, R.string.io_error_cachedir_null);
    }
    Result result = DbUtils.backup(cacheDir);
    String failureMessage = application.getString(R.string.backup_failure,
        FileUtils.getPath(application, backupFile.getUri()));
    if (result.success) {
      try {
        ZipUtils.zipBackup(
            cacheDir,
            backupFile);
        return new Result(
            true,
            R.string.backup_success,
            backupFile.getUri());
      } catch (IOException e) {
        AcraHelper.report(e);
        return new Result(
            false,
            failureMessage + " " + e.getMessage());
      } finally {
        getBackupDbFile(cacheDir).delete();
        getBackupPrefFile(cacheDir).delete();
      }
    }
    return new Result(
        false,
        failureMessage + " " + result.print(application));
  }

  public static DocumentFile requireBackupFile(@NonNull DocumentFile appDir) {
    return AppDirHelper.timeStampedFile(appDir, "backup", "application/zip", false);
  }

  public static File getBackupDbFile(File backupDir) {
    return new File(backupDir, BACKUP_DB_FILE_NAME);
  }

  public static File getBackupPrefFile(File backupDir) {
    return new File(backupDir, BACKUP_PREF_FILE_NAME);
  }
}
