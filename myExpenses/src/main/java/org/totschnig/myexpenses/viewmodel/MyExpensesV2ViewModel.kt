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


    // Functions for the UI to call
    fun setGrouping(grouping: AccountGrouping) {
        _activeGrouping.value = grouping
    }

    val accountDataV2: StateFlow<Result<List<FullAccount>>?> by lazy {
        contentResolver.observeQuery(
            uri = ACCOUNTS_URI
                .buildUpon()
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_FULL_PROJECTION_WITH_SUMS, "now")
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
    val availableGroupFilters: StateFlow<List<Any>> = activeGrouping.flatMapLatest { grouping ->
        // This maps the master account list to a list of unique group keys
        accountDataV2.map { result ->
            result?.getOrNull()?.let { accounts ->
                when (grouping) {
                    AccountGrouping.CURRENCY -> accounts.map { it.currencyUnit }.distinct()
                    AccountGrouping.TYPE -> accounts.map { it.type }.distinct() // Assuming type is an enum
                    AccountGrouping.FLAG -> { accounts.map { it.flag }.distinct() }
                    AccountGrouping.NONE -> emptyList()
                }
            } ?: emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}