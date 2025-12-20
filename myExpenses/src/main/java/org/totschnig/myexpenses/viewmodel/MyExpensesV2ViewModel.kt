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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.provider.KEY_VISIBLE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.mapToListCatching
import org.totschnig.myexpenses.viewmodel.data.FullAccount

class MyExpensesV2ViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : MyExpensesViewModel(application, savedStateHandle) {

    // State for Level 1 decision
    private val _activeGrouping = MutableStateFlow(AccountGrouping.NONE)
    val activeGrouping: StateFlow<AccountGrouping> = _activeGrouping.asStateFlow()

    // State for Level 2 decision (String key, e.g., "EUR" or "Checking")
    private val _activeGroupFilter = MutableStateFlow<String?>(null)
    val activeGroupFilter: StateFlow<String?> = _activeGroupFilter.asStateFlow()

    // Functions for the UI to call
    fun setGrouping(grouping: AccountGrouping) {
        _activeGrouping.value = grouping
        _activeGroupFilter.value = null // Reset filter when grouping changes
    }

    fun setGroupFilter(filter: String?) {
        _activeGroupFilter.value = filter
    }

    val accountDataV2: StateFlow<Result<List<FullAccount>>?> by lazy {
        contentResolver.observeQuery(
            uri = ACCOUNTS_URI
                .buildUpon()
                .appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_FULL_PROJECTION_WITH_SUMS)
                .build(),
            selection = "$KEY_VISIBLE = 1",
            notifyForDescendants = true
        )
            .mapToListCatching {
                FullAccount.fromCursor(it, currencyContext)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    }

    // Derived state: What are the available filter options for the current grouping?
    @OptIn(ExperimentalCoroutinesApi::class)
    val availableGroupFilters: StateFlow<List<String>> = activeGrouping.flatMapLatest { grouping ->
        // This maps the master account list to a list of unique group keys
        accountDataV2.map { result ->
            result?.getOrNull()?.let { accounts ->
                when (grouping) {
                    AccountGrouping.CURRENCY -> accounts.map { it.currencyUnit.code }.distinct()
                    AccountGrouping.TYPE -> accounts.map { it.type.name }.distinct() // Assuming type is an enum
                    //AccountGrouping.BY_FLAG -> { /* logic to get distinct flags */ emptyList() }
                    AccountGrouping.NONE -> emptyList()
                }
            } ?: emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived state: The final, filtered list of accounts for the TabRow
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredAccounts: StateFlow<Result<List<FullAccount>>?> =
        combine(accountDataV2, activeGrouping, activeGroupFilter) { result, grouping, filter ->
            result?.map { accounts ->
                if (grouping == AccountGrouping.NONE || filter == null) {
                    accounts // If no grouping or filter, show all
                } else {
                    accounts.filter { account ->
                        // Filter the list based on the active strategy and filter key
                        when (grouping) {
                            AccountGrouping.CURRENCY -> account.currencyUnit.code == filter
                            AccountGrouping.TYPE -> account.type.name == filter
                            //AccountGrouping.BY_FLAG -> { /* logic to filter by flag */ true }
                            AccountGrouping.NONE -> true
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}