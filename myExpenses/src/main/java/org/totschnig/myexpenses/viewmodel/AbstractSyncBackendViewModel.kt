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

abstract class AbstractSyncBackendViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    private val localAccountInfo = MutableLiveData<Map<String, String?>>()
    abstract fun getAccounts(context: Context): List<Pair<String, Boolean>>

    fun getLocalAccountInfo(): LiveData<Map<String, String?>> = localAccountInfo

    abstract fun accountMetadata(accountName: String): LiveData<Exceptional<List<Exceptional<AccountMetaData>>>>

    fun loadLocalAccountInfo() {
        disposable = briteContentResolver.createQuery(ACCOUNTS_BASE_URI,
                arrayOf(DatabaseConstants.KEY_UUID, DatabaseConstants.KEY_SYNC_ACCOUNT_NAME), null, null, null, false)
                .map(SqlBrite.Query::run)
                .subscribe { c ->
                    c?.use { cursor ->
                        val uuid2syncMap: MutableMap<String, String?> = HashMap()
                        cursor.moveToFirst()
                        while (!cursor.isAfterLast) {
                            val columnIndexUuid = cursor.getColumnIndex(DatabaseConstants.KEY_UUID)
                            val columnIndexSyncAccountName = cursor.getColumnIndex(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)
                            cursor.getString(columnIndexUuid)?.let { uuid2syncMap[it] = cursor.getString(columnIndexSyncAccountName) }
                            cursor.moveToNext()
                        }
                        localAccountInfo.postValue(uuid2syncMap)
                    }
                }
    }
}