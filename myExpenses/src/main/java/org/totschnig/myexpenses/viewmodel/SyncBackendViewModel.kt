package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.annimon.stream.Collectors
import com.annimon.stream.Exceptional
import kotlinx.coroutines.Dispatchers
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.sync.json.AccountMetaData

class SyncBackendViewModel(application: Application) : AbstractSyncBackendViewModel(application) {

    override fun getAccounts(context: Context) = GenericAccountService.getAccountNamesWithEncryption(context)

    override fun accountMetadata(accountName: String): LiveData<Exceptional<List<Exceptional<AccountMetaData>>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(SyncBackendProviderFactory.get(getApplication<MyApplication>(), GenericAccountService.getAccount(accountName), false).map { it.remoteAccountList.collect(Collectors.toList()) })
    }

}