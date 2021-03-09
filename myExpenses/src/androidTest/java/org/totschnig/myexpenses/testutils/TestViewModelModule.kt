package org.totschnig.myexpenses.testutils

import android.app.Application
import android.content.Context
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.annimon.stream.Exceptional
import dagger.Provides
import org.totschnig.myexpenses.TestApp
import org.totschnig.myexpenses.di.ViewModelModule
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.viewmodel.AbstractSyncBackendViewModel

object TestViewModelModule : ViewModelModule() {
    @Provides
    override fun provideSyncBackendViewModelClass(): Class<out AbstractSyncBackendViewModel> = FakeSyncBackendViewModel::class.java
}

class FakeSyncBackendViewModel(application: Application) : AbstractSyncBackendViewModel(application) {
    override fun getAccounts(context: Context): List<Pair<String, Boolean>> = with(getApplication<TestApp>().fixture) {
        listOf(
                Pair.create(syncAccount1, true),
                Pair.create(syncAccount2, false),
                Pair.create(syncAccount3, false)
        )
    }

    override fun accountMetadata(accountName: String): LiveData<Exceptional<List<Exceptional<AccountMetaData>>>> = liveData {
        val syncedAccount = with(getApplication<TestApp>().fixture) {
            when (accountName) {
                syncAccount1 -> account1
                syncAccount2 -> account2
                syncAccount3 -> account3
                else -> throw IllegalStateException()
            }
        }
        emit(Exceptional.of {
            listOf<Exceptional<AccountMetaData>>(Exceptional.of { AccountMetaData.from(syncedAccount) })
        })
    }
}
