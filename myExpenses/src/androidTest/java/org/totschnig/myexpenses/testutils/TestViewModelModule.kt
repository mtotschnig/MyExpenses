package org.totschnig.myexpenses.testutils

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.test.espresso.idling.CountingIdlingResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.TestApp
import org.totschnig.myexpenses.adapter.TransactionPagingSource
import org.totschnig.myexpenses.di.ViewModelModule
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.sync.json.AccountMetaData
import org.totschnig.myexpenses.viewmodel.AbstractSyncBackendViewModel
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2

object TestViewModelModule : ViewModelModule() {
    override fun provideSyncBackendViewModelClass(): Class<out AbstractSyncBackendViewModel> =
        FakeSyncBackendViewModel::class.java

    override fun provideMyExpensesViewModelClass(): Class<out MyExpensesViewModel> {
        return DecoratingMyExpensesViewModel::class.java
    }
}

class DecoratingMyExpensesViewModel(application: Application,
                                    savedStateHandle: SavedStateHandle
) : MyExpensesViewModel(application, savedStateHandle) {
    val countingResource = CountingIdlingResource("TransactionPaging", true)

    override fun buildTransactionPagingSource(account: PageAccount) =
        DecoratedTransactionPagingSource(
            getApplication(),
            account,
            filterPersistence.getValue(account.id).whereFilter,
            tags,
            currencyContext,
            viewModelScope,
            countingResource,
            prefHandler
        )
}

class FakeSyncBackendViewModel(application: Application) :
    AbstractSyncBackendViewModel(application) {
    override fun getAccounts(context: Context): List<Pair<String, Boolean>> =
        with(getApplication<TestApp>().fixture) {
            listOf(
                syncAccount1 to true,
                syncAccount2 to false,
                syncAccount3 to false
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
        emit(Result.success(listOf(Result.success(AccountMetaData.from(syncedAccount, getApplication<MyApplication>().appComponent.currencyContext().homeCurrencyString)))))
    }
}

class DecoratedTransactionPagingSource(
    context: Context,
    account: PageAccount,
    whereFilter: StateFlow<Criterion?>,
    tags: StateFlow<Map<String, Pair<String, Int?>>>,
    currencyContext: CurrencyContext,
    coroutineScope: CoroutineScope,
    private val countingIdlingResource: CountingIdlingResource,
    prefHandler: PrefHandler
) : TransactionPagingSource(
    context,
    account,
    whereFilter,
    tags,
    currencyContext,
    coroutineScope,
    prefHandler
) {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction2> {
        countingIdlingResource.increment()
        return super.load(params)
    }

    override fun onLoadFinished() {
        countingIdlingResource.decrement()
    }
}
