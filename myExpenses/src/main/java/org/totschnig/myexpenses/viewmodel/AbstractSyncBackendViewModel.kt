package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.annimon.stream.Exceptional
import com.squareup.sqlbrite3.SqlBrite
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_BASE_URI
import org.totschnig.myexpenses.sync.json.AccountMetaData

abstract class AbstractSyncBackendViewModel(application: Application): ContentResolvingAndroidViewModel(application) {
    protected val localAccountInfo = MutableLiveData<Map<String, String?>>()
    abstract fun getAccounts(context: Context): List<Pair<String, Boolean>>

    fun getLocalAccountInfo(): LiveData<Map<String, String?>> = localAccountInfo

    abstract fun accountMetadata(accountName: String): LiveData<Exceptional<List<Exceptional<AccountMetaData>>>>

    fun loadLocalAccountInfo() {
        disposable = briteContentResolver.createQuery(ACCOUNTS_BASE_URI,
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