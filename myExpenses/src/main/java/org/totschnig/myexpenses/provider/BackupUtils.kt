package org.totschnig.myexpenses.provider

import android.content.ContentResolver
import android.content.Context
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.AccountPreference
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ZipUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.failure
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.text.SimpleDateFormat
import java.util.*

const val BACKUP_DB_FILE_NAME = "BACKUP"
const val BACKUP_PREF_FILE_NAME = "BACKUP_PREF"

fun doBackup(
    context: Context,
    prefHandler: PrefHandler,
    withSync: String?
): Result<Pair<DocumentFile, List<DocumentFile>>> {
    val password = prefHandler.getString(PrefKey.EXPORT_PASSWORD, null)
    if (!AppDirHelper.isExternalStorageAvailable) {
        return Result.failure(context, R.string.external_storage_unavailable)
    }
    val appDir = AppDirHelper.getAppDir(context)
        ?: return Result.failure(context, R.string.io_error_appdir_null)
    if (!AppDirHelper.isWritableDirectory(appDir)) {
        return Result.failure(
            context, R.string.app_dir_not_accessible, FileUtils.getPath(context, appDir.uri)
        )
    }
    val backupFile = requireBackupFile(appDir, !TextUtils.isEmpty(password))
        ?: return Result.failure(context, R.string.io_error_backupdir_null)
    val cacheDir = AppDirHelper.cacheDir(context)
    return backup(cacheDir, context, prefHandler).mapCatching {
        try {
            ZipUtils.zipBackup(
                cacheDir,
                backupFile, password
            )

            sync(context.contentResolver, withSync, backupFile)
            backupFile to listOldBackups(appDir, prefHandler)
        } catch (e: IOException) {
            CrashHandler.report(e)
            throw e
        } catch (e: GeneralSecurityException) {
            CrashHandler.report(e)
            throw e
        } finally {
            getBackupDbFile(cacheDir).delete()
            getBackupPrefFile(cacheDir).delete()
        }
    }
}

fun listOldBackups(appDir: DocumentFile, prefHandler: PrefHandler): List<DocumentFile> {
    val keep = prefHandler.getInt(PrefKey.PURGE_BACKUP_KEEP, 0)
    return if (prefHandler.getBoolean(PrefKey.PURGE_BACKUP, false) && keep > 0) {
        appDir.listFiles()
            .filter {
                it.name?.matches("""backup-\d\d\d\d\d\d\d\d-\d\d\d\d\d\d\..+""".toRegex()) == true
            }
            .sortedBy { it.lastModified() }
            .dropLast(keep)
    } else emptyList()
}

private fun sync(contentResolver: ContentResolver, backend: String?, backupFile: DocumentFile) {
    backend?.takeIf { it != AccountPreference.SYNCHRONIZATION_NONE }?.let {
        var backupFileName = backupFile.name
        if (backupFileName == null) {
            CrashHandler.report(Exception("Could not get name from uri ${backupFile.uri}"))
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