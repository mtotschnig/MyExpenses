package org.totschnig.myexpenses.viewmodel

import android.accounts.AccountManager
import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.OperationApplicationException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.provider.CalendarContract
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.BACKUP_DB_FILE_NAME
import org.totschnig.myexpenses.provider.BACKUP_PREF_FILE_NAME
import org.totschnig.myexpenses.provider.DATABASE_VERSION
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.getBackupDbFile
import org.totschnig.myexpenses.provider.getBackupPrefFile
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.SyncBackendProvider
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.ZipUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.crypt.EncryptionHelper
import org.totschnig.myexpenses.util.io.FileCopyUtils
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.PushbackInputStream
import java.security.GeneralSecurityException
import java.util.*

class RestoreViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    private val _publishProgress: MutableSharedFlow<String?> = MutableSharedFlow()
    private val _result: MutableStateFlow<Result<Unit>?> = MutableStateFlow(null)
    val publishProgress: SharedFlow<String?> = _publishProgress
    val result: StateFlow<Result<Unit>?> = _result

    private fun failureResult(throwable: Throwable) {
        _result.update {
            Result.failure(throwable)
        }
    }

    private fun failureResult(error: String) {
        failureResult(Exception(error))
    }

    private fun failureResult(error: Int, vararg formatArgs: Any?) {
        failureResult(Exception(getString(error, *formatArgs)))
    }

    private suspend fun publishProgress(string: String) {
        _publishProgress.emit(string)
    }

    private suspend fun publishProgress(resId: Int, vararg formatArgs: Any?) {
        _publishProgress.emit(getString(resId, *formatArgs))
    }

    fun resultProcessed() {
        _result.update {
            null
        }
    }

    fun startRestore(args: Bundle) {
        viewModelScope.launch(coroutineDispatcher) {
            val restorePlanStrategy: Int = args.getInt(KEY_RESTORE_PLAN_STRATEGY)
            val fileUri: Uri? = args.getParcelable(KEY_FILE_PATH)
            val syncAccountName: String? =
                if (fileUri == null) args.getString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME) else null
            val backupFromSync: String? =
                if (fileUri == null) args.getString(KEY_BACKUP_FROM_SYNC) else null
            val password: String? = args.getString(KEY_PASSWORD)
            val workingDir: File
            var currentPlannerId: String? = null
            var currentPlannerPath: String? = null
            val application = getApplication<MyApplication>()

            workingDir = AppDirHelper.cacheDir(application)
            try {

                val syncBackendProvider: SyncBackendProvider
                val inputStream: PushbackInputStream? = if (syncAccountName != null) {
                    val account = GenericAccountService.getAccount(syncAccountName)
                    try {
                        syncBackendProvider =
                            SyncBackendProviderFactory.getLegacy(application, account, false)
                    } catch (throwable: Throwable) {
                        val error = Exception(
                            "Unable to get sync backend provider for $syncAccountName",
                            throwable
                        )
                        CrashHandler.report(error)
                        failureResult(error)
                        return@launch
                    }
                    try {
                        EncryptionHelper.wrap(
                            syncBackendProvider.getInputStreamForBackup(
                                backupFromSync!!
                            )
                        )
                    } catch (e: IOException) {
                        failureResult(e)
                        return@launch
                    }
                } else {
                    EncryptionHelper.wrap(contentResolver.openInputStream(fileUri!!))
                }
                if (inputStream == null) {
                    failureResult("Unable to open backup file")
                    return@launch
                }
                val isEncrypted = EncryptionHelper.isEncrypted(inputStream)
                if (isEncrypted) {
                    if (TextUtils.isEmpty(password)) {
                        failureResult(R.string.backup_is_encrypted)
                        return@launch
                    }
                }
                inputStream.use {
                    try {
                        ZipUtils.unzip(inputStream, workingDir, if (isEncrypted) password else null)
                    } catch (e: IOException) {
                        if (e.cause is GeneralSecurityException) {
                            failureResult(R.string.backup_wrong_password)
                        } else {
                            failureResult(e)
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                CrashHandler.report(e, mapOf(
                        "fileUri" to (fileUri?.toString() ?: "null"),
                        "syncAccountName" to (syncAccountName ?: "null"),
                        "backupFromSync" to (backupFromSync ?: "null")
                    )
                )
                failureResult(e)
                return@launch
            }
            val backupFile = getBackupDbFile(workingDir)
            val backupPrefFile = getBackupPrefFile(workingDir)
            if (!backupFile.exists()) {
                failureResult(
                    R.string.restore_backup_file_not_found,
                    BACKUP_DB_FILE_NAME, workingDir
                )
                return@launch
            }
            if (!backupPrefFile.exists()) {
                failureResult(
                    R.string.restore_backup_file_not_found,
                    BACKUP_PREF_FILE_NAME, workingDir
                )
                return@launch
            }

            //peek into file to inspect version
            try {
                SQLiteDatabase.openDatabase(
                    backupFile.path,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                ).use {
                    val version = it.version
                    if (version > DATABASE_VERSION) {
                        failureResult(
                            R.string.restore_cannot_downgrade,
                            version, DATABASE_VERSION
                        )
                        return@launch
                    }
                }
            } catch (e: SQLiteException) {
                failureResult(R.string.restore_db_not_valid)
                return@launch
            }

            //peek into preferences to see if there is a calendar configured
            val internalAppDir = application.filesDir.parentFile!!
            val sharedPrefsDir = File(internalAppDir.path + "/shared_prefs/")
            sharedPrefsDir.mkdir()
            if (!sharedPrefsDir.isDirectory) {
                CrashHandler.report(
                    Exception("Could not access shared preferences directory at ${sharedPrefsDir.absolutePath}")
                )
                failureResult(R.string.restore_preferences_failure)
                return@launch
            }
            val tempPrefFile = File(sharedPrefsDir, "backup_temp.xml")
            if (!FileCopyUtils.copy(backupPrefFile, tempPrefFile)) {
                CrashHandler.report(
                    Exception("Preferences restore failed"),
                    "FAILED_COPY_OPERATION", String.format(
                        "%s => %s",
                        backupPrefFile.absolutePath,
                        tempPrefFile.absolutePath
                    )
                )
                failureResult(R.string.restore_preferences_failure)
                return@launch
            }
            val backupPref = application.getSharedPreferences("backup_temp", 0)
            if (restorePlanStrategy == R.id.restore_calendar_handling_configured) {
                currentPlannerId = application.checkPlanner()
                currentPlannerPath = prefHandler.getString(PrefKey.PLANNER_CALENDAR_PATH,"")
                if (MyApplication.INVALID_CALENDAR_ID == currentPlannerId) {
                    failureResult(R.string.restore_not_possible_local_calendar_missing)
                    return@launch
                }
            } else if (restorePlanStrategy == R.id.restore_calendar_handling_backup) {
                var found = false
                val calendarId = backupPref
                    .getString(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID), "-1")
                val calendarPath = backupPref
                    .getString(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_PATH), "")
                if (!(calendarId == "-1" || calendarPath == "")) {

                    try {
                        contentResolver.query(
                            CalendarContract.Calendars.CONTENT_URI,
                            arrayOf(CalendarContract.Calendars._ID),
                            MyApplication.getCalendarFullPathProjection() + " = ?",
                            arrayOf(calendarPath),
                            null
                        )?.use {
                            if (it.moveToFirst()) {
                                found = true
                            }
                        }
                    } catch (e: SecurityException) {
                        failureResult(e)
                        return@launch
                    }
                }
                if (!found) {
                    failureResult(
                        R.string.restore_not_possible_target_calendar_missing,
                        calendarPath
                    )
                    return@launch
                }
            }
            if (DbUtils.restore(backupFile)) {
                publishProgress(R.string.restore_db_success)

                //since we already started reading settings, we can not just copy the file
                //unless I found a way
                //either to close the shared preferences and read it again
                //or to find out if we are on a new install without reading preferences
                //
                //we open the backup file and read every entry
                //getSharedPreferences does not allow to access file if it not in private data directory
                //hence we copy it there first
                //upon application install does not exist yet
                application.settings
                    .unregisterOnSharedPreferenceChangeListener(application)
                val edit = application.settings.edit()
                application.settings.all.forEach {
                    val key = it.key
                    if (key != prefHandler.getKey(PrefKey.NEW_LICENCE) && key != prefHandler.getKey(PrefKey.LICENCE_EMAIL)
                        && !key.startsWith("acra") && key != prefHandler.getKey(PrefKey.FIRST_INSTALL_VERSION)
                    ) {
                        edit.remove(key)
                    }
                }

                backupPref.all.forEach {
                    val key = it.key
                    if (key == prefHandler.getKey(PrefKey.LICENCE_LEGACY) ||
                        key == prefHandler.getKey(PrefKey.FIRST_INSTALL_VERSION) ||
                        key == prefHandler.getKey(PrefKey.UI_WEB)) {
                        return@forEach
                    }
                    val value = it.value
                    if (value == null) {
                        Timber.i("Found: %s null", key)
                        return@forEach
                    }
                    if (value is Long) {
                        edit.putLong(key, backupPref.getLong(key, 0))
                    } else if (value is Int) {
                        edit.putInt(key, backupPref.getInt(key, 0))
                    } else if (value is String) {
                        edit.putString(key, backupPref.getString(key, ""))
                    } else if (value is Boolean) {
                        edit.putBoolean(key, backupPref.getBoolean(key, false))
                    } else {
                        Timber.i("Found: %s of type %s", key, value.javaClass.name)
                    }
                }
                if (restorePlanStrategy == R.id.restore_calendar_handling_configured) {
                    edit.putString(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_PATH), currentPlannerPath)
                    edit.putString(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID), currentPlannerId)
                } else if (restorePlanStrategy == R.id.restore_calendar_handling_ignore) {
                    edit.remove(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_PATH))
                    edit.remove(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID))
                }
                edit.apply()
                application.settings
                    .registerOnSharedPreferenceChangeListener(application)
                tempPrefFile.delete()
                if (fileUri != null) {
                    backupFile.delete()
                    backupPrefFile.delete()
                }
                publishProgress(R.string.restore_preferences_success)
                //if a user restores a backup we do not want past plan instances to flood the database
                prefHandler.putLong(PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP, System.currentTimeMillis())
                //now handling plans
                if (restorePlanStrategy == R.id.restore_calendar_handling_ignore) {
                    //we remove all links to plans we did not restore
                    val planValues = ContentValues()
                    planValues.putNull(DatabaseConstants.KEY_PLANID)
                    contentResolver.update(
                        Template.CONTENT_URI,
                        planValues, null, null
                    )
                } else {
                    publishProgress(R.string.restore_calendar_success, application.restorePlanner())
                }
                Timber.i("now emptying event cache")
                contentResolver.delete(
                    TransactionProvider.EVENT_CACHE_URI, null, null
                )

                //now handling pictures
                //1.stale uris in the backup can be ignored1
                //delete from db
                contentResolver.delete(
                    TransactionProvider.STALE_IMAGES_URI, null, null
                )
                //2. all images that are left over in external and
                //internal picture dir are now stale
                registerAsStale(false)
                registerAsStale(true)

                //3. move pictures home and update uri
                val backupPictureDir = File(workingDir, ZipUtils.PICTURES)
                contentResolver.query(
                    TransactionProvider.TRANSACTIONS_URI,
                    arrayOf(
                        DatabaseConstants.KEY_ROWID,
                        DatabaseConstants.KEY_PICTURE_URI,
                        DatabaseConstants.KEY_ACCOUNTID,
                        DatabaseConstants.KEY_TRANSFER_ACCOUNT
                    ),
                    DatabaseConstants.KEY_PICTURE_URI + " IS NOT NULL",
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        do {
                            val uriValues = ContentValues()
                            val rowId = arrayOf(cursor.getString(0))
                            val accountId = cursor.getString(2)
                            val transferAccount = cursor.getString(3)
                            val accountSelectionArgs =
                                transferAccount?.let { arrayOf(accountId, it) }
                                    ?: arrayOf(accountId)
                            val fromBackup = Uri.parse(cursor.getString(1))
                            val fileName = fromBackup.lastPathSegment
                            val backupImage = fileName?.let { File(backupPictureDir, it) }
                            var restored: Uri? = null
                            if (backupImage?.exists() == true) {
                                val restoredImage = PictureDirHelper.getOutputMediaFile(
                                    fileName.substring(0, fileName.lastIndexOf('.')), false, true
                                )
                                if (restoredImage == null || !FileCopyUtils.copy(
                                        backupImage,
                                        restoredImage
                                    )
                                ) {
                                    CrashHandler.report(
                                        Exception("Could not restore file $fromBackup from backup")
                                    )
                                } else {
                                    restored =
                                        AppDirHelper.getContentUriForFile(
                                            application,
                                            restoredImage
                                        )
                                }
                            } else {
                                CrashHandler.report(
                                    Exception("Could not restore file $fromBackup from backup")
                                )
                            }
                            if (restored != null) {
                                uriValues.put(
                                    DatabaseConstants.KEY_PICTURE_URI,
                                    restored.toString()
                                )
                            } else {
                                uriValues.putNull(DatabaseConstants.KEY_PICTURE_URI)
                            }
                            val ops = ArrayList<ContentProviderOperation>()
                            try {
                                val accountSelection =
                                    " AND " + DatabaseConstants.KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(
                                        accountSelectionArgs.size
                                    )
                                ops.add(
                                    ContentProviderOperation.newUpdate(Account.CONTENT_URI)
                                        .withValue(DatabaseConstants.KEY_SEALED, -1)
                                        .withSelection(
                                            DatabaseConstants.KEY_SEALED + " = 1 " + accountSelection,
                                            accountSelectionArgs
                                        ).build()
                                )
                                ops.add(
                                    ContentProviderOperation.newUpdate(TransactionProvider.TRANSACTIONS_URI)
                                        .withValues(uriValues)
                                        .withSelection(DatabaseConstants.KEY_ROWID + " = ?", rowId)
                                        .build()
                                )
                                ops.add(
                                    ContentProviderOperation.newUpdate(Account.CONTENT_URI)
                                        .withValue(DatabaseConstants.KEY_SEALED, 1)
                                        .withSelection(
                                            DatabaseConstants.KEY_SEALED + " = -1 " + accountSelection,
                                            accountSelectionArgs
                                        ).build()
                                )
                                contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
                            } catch (e: OperationApplicationException) {
                                CrashHandler.report(e)
                            } catch (e: RemoteException) {
                                CrashHandler.report(e)
                            }
                        } while (cursor.moveToNext())
                    }
                }
                    ?: run {
                        failureResult(R.string.restore_db_failure)
                        return@launch
                    }

                restoreSyncState().takeIf { it.isNotEmpty() }?.let { publishProgress(it) }
                _result.update { Result.success(Unit) }
            } else {
                failureResult(R.string.restore_db_failure)
            }
        }
    }

    private fun restoreSyncState(): String {
        var message = ""
        val application = getApplication<MyApplication>()
        val accountManager = AccountManager.get(application)
        val accounts = Arrays.asList(*GenericAccountService.getAccountNames(application))
        val projection =
            arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)
        contentResolver.query(
            TransactionProvider.ACCOUNTS_URI, projection,
            DatabaseConstants.KEY_SYNC_ACCOUNT_NAME + " IS NOT null", null, null
        )?.use {
            val sharedPreferences = application.settings
            val editor = sharedPreferences.edit()
            if (it.moveToFirst()) {
                var restored = 0
                var failed = 0
                do {
                    val accountId = it.getLong(0)
                    val accountName = it.getString(1)
                    val localKey = SyncAdapter.KEY_LAST_SYNCED_LOCAL(accountId)
                    val remoteKey = SyncAdapter.KEY_LAST_SYNCED_REMOTE(accountId)
                    if (accounts.contains(accountName)) {
                        val account = GenericAccountService.getAccount(accountName)
                        accountManager.setUserData(
                            account,
                            localKey,
                            sharedPreferences.getString(localKey, null)
                        )
                        accountManager.setUserData(
                            account,
                            remoteKey,
                            sharedPreferences.getString(remoteKey, null)
                        )
                        restored++
                    } else {
                        failed++
                    }
                    editor.remove(localKey)
                    editor.remove(remoteKey)
                } while (it.moveToNext())
                editor.apply()
                if (restored > 0) {
                    message += getString(R.string.sync_state_restored, restored)
                }
                if (failed > 0) {
                    message += getString(
                        R.string.sync_state_could_not_be_restored,
                        failed
                    )
                }
                Account.checkSyncAccounts(application)
            }
            it.close()
        }
        return message
    }

    private fun registerAsStale(secure: Boolean) {
        val dir = PictureDirHelper.getPictureDir(secure) ?: return
        val files = dir.listFiles() ?: return
        val values = ContentValues()
        for (file: File in files) {
            val uri = if (secure) FileProvider.getUriForFile(
                getApplication(),
                "org.totschnig.myexpenses.fileprovider", file
            ) else Uri.fromFile(file)
            values.put(DatabaseConstants.KEY_PICTURE_URI, uri.toString())
            contentResolver.insert(TransactionProvider.STALE_IMAGES_URI, values)
        }
    }

    companion object {
        const val KEY_BACKUP_FROM_SYNC = "backupFromSync"
        const val KEY_RESTORE_PLAN_STRATEGY = "restorePlanStrategy"
        const val KEY_PASSWORD = "passwordEncryption"
        const val KEY_FILE_PATH = "filePath"

    }
}