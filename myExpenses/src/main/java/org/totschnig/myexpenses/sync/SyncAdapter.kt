package org.totschnig.myexpenses.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.util.SparseArray
import androidx.core.util.Pair
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.service.SyncNotificationDismissHandler
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.deactivateSync
import org.totschnig.myexpenses.sync.SequenceNumber.Companion.parse
import org.totschnig.myexpenses.sync.SyncBackendProvider.*
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory.Companion.getLegacy
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.NotificationBuilderWrapper
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.isConnectedWifi
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class SyncAdapter : AbstractThreadedSyncAdapter {
    private val syncDelegate: SyncDelegate
    private val notificationContent = SparseArray<MutableList<StringBuilder>?>()
    private var shouldNotify = true

    constructor(context: Context, autoInitialize: Boolean) : this(context, autoInitialize, false)

    constructor(context: Context, autoInitialize: Boolean, allowParallelSyncs: Boolean) : super(
        context,
        autoInitialize,
        allowParallelSyncs
    ) {
        syncDelegate = SyncDelegate(
            currencyContext,
            (context.applicationContext as MyApplication).appComponent.featureManager(),
            (context.applicationContext as MyApplication).appComponent.repository()
        )
    }

    @Suppress("SameParameterValue")
    private fun getUserDataWithDefault(
        accountManager: AccountManager, account: Account,
        key: String, defaultValue: String
    ): String = accountManager.getUserData(account, key) ?: defaultValue

    override fun onPerformSync(
        account: Account, extras: Bundle, authority: String,
        provider: ContentProviderClient, syncResult: SyncResult
    ) {
        log().i("onPerformSync %s", extras)
        val canceledDelayUntil = extras.getLong(KEY_NOTIFICATION_CANCELLED)
        if (canceledDelayUntil > 0L) {
            syncResult.delayUntil = System.currentTimeMillis() / 1000 + canceledDelayUntil
            notificationContent.remove(account.hashCode())
            return
        }
        val uuidFromExtras = extras.getString(DatabaseConstants.KEY_UUID)
        val notificationId = account.hashCode()
        if (notificationContent[notificationId] == null) {
            notificationContent.put(notificationId, ArrayList())
        }
        shouldNotify = getBooleanSetting(provider, PrefKey.SYNC_NOTIFICATION, true)
        if (getBooleanSetting(provider, PrefKey.SYNC_WIFI_ONLY, false) &&
            !isConnectedWifi(context)
        ) {
            val message = context.getString(R.string.wifi_not_connected)
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
        val backend: SyncBackendProvider
        try {
            backend = getLegacy(context, account, false)
        } catch (throwable: Throwable) {
            if (throwable is SyncParseException || throwable is EncryptionException) {
                syncResult.databaseError = true
                (throwable as? SyncParseException)?.let { report(it) }
                deactivateSync(account)
                accountManager.setUserData(account, GenericAccountService.KEY_BROKEN, "1")
                notifyUser(
                    "Synchronization backend deactivated", String.format(
                        Locale.ROOT,
                        "The backend could not be instantiated. Reason: %s. Please try to delete and recreate it.",
                        throwable.message
                    ),
                    null,
                    manageSyncBackendsIntent
                )
            } else if (handleAuthException(throwable, account)) {
                return
            } else {
                if (throwable is IOException) {
                    log().i(throwable, "Error setting up account %s", account)
                } else {
                    log().e(throwable, "Error setting up account %s", account)
                }
                syncResult.stats.numIoExceptions++
                syncResult.delayUntil = ioDefaultDelaySeconds
                appendToNotification(
                    concatResStrings(
                        context,
                        " ",
                        R.string.sync_io_error_cannot_connect,
                        R.string.sync_error_will_try_again_later
                    ), account, true
                )
            }
            return
        }
        handleAutoBackupSync(account, provider, backend)
        var cursor: Cursor?
        val selectionArgs: Array<String>
        var selection = DatabaseConstants.KEY_SYNC_ACCOUNT_NAME + " = ?"
        if (uuidFromExtras != null) {
            selection += " AND " + DatabaseConstants.KEY_UUID + " = ?"
            selectionArgs = arrayOf(account.name, uuidFromExtras)
        } else {
            selectionArgs = arrayOf(account.name)
        }
        val projection = arrayOf(DatabaseConstants.KEY_ROWID)
        try {
            cursor = provider.query(
                TransactionProvider.ACCOUNTS_URI,
                projection,
                selection + " AND " + DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL + " = 0",
                selectionArgs,
                DatabaseConstants.KEY_ROWID
            )
        } catch (e: RemoteException) {
            syncResult.databaseError = true
            notifyDatabaseError(e, account)
            return
        }
        if (cursor == null) {
            syncResult.databaseError = true
            val exception = Exception("Cursor is null")
            notifyDatabaseError(exception, account)
            return
        }
        if (cursor.moveToFirst()) {
            do {
                val accountId = cursor.getLong(0)
                try {
                    provider.update(buildInitializationUri(accountId), ContentValues(0), null, null)
                    //make sure user data did not stick around after a user might have cleared data
                    accountManager.setUserData(account, KEY_LAST_SYNCED_LOCAL(accountId), null)
                    accountManager.setUserData(account, KEY_LAST_SYNCED_REMOTE(accountId), null)
                } catch (e: RemoteException) {
                    syncResult.databaseError = true
                    notifyDatabaseError(e, account)
                    return
                } catch (e: SQLiteConstraintException) {
                    syncResult.databaseError = true
                    notifyDatabaseError(e, account)
                    return
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        try {
            cursor = provider.query(
                TransactionProvider.ACCOUNTS_URI, projection, selection, selectionArgs,
                DatabaseConstants.KEY_ROWID
            )
        } catch (e: RemoteException) {
            syncResult.databaseError = true
            notifyDatabaseError(e, account)
            return
        }
        if (cursor != null) {
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
                    val instanceFromDb =
                        org.totschnig.myexpenses.model.Account.getInstanceFromDb(accountId)
                            ?: // might have been deleted by user in the meantime
                            continue
                    syncDelegate.account = instanceFromDb
                    if (uuidFromExtras != null && extras.getBoolean(KEY_RESET_REMOTE_ACCOUNT)) {
                        try {
                            backend.resetAccountData(uuidFromExtras)
                            appendToNotification(
                                context.getString(
                                    R.string.sync_success_reset_account_data,
                                    instanceFromDb.label
                                ), account, true
                            )
                        } catch (e: IOException) {
                            log().w(e)
                            if (handleAuthException(e, account)) {
                                return
                            }
                            syncResult.stats.numIoExceptions++
                            syncResult.delayUntil = ioDefaultDelaySeconds
                            notifyIoException(
                                R.string.sync_io_exception_reset_account_data,
                                account
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
                        log().w(e)
                        if (handleAuthException(e, account)) {
                            return
                        }
                        syncResult.stats.numIoExceptions++
                        syncResult.delayUntil = ioDefaultDelaySeconds
                        notifyIoException(R.string.sync_io_exception_setup_remote_account, account)
                        continue
                    }
                    try {
                        backend.lock()
                    } catch (e: IOException) {
                        log().w(e)
                        if (handleAuthException(e, account)) {
                            return
                        }
                        notifyIoException(R.string.sync_io_exception_locking, account)
                        syncResult.stats.numIoExceptions++
                        syncResult.delayUntil = ioLockDelaySeconds
                        continue
                    }
                    var completedWithoutError = false
                    var successRemote2Local = 0
                    var successLocal2Remote = 0
                    try {
                        val changeSetSince = backend.getChangeSetSince(lastSyncedRemote, context)
                        var remoteChanges: List<TransactionChange>
                        if (changeSetSince != null) {
                            lastSyncedRemote = changeSetSince.sequenceNumber
                            remoteChanges = changeSetSince.changes
                        } else {
                            remoteChanges = emptyList()
                        }
                        var localChanges: MutableList<TransactionChange> = mutableListOf()
                        var sequenceToTest = lastSyncedLocal
                        while (true) {
                            sequenceToTest++
                            val nextChanges = getLocalChanges(provider, accountId, sequenceToTest)
                            lastSyncedLocal = if (nextChanges.isNotEmpty()) {
                                localChanges.addAll(nextChanges)
                                sequenceToTest
                            } else {
                                break
                            }
                        }
                        if (localChanges.isNotEmpty() || remoteChanges.isNotEmpty()) {
                            var localMetadataChange =
                                syncDelegate.findMetadataChange(localChanges)
                            var remoteMetadataChange =
                                syncDelegate.findMetadataChange(remoteChanges)
                            if (remoteMetadataChange != null) {
                                remoteChanges = syncDelegate.removeMetadataChange(remoteChanges)
                            }
                            if (localMetadataChange != null && remoteMetadataChange != null) {
                                if (localMetadataChange.timeStamp() > remoteMetadataChange.timeStamp()) {
                                    remoteMetadataChange = null
                                } else {
                                    localMetadataChange = null
                                    localChanges = syncDelegate.removeMetadataChange(localChanges).toMutableList()
                                }
                            }
                            if (localMetadataChange != null) {
                                backend.updateAccount(instanceFromDb)
                            } else if (remoteMetadataChange != null) {
                                backend.readAccountMetaData().onSuccess {
                                    if (updateAccountFromMetadata(provider, it)) {
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
                            if (localChanges.size > 0) {
                                localChanges = syncDelegate.collectSplits(localChanges).toMutableList()
                            }
                            val mergeResult: Pair<List<TransactionChange>, List<TransactionChange>> =
                                syncDelegate.mergeChangeSets(localChanges, remoteChanges)
                            localChanges = mergeResult.first.toMutableList()
                            remoteChanges = mergeResult.second
                            if (remoteChanges.isNotEmpty()) {
                                syncDelegate.writeRemoteChangesToDb(provider, remoteChanges)
                                accountManager.setUserData(
                                    account,
                                    lastRemoteSyncKey,
                                    lastSyncedRemote.toString()
                                )
                                successRemote2Local += remoteChanges.size
                            }
                            if (localChanges.size > 0) {
                                lastSyncedRemote =
                                    backend.writeChangeSet(lastSyncedRemote, localChanges, context)
                                accountManager.setUserData(
                                    account,
                                    lastLocalSyncKey,
                                    lastSyncedLocal.toString()
                                )
                                accountManager.setUserData(
                                    account,
                                    lastRemoteSyncKey,
                                    lastSyncedRemote.toString()
                                )
                                successLocal2Remote = localChanges.size
                            }
                            if (!BuildConfig.DEBUG) {
                                // on debug build for auditing purposes, we keep changes in the table
                                provider.delete(
                                    TransactionProvider.CHANGES_URI,
                                    DatabaseConstants.KEY_ACCOUNTID + " = ? AND " + DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL + " <= ?",
                                    arrayOf(accountId.toString(), lastSyncedLocal.toString())
                                )
                            }
                        }
                        completedWithoutError = true
                    } catch (e: IOException) {
                        log().w(e)
                        if (handleAuthException(e, account)) {
                            return
                        }
                        syncResult.stats.numIoExceptions++
                        syncResult.delayUntil = ioDefaultDelaySeconds
                        notifyIoException(R.string.sync_io_exception_syncing, account)
                    } catch (e: RemoteException) {
                        syncResult.databaseError = true
                        notifyDatabaseError(e, account)
                    } catch (e: OperationApplicationException) {
                        syncResult.databaseError = true
                        notifyDatabaseError(e, account)
                    } catch (e: SQLiteException) {
                        syncResult.databaseError = true
                        notifyDatabaseError(e, account)
                    } catch (e: Exception) {
                        appendToNotification(
                            String.format("ERROR (%s): %s ", e.javaClass.simpleName, e.message),
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
                            log().w(e)
                            if (!handleAuthException(e, account)) {
                                notifyIoException(R.string.sync_io_exception_unlocking, account)
                                syncResult.stats.numIoExceptions++
                                syncResult.delayUntil = ioLockDelaySeconds
                            }
                        }
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    private val currencyContext: CurrencyContext
        get() = (context.applicationContext as MyApplication).appComponent.currencyContext()

    @Throws(RemoteException::class, OperationApplicationException::class)
    private fun updateAccountFromMetadata(
        provider: ContentProviderClient,
        accountMetaData: AccountMetaData
    ): Boolean {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(TransactionProvider.pauseChangeTrigger())
        val values = ContentValues()
        values.put(DatabaseConstants.KEY_LABEL, accountMetaData.label())
        values.put(DatabaseConstants.KEY_OPENING_BALANCE, accountMetaData.openingBalance())
        values.put(DatabaseConstants.KEY_DESCRIPTION, accountMetaData.description())
        val currency = accountMetaData.currency()
        values.put(DatabaseConstants.KEY_CURRENCY, currency)
        values.put(DatabaseConstants.KEY_TYPE, accountMetaData.type())
        values.put(DatabaseConstants.KEY_COLOR, accountMetaData.color())
        values.put(DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS, accountMetaData._excludeFromTotals())
        if (accountMetaData._criterion() != 0L) {
            values.put(DatabaseConstants.KEY_CRITERION, accountMetaData._criterion())
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
        val homeCurrency = PrefKey.HOME_CURRENCY.getString(null)
        val exchangeRate = accountMetaData.exchangeRate()
        if (exchangeRate != null && homeCurrency != null && homeCurrency == accountMetaData.exchangeRateOtherCurrency()) {
            val uri =
                ContentUris.appendId(TransactionProvider.ACCOUNT_EXCHANGE_RATE_URI.buildUpon(), id)
                    .appendEncodedPath(currency)
                    .appendEncodedPath(homeCurrency).build()
            val minorUnitDelta =
                Utils.getHomeCurrency().fractionDigits - currencyContext[currency].fractionDigits
            ops.add(
                ContentProviderOperation.newInsert(uri).withValue(
                    DatabaseConstants.KEY_EXCHANGE_RATE,
                    exchangeRate * 10.0.pow(minorUnitDelta.toDouble())
                ).build()
            )
        }
        ops.add(TransactionProvider.resumeChangeTrigger())
        val contentProviderResults = provider.applyBatch(ops)
        val opsSize = ops.size
        val resultsSize = contentProviderResults.size
        if (opsSize != resultsSize) {
            CrashHandler.reportWithTag(
                String.format(
                    Locale.ROOT, "applied %d operations, received %d results",
                    opsSize, resultsSize
                ), TAG
            )
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
            val autoBackupCloud = getStringSetting(provider, PrefKey.AUTO_BACKUP_CLOUD.key)
            if (autoBackupCloud != null && autoBackupCloud == account.name) {
                var fileName = getStringSetting(provider, KEY_UPLOAD_AUTO_BACKUP_NAME)
                try {
                    if (fileName == null) {
                        CrashHandler.report("KEY_UPLOAD_AUTO_BACKUP_NAME empty")
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
                    report(e)
                    if (!handleAuthException(e, account)) {
                        notifyUser(
                            context.getString(R.string.pref_auto_backup_title),
                            context.getString(
                                R.string.auto_backup_cloud_failure,
                                fileName,
                                account.name
                            )
                                    + " " + e.message, null, null
                        )
                    }
                }
            }
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

    private fun appendToNotification(content: String, account: Account, newLine: Boolean) {
        log().i(content)
        if (shouldNotify) {
            val contentBuilders = notificationContent[account.hashCode()]
            val contentBuilder: StringBuilder
            if (contentBuilders!!.isEmpty() || newLine) {
                contentBuilder = StringBuilder()
                contentBuilders.add(0, contentBuilder)
            } else {
                contentBuilder = contentBuilders[0]
            }
            if (contentBuilder.isNotEmpty()) {
                contentBuilder.append(" ")
            }
            contentBuilder.append(content)
            notifyUser(
                notificationTitle,
                syncDelegate.concat(contentBuilders),
                account, null
            )
        }
    }

    private fun report(e: Throwable) {
        CrashHandler.report(e, TAG)
    }

    private fun maybeNotifyUser(title: String, content: String, account: Account?) {
        if (shouldNotify) {
            notifyUser(title, content, account, null)
        }
    }

    private fun notifyUser(
        title: String,
        content: CharSequence,
        account: Account?,
        intent: Intent?
    ) {
        val builder = NotificationBuilderWrapper.bigTextStyleBuilder(
            context, NotificationBuilderWrapper.CHANNEL_ID_SYNC, title, content
        )
        if (intent != null) {
            builder.setContentIntent(
                PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT
                )
            )
        }
        if (account != null) {
            val dismissIntent = Intent(context, SyncNotificationDismissHandler::class.java)
            dismissIntent.putExtra(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME, account.name)
            builder.setDeleteIntent(
                PendingIntent.getService(
                    context, 0,
                    dismissIntent, PendingIntent.FLAG_ONE_SHOT
                )
            )
        }
        val notification = builder.build()
        notification.flags = Notification.FLAG_AUTO_CANCEL
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            "SYNC", account?.hashCode() ?: 0, notification
        )
    }

    private fun notifyIoException(resId: Int, account: Account) {
        appendToNotification(context.getString(resId), account, true)
    }

    private fun notifyDatabaseError(e: Exception, account: Account) {
        report(e)
        appendToNotification(
            context.getString(R.string.sync_database_error) + " " + e.message,
            account, true
        )
    }

    private val notificationTitle: String
        get() = concatResStrings(context, " ", R.string.app_name, R.string.synchronization)

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
            currentSyncIncrease.put(DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL, nextSequence)
            //in case of failed syncs due to non-available backends, sequence number might already be higher than nextSequence
            //we must take care to not decrease it here
            provider.update(
                TransactionProvider.ACCOUNTS_URI.buildUpon()
                    .appendQueryParameter(
                        TransactionProvider.QUERY_PARAMETER_CALLER_IS_SYNCADAPTER,
                        "1"
                    ).build(),
                currentSyncIncrease,
                DatabaseConstants.KEY_ROWID + " = ? AND " + DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL + " < ?",
                arrayOf(accountId.toString(), nextSequence.toString())
            )
        }
        if (hasLocalChanges) {
            val c = provider.query(changesUri, null, null, null, null)
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        var transactionChange = TransactionChange.create(c)
                        if (!transactionChange.isEmpty) {
                            if (transactionChange.type() == TransactionChange.Type.created || transactionChange.type() == TransactionChange.Type.updated) {
                                val tagCursor = provider.query(
                                    TransactionProvider.TRANSACTIONS_TAGS_URI,
                                    null,
                                    String.format(
                                        "%s = (SELECT %s FROM %s WHERE %s = ?)",
                                        DatabaseConstants.KEY_TRANSACTIONID,
                                        DatabaseConstants.KEY_ROWID,
                                        DatabaseConstants.TABLE_TRANSACTIONS,
                                        DatabaseConstants.KEY_UUID
                                    ),
                                    arrayOf(transactionChange.uuid()),
                                    null
                                )
                                if (tagCursor != null) {
                                    if (tagCursor.moveToFirst()) {
                                        val tags: MutableList<String> = ArrayList()
                                        do {
                                            tags.add(
                                                tagCursor.getString(
                                                    tagCursor.getColumnIndexOrThrow(
                                                        DatabaseConstants.KEY_LABEL
                                                    )
                                                )
                                            )
                                        } while (tagCursor.moveToNext())
                                        transactionChange =
                                            transactionChange.toBuilder().setTags(tags).build()
                                    }
                                    tagCursor.close()
                                }
                            }
                            result.add(transactionChange)
                        }
                    } while (c.moveToNext())
                }
                c.close()
            }
        }
        return result
    }

    private fun buildChangesUri(current_sync: Long, accountId: Long): Uri {
        return TransactionProvider.CHANGES_URI.buildUpon()
            .appendQueryParameter(DatabaseConstants.KEY_ACCOUNTID, accountId.toString())
            .appendQueryParameter(
                DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL,
                current_sync.toString()
            )
            .build()
    }

    private fun buildInitializationUri(accountId: Long): Uri {
        return TransactionProvider.CHANGES_URI.buildUpon()
            .appendQueryParameter(DatabaseConstants.KEY_ACCOUNTID, accountId.toString())
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_INIT, "1")
            .build()
    }

    @Throws(RemoteException::class)
    private fun hasLocalChanges(provider: ContentProviderClient, changesUri: Uri): Boolean {
        var result = false
        val c = provider.query(changesUri, arrayOf("count(*)"), null, null, null)
        if (c != null) {
            if (c.moveToFirst()) {
                result = c.getLong(0) > 0
            }
            c.close()
        }
        return result
    }

    private fun getBooleanSetting(
        provider: ContentProviderClient,
        prefKey: PrefKey,
        defaultValue: Boolean
    ): Boolean {
        val value = getStringSetting(provider, prefKey.key)
        return if (value != null) value == java.lang.Boolean.TRUE.toString() else defaultValue
    }

    private fun getStringSetting(provider: ContentProviderClient, prefKey: String): String? {
        var result: String? = null
        try {
            val cursor = provider.query(
                TransactionProvider.SETTINGS_URI, arrayOf(DatabaseConstants.KEY_VALUE),
                DatabaseConstants.KEY_KEY + " = ?", arrayOf(prefKey), null
            )
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    result = cursor.getString(0)
                }
                cursor.close()
            }
        } catch (remoteException: RemoteException) {
            CrashHandler.report(remoteException)
        }
        return result
    }

    private fun removeSetting(provider: ContentProviderClient, prefKey: String) {
        try {
            provider.delete(
                TransactionProvider.SETTINGS_URI,
                DatabaseConstants.KEY_KEY + " = ?",
                arrayOf(prefKey)
            )
        } catch (remoteException: RemoteException) {
            CrashHandler.report(remoteException)
        }
    }

    companion object {
        const val BATCH_SIZE = 100
        const val KEY_RESET_REMOTE_ACCOUNT = "reset_remote_account"
        const val KEY_UPLOAD_AUTO_BACKUP_URI = "upload_auto_backup_uri"
        const val KEY_UPLOAD_AUTO_BACKUP_NAME = "upload_auto_backup_name"

        //we pass the delay to the next sync via this extra
        const val KEY_NOTIFICATION_CANCELLED = "notification_cancelled"
        val LOCK_TIMEOUT_MINUTES = if (BuildConfig.DEBUG) 1 else 5
        private val IO_DEFAULT_DELAY_SECONDS = TimeUnit.MINUTES.toSeconds(5)
        private val IO_LOCK_DELAY_SECONDS =
            TimeUnit.MINUTES.toSeconds(LOCK_TIMEOUT_MINUTES.toLong())
        const val TAG = "SyncAdapter"
        @JvmStatic
        fun KEY_LAST_SYNCED_REMOTE(accountId: Long): String {
            return "last_synced_remote_$accountId"
        }

        @JvmStatic
        fun KEY_LAST_SYNCED_LOCAL(accountId: Long): String {
            return "last_synced_local_$accountId"
        }

        private val ioDefaultDelaySeconds: Long
            get() = System.currentTimeMillis() / 1000 + IO_DEFAULT_DELAY_SECONDS
        private val ioLockDelaySeconds: Long
            get() = System.currentTimeMillis() / 1000 + IO_LOCK_DELAY_SECONDS
        private val featureLoadDelaySeconds: Long
            get() = System.currentTimeMillis() / 1000 + 60

        fun log(): Timber.Tree {
            return Timber.tag(TAG)
        }
    }
}