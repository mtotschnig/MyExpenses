package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.json.AccountMetaData

class SetupSyncViewModel(application: Application) : SyncViewModel(application) {
    val dialogState: SnapshotStateMap<String, SyncSource?> = mutableStateMapOf()

    fun setupSynchronization(
        accountName: String,
        localAccounts: List<LocalAccount>,
        remoteAccounts: List<AccountMetaData>,
        conflicts: List<Triple<LocalAccount, AccountMetaData, SyncSource>>
    ): LiveData<Result<Boolean>> = liveData(context = coroutineContext()) {
        emit(kotlin.runCatching {
            val syncLocalList =
                conflicts.filter { it.third == SyncSource.LOCAL }
            syncLocalList.forEach {
                resetRemote(accountName, it.first.uuid)
            }
            val syncRemoteList = conflicts.filter { it.third == SyncSource.REMOTE }
            deleteAccountsInternal(syncRemoteList.map { it.first.id }.toLongArray()).onFailure {
                throw it
            }
            (remoteAccounts + syncRemoteList.map { it.second }).map { it.toAccount(currencyContext.homeCurrencyString, accountName) }.forEach {
                doSave(it)
                dialogState[it.uuid!!] = SyncSource.COMPLETED
            }
            val uuids =
                (localAccounts + syncLocalList.map { it.first }).map { it.uuid }.toTypedArray()
            configureLocalAccountForSync(accountName, *uuids)
            for (uuid in uuids) {
                dialogState[uuid] = SyncSource.COMPLETED
            }
            GenericAccountService.requestSync(accountName, expedited = false)
        })
    }

    enum class SyncSource {
        /**
         * DEFAULT is used for accounts that only exist locally or remotely, so there is no conflict,
         * and there is only one possible source
         */
        DEFAULT,
        LOCAL,
        REMOTE,

        /**
         * Set after setup is completed
         */
        COMPLETED
    }
}