package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.sync.json.AccountMetaData

class SyncBackendViewModel(application: Application) : AbstractSyncBackendViewModel(application) {

    override fun getAccounts(context: Context) =
        GenericAccountService.getAccountNamesWithEncryption(context)

    override fun accountMetadata(accountName: String): LiveData<Result<List<Result<AccountMetaData>>>> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(
                SyncBackendProviderFactory[
                        getApplication(),
                        GenericAccountService.getAccount(accountName),
                        false
                ]
                    .mapCatching { it.remoteAccountList })
        }
}