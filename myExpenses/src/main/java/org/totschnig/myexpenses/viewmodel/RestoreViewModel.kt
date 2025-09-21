package org.totschnig.myexpenses.viewmodel

import android.accounts.AccountManager
import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.text.TextUtils
import androidx.core.os.BundleCompat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.BACKUP_DB_FILE_NAME
import org.totschnig.myexpenses.provider.BACKUP_PREF_FILE_NAME
import org.totschnig.myexpenses.provider.CALENDAR_FULL_PATH_PROJECTION
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseVersionPeekHelper
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.provider.PlannerUtils.Companion.copyEventData
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.checkSyncAccounts
import org.totschnig.myexpenses.provider.getBackupDataStoreFile
import org.totschnig.myexpenses.provider.getBackupDbFile
import org.totschnig.myexpenses.provider.getBackupPrefFile
import org.totschnig.myexpenses.provider.getCalendarPath
import org.totschnig.myexpenses.provider.insertEventAndUpdatePlan
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.ZipUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.crypt.EncryptionHelper
import org.totschnig.myexpenses.util.io.FileCopyUtils
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.PushbackInputStream
import java.security.GeneralSecurityException
import javax.inject.Inject

class RestoreViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    private val _publishProgress: MutableSharedFlow<String?> = MutableSharedFlow()
    private val _result: MutableStateFlow<Result<Unit>?> = MutableStateFlow(null)
    val publishProgress: SharedFlow<String?> = _publishProgress
    val result: StateFlow<Result<Unit>?> = _result

    private val _permissionRequested = MutableLiveData<Unit?>(null)
    val permissionRequested: LiveData<Unit?> = _permissionRequested
    var permissionRequestFuture: CompletableDeferred<Boolean>? = null
    fun submitPermissionRequestResult(granted: Boolean) {
        permissionRequestFuture?.complete(granted)
    }

    @Inject
    lateinit var versionPeekHelper: DatabaseVersionPeekHelper

    @Inject
    lateinit var plannerUtils: PlannerUtils

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
            val fileUri: Uri? = BundleCompat.getParcelable(args, KEY_FILE_PATH, Uri::class.java)
            val syncAccountName: String? =
                if (fileUri == null) args.getString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME) else null
            val backupFromSync: String? =
                if (fileUri == null) args.getString(KEY_BACKUP_FROM_SYNC) else null
            val password: String? = args.getString(KEY_PASSWORD)
            var currentPlannerId: String? = null
            var currentPlannerPath: String? = null
            val application = getApplication<MyApplication>()

            val workingDir = AppDirHelper.newWorkingDirectory(application, "restore").getOrElse {
                failureResult(it)
                return@launch
            }
            try {
                val inputStream: PushbackInputStream? = if (syncAccountName != null) {
                    val account = GenericAccountService.getAccount(syncAccountName)
                    SyncBackendProviderFactory.get(application, account, false).onFailure {
                        val error = Exception(
                            "Unable to get sync backend provider for $syncAccountName",
                            it
                        )
                        CrashHandler.report(error)
                        failureResult(error)
                        return@launch
                    }.mapCatching {
                        EncryptionHelper.wrap(it.getInputStreamForBackup(backupFromSync!!))
                    }.onFailure {
                        failureResult(it)
                        return@launch
                    }.getOrNull()
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
                versionPeekHelper.checkVersion(application, backupFile.path).onFailure {
                    failureResult(it)
                    if (it is SQLiteException) {
                        CrashHandler.report(it)
                    }
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
                val calendarId = backupPref
                    .getString(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID), "-1")
                val calendarPath = backupPref
                    .getString(prefHandler.getKey(PrefKey.PLANNER_CALENDAR_PATH), "")
                if (!(calendarId == "-1" || calendarPath == "")) {
                    if (!PermissionHelper.hasCalendarPermission(getApplication())) {
                        _permissionRequested.postValue(Unit)
                        val granted = CompletableDeferred<Boolean>().also {
                            permissionRequestFuture = it
                        }.await()
                        _permissionRequested.postValue(null)
                        if (!granted) {
                            failureResult(
                                Utils.getTextWithAppName(
                                    getApplication(),
                                    R.string.notifications_permission_required_planner
                                ).toString()
                            )
                            return@launch
                        }
                    }
                    if (try {
                            contentResolver.query(
                                CalendarContract.Calendars.CONTENT_URI,
                                arrayOf(CalendarContract.Calendars._ID),
                                "$CALENDAR_FULL_PATH_PROJECTION = ?",
                                arrayOf(calendarPath),
                                null
                            )?.use {
                                it.moveToFirst()
                            }
                        } catch (e: SecurityException) {
                            failureResult(e)
                            return@launch
                        } == false
                    ) {
                        //the calendar configured in the backup does not exist
                        currentPlannerId = plannerUtils.checkPlanner()
                        currentPlannerPath =
                            prefHandler.getString(PrefKey.PLANNER_CALENDAR_PATH, "")
                        if (INVALID_CALENDAR_ID == currentPlannerId) {
                            //there is no locally configured calendar, we create a new one
                            //noinspection MissingPermission
                            currentPlannerId = plannerUtils.createPlanner(false)
                            currentPlannerPath =
                                getCalendarPath(contentResolver, currentPlannerId)
                        }
                    }
                }

                val dataStoreBackup =
                    getBackupDataStoreFile(workingDir).takeIf { it.exists() }?.let {
                        PreferenceDataStoreFactory.create(
                            scope = this,
                            produceFile = { it }
                        )
                    }

                dataStore.edit {
                    it.clear()
                    if (dataStoreBackup != null) {
                        it.plusAssign(dataStoreBackup.data.first())
                    }
                }

                val encrypt = args.getBoolean(KEY_ENCRYPT, false)
                if (DbUtils.restore(getApplication(), backupFile, encrypt)) {
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
                        if (key != prefHandler.getKey(PrefKey.NEW_LICENCE) && key != prefHandler.getKey(
                                PrefKey.LICENCE_EMAIL
                            )
                            && !key.startsWith("acra") && key != prefHandler.getKey(PrefKey.FIRST_INSTALL_VERSION)
                        ) {
                            edit.remove(key)
                        }
                    }

                    backupPref.all.forEach {
                        val key = it.key
                        if (key == prefHandler.getKey(PrefKey.LICENCE_LEGACY) ||
                            key == prefHandler.getKey(PrefKey.FIRST_INSTALL_VERSION) ||
                            key == prefHandler.getKey(PrefKey.UI_WEB)
                        ) {
                            return@forEach
                        }
                        val value = it.value
                        if (value == null) {
                            Timber.i("Found: %s null", key)
                            return@forEach
                        }
                        when (value) {
                            is Long -> {
                                edit.putLong(key, backupPref.getLong(key, 0))
                            }

                            is Int -> {
                                edit.putInt(key, backupPref.getInt(key, 0))
                            }

                            is String -> {
                                edit.putString(key, backupPref.getString(key, ""))
                            }

                            is Boolean -> {
                                edit.putBoolean(key, backupPref.getBoolean(key, false))
                            }

                            else -> {
                                Timber.i("Found: %s of type %s", key, value.javaClass.name)
                            }
                        }
                    }
                    if (currentPlannerId != null) {
                        edit.putString(
                            prefHandler.getKey(PrefKey.PLANNER_CALENDAR_PATH),
                            currentPlannerPath
                        )
                        edit.putString(
                            prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID),
                            currentPlannerId
                        )
                    }
                    edit.putBoolean(prefHandler.getKey(PrefKey.ENCRYPT_DATABASE), encrypt)
                    edit.apply()
                    tempPrefFile.delete()
                    publishProgress(R.string.restore_preferences_success)
                    //if a user restores a backup we do not want past plan instances to flood the database
                    prefHandler.putLong(
                        PrefKey.PLANNER_LAST_EXECUTION_TIMESTAMP,
                        System.currentTimeMillis()
                    )
                    publishProgress(
                        R.string.restore_calendar_success,
                        restorePlanner()
                    )
                    application.settings
                        .registerOnSharedPreferenceChangeListener(application)
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
                    val backupFiles = backupPictureDir.listFiles() ?: emptyArray()
                    contentResolver.query(
                        TransactionProvider.ATTACHMENTS_URI,
                        arrayOf(DatabaseConstants.KEY_ROWID, KEY_URI),
                        null,
                        null,
                        null
                    )!!.use { c ->
                        c.asSequence.forEach { cursor ->
                            val uriValues = ContentValues(1)
                            val rowId = cursor.getLong(0)
                            val fromBackup = cursor.getString(1)
                            val selection = "$KEY_URI = ?"
                            val selectionArguments = arrayOf(fromBackup)
                            val restored = (backupFiles.firstOrNull { file ->
                                file.name.startsWith("${rowId}_")
                            }?.let {
                                it to it.nameWithoutExtension.substringAfter('_')
                            } ?: Uri.parse(fromBackup).lastPathSegment?.let { fileName ->
                                //legacy backups
                                backupFiles.firstOrNull { it.name == fileName }
                                    ?.let { it to it.nameWithoutExtension }
                            })?.let { (image, fileName) ->
                                val restoredImage = PictureDirHelper.getOutputMediaFile(
                                    fileName = fileName,
                                    temp = false,
                                    checkUnique = true,
                                    application = getApplication(),
                                    extension = image.extension
                                )
                                if (FileCopyUtils.copy(image, restoredImage)) {
                                    val restored =
                                        AppDirHelper.getContentUriForFile(
                                            application,
                                            restoredImage
                                        )
                                    uriValues.put(KEY_URI, restored.toString())
                                    contentResolver.update(
                                        TransactionProvider.ATTACHMENTS_URI,
                                        uriValues,
                                        selection,
                                        selectionArguments
                                    )
                                    true
                                } else false
                            }
                            if (restored != true) {
                                CrashHandler.report(
                                    Exception("Could not restore file $fromBackup from backup")
                                )
                            }
                        }
                    }

                    restoreSyncState().takeIf { it.isNotEmpty() }?.let { publishProgress(it) }
                    updateTransferShortcut()
                    _result.update { Result.success(Unit) }
                } else {
                    failureResult(R.string.restore_db_failure)
                }
            } catch (e: Exception) {
                CrashHandler.report(
                    e, mapOf(
                        "fileUri" to (fileUri?.toString() ?: "null"),
                        "syncAccountName" to (syncAccountName ?: "null"),
                        "backupFromSync" to (backupFromSync ?: "null")
                    )
                )
                failureResult(e)
                return@launch
            } finally {
                workingDir.deleteRecursively()
            }
        }
    }

    private fun restoreSyncState(): String {
        var message = ""
        val application = getApplication<MyApplication>()
        val accountManager = AccountManager.get(application)
        val accounts = listOf(*GenericAccountService.getAccountNames(application))
        val activeAccounts = mutableSetOf<String>()
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
                        activeAccounts.add(accountName)
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
                    activeAccounts.forEach { account ->
                        GenericAccountService.activateSync(account, prefHandler)
                    }
                }
                if (failed > 0) {
                    message += getString(
                        R.string.sync_state_could_not_be_restored,
                        failed
                    )
                }
                checkSyncAccounts(application)
            }
        }
        return message
    }

    private fun registerAsStale(secure: Boolean) {
        val dir = PictureDirHelper.getPictureDir(getApplication(), secure) ?: return
        val files = dir.listFiles() ?: return
        val values = ContentValues(1)
        for (file: File in files) {
            values.put(
                KEY_URI,
                AppDirHelper.getContentUriForFile(getApplication(), file).toString()
            )
            contentResolver.insert(TransactionProvider.ATTACHMENTS_URI, values)
        }
    }

    /**
     * 1.check if a planner is configured. If no, nothing to do 2.check if the
     * configured planner exists on the device 2.1 if yes go through all events
     * and look for them based on UUID added to description recreate events that
     * we did not find (2.2 if no, user should have been asked to select a target
     * calendar where we will store the recreated events)
     *
     * @return number of restored plans
     */
    private fun restorePlanner(): Int {
        val calendarIdFromBackup = prefHandler.getString(
            PrefKey.PLANNER_CALENDAR_ID,
            INVALID_CALENDAR_ID
        )
        val calendarPath = prefHandler.getString(PrefKey.PLANNER_CALENDAR_PATH, "")
        Timber.d(
            "restore plans to calendar with id %s and path %s", calendarIdFromBackup,
            calendarPath
        )
        var restoredPlansCount = 0
        if (!(calendarIdFromBackup == INVALID_CALENDAR_ID || calendarPath == "")) {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID),
                CALENDAR_FULL_PATH_PROJECTION
                        + " = ?",
                arrayOf(calendarPath),
                null
            )?.use {
                if (it.moveToFirst()) {
                    val calendarId = it.getLong(0)
                    Timber.d("restorePlaner: found calendar with id %d", calendarId)
                    prefHandler.putString(PrefKey.PLANNER_CALENDAR_ID, calendarId.toString())
                    val planValues = ContentValues()
                    val eventValues = ContentValues()
                    eventValues.put(
                        CalendarContract.Events.CALENDAR_ID,
                        calendarId
                    )
                    contentResolver.query(
                        Template.CONTENT_URI, arrayOf(
                            DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PLANID,
                            DatabaseConstants.KEY_UUID
                        ), DatabaseConstants.KEY_PLANID
                                + " IS NOT null", null, null
                    )?.use { plan ->
                        if (plan.moveToFirst()) {
                            do {
                                val templateId = plan.getLong(0)
                                val oldPlanId = plan.getLong(1)
                                val uuid = plan.getString(2)
                                if (contentResolver.query(
                                        CalendarContract.Events.CONTENT_URI,
                                        arrayOf(CalendarContract.Events._ID),
                                        CalendarContract.Events.CALENDAR_ID + " = ? AND " + CalendarContract.Events.DESCRIPTION
                                                + " LIKE ?",
                                        arrayOf(calendarId.toString(), "%$uuid%"),
                                        null
                                    )?.use { event ->
                                        if (event.moveToFirst()) {
                                            val newPlanId = event.getLong(0)
                                            Timber.d(
                                                "Looking for event with uuid %s: found id %d. Original event had id %d",
                                                uuid, newPlanId, oldPlanId
                                            )
                                            if (newPlanId != oldPlanId) {
                                                planValues.put(
                                                    DatabaseConstants.KEY_PLANID,
                                                    newPlanId
                                                )
                                                val updated = contentResolver.update(
                                                    ContentUris.withAppendedId(
                                                        Template.CONTENT_URI, templateId
                                                    ), planValues, null,
                                                    null
                                                )
                                                if (updated > 0) {
                                                    Timber.i(
                                                        "updated plan id in template: %d",
                                                        templateId
                                                    )
                                                    restoredPlansCount++
                                                }
                                            } else {
                                                restoredPlansCount++
                                            }
                                            true
                                        } else false
                                    } == false
                                ) {
                                    Timber.d(
                                        "Looking for event with uuid %s did not find, now reconstructing from cache",
                                        uuid
                                    )
                                    if (contentResolver.query(
                                            TransactionProvider.EVENT_CACHE_URI,
                                            PlannerUtils.eventProjection,
                                            CalendarContract.Events.DESCRIPTION + " LIKE ?",
                                            arrayOf("%$uuid%"),
                                            null
                                        )?.use { event ->
                                            if (event.moveToFirst()) {
                                                eventValues.copyEventData(event)
                                                if (insertEventAndUpdatePlan(
                                                        contentResolver,
                                                        eventValues,
                                                        templateId
                                                    )
                                                ) {
                                                    Timber.i(
                                                        "updated plan id in template %d",
                                                        templateId
                                                    )
                                                    restoredPlansCount++
                                                }
                                                true
                                            } else false
                                        } == false
                                    ) {
                                        //need to set eventId to null
                                        planValues.putNull(DatabaseConstants.KEY_PLANID)
                                        contentResolver.update(
                                            ContentUris.withAppendedId(
                                                Template.CONTENT_URI,
                                                templateId
                                            ),
                                            planValues, null, null
                                        )
                                    }
                                }
                            } while (plan.moveToNext())
                        }
                    }
                }
            }
        }
        return restoredPlansCount
    }

    companion object {
        const val KEY_BACKUP_FROM_SYNC = "backupFromSync"
        const val KEY_PASSWORD = "passwordEncryption"
        const val KEY_FILE_PATH = "filePath"
        const val KEY_ENCRYPT = "encrypt"
    }
}