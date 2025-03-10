package org.totschnig.myexpenses.provider

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ZipUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.localizedThrowable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val BACKUP_DB_FILE_NAME = "BACKUP"
const val BACKUP_PREF_FILE_NAME = "BACKUP_PREF"
const val BACKUP_DATASTORE_FILE_NAME = "ui_settings.preferences_pb"

@Synchronized
fun doBackup(
    context: Context,
    prefHandler: PrefHandler,
    withSync: Boolean,
    lenientMode: Boolean = false
): Result<Pair<DocumentFile, List<DocumentFile>>> {
    val password = prefHandler.getString(PrefKey.EXPORT_PASSWORD, null)
    val cloudStorage = if (withSync) prefHandler.cloudStorage else null
    return AppDirHelper.checkAppDir(context).mapCatching { appDir ->
        val encrypted = !TextUtils.isEmpty(password)
        val backupFile =
            requireBackupFile(appDir, prefHandler.backupFilePrefix, encrypted)
                ?: throw localizedThrowable(context, R.string.io_error_backupdir_null)
        val cacheDir = AppDirHelper.newWorkingDirectory(context, "backup").getOrThrow()
        backup(cacheDir, context, prefHandler, lenientMode).getOrThrow()
        try {
            ZipUtils.zipBackup(context, cacheDir, backupFile, password, lenientMode)
            try {
                sync(context.contentResolver, cloudStorage, backupFile)
            } catch (e: Exception) {
                if (!lenientMode) throw e
            }
            backupFile to listOldBackups(appDir, prefHandler)
        } catch (e: Throwable) {
            CrashHandler.report(e)
            throw if (e is OutOfMemoryError && encrypted) {
                Exception("Encrypting large backup file has failed. Unencrypted backups should work.")
            } else e
        } finally {
            cacheDir.deleteRecursively()
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
    backend?.let {
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
        GenericAccountService.requestSync(backend, extras = Bundle().apply {
            putBoolean(SyncAdapter.KEY_AUTO_BACKUP_ONLY, true)
        })
    }
}

private fun requireBackupFile(
    appDir: DocumentFile,
    prefix: String,
    encrypted: Boolean
): DocumentFile? {
    return AppDirHelper.timeStampedFile(
        parentDir = appDir,
        prefix = prefix,
        mimeType = if (encrypted) "application/octet-stream" else "application/zip",
        extension = if (encrypted) "enc" else "zip"
    )
}

fun getBackupDbFile(backupDir: File?) = File(backupDir, BACKUP_DB_FILE_NAME)
fun getBackupPrefFile(backupDir: File?) = File(backupDir, BACKUP_PREF_FILE_NAME)
fun getBackupDataStoreFile(backupDir: File?) = File(backupDir, BACKUP_DATASTORE_FILE_NAME)