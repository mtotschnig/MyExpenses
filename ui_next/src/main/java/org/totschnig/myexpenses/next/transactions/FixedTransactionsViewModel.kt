package org.totschnig.myexpenses.next.transactions

import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import androidx.lifecycle.AndroidViewModel
import android.content.ContentResolver
import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.adapter.TransactionPagingSource
import org.totschnig.myexpenses.db2.tagMapFlow
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.TransactionIdCriterion
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Transaction2

/**
 * A fixed set of transactions, e.g. the debits of a contract or the payments of an income,
 * shown as in the transaction list of an account. They are loaded from the aggregate of all
 * accounts, since they can span several accounts.
 */
class FixedTransactionsViewModel(
    application: Application,
    private val currencyContext: CurrencyContext,
    private val prefHandler: PrefHandler,
) : AndroidViewModel(application) {

    private val contentResolver: ContentResolver get() = getApplication<Application>().contentResolver

    /** Never persisted, so that it cannot affect the account screens */
    private val filter = MutableStateFlow<Criterion?>(null)

    private val tags: StateFlow<Map<String, Pair<String, Int?>>> = contentResolver.tagMapFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** The transactions set with [show] */
    val transactions: Flow<PagingData<Transaction2>> = run {
        val allAccounts = PageAccount(
            id = HOME_AGGREGATE_ID,
            currencyUnit = currencyContext.homeCurrencyUnit,
            label = "",
            accountGrouping = AccountGrouping.NONE
        )
        Pager(PagingConfig(pageSize = 50)) {
            TransactionPagingSource(
                getApplication(),
                allAccounts,
                filter,
                tags,
                currencyContext,
                viewModelScope,
                prefHandler
            )
        }.flow.cachedIn(viewModelScope)
    }

    fun show(label: String, ids: List<Long>) {
        filter.value = TransactionIdCriterion(label, ids)
    }
}
