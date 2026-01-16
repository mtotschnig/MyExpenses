package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.activity.StartScreen
import org.totschnig.myexpenses.compose.main.AccountScreenTab
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.TransactionSort
import org.totschnig.myexpenses.preference.EnumPreferenceAccessor
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PreferenceAccessor
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.GROUPING_AGGREGATE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.mapToListCatching
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount.Companion.fromCursor

class MyExpensesV2ViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : MyExpensesViewModel(application, savedStateHandle) {

    private val _activeFilter = MutableStateFlow<AccountGroupingKey?>(null)
    val activeFilter: StateFlow<AccountGroupingKey?> = _activeFilter.asStateFlow()

    // Functions for the UI to call
    fun setGrouping(grouping: AccountGrouping<*>) {
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
                it.fromCursor(currencyContext)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    }

    // Derived state: What are the available filter options for the current grouping?
    @OptIn(ExperimentalCoroutinesApi::class)
    val availableGroupFilters: StateFlow<List<AccountGroupingKey>> by lazy {
        accountGrouping.flow.flatMapLatest { grouping ->
            // This maps the master account list to a list of unique group keys
            accountDataV2.map { result ->
                result
                    ?.getOrNull()
                    ?.let { accounts -> grouping.sortedGroupKeys(accounts) }
                    ?: emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }


    val groupingMap: Map<String, PreferenceAccessor<Grouping, String>> = lazyMap {
        EnumPreferenceAccessor(
            dataStore = dataStore,
            key = stringPreferencesKey(it),
            defaultValue = Grouping.NONE
        )
    }

    val sortMap: Map<String, PreferenceAccessor<TransactionSort, String>> = lazyMap {
        EnumPreferenceAccessor(
            dataStore = dataStore,
            key = stringPreferencesKey(it),
            defaultValue = TransactionSort.DATE_DESC
        )
    }

    fun aggregateKey(grouping: AccountGrouping<*>, filter: AccountGroupingKey?) =
        if (grouping == AccountGrouping.NONE || filter == null) {
            GROUPING_AGGREGATE
        } else {
            "${grouping.name}_${filter.id}"
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAggregateGrouping: Flow<Grouping> by lazy {
        combine(accountGrouping.flow, _activeFilter) { grouping, filter ->
            grouping to filter
        }.flatMapLatest { (grouping, filter) ->
            groupingMap.getValue(
                aggregateKey(
                    grouping,
                    filter
                )
            ).flow
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAggregateSort: Flow<TransactionSort> by lazy {
        combine(accountGrouping.flow, _activeFilter) { grouping, filter ->
            grouping to filter
        }.flatMapLatest { (grouping, filter) ->
            sortMap.getValue(
                aggregateKey(
                    grouping,
                    filter
                )
            ).flow
        }
    }

    fun persistGroupingV2(grouping: Grouping) {
        viewModelScope.launch(context = coroutineContext()) {
            if (selectedAccountId == 0L) {
                groupingMap.getValue(
                    aggregateKey(
                        accountGrouping.flow.first(),
                        _activeFilter.value
                    )
                ).set(grouping)
            } else {
                persistGrouping(grouping)
            }
        }
    }

    fun persistSortV2(transactionSort: TransactionSort) {
        viewModelScope.launch(context = coroutineContext()) {
            if (selectedAccountId == 0L) {
                sortMap.getValue(
                    aggregateKey(
                        accountGrouping.flow.first(),
                        _activeFilter.value
                    )
                ).set(transactionSort)
            } else {
                persistSort(transactionSort.column, transactionSort.sortDirection)
            }
        }
    }

    val startScreen: StartScreen by lazy {
        prefHandler.enumValueOrDefault(PrefKey.UI_START_SCREEN, StartScreen.LastVisited)
    }

    fun setLastVisited(screen: StartScreen) {
        prefHandler.putString(PrefKey.UI_SCREEN_LAST_VISITED, screen.name)
    }

    fun setLastVisited(accountScreenTab: AccountScreenTab) {
        setLastVisited(
            when (accountScreenTab) {
                AccountScreenTab.LIST -> StartScreen.Accounts
                AccountScreenTab.BALANCE_SHEET -> StartScreen.BalanceSheet
            }
        )
    }

}