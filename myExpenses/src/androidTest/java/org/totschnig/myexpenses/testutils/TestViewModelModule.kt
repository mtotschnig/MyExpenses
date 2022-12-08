package org.totschnig.myexpenses.testutils

import android.app.Application
import android.content.Context
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.test.espresso.idling.CountingIdlingResource
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.totschnig.myexpenses.TestApp
import org.totschnig.myexpenses.adapter.TransactionPagingSource
import org.totschnig.myexpenses.di.ViewModelModule
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.viewmodel.AbstractSyncBackendViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2

object TestViewModelModule : ViewModelModule() {
    @Provides
    override fun provideSyncBackendViewModelClass(): Class<out AbstractSyncBackendViewModel> =
        FakeSyncBackendViewModel::class.java

    override fun provideMyExpensesViewModelClass(): Class<out MyExpensesViewModel> {
        return DecoratingMyExpensesViewModel::class.java
    }
}

class DecoratingMyExpensesViewModel(application: Application,
                                    savedStateHandle: SavedStateHandle
) : MyExpensesViewModel(application, savedStateHandle) {
    val countingResource = CountingIdlingResource("TransactionPaging")

    override fun buildTransactionPagingSource(account: PageAccount) =
        DecoratedTransactionPagingSource(
            getApplication(),
            account,
            filterPersistence.getValue(account.id).whereFilterAsFlow,
            viewModelScope,
            countingResource
        )
}

class FakeSyncBackendViewModel(application: Application) :
    AbstractSyncBackendViewModel(application) {
    override fun getAccounts(context: Context): List<Pair<String, Boolean>> =
        with(getApplication<TestApp>().fixture) {
            listOf(
                Pair.create(syncAccount1, true),
                Pair.create(syncAccount2, false),
                Pair.create(syncAccount3, false)
            )
        }

    override fun accountMetadata(
        accountName: String,
        isFeatureAvailable: Boolean
    ): LiveData<Result<List<Result<AccountMetaData>>>> = liveData {
        val syncedAccount = with(getApplication<TestApp>().fixture) {
            when (accountName) {
                syncAccount1 -> account1
                syncAccount2 -> account2
                syncAccount3 -> account3
                else -> throw IllegalStateException()
            }
        }
        emit(Result.success(listOf(Result.success(AccountMetaData.from(syncedAccount)))))
    }
}

class DecoratedTransactionPagingSource(
    context: Context,
    account: PageAccount, whereFilter: StateFlow<WhereFilter>,
    coroutineScope: CoroutineScope,
    private val countingIdlingResource: CountingIdlingResource
) : TransactionPagingSource(context, account, whereFilter, coroutineScope) {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction2> {
        countingIdlingResource.increment()
        return super.load(params)
    }

    override fun onLoadFinished() {
        countingIdlingResource.decrement()
    }
}
