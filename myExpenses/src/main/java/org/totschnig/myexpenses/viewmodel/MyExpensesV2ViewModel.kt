package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Intent
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.StartScreen
import org.totschnig.myexpenses.compose.transactions.Action
import org.totschnig.myexpenses.compose.transactions.FabStyle
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.TransactionSort
import org.totschnig.myexpenses.preference.EnumPreferenceAccessor
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PreferenceAccessor
import org.totschnig.myexpenses.preference.PreferenceState
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.preference.menu
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.GROUPING_AGGREGATE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.mapToListCatching
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.data.AggregateAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount.Companion.fromCursor
import timber.log.Timber

enum class AccountsScreenTab(@param:StringRes val resourceId: Int) {
    LIST(R.string.accounts),
    BALANCE_SHEET(R.string.balance_sheet)
}

class MyExpensesV2ViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : MyExpensesViewModel(application, savedStateHandle) {

    private val _intentEvents = MutableSharedFlow<Intent>(replay = 1)
    val intentEvents = _intentEvents.asSharedFlow()

    fun handleIntent(intent: Intent) {
        viewModelScope.launch {
            intent.extras?.let { extras ->
                if (extras.containsKey(KEY_ROWID)) {
                    val accountId = extras.getLong(KEY_ROWID)
                    if (accountId >= 0) {
                        selectAccount(extras.getLong(KEY_ROWID))
                    } else {
                        //legacy handling from account widget
                        if (accountId == HOME_AGGREGATE_ID) {
                            setGrouping(AccountGrouping.NONE)
                        } else {
                            setGrouping(AccountGrouping.CURRENCY)
                            extras.getString(KEY_CURRENCY)?.let {
                                setFilter(currencyContext[it])
                            }
                        }
                        selectAccount(0)
                    }
                    _intentEvents.emit(intent)
                }
            }
        }
    }

    private val _activeFilter = MutableStateFlow<AccountGroupingKey?>(null)
    val activeFilter: StateFlow<AccountGroupingKey?> = _activeFilter.asStateFlow()

    private val _currentAccountsTab by lazy {
        MutableStateFlow(if (startScreen == StartScreen.BalanceSheet) AccountsScreenTab.BALANCE_SHEET else AccountsScreenTab.LIST)
    }

    val currentAccountsTab by lazy { _currentAccountsTab.asStateFlow() }

    val lastAction by lazy {
        EnumPreferenceAccessor(
            dataStore,
            stringPreferencesKey("lastAction"),
            Action.Expense
        )
    }

    val fabStyle by lazy {
        dataStore.data.map {
            enumValueOrDefault(
                it[prefHandler.getStringPreferencesKey(PrefKey.FAB_STYLE)],
                FabStyle.Standard
            )
        }
    }

    fun setGrouping(grouping: AccountGrouping<*>) {
        setFilter(null)
        viewModelScope.launch {
            accountGrouping.set(grouping)
        }
    }

    fun maybeResetFilter(filter: AccountGroupingKey) {
        if (_activeFilter.value != filter) {
            setFilter(null)
        }
    }

    fun setFilter(filter: AccountGroupingKey?) {
        _activeFilter.value = filter
        if (filter != null) {
            prefHandler.putString(PrefKey.UI_SCREEN_LAST_ACCOUNT_GROUP_FILTER, filter.id.toString())
        } else {
            prefHandler.remove(PrefKey.UI_SCREEN_LAST_ACCOUNT_GROUP_FILTER)
        }
    }

    fun setStartFilter() {
        Timber.d("setStartFilter")
        startFilter?.let { start ->
            availableGroupFilters.value?.let { available ->
                available.firstOrNull { it.id.toString() == start }?.let {
                    setFilter(it)
                }
            }
        }
    }

    fun navigateToGroup(filter: AccountGroupingKey?) {
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

    val accountList by lazy {
        combine(
            accountDataV2.mapNotNull { it?.getOrNull() },
            _activeFilter,
            accountGrouping.flow,
            currentAggregateGrouping,
            currentAggregateSort,
        ) { accounts, activeFilter, accountGrouping, aggregateGrouping, aggregateSort ->
            val filteredByGroupFilter =
                if (activeFilter == null || accountGrouping == AccountGrouping.NONE)
                    accounts
                else
                    accounts.filter { account -> accountGrouping.getGroupKey(account) == activeFilter }
            val aggregateAccountGrouping =
                if (activeFilter != null) accountGrouping else AccountGrouping.NONE
            //if we group by flag, and filter by a given flag,
            // we want to show all accounts with that flag ignoring visibility
            val filteredByVisibility =
                if (accountGrouping == AccountGrouping.FLAG && activeFilter != null)
                    filteredByGroupFilter
                else
                    filteredByGroupFilter.filter { it.visible }
            if (filteredByGroupFilter.size < 2) filteredByVisibility
            else {
                val filteredForTotals = filteredByGroupFilter.filter { !it.excludeFromTotals }
                filteredByVisibility + AggregateAccount(
                    currencyUnit = activeFilter as? CurrencyUnit
                        ?: currencyContext.homeCurrencyUnit,
                    type = if (accountGrouping == AccountGrouping.TYPE) activeFilter as? AccountType else null,
                    flag = if (accountGrouping == AccountGrouping.FLAG) activeFilter as? AccountFlag else null,
                    grouping = aggregateGrouping,
                    accountGrouping = aggregateAccountGrouping,
                    sortBy = aggregateSort.column,
                    sortDirection = aggregateSort.sortDirection,
                    equivalentOpeningBalance = filteredForTotals.sumOf { it.equivalentOpeningBalance },
                    equivalentCurrentBalance = filteredForTotals.sumOf { it.equivalentCurrentBalance },
                    equivalentSumIncome = filteredForTotals.sumOf { it.equivalentSumIncome },
                    equivalentSumExpense = filteredForTotals.sumOf { it.equivalentSumExpense },
                    equivalentSumTransfer = filteredForTotals.sumOf { it.equivalentSumTransfer },
                    equivalentTotal = filteredForTotals.sumOf {
                        it.equivalentTotal ?: it.equivalentCurrentBalance
                    },
                ).let { aggregateAccount ->
                    if (aggregateAccountGrouping == AccountGrouping.CURRENCY) aggregateAccount.copy(
                        openingBalance = filteredForTotals.sumOf { it.openingBalance },
                        currentBalance = filteredForTotals.sumOf { it.currentBalance },
                        sumIncome = filteredForTotals.sumOf { it.sumIncome },
                        sumExpense = filteredForTotals.sumOf { it.sumExpense },
                        sumTransfer = filteredForTotals.sumOf { it.sumTransfer },
                        total = filteredForTotals.sumOf { it.total ?: it.currentBalance },
                    ) else aggregateAccount
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    // Derived state: What are the available filter options for the current grouping?
    @OptIn(ExperimentalCoroutinesApi::class)
    val availableGroupFilters: StateFlow<List<AccountGroupingKey>?> by lazy {
        accountGrouping.statefulFlow.flatMapLatest { preferenceState ->
            if (preferenceState is PreferenceState.Loading) {
                emptyFlow()
            } else {
                accountDataV2.map { result ->
                    result
                        ?.getOrNull()
                        ?.let { accounts ->
                            (preferenceState as PreferenceState.Loaded).value.sortedGroupKeys(
                                accounts
                            )
                        }
                }
            }

        }.stateIn(viewModelScope, SharingStarted.Lazily, null)
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
            if (selectedAccountId.value == 0L) {
                groupingMap.getValue(
                    aggregateKey(
                        accountGrouping.get(),
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
            if (selectedAccountId.value == 0L) {
                sortMap.getValue(
                    aggregateKey(
                        accountGrouping.get(),
                        _activeFilter.value
                    )
                ).set(transactionSort)
            } else {
                persistSort(transactionSort.column, transactionSort.sortDirection)
            }
        }
    }

    private val _startScreen: StartScreen by lazy {
        prefHandler.enumValueOrDefault(PrefKey.UI_START_SCREEN, StartScreen.LastVisited)
    }

    val startScreen: StartScreen by lazy {
        if (savedStateHandle.contains(KEY_ROWID)) StartScreen.Transactions else {
            val preference = _startScreen
            if (preference == StartScreen.LastVisited)
                prefHandler.enumValueOrDefault(
                    PrefKey.UI_SCREEN_LAST_VISITED,
                    StartScreen.Transactions
                )
            else preference
        }
    }

    val startFilter by lazy {
        prefHandler.getString(PrefKey.UI_SCREEN_LAST_ACCOUNT_GROUP_FILTER)
    }

    fun setLastVisited(screen: StartScreen) {
        prefHandler.putString(PrefKey.UI_SCREEN_LAST_VISITED, screen.name)
    }

    fun setAccountsTab(tab: AccountsScreenTab) {
        _currentAccountsTab.value = tab
        // You already have setLastVisited, you can call it here too
        setLastVisited(tab)
    }

    fun setLastVisited(accountsScreenTab: AccountsScreenTab) {
        setLastVisited(
            when (accountsScreenTab) {
                AccountsScreenTab.LIST -> StartScreen.Accounts
                AccountsScreenTab.BALANCE_SHEET -> StartScreen.BalanceSheet
            }
        )
    }

    val sortByFlagFirst by lazy {
        PreferenceAccessor(
            dataStore,
            prefHandler.getBooleanPreferencesKey(PrefKey.SORT_ACCOUNT_LIST_BY_FLAG_FIRST),
            defaultValue = true
        )
    }

    enum class AccountPanelState {
        EXPANDED, COLLAPSED, DEFAULT
    }

    val accountPanelState by lazy {
        EnumPreferenceAccessor(
            dataStore,
            stringPreferencesKey("accountPanelState"),
            AccountPanelState.DEFAULT
        )
    }

    val navigationMode by lazy {
        EnumPreferenceAccessor(
            dataStore,
            MenuItem.NavigationMode.PREFERENCE_KEY,
            MenuItem.NavigationMode.DEFAULT
        )
    }

    val mainMenu: StateFlow<List<MenuItem>> by lazy {
       menuFlow(MenuItem.MenuContext.V2Navigation)
    }

    val transactionScreenMenu by lazy {
        menuFlow(MenuItem.MenuContext.V2Transactions)
    }

    private fun menuFlow(menuContext: MenuItem.MenuContext): StateFlow<List<MenuItem>> {
        val defaultConfiguration =
            MenuItem.getDefaultConfiguration(menuContext)
        return dataStore.menu(
            key = prefHandler.getStringPreferencesKey(menuContext.prefKey),
        )
            .map { it ?: defaultConfiguration }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = defaultConfiguration
            )
    }

    val isWebUiActive: Flow<Boolean> by lazy {
        dataStore.data.map { preferences -> preferences[prefHandler.getBooleanPreferencesKey(PrefKey.UI_WEB)] ?: false }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    }
}