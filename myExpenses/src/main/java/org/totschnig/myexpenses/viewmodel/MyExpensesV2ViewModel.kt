package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.mapToListCatching
import org.totschnig.myexpenses.viewmodel.data.FullAccount

class MyExpensesV2ViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : MyExpensesViewModel(application, savedStateHandle) {

    private val _activeFilter = MutableStateFlow<AccountGroupingKey?>(null)
    val activeFilter: StateFlow<AccountGroupingKey?> = _activeFilter.asStateFlow()


    // Functions for the UI to call
    fun setGrouping(grouping: AccountGrouping) {
        viewModelScope.launch {
            accountGrouping.set(grouping)
        }
    }

    //remove active filter if it is different from passed in grouping key
    fun maybeResetFilter(filter: AccountGroupingKey) {
        if (_activeFilter.value != filter) {
            setFilter(null)
        }
    }

    fun setFilter(filter: AccountGroupingKey?) {
        _activeFilter.value = filter
    }

    fun navigateToGroup(filter: AccountGroupingKey) {
        setFilter(filter)
        selectAccount(0)
    }

    val accountDataV2: StateFlow<Result<List<FullAccount>>?> by lazy {
        contentResolver.observeQuery(
            uri = ACCOUNTS_URI
                .buildUpon()
                .appendQueryParameter(
                    TransactionProvider.QUERY_PARAMETER_FULL_PROJECTION_WITH_SUMS,
                    "now"
                )
                .build(),
            notifyForDescendants = true
        )
            .mapToListCatching {
                FullAccount.fromCursor(it, currencyContext)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    }

    // Derived state: What are the available filter options for the current grouping?
    @OptIn(ExperimentalCoroutinesApi::class)
    val availableGroupFilters: StateFlow<List<AccountGroupingKey>> by lazy {
        accountGrouping.flow.flatMapLatest { grouping ->
            // This maps the master account list to a list of unique group keys
            accountDataV2.map { result ->
                result?.getOrNull()?.let { accounts ->
                    when (grouping) {
                        AccountGrouping.NONE -> emptyList()
                        else -> accounts.map { grouping.getGroupKey(it) }.distinct()
                    }
                } ?: emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
}