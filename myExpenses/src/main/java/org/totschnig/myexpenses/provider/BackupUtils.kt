package org.totschnig.myexpenses.provider

import android.content.ContentResolver
import android.content.Context
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.AccountPreference
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ZipUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.text.SimpleDateFormat
import java.util.*

const val BACKUP_DB_FILE_NAME = "BACKUP"
const val BACKUP_PREF_FILE_NAME = "BACKUP_PREF"

fun doBackup(context: Context, password: String?, withSync: String?): Result<DocumentFile> {
    if (!AppDirHelper.isExternalStorageAvailable()) {
        return failure(context, R.string.external_storage_unavailable)
    }
    val appDir = AppDirHelper.getAppDir(context)
        ?: return failure(context, R.string.io_error_appdir_null)
    if (!AppDirHelper.isWritableDirectory(appDir)) {
        return failure(
            context, R.string.app_dir_not_accessible, FileUtils.getPath(context, appDir.uri)
        )
    }
    val backupFile = requireBackupFile(appDir, !TextUtils.isEmpty(password))
        ?: return failure(context, R.string.io_error_backupdir_null)
    val cacheDir = AppDirHelper.getCacheDir()
    if (cacheDir == null) {
        CrashHandler.report("CacheDir is null")
        return failure(context, R.string.io_error_cachedir_null)
    }
    val result = DbUtils.backup(cacheDir, context)
    val failure: Throwable
    if (result.isSuccess) {
        try {
            ZipUtils.zipBackup(
                cacheDir,
                backupFile, password
            )
            sync(context.contentResolver, withSync, backupFile)
            return Result.success(backupFile)
        } catch (e: IOException) {
            CrashHandler.report(e)
            failure = e
        } catch (e: GeneralSecurityException) {
            CrashHandler.report(e)
            failure = e
        } finally {
            getBackupDbFile(cacheDir).delete()
            getBackupPrefFile(cacheDir).delete()
        }
    } else {
        failure = Throwable(result.print(context))
    }
    return Result.failure(failure)
}

private fun sync(contentResolver: ContentResolver, backend: String?, backupFile: DocumentFile) {
    backend?.takeIf { it != AccountPreference.SYNCHRONIZATION_NONE }?.let {
        var backupFileName = backupFile.name
        if (backupFileName == null) {
            CrashHandler.report(
                String.format(
                    "Could not get name from uri %s",
                    backupFile.uri
                )
            )
            backupFileName = "backup-" + SimpleDateFormat("yyyMMdd", Locale.US)
                .format(Date())
        }
        DbUtils.storeSetting(
            contentResolver,
            SyncAdapter.KEY_UPLOAD_AUTO_BACKUP_NAME,
            backupFileName
        )
        DbUtils.storeSetting(
            contentResolver,
            SyncAdapter.KEY_UPLOAD_AUTO_BACKUP_URI,
            backupFile.uri.toString()
        )
        GenericAccountService.requestSync(backend)
    }
}

private fun <T> failure(
    context: Context,
    @StringRes resId: Int,
    vararg formatArgs: Any?
): Result<T> =
    Result.failure(Throwable(context.getString(resId, *formatArgs)))

private fun requireBackupFile(appDir: DocumentFile, encrypted: Boolean): DocumentFile? {
    return AppDirHelper.timeStampedFile(
        appDir,
        "backup",
        if (encrypted) "application/octet-stream" else "application/zip",
        if (encrypted) "enc" else null
    )
}

fun getBackupDbFile(backupDir: File?) = File(backupDir, BACKUP_DB_FILE_NAME)

fun getBackupPrefFile(backupDir: File?) = File(backupDir, BACKUP_PREF_FILE_NAME)