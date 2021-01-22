package org.totschnig.myexpenses.util;

import android.content.Context;
import android.text.TextUtils;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

public class BackupUtils {
  public static final String BACKUP_DB_FILE_NAME = "BACKUP";
  public static final String BACKUP_PREF_FILE_NAME = "BACKUP_PREF";

  @NonNull
  public static Result<DocumentFile> doBackup(String password, Context context) {
    if (!AppDirHelper.isExternalStorageAvailable()) {
      return Result.ofFailure(R.string.external_storage_unavailable);
    }
    DocumentFile appDir = AppDirHelper.getAppDir(context);
    if (appDir == null) {
      return Result.ofFailure(R.string.io_error_appdir_null);
    }
    if (!AppDirHelper.isWritableDirectory(appDir)) {
      return Result.ofFailure(R.string.app_dir_not_accessible, null,
          FileUtils.getPath(context, appDir.getUri()));
    }

    DocumentFile backupFile = requireBackupFile(appDir, !TextUtils.isEmpty(password));
    if (backupFile == null) {
      return Result.ofFailure(R.string.io_error_backupdir_null);
    }
    File cacheDir = AppDirHelper.getCacheDir();
    if (cacheDir == null) {
      CrashHandler.report(context.getString(R.string.io_error_cachedir_null));
      return Result.ofFailure(R.string.io_error_cachedir_null);
    }
    Result result = DbUtils.backup(cacheDir);
    String failureMessage;
    if (result.isSuccess()) {
      try {
        ZipUtils.zipBackup(
            cacheDir,
            backupFile, password);
        return Result.ofSuccess(R.string.backup_success, backupFile);
      } catch (IOException | GeneralSecurityException e) {
        CrashHandler.report(e);
        failureMessage = e.getMessage();
      } finally {
        getBackupDbFile(cacheDir).delete();
        getBackupPrefFile(cacheDir).delete();
      }
    } else {
      failureMessage = result.print(context);
    }
    return Result.ofFailure(context.getString(R.string.backup_failure,
        FileUtils.getPath(context, backupFile.getUri())) + " " + failureMessage);
  }

  private static DocumentFile requireBackupFile(@NonNull DocumentFile appDir, boolean encrypted) {
    return AppDirHelper.timeStampedFile(appDir, "backup",
        encrypted ? "application/octet-stream" : "application/zip", encrypted ? "enc" : null);
  }

  public static File getBackupDbFile(File backupDir) {
    return new File(backupDir, BACKUP_DB_FILE_NAME);
  }

  public static File getBackupPrefFile(File backupDir) {
    return new File(backupDir, BACKUP_PREF_FILE_NAME);
  }
}
