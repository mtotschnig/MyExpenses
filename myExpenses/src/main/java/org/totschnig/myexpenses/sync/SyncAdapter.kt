package org.totschnig.myexpenses.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.OperationApplicationException
import android.content.SyncResult
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.util.SparseArray
import androidx.annotation.StringRes
import kotlinx.coroutines.runBlocking
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.requireAccountTypeForSync
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model2.CategoryInfo
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CRITERION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.NULL_CHANGE_INDICATOR
import org.totschnig.myexpenses.provider.DatabaseConstants.NULL_ROW_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.fromSyncAdapter
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.useAndMapToSet
import org.totschnig.myexpenses.service.SyncNotificationDismissHandler
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.deactivateSync
import org.totschnig.myexpenses.sync.SequenceNumber.Companion.parse
import org.totschnig.myexpenses.sync.SyncBackendProvider.AuthException
import org.totschnig.myexpenses.sync.SyncBackendProvider.EncryptionException
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.sync.json.TagInfo
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.isConnectedWifi
import org.totschnig.myexpenses.util.safeMessage
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

class SyncAdapter @JvmOverloads constructor(
    context: Context,
    autoInitialize: Boolean,
    allowParallelSyncs: Boolean = false
) : AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {
    private var shouldNotify = true

    private var lastSynStart = LocalTime.now()

    lateinit var currentAccount: Account

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var syncDelegateProvider: Provider<SyncDelegate>

    @Inject
    lateinit var repository: Repository

    @Suppress("SameParameterValue")
    private fun getUserDataWithDefault(
        accountManager: AccountManager, account: Account,
        key: String, defaultValue: String
    ): String = accountManager.getUserData(account, key) ?: defaultValue

    @SuppressLint("MissingPermission")
    override fun onPerformSync(
        account: Account, extras: Bundle, authority: String,
        provider: ContentProviderClient, syncResult: SyncResult
    ) {
        lastSynStart = LocalTime.now()
        currentAccount = account
        val syncDelegate = syncDelegateProvider.get()
        log().i("onPerformSync for %s at %s with extras %s", currentAccount, lastSynStart, extras)
        val uuidFromExtras = extras.getString(KEY_UUID)
        val notificationId = account.hashCode()
        if (notificationContent[notificationId] == null) {
            notificationContent.put(notificationId, mutableListOf())
        }
        shouldNotify = getBooleanSetting(provider, PrefKey.SYNC_NOTIFICATION, true)
        if (getBooleanSetting(provider, PrefKey.SYNC_WIFI_ONLY, false) &&
            !isConnectedWifi(context)
        ) {
            val message = concatResStrings(context, R.string.wifi_not_connected, R.string.synchronization_postponed)
            log().i(message)
            if (extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL)) {
                maybeNotifyUser(notificationTitle, message, account)
            }
            return
        }
        val accountManager = AccountManager.get(context)
        val missingFeature = syncDelegate.requireFeatureForAccount(context, account.name)
        if (missingFeature != null) {
            syncResult.stats.numIoExceptions++
            syncResult.delayUntil = featureLoadDelaySeconds
            appendToNotification(
                context.getString(
                    R.string.feature_download_requested,
                    context.getString(missingFeature.labelResId)
                ), account, true
            )
            return
        }

        try {
            runBlocking {
                SyncBackendProviderFactory.get(context, account, false).onFailure { throwable ->
                    if (throwable is SyncParseException || throwable is EncryptionException) {
                        syncResult.databaseError = true
                        (throwable as? SyncParseException)?.let { report(it) }
                        nonRecoverableError(
                            account,
                            "The backend could not be instantiated. Reason: ${throwable.message}. Please try to delete and recreate it."
                        )
                    } else if (!handleAuthException(throwable, account)) {
                        if (throwable is IOException) {
                            log().i(throwable, "Error setting up account %s", account)
                        } else {
                            log().e(throwable, "Error setting up account %s", account)
                        }
                        syncResult.stats.numIoExceptions++
                        syncResult.delayUntil = getIoDelaySeconds()
                        appendToNotification(
                            concatResStrings(
                                context,
                                R.string.sync_io_error_cannot_connect,
                                R.string.sync_error_will_try_again_later
                            ), account, true
                        )
                    }
                    return@runBlocking
                }.onSuccess { backend ->
                    handleAutoBackupSync(account, provider, backend)
                    if (extras.getBoolean(
                            KEY_AUTO_BACKUP_ONLY
                        )) {
                        return@runBlocking
                    }
                    val selectionArgs: Array<String>
                    var selection = "$KEY_SYNC_ACCOUNT_NAME = ?"
                    if (uuidFromExtras != null) {
                        selection += " AND $KEY_UUID = ?"
                        selectionArgs = arrayOf(account.name, uuidFromExtras)
                    } else {
                        selectionArgs = arrayOf(account.name)
                    }
                    val projection = arrayOf(KEY_ROWID)
                    try {
                        provider.query(
                            TransactionProvider.ACCOUNTS_URI,
                            projection,
                            "$selection AND $KEY_SYNC_SEQUENCE_LOCAL = 0",
                            selectionArgs,
                            KEY_ROWID
                        ).also {
                            if (it == null) {
                                syncResult.databaseError = true
                                val exception = Exception("Cursor is null")
                                notifyDatabaseError(exception, account)
                                return@runBlocking
                            }
                        }
                    } catch (e: RemoteException) {
                        syncResult.databaseError = true
                        notifyDatabaseError(e, account)
                        return@runBlocking
                    }?.use {
                        if (it.moveToFirst()) {
                            do {
                                val accountId = it.getLong(0)
                                try {
                                    provider.update(
                                        buildInitializationUri(accountId),
                                        ContentValues(0),
                                        null,
                                        null
                                    )
                                    //make sure user data did not stick around after a user might have cleared data
                                    accountManager.setUserData(
                                        account,
                                        KEY_LAST_SYNCED_LOCAL(accountId),
                                        null
                                    )
                                    accountManager.setUserData(
                                        account,
                                        KEY_LAST_SYNCED_REMOTE(accountId),
                                        null
                                    )
                                } catch (e: RemoteException) {
                                    syncResult.databaseError = true
                                    notifyDatabaseError(e, account)
                                    return@runBlocking
                                } catch (e: SQLiteConstraintException) {
                                    syncResult.databaseError = true
                                    notifyDatabaseError(e, account)
                                    return@runBlocking
                                }
                            } while (it.moveToNext())
                        }
                    }


                    try {
                        provider.query(
                            TransactionProvider.ACCOUNTS_URI, projection, selection, selectionArgs,
                            KEY_ROWID
                        )
                    } catch (e: RemoteException) {
                        syncResult.databaseError = true
                        notifyDatabaseError(e, account)
                        return@runBlocking
                    }?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            do {
                                val accountId = cursor.getLong(0)
                                val lastLocalSyncKey = KEY_LAST_SYNCED_LOCAL(accountId)
                                val lastRemoteSyncKey = KEY_LAST_SYNCED_REMOTE(accountId)
                                var lastSyncedLocal = getUserDataWithDefault(
                                    accountManager, account,
                                    lastLocalSyncKey, "0"
                                ).toLong()
                                var lastSyncedRemote = parse(
                                    getUserDataWithDefault(
                                        accountManager, account,
                                        lastRemoteSyncKey, "0"
                                    )
                                )
                                log().i("lastSyncedLocal: $lastSyncedLocal; lastSyncedRemote: $lastSyncedRemote")
                                val instanceFromDb = repository.loadAccount(accountId)
                                    ?: // might have been deleted by user in the meantime
                                    continue
                                syncDelegate.account = instanceFromDb
                                if (uuidFromExtras != null && extras.getBoolean(
                                        KEY_RESET_REMOTE_ACCOUNT
                                    )
                                ) {
                                    try {
                                        backend.resetAccountData(uuidFromExtras)
                                        appendToNotification(
                                            context.getString(
                                                R.string.sync_success_reset_account_data,
                                                instanceFromDb.label
                                            ), account, true
                                        )
                                    } catch (e: IOException) {
                                        handleIoException(
                                            e,
                                            account,
                                            backend,
                                            syncResult,
                                            R.string.sync_io_exception_reset_account_data
                                        )
                                    }
                                    break
                                }
                                appendToNotification(
                                    context.getString(
                                        R.string.synchronization_start,
                                        instanceFromDb.label
                                    ), account, true
                                )
                                try {
                                    backend.withAccount(instanceFromDb)
                                } catch (e: IOException) {
                                    handleIoException(
                                        e,
                                        account,
                                        backend,
                                        syncResult,
                                        R.string.sync_io_exception_setup_remote_account
                                    )
                                    break
                                }
                                try {
                                    backend.lock()
                                } catch (e: IOException) {
                                    handleIoException(
                                        e,
                                        account,
                                        backend,
                                        syncResult,
                                        R.string.sync_io_exception_locking,
                                        IO_LOCK_DELAY_MILLIS
                                    )
                                    log().w(e)
                                    break
                                }
                                var completedWithoutError = false
                                var successRemote2Local = 0
                                var successLocal2Remote = 0
                                try {
                                    val changeSetSince =
                                        backend.getChangeSetSince(lastSyncedRemote)
                                    var remoteChanges: List<TransactionChange> =
                                        if (changeSetSince != null) {
                                            lastSyncedRemote = changeSetSince.sequenceNumber
                                            log().i("lastSyncedRemote: $lastSyncedRemote")
                                            changeSetSince.changes
                                        } else emptyList()
                                    var localChanges: MutableList<TransactionChange> =
                                        mutableListOf()
                                    var sequenceToTest = lastSyncedLocal
                                    while (true) {
                                        sequenceToTest++
                                        val nextChanges =
                                            getLocalChanges(provider, accountId, sequenceToTest)
                                        lastSyncedLocal = if (nextChanges.isNotEmpty()) {
                                            localChanges.addAll(nextChanges.filter { !it.isEmpty })
                                            sequenceToTest
                                        } else {
                                            break
                                        }
                                    }
                                    log().i("lastSyncedLocal: $lastSyncedLocal")
                                    if (localChanges.isNotEmpty() || remoteChanges.isNotEmpty()) {
                                        var localMetadataChange =
                                            syncDelegate.findMetadataChange(localChanges)
                                        var remoteMetadataChange =
                                            syncDelegate.findMetadataChange(remoteChanges)
                                        if (remoteMetadataChange != null) {
                                            remoteChanges =
                                                syncDelegate.removeMetadataChange(remoteChanges)
                                        }
                                        if (localMetadataChange != null && remoteMetadataChange != null) {
                                            if (localMetadataChange.timeStamp() > remoteMetadataChange.timeStamp()) {
                                                remoteMetadataChange = null
                                            } else {
                                                localMetadataChange = null
                                                localChanges =
                                                    syncDelegate.removeMetadataChange(localChanges)
                                                        .toMutableList()
                                            }
                                        }
                                        if (localMetadataChange != null) {
                                            backend.updateAccount(instanceFromDb)
                                        } else if (remoteMetadataChange != null) {
                                            backend.readAccountMetaData().onSuccess {
                                                if (updateAccountFromMetadata(
                                                        provider,
                                                        syncDelegate,
                                                        it
                                                    )
                                                ) {
                                                    successRemote2Local += 1
                                                } else {
                                                    appendToNotification(
                                                        "Error while writing account metadata to database",
                                                        account,
                                                        false
                                                    )
                                                }
                                            }
                                        }
                                        if (localChanges.isNotEmpty()) {
                                            localChanges =
                                                syncDelegate.collectSplits(localChanges)
                                                    .toMutableList()
                                        }
                                        val localChangesWasNotEmpty = localChanges.isNotEmpty()
                                        val remoteChangesWasNotEmpty = remoteChanges.isNotEmpty()
                                        val mergeResult: Pair<List<TransactionChange>, List<TransactionChange>> =
                                            syncDelegate.mergeChangeSets(
                                                localChanges,
                                                remoteChanges
                                            )
                                        localChanges = mergeResult.first.toMutableList()
                                        remoteChanges = mergeResult.second
                                        if (remoteChanges.isNotEmpty()) {
                                            syncDelegate.writeRemoteChangesToDb(
                                                provider,
                                                remoteChanges
                                            )
                                        }
                                        if (remoteChangesWasNotEmpty) {
                                            accountManager.setUserData(
                                                account,
                                                lastRemoteSyncKey,
                                                lastSyncedRemote.toString()
                                            )
                                            log().i("storing lastSyncedRemote: $lastSyncedRemote")
                                            successRemote2Local += remoteChanges.size
                                        }
                                        if (localChanges.isNotEmpty()) {
                                            lastSyncedRemote =
                                                backend.writeChangeSet(
                                                    lastSyncedRemote,
                                                    localChanges,
                                                    context
                                                )
                                        }
                                        if (localChangesWasNotEmpty) {
                                            accountManager.setUserData(
                                                account,
                                                lastLocalSyncKey,
                                                lastSyncedLocal.toString()
                                            )
                                            log().i("storing lastSyncedLocal: $lastSyncedLocal")
                                            if (localChanges.isNotEmpty()) {
                                                accountManager.setUserData(
                                                    account,
                                                    lastRemoteSyncKey,
                                                    lastSyncedRemote.toString()
                                                )
                                                log().i("storing lastSyncedRemote: $lastSyncedRemote")
                                                successLocal2Remote = localChanges.size
                                            }
                                        }
                                        if (!BuildConfig.DEBUG) {
                                            // on debug build for auditing purposes, we keep changes in the table
                                            provider.delete(
                                                TransactionProvider.CHANGES_URI,
                                                "$KEY_ACCOUNTID = ? AND $KEY_SYNC_SEQUENCE_LOCAL <= ?",
                                                arrayOf(
                                                    accountId.toString(),
                                                    lastSyncedLocal.toString()
                                                )
                                            )
                                        }
                                    }
                                    completedWithoutError = true
                                } catch (e: IOException) {
                                    handleIoException(
                                        e,
                                        account,
                                        backend,
                                        syncResult,
                                        R.string.sync_io_exception_syncing
                                    )
                                    log().w(e)
                                    break
                                } catch (e: RemoteException) {
                                    syncResult.databaseError = true
                                    notifyDatabaseError(e, account)
                                } catch (e: OperationApplicationException) {
                                    syncResult.databaseError = true
                                    notifyDatabaseError(e, account)
                                } catch (e: SQLiteException) {
                                    syncResult.databaseError = true
                                    nonRecoverableError(account, e.safeMessage)
                                } catch (e: Exception) {
                                    appendToNotification(
                                        "ERROR (${e.javaClass.simpleName}): ${e.message} ",
                                        account, true
                                    )
                                    report(e)
                                } finally {
                                    if (successLocal2Remote > 0 || successRemote2Local > 0) {
                                        appendToNotification(
                                            context.getString(
                                                R.string.synchronization_end_success,
                                                successRemote2Local,
                                                successLocal2Remote
                                            ), account, false
                                        )
                                    } else if (completedWithoutError) {
                                        appendToNotification(
                                            context.getString(R.string.synchronization_end_success_none),
                                            account,
                                            false
                                        )
                                    }
                                    try {
                                        backend.unlock()
                                    } catch (e: IOException) {
                                        handleIoException(
                                            e,
                                            account,
                                            backend,
                                            syncResult,
                                            R.string.sync_io_exception_unlocking,
                                            IO_LOCK_DELAY_MILLIS
                                        )
                                        break
                                    }
                                }
                            } while (cursor.moveToNext())
                        }
                    }
                }
            }
        } catch (_: InterruptedException) {
            if (BuildConfig.DEBUG) {
                notifyUser("Debug", "InterruptedException")
            }

        }
    }

    @Throws(RemoteException::class, OperationApplicationException::class)
    private fun updateAccountFromMetadata(
        provider: ContentProviderClient,
        syncDelegate: SyncDelegate,
        accountMetaData: AccountMetaData
    ): Boolean {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(TransactionProvider.pauseChangeTrigger())
        val values = ContentValues()

        values.put(KEY_LABEL, accountMetaData.label())
        values.put(KEY_OPENING_BALANCE, accountMetaData.openingBalance())
        values.put(KEY_DESCRIPTION, accountMetaData.description())
        val currency = accountMetaData.currency()
        values.put(KEY_CURRENCY, currency)
        values.put(KEY_TYPE, repository.requireAccountTypeForSync(accountMetaData.type()).id)
        values.put(KEY_COLOR, accountMetaData.color())
        values.put(KEY_EXCLUDE_FROM_TOTALS, accountMetaData._excludeFromTotals())
        if (accountMetaData._criterion() != 0L) {
            values.put(KEY_CRITERION, accountMetaData._criterion())
        }
        val id: Long = syncDelegate.account.id
        ops.add(
            ContentProviderOperation.newUpdate(
                ContentUris.withAppendedId(
                    TransactionProvider.ACCOUNTS_URI,
                    id
                )
            ).withValues(values).build()
        )
        val homeCurrency = prefHandler.getString(PrefKey.HOME_CURRENCY, null)
        val exchangeRate = accountMetaData.exchangeRate()
        if (exchangeRate != null && homeCurrency != null && homeCurrency == accountMetaData.exchangeRateOtherCurrency()) {
            val uri =
                ContentUris.appendId(TransactionProvider.ACCOUNT_EXCHANGE_RATE_URI.buildUpon(), id)
                    .appendEncodedPath(currency)
                    .appendEncodedPath(homeCurrency).build()
            ops.add(
                ContentProviderOperation.newInsert(uri).withValue(
                    KEY_EXCHANGE_RATE,
                    exchangeRate
                ).build()
            )
        }
        ops.add(TransactionProvider.resumeChangeTrigger())
        val contentProviderResults = provider.applyBatch(ops)
        val opsSize = ops.size
        val resultsSize = contentProviderResults.size
        if (opsSize != resultsSize) {
            report(Exception("applied $opsSize operations, received $resultsSize results"))
            return false
        }
        return true
    }

    private fun handleAutoBackupSync(
        account: Account,
        provider: ContentProviderClient,
        backend: SyncBackendProvider
    ) {
        val autoBackupFileUri = getStringSetting(provider, KEY_UPLOAD_AUTO_BACKUP_URI)
        if (autoBackupFileUri != null) {
            val autoBackupCloud =
                getStringSetting(provider, prefHandler.getKey(PrefKey.AUTO_BACKUP_CLOUD))
            if (autoBackupCloud != null && autoBackupCloud == account.name) {
                var fileName = getStringSetting(provider, KEY_UPLOAD_AUTO_BACKUP_NAME)
                try {
                    if (fileName == null) {
                        report(Exception("KEY_UPLOAD_AUTO_BACKUP_NAME empty"))
                        fileName = "backup-" + SimpleDateFormat("yyyMMdd", Locale.US).format(Date())
                    }
                    log().i("Storing backup %s (%s)", fileName, autoBackupFileUri)
                    backend.storeBackup(Uri.parse(autoBackupFileUri), fileName)
                    removeSetting(provider, KEY_UPLOAD_AUTO_BACKUP_URI)
                    removeSetting(provider, KEY_UPLOAD_AUTO_BACKUP_NAME)
                    maybeNotifyUser(
                        context.getString(R.string.pref_auto_backup_title),
                        context.getString(
                            R.string.auto_backup_cloud_success,
                            fileName,
                            account.name
                        ), null
                    )
                } catch (e: Exception) {
                    if (e is IOException) {
                        log().i(e, "Error storing backup")
                    } else {
                        report(e)
                    }
                    if (!handleAuthException(e, account)) {
                        notifyUser(
                            context.getString(R.string.pref_auto_backup_title),
                            context.getString(
                                R.string.write_fail_reason_cannot_write,
                            )
                                    + "(" + fileName + "): " + e.message
                        )
                    }
                }
            }
        }
    }

    private fun handleIoException(
        ioException: IOException,
        account: Account,
        backend: SyncBackendProvider,
        syncResult: SyncResult,
        @StringRes resId: Int,
        defaultDelay: Long = IO_DEFAULT_DELAY_MILLIS
    ) {
        log().w(ioException)
        if (!handleAuthException(ioException, account)) {
            syncResult.stats.numIoExceptions++
            syncResult.delayUntil = getIoDelaySeconds(backend.suggestDelay(ioException, defaultDelay))
            notifyIoException(resId, account)
        }
    }

    private fun handleAuthException(e: Throwable, account: Account): Boolean {
        if (e is AuthException) {
            val resolution = e.resolution
            if (resolution != null) {
                notifyUser(
                    context.getString(R.string.sync_auth_exception),
                    context.getString(R.string.sync_login_again),
                    account,
                    e.resolution
                )
                return true
            }
        }
        return false
    }

    private val manageSyncBackendsIntent: Intent
        get() = Intent(context, ManageSyncBackends::class.java)

    @Synchronized
    private fun appendToNotification(content: String, account: Account, newLine: Boolean) {
        log().i(content)
        if (shouldNotify) {
            notificationContent[account.hashCode()]?.let { contentArray ->
                synchronized(contentArray) {
                    val contentBuilder: StringBuilder
                    if (contentArray.isEmpty() || newLine) {
                        contentBuilder = StringBuilder()
                        contentArray.add(0, contentBuilder)
                    } else {
                        contentBuilder = contentArray[0]
                    }
                    if (contentBuilder.isNotEmpty()) {
                        contentBuilder.append(" ")
                    }
                    contentBuilder.append(content)
                    notifyUser(
                        notificationTitle,
                        concat(contentArray),
                        account
                    )
                }
            }
        }
    }

    fun concat(contentBuilders: List<CharSequence>) =
        contentBuilders.foldIndexed(StringBuilder()) { index, sum, element ->
            if (index > 0) {
                sum.append("\n")
            }
            sum.append(element)
        }

    private fun report(e: Throwable) {
        CrashHandler.report(e, TAG)
    }

    private fun maybeNotifyUser(title: String, content: String, account: Account?) {
        if (shouldNotify) {
            notifyUser(title, content, account)
        }
    }

    private fun notifyUser(
        title: String,
        content: CharSequence,
        account: Account? = null,
        intent: Intent? = null
    ) {
        val builder = NotificationBuilderWrapper.bigTextStyleBuilder(
            context, NotificationBuilderWrapper.CHANNEL_ID_SYNC, title, content
        )
        if (intent != null) {
            builder.setContentIntent(
                //noinspection InlinedApi
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        if (account != null) {
            val dismissIntent = Intent(context, SyncNotificationDismissHandler::class.java)
            dismissIntent.putExtra(KEY_SYNC_ACCOUNT_NAME, account.name)
            //noinspection InlinedApi
            builder.setDeleteIntent(
                PendingIntent.getService(
                    context, 0,
                    dismissIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        val notification = builder.build()
        notification.flags = Notification.FLAG_AUTO_CANCEL
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            "SYNC", account?.hashCode() ?: 0, notification
        )
    }

    private fun notifyIoException(@StringRes resId: Int, account: Account) {
        appendToNotification(context.getString(resId), account, true)
    }

    private fun notifyDatabaseError(e: Exception, account: Account) {
        report(e)
        appendToNotification(
            context.getString(R.string.sync_database_error) + " " + e.message,
            account, true
        )
    }

    private fun nonRecoverableError(account: Account, message: String) {
        log().w("nonRecoverableError: $message")
        deactivateSync(account)
        AccountManager.get(context).setUserData(account, GenericAccountService.KEY_BROKEN, "1")
        notifyUser(
            "Synchronization backend deactivated", message,
            null,
            manageSyncBackendsIntent
        )
    }

    private val notificationTitle: String
        get() = concatResStrings(context, R.string.app_name, R.string.synchronization)

    @Throws(RemoteException::class)
    private fun getLocalChanges(
        provider: ContentProviderClient, accountId: Long,
        sequenceNumber: Long
    ): List<TransactionChange> {
        val result: MutableList<TransactionChange> = mutableListOf()
        val changesUri = buildChangesUri(sequenceNumber, accountId)
        val hasLocalChanges = hasLocalChanges(provider, changesUri)
        if (hasLocalChanges) {
            val currentSyncIncrease = ContentValues(1)
            val nextSequence = sequenceNumber + 1
            currentSyncIncrease.put(KEY_SYNC_SEQUENCE_LOCAL, nextSequence)
            //in case of failed syncs due to non-available backends, sequence number might already be higher than nextSequence
            //we must take care to not decrease it here
            provider.update(
                TransactionProvider.ACCOUNTS_URI.fromSyncAdapter(),
                currentSyncIncrease,
                "$KEY_ROWID = ? AND $KEY_SYNC_SEQUENCE_LOCAL < ?",
                arrayOf(accountId.toString(), nextSequence.toString())
            )

            provider.query(changesUri, null, null, null, null)?.use { changesCursor ->
                if (changesCursor.moveToFirst()) {
                    do {
                        var transactionChange = TransactionChange.create(changesCursor).let {
                            if (it.equivalentAmount() != null) {
                                val homeCurrency = currencyContext.homeCurrencyString
                                it.toBuilder().setEquivalentCurrency(homeCurrency).build()
                            } else it
                        }
                        changesCursor.getLongOrNull(KEY_CATID)?.let { catId ->
                            if (catId == NULL_ROW_ID) {
                                transactionChange = transactionChange.toBuilder().setCategoryInfo(
                                    listOf(CategoryInfo(NULL_CHANGE_INDICATOR, ""))
                                ).build()
                            } else {
                                provider.query(
                                    ContentUris.withAppendedId(
                                        BaseTransactionProvider.CATEGORY_TREE_URI,
                                        catId
                                    ),
                                    null, null, null, null
                                )?.use { cursor ->
                                    transactionChange =
                                        transactionChange.toBuilder().setCategoryInfo(
                                            CategoryInfo.fromCursor(cursor)
                                        ).build()
                                }
                            }
                        }
                        result.add(
                            when (transactionChange.type()) {
                                TransactionChange.Type.tags -> transactionChange.toBuilder()
                                    .setType(TransactionChange.Type.updated)
                                    .setTagsV2(
                                        //noinspection Recycle
                                        provider.query(
                                            TransactionProvider.TRANSACTIONS_TAGS_URI,
                                            null,
                                            "$KEY_TRANSACTIONID = (SELECT $KEY_ROWID FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?)",
                                            arrayOf(transactionChange.uuid()),
                                            null
                                        )?.useAndMapToSet { cursor ->
                                            TagInfo(
                                                cursor.getString(KEY_LABEL),
                                                cursor.getIntOrNull(KEY_COLOR)
                                            )
                                        } ?: emptySet()
                                    )
                                    .build()

                                TransactionChange.Type.attachments -> transactionChange.toBuilder()
                                    .setType(TransactionChange.Type.updated)
                                    .setAttachments(
                                        //noinspection Recycle
                                        provider.query(
                                            TransactionProvider.ATTACHMENTS_URI
                                                .buildUpon()
                                                .appendQueryParameter(
                                                    KEY_UUID,
                                                    transactionChange.uuid()
                                                )
                                                .build(),
                                            arrayOf(KEY_UUID),
                                            null,
                                            arrayOf(transactionChange.uuid()),
                                            null
                                        )?.useAndMapToSet { it.getString(0) } ?: emptySet()
                                    )
                                    .build()

                                else -> transactionChange
                            }
                        )
                    } while (changesCursor.moveToNext())
                }
            }
        }
        return result
    }

    private fun buildChangesUri(currentSync: Long, accountId: Long): Uri {
        return TransactionProvider.CHANGES_URI.buildUpon()
            .appendQueryParameter(KEY_ACCOUNTID, accountId.toString())
            .appendQueryParameter(
                KEY_SYNC_SEQUENCE_LOCAL,
                currentSync.toString()
            )
            .build()
    }

    private fun buildInitializationUri(accountId: Long): Uri {
        return TransactionProvider.CHANGES_URI.buildUpon()
            .appendQueryParameter(KEY_ACCOUNTID, accountId.toString())
            .appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_INIT)
            .build()
    }

    @Throws(RemoteException::class)
    private fun hasLocalChanges(provider: ContentProviderClient, changesUri: Uri) =
        provider.query(changesUri, arrayOf("count(*)"), null, null, null)?.use {
            if (it.moveToFirst()) {
                it.getLong(0) > 0
            } else false
        } == true

    private fun getBooleanSetting(
        provider: ContentProviderClient,
        prefKey: PrefKey,
        defaultValue: Boolean
    ): Boolean {
        val value = getStringSetting(provider, prefHandler.getKey(prefKey))
        return if (value != null) value == java.lang.Boolean.TRUE.toString() else defaultValue
    }

    private fun getStringSetting(provider: ContentProviderClient, prefKey: String): String? {
        val result: String? = try {
            provider.query(
                TransactionProvider.SETTINGS_URI, arrayOf(KEY_VALUE),
                "$KEY_KEY = ?", arrayOf(prefKey), null
            )?.use {
                if (it.moveToFirst()) {
                    it.getString(0)
                } else null
            }
        } catch (remoteException: RemoteException) {
            report(remoteException)
            null
        }
        return result
    }

    private fun removeSetting(provider: ContentProviderClient, prefKey: String) {
        try {
            provider.delete(
                TransactionProvider.SETTINGS_URI,
                "$KEY_KEY = ?",
                arrayOf(prefKey)
            )
        } catch (remoteException: RemoteException) {
            report(remoteException)
        }
    }

    override fun onSyncCanceled() {
        if (BuildConfig.DEBUG) {
            notifyUser("Debug", "Sync canceled")
            log().w("Sync for %s was started at %s and is now cancelled", currentAccount, lastSynStart)
        }
        super.onSyncCanceled()
    }

    companion object {
        const val BATCH_SIZE = 100
        const val KEY_RESET_REMOTE_ACCOUNT = "reset_remote_account"
        const val KEY_UPLOAD_AUTO_BACKUP_URI = "upload_auto_backup_uri"
        const val KEY_UPLOAD_AUTO_BACKUP_NAME = "upload_auto_backup_name"
        const val KEY_AUTO_BACKUP_ONLY = "auto_backup_only"

        private val LOCK_TIMEOUT_MINUTES = if (BuildConfig.DEBUG) 1L else 5L
        private val IO_DEFAULT_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(5)
        val IO_LOCK_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(LOCK_TIMEOUT_MINUTES)
        const val TAG = "SyncAdapter"

        @JvmStatic
        fun KEY_LAST_SYNCED_REMOTE(accountId: Long): String {
            return "last_synced_remote_$accountId"
        }

        @JvmStatic
        fun KEY_LAST_SYNCED_LOCAL(accountId: Long): String {
            return "last_synced_local_$accountId"
        }

        private fun getIoDelaySeconds(
            backOffMillis: Long = IO_DEFAULT_DELAY_MILLIS,
        ) = (System.currentTimeMillis() + backOffMillis) / 1000

        private val featureLoadDelaySeconds: Long
            get() = System.currentTimeMillis() / 1000 + 60

        fun log(): Timber.Tree {
            return Timber.tag(TAG)
        }

        private val notificationContent = SparseArray<MutableList<StringBuilder>?>()

        fun clearNotificationContent(account: Account) {
            notificationContent[account.hashCode()]?.clear()
        }
    }
}