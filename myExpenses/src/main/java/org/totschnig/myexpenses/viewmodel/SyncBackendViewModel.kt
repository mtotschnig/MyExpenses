package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.annimon.stream.Collectors
import com.annimon.stream.Exceptional
import com.squareup.sqlbrite3.SqlBrite
import kotlinx.coroutines.Dispatchers
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.sync.json.AccountMetaData
import java.util.*

class SyncBackendViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    private val localAccountInfo = MutableLiveData<Map<String, String?>>()

    fun getAccounts(context: Context) = GenericAccountService.getAccountNamesWithEncryption(context)

    fun getLocalAccountInfo(): LiveData<Map<String, String?>> = localAccountInfo

    fun accountMetadata(accountName: String): LiveData<Exceptional<List<AccountMetaData>>> = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(SyncBackendProviderFactory.get(getApplication<MyApplication>(), GenericAccountService.GetAccount(accountName), false).map { it.remoteAccountList.collect(Collectors.toList()) })
    }

    fun loadLocalAccountInfo() {
        disposable = briteContentResolver.createQuery(TransactionProvider.ACCOUNTS_BASE_URI,
                arrayOf(DatabaseConstants.KEY_UUID, DatabaseConstants.KEY_SYNC_ACCOUNT_NAME), null, null, null, false)
                .map(SqlBrite.Query::run)
                .subscribe { cursor ->
                    val uuid2syncMap: MutableMap<String, String?> = HashMap()
                    cursor?.let {
                        it.use {
                            it.moveToFirst()
                            while (!it.isAfterLast) {
                                val columnIndexUuid = it.getColumnIndex(DatabaseConstants.KEY_UUID)
                                val columnIndexSyncAccountName = it.getColumnIndex(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)
                                uuid2syncMap[it.getString(columnIndexUuid)] = it.getString(columnIndexSyncAccountName)
                                it.moveToNext()
                            }
                        }
                        localAccountInfo.postValue(uuid2syncMap)
                    }
                }
    }
}