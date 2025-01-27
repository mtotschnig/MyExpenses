package org.totschnig.myexpenses.viewmodel

import android.accounts.AccountManager
import android.app.Application
import android.content.Context
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.LocalAccountInfo
import org.totschnig.myexpenses.db2.findAccountByUuidWithExtraColumn
import org.totschnig.myexpenses.db2.updateAccount
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_BASE_URI
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.loadPassword
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.TextUtils

abstract class AbstractSyncBackendViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    val localAccountInfo = contentResolver.observeQuery(
        uri = ACCOUNTS_BASE_URI,
        projection = arrayOf(KEY_UUID, KEY_SYNC_ACCOUNT_NAME, KEY_SEALED),
        selection = null,
        selectionArgs = null,
        sortOrder = null,
        notifyForDescendants = false
    ).mapToList { LocalAccountInfo(it.getString(0), it.getStringOrNull(1), it.getBoolean(2)) }

    abstract fun getAccounts(context: Context): List<Pair<String, Boolean>>

    abstract fun accountMetadata(accountName: String, isFeatureAvailable: Boolean): LiveData<Result<List<Result<AccountMetaData>>>>?

    fun syncUnlink(uuid: String) = liveData(context = coroutineContext()) {
        emit(if (repository.findAccountByUuidWithExtraColumn(uuid, KEY_SYNC_ACCOUNT_NAME)?.let { account ->
                account.second?.let { syncAccountName ->
                    val accountManager = AccountManager.get(getApplication())
                    val syncAccount = getAccount(syncAccountName)
                    accountManager.setUserData(
                        syncAccount,
                        SyncAdapter.KEY_LAST_SYNCED_LOCAL(account.first),
                        null
                    )
                    accountManager.setUserData(
                        syncAccount,
                        SyncAdapter.KEY_LAST_SYNCED_REMOTE(account.first),
                        null
                    )

                    repository.updateAccount(account.first) {
                        putNull(KEY_SYNC_ACCOUNT_NAME)
                    }
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
                            getApplication(),
                            R.string.link_account_failure_2, R.string.link_account_failure_3
                        )
                                + "(" + TextUtils.concatResStrings(
                            getApplication(), ", ", R.string.settings_label,
                            R.string.pref_manage_sync_backends_title
                        ) + ")"
                    )
                }
            })
    }

    fun loadPassword(syncAccountName: String) = liveData(context = coroutineContext()) {
        emit(loadPassword(getApplication(), syncAccountName))
    }
}