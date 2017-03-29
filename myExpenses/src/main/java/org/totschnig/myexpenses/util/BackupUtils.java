package org.totschnig.myexpenses.util;

import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DbUtils;

import java.io.File;
import java.io.IOException;

public class BackupUtils {
  @NonNull
  public static Result doBackup() {
    if (!AppDirHelper.isExternalStorageAvailable()) {
      return new Result(false, R.string.external_storage_unavailable);
    }
    DocumentFile appDir = AppDirHelper.getAppDir();
    if (appDir == null) {
      return new Result(false, R.string.io_error_appdir_null);
    }
    if (!AppDirHelper.dirExistsAndIsWritable(appDir)) {
      return new Result(false, R.string.app_dir_not_accessible,
          FileUtils.getPath(MyApplication.getInstance(), appDir.getUri()));
    }
    DocumentFile backupFile = MyApplication.requireBackupFile(appDir);
    if (backupFile == null) {
      return new Result(false, R.string.io_error_backupdir_null);
    }
    File cacheDir = AppDirHelper.getCacheDir();
    if (cacheDir == null) {
      AcraHelper.report(new Exception(
          MyApplication.getInstance().getString(R.string.io_error_cachedir_null)));
      return new Result(false, R.string.io_error_cachedir_null);
    }
    Result result = DbUtils.backup(cacheDir);
    String failureMessage = MyApplication.getInstance().getString(R.string.backup_failure,
        FileUtils.getPath(MyApplication.getInstance(), backupFile.getUri()));
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
        MyApplication.getBackupDbFile(cacheDir).delete();
        MyApplication.getBackupPrefFile(cacheDir).delete();
      }
    }
    return new Result(
        false,
        failureMessage + " " + result.print(MyApplication.getInstance()));
  }
}
