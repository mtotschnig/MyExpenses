package org.totschnig.myexpenses.viewmodel

import android.accounts.AccountManager
import android.app.Application
import android.content.Context
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.squareup.sqlbrite3.SqlBrite
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_BASE_URI
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.loadPassword
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.TextUtils

abstract class AbstractSyncBackendViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    private val localAccountInfo = MutableLiveData<Map<String, String?>>()
    abstract fun getAccounts(context: Context): List<Pair<String, Boolean>>

    fun getLocalAccountInfo(): LiveData<Map<String, String?>> = localAccountInfo

    abstract fun accountMetadata(accountName: String): LiveData<Result<List<Result<AccountMetaData>>>>

    fun loadLocalAccountInfo() {
        disposable = briteContentResolver.createQuery(
            ACCOUNTS_BASE_URI,
            arrayOf(DatabaseConstants.KEY_UUID, DatabaseConstants.KEY_SYNC_ACCOUNT_NAME),
            null,
            null,
            null,
            false
        )
            .map(SqlBrite.Query::run)
            .subscribe { c ->
                c?.use { cursor ->
                    val uuid2syncMap: MutableMap<String, String?> = HashMap()
                    cursor.moveToFirst()
                    while (!cursor.isAfterLast) {
                        val columnIndexUuid = cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_UUID)
                        val columnIndexSyncAccountName =
                            cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)
                        cursor.getString(columnIndexUuid)?.let {
                            uuid2syncMap[it] = cursor.getString(columnIndexSyncAccountName)
                        }
                        cursor.moveToNext()
                    }
                    localAccountInfo.postValue(uuid2syncMap)
                }
            }
    }

    fun syncUnlink(uuid: String) = liveData(context = coroutineContext()) {
        emit(if (Account.findByUuid(uuid).takeIf { it != -1L }?.let {
                Account.getInstanceFromDb(it)
            }?.let { account ->
                account.syncAccountName?.let { syncAccountName ->
                    val accountManager = AccountManager.get(getApplication())
                    val syncAccount = getAccount(syncAccountName)
                    accountManager.setUserData(
                        syncAccount,
                        SyncAdapter.KEY_LAST_SYNCED_LOCAL(account.id),
                        null
                    )
                    accountManager.setUserData(
                        syncAccount,
                        SyncAdapter.KEY_LAST_SYNCED_REMOTE(account.id),
                        null
                    )
                    account.syncAccountName = null
                    account.save()
                }
            } != null) ResultUnit else Result.failure(Exception("ERROR")))
    }

    fun syncCheck(uuid: String, syncAccountName: String) = liveData(context = coroutineContext()) {
        emit(GenericAccountService.getSyncBackendProvider(getApplication(), syncAccountName)
            .mapCatching { syncBackendProvider ->
                if (syncBackendProvider.remoteAccountList.mapNotNull { it.getOrNull() }
                        .any { it.uuid() == uuid }
                ) {
                    throw Exception(
                        TextUtils.concatResStrings(
                            getApplication(), " ",
                            R.string.link_account_failure_2, R.string.link_account_failure_3
                        )
                                + "(" + TextUtils.concatResStrings(
                            getApplication(), ", ", R.string.menu_settings,
                            R.string.pref_manage_sync_backends_title
                        ) + ")"
                    )
                }
            })
    }

    fun loadPassword(syncAccountName: String) = liveData(context = coroutineContext()) {
        emit(loadPassword(contentResolver, syncAccountName))
    }
}