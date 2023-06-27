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
import org.totschnig.myexpenses.util.io.displayName
import timber.log.Timber
import java.io.File
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
    val appDir = AppDirHelper.getAppDir(context)
        ?: return Result.failure(context, R.string.io_error_appdir_null)
    if (!AppDirHelper.isWritableDirectory(appDir)) {
        return Result.failure(
            context, R.string.app_dir_not_accessible, appDir.displayName
        )
    }
    val backupFile = requireBackupFile(appDir, prefHandler.backupFilePrefix,  !TextUtils.isEmpty(password))
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
        } catch (e: Exception) {
            CrashHandler.report(e)
            throw e
        } finally {
            Timber.w("now cleaning up cacheDir")
            getBackupDbFile(cacheDir).delete()
            getBackupPrefFile(cacheDir).delete()
        }
    }
}

private val PrefHandler.backupFilePrefix
    get() = requireString(
        PrefKey.BACKUP_FILE_PREFIX,
        "myexpenses-backup"
    )

fun listOldBackups(appDir: DocumentFile, prefHandler: PrefHandler): List<DocumentFile> {
    val keep = prefHandler.getInt(PrefKey.PURGE_BACKUP_KEEP, 0)
    return if (prefHandler.getBoolean(PrefKey.PURGE_BACKUP, false) && keep > 0) {
        appDir.listFiles()
            .filter {
                it.name?.matches("""${prefHandler.backupFilePrefix}-\d\d\d\d\d\d\d\d-\d\d\d\d\d\d\..+""".toRegex()) == true
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

private fun requireBackupFile(appDir: DocumentFile, prefix: String, encrypted: Boolean): DocumentFile? {
    return AppDirHelper.timeStampedFile(
        parentDir = appDir,
        prefix = prefix,
        mimeType = if (encrypted) "application/octet-stream" else "application/zip",
        extension = if (encrypted) "enc" else "zip"
    )
}

fun getBackupDbFile(backupDir: File?) = File(backupDir, BACKUP_DB_FILE_NAME)

fun getBackupPrefFile(backupDir: File?) = File(backupDir, BACKUP_PREF_FILE_NAME)