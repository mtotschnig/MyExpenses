package org.totschnig.myexpenses.activity

import android.content.Context
import android.database.Cursor
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.idling.CountingIdlingResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.totschnig.myexpenses.adapter.TransactionPagingSource
import org.totschnig.myexpenses.provider.CheckSealedHandler
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2

class TestMyExpenses: MyExpenses() {
    val countingResource = CountingIdlingResource("TransactionPaging")

    lateinit var decoratedCheckSealedHandler: CheckSealedHandler

    override fun buildCheckSealedHandler() = decoratedCheckSealedHandler

    override fun buildTransactionPagingSourceFactory(account: PageAccount): () -> TransactionPagingSource = {
        DecoratedTransactionPagingSource(
            this,
            account,
            viewModel.filterPersistence.getValue(account.id).whereFilterAsFlow,
            lifecycleScope,
            countingResource
        )
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

    override fun onLoadFinished(cursor: Cursor): List<Transaction2> {
        countingIdlingResource.decrement()
        return super.onLoadFinished(cursor)
    }
}