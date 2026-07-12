package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Intent
import androidx.annotation.OpenForTesting
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.cash.copper.flow.observeQuery
import arrow.core.Tuple4
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
import org.totschnig.myexpenses.adapter.ClearingLastPagingSourceFactory
import org.totschnig.myexpenses.adapter.TradePagingSource
import org.totschnig.myexpenses.compose.transactions.Action
import org.totschnig.myexpenses.compose.transactions.FabStyle
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.createTransaction
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.db2.setBalanceType
import org.totschnig.myexpenses.db2.updateTransaction
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.BalanceType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.model.sort.Sort
import org.totschnig.myexpenses.model.sort.TransactionSort
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Account.Companion.DEFAULT_COLOR
import org.totschnig.myexpenses.preference.EnumPreferenceAccessor
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PreferenceAccessor
import org.totschnig.myexpenses.preference.PreferenceState
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.preference.isWebUiActive
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.GROUPING_AGGREGATE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.SORT_BY_AGGREGATE
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.mapToListCatching
import org.totschnig.myexpenses.provider.triggerAccountListRefresh
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.data.AggregateAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount.Companion.fromCursor
import org.totschnig.myexpenses.viewmodel.data.FullAccount.Companion.nest
import org.totschnig.myexpenses.viewmodel.data.FundingSource
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Trade
import org.totschnig.myexpenses.viewmodel.data.TradeIntent
import org.totschnig.myexpenses.viewmodel.data.TradeType
import org.totschnig.myexpenses.viewmodel.data.TransactionEditData
import org.totschnig.myexpenses.viewmodel.data.TransferEditData
import org.totschnig.myexpenses.viewmodel.data.mapper.TransactionMapper.mapTransaction
import timber.log.Timber
import java.math.BigDecimal

enum class AccountsScreenTab(@param:StringRes val resourceId: Int) {
    LIST(R.string.accounts),
    BALANCE_SHEET(R.string.balance_sheet)
}

@OpenForTesting
open class MyExpensesV2ViewModel(
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

    val lastActionPortfolio by lazy {
        EnumPreferenceAccessor(
            dataStore,
            stringPreferencesKey("lastActionPortfolio"),
            Action.Buy
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

    val aggregateAccountBalanceType by lazy {
        EnumPreferenceAccessor(
            dataStore,
            stringPreferencesKey("aggregateAccountBalanceType"),
            BalanceType.CURRENT
        )
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

    fun setSortOrderAccounts(sort: Sort, isFlagFirst: Boolean) {
        viewModelScope.launch {
            prefHandler.putString(
                PrefKey.SORT_ORDER_ACCOUNTS,
                sort.name
            )
            sortByFlagFirst.set(isFlagFirst)
            contentResolver.triggerAccountListRefresh()
        }
    }

    val accountDataV2: StateFlow<Result<List<FullAccount>>?> by lazy {
        combine(
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
                .map { result ->
                    result.map { it.nest().withNaturalSort }
                }, getLatestPrices()
        ) { accounts, prices ->
            accounts.map { result ->
                result.map {
                    it.enrich(
                        prices,
                        currencyContext.homeCurrencyUnit
                    )
                }
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribedWithTimeout, null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val accountList by lazy {
        combine(
            accountDataV2.mapNotNull { it?.getOrNull() },
            _activeFilter,
            accountGrouping.flow,
            aggregateAccountBalanceType.flow
        ) { accounts, activeFilter, accountGrouping, aggregateAccountBalanceType ->
            Tuple4(accounts, activeFilter, accountGrouping, aggregateAccountBalanceType)
        }.flatMapLatest { (accounts, activeFilter, grouping, aggregateAccountBalanceType) ->

            combine(
                groupingMap.getValue(groupingAggregateKey(grouping, activeFilter)).flow,
                sortMap.getValue(sortAggregateKey(grouping, activeFilter)).flow
            ) { aggregateGrouping, aggregateSort ->

                val filteredByGroupFilter =
                    if (activeFilter == null || grouping == AccountGrouping.NONE)
                        accounts
                    else
                        accounts.filter { account -> grouping.getGroupKey(account) == activeFilter }

                val aggregateAccountGrouping =
                    if (activeFilter != null) grouping else AccountGrouping.NONE

                val filteredByVisibility =
                    if (grouping == AccountGrouping.FLAG && activeFilter != null)
                        filteredByGroupFilter
                    else
                        filteredByGroupFilter.filter { it.visible }

                val result = if (filteredByGroupFilter.size < 2) {
                    filteredByVisibility
                } else {
                    val filteredForTotals = filteredByGroupFilter.filter { !it.excludeFromTotals }
                    filteredByVisibility + AggregateAccount(
                        currencyUnit = activeFilter as? CurrencyUnit
                            ?: currencyContext.homeCurrencyUnit,
                        type = activeFilter as? AccountType ?: AccountType.CASH,
                        flag = activeFilter as? AccountFlag ?: AccountFlag.DEFAULT,
                        isSingleCurrency = filteredByVisibility.distinctBy { it.currency }.size == 1,
                        grouping = if (aggregateSort.column == KEY_DATE) aggregateGrouping else Grouping.NONE,
                        accountGrouping = aggregateAccountGrouping,
                        sortBy = aggregateSort.column,
                        sortDirection = aggregateSort.sortDirection,
                        balanceType = aggregateAccountBalanceType,
                        equivalentOpeningBalance = filteredForTotals.sumOf { it.equivalentOpeningBalance },
                        equivalentCurrentBalance = filteredForTotals.sumOf { it.equivalentEffectiveBalance },
                        equivalentSumIncome = filteredForTotals.sumOf { it.equivalentSumIncome },
                        equivalentSumExpense = filteredForTotals.sumOf { it.equivalentSumExpense },
                        equivalentSumTransfer = filteredForTotals.sumOf { it.equivalentSumTransfer },
                        equivalentTotal = if (filteredForTotals.any { it.total != null })
                            filteredForTotals.sumOf {
                                it.equivalentTotal ?: it.equivalentCurrentBalance
                            }
                        else null,
                    ).let { aggregateAccount ->
                        if (aggregateAccountGrouping == AccountGrouping.CURRENCY) aggregateAccount.copy(
                            openingBalance = filteredForTotals.sumOf { it.openingBalance },
                            currentBalance = filteredForTotals.sumOf { it.effectiveBalance },
                            sumIncome = filteredForTotals.sumOf { it.sumIncome },
                            sumExpense = filteredForTotals.sumOf { it.sumExpense },
                            sumTransfer = filteredForTotals.sumOf { it.sumTransfer },
                            total = if (filteredForTotals.any { it.total != null }) filteredForTotals.sumOf {
                                it.total ?: it.currentBalance
                            } else null,
                        ) else aggregateAccount
                    }
                }
                if (result.none { it.accountId == selectedAccountId.value }) {
                    result.firstOrNull()?.let {
                        selectAccount(it.id)
                    }
                }
                result
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribedWithTimeout, emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableGroupFilters: StateFlow<List<AccountGroupingKey>?> by lazy {
        accountGrouping.statefulFlow.flatMapLatest { preferenceState ->
            when (preferenceState) {
                is PreferenceState.Loading -> emptyFlow()
                is PreferenceState.Loaded -> {
                    accountDataV2.map { result ->
                        result?.getOrNull()?.let { accounts ->
                            preferenceState.value.sortedGroupKeys(accounts)
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribedWithTimeout, null)
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

    fun groupingAggregateKey(grouping: AccountGrouping<*>, filter: AccountGroupingKey?) =
        aggregateKey(grouping, filter, "", GROUPING_AGGREGATE)

    fun sortAggregateKey(grouping: AccountGrouping<*>, filter: AccountGroupingKey?) =
        aggregateKey(grouping, filter, "sort_", SORT_BY_AGGREGATE)

    fun aggregateKey(
        grouping: AccountGrouping<*>,
        filter: AccountGroupingKey?,
        prefix: String,
        homeKey: String,
    ) =
        if (grouping == AccountGrouping.NONE || filter == null) {
            homeKey
        } else {
            "$prefix${grouping.name}_${filter.id}"
        }

    fun persistGroupingV2(grouping: Grouping) {
        viewModelScope.launch(context = coroutineContext()) {
            if (selectedAccountId.value == 0L) {
                groupingMap.getValue(
                    groupingAggregateKey(
                        accountGrouping.get(),
                        _activeFilter.value
                    )
                ).set(grouping)
            } else {
                performPersistGrouping(grouping)
            }
        }
    }

    fun persistSortV2(transactionSort: TransactionSort) {
        viewModelScope.launch(context = coroutineContext()) {
            if (selectedAccountId.value == 0L) {
                sortMap.getValue(
                    sortAggregateKey(
                        accountGrouping.get(),
                        _activeFilter.value
                    )
                ).set(transactionSort)
            } else {
                performPersistSort(transactionSort)
            }
        }
    }

    fun persistBalanceType(balanceType: BalanceType) {
        viewModelScope.launch(context = coroutineContext()) {
            if (selectedAccountId.value == 0L) {
                aggregateAccountBalanceType.set(balanceType)
            } else {
                repository.setBalanceType(selectedAccountId.value, balanceType)
            }
        }
    }

    fun createPortfolio(label: String, currency: String, color: Int) {
        viewModelScope.launch(coroutineDispatcher) {
            val portfolio = Account(
                label = label,
                currency = currency,
                color = color,
                type = repository.findAccountType(AccountType.INVESTMENT.name),
                isPortfolio = true
            )
            val accountId = repository.createAccount(portfolio).id
            selectAccount(accountId)
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

    val mainMenuAccessor by lazy {
        menuAccessor(MenuItem.MenuContext.V2Navigation)
    }

    val transactionMenuAccessor by lazy {
        menuAccessor(MenuItem.MenuContext.V2Transactions)
    }

    private fun menuAccessor(menuContext: MenuItem.MenuContext.V2) =
        PreferenceAccessor(
            dataStore,
            menuContext.prefKey,
            MenuItem.getDefaultConfiguration(menuContext),
            MenuItem.mapper
        )

    val isWebUiActive: Flow<Boolean> by lazy {
        dataStore.isWebUiActive
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribedWithTimeout,
                initialValue = false
            )
    }

    fun saveTrade(currentAccount: FullAccount, intent: TradeIntent, tradeId: Long? = null) {
        viewModelScope.launch(coroutineDispatcher) {

            val isAssetTrade = intent.type is TradeType.AssetTrade
            val portfolioCurrency = currentAccount.currencyUnit
            val transferCategory = prefHandler.defaultTransferCategory

            val targetAccountId = if (isAssetTrade) {
                intent.targetAccountId ?: run {
                    val account = Account(
                        label = intent.targetAsset.code,
                        currency = intent.targetAsset.code,
                        parentId = currentAccount.id,
                        type = repository.findAccountType(AccountType.INVESTMENT.name)!!,
                        color = DEFAULT_COLOR,
                        isPortfolioAsset = true,
                        dynamicExchangeRates = true
                    )
                    repository.createAccount(account).id
                }
            } else {
                currentAccount.children
                    .find { it.type.isCashAccount }?.id
                    ?: run {
                        val cashAccount = Account(
                            label = localizedContext.getString(R.string.account_type_cash),
                            currency = currentAccount.currency,
                            parentId = currentAccount.id,
                            type = repository.findAccountType(AccountType.CASH.name)!!,
                            color = currentAccount.color,
                        )
                        repository.createAccount(cashAccount).id
                    }
            }

            val principalAmount = if (isAssetTrade) intent.quantity.multiply(intent.price) else intent.quantity
            val totalImpact = if (intent.type == TradeType.AssetTrade.BUY || intent.type == TradeType.CashMovement.DEPOSIT) {
                principalAmount.add(intent.fee)
            } else {
                principalAmount.subtract(intent.fee)
            }

            val parts = mutableListOf<TransactionEditData>()

            // Part A: Target Leg (Portfolio <-> Asset/Cash sub-account)
            val targetLegHubAmount = if (intent.type == TradeType.AssetTrade.BUY || intent.type == TradeType.CashMovement.DEPOSIT) {
                principalAmount.negate()
            } else {
                principalAmount
            }
            val targetLegSubAmount = if (intent.type == TradeType.AssetTrade.BUY || intent.type == TradeType.CashMovement.DEPOSIT) {
                intent.quantity
            } else {
                intent.quantity.negate()
            }

            parts.add(
                TransactionEditData(
                    accountId = currentAccount.id,
                    amount = Money.buildWithMajor(portfolioCurrency, targetLegHubAmount)
                        .getOrThrow(),
                    transferEditData = TransferEditData(
                        transferAccountId = targetAccountId,
                        transferAmount = Money.buildWithMajor(
                            intent.targetAsset,
                            targetLegSubAmount
                        ).getOrThrow()
                    ),
                    isSplitPart = true,
                    uuid = generateUuid(),
                    categoryId = transferCategory
                )
            )

            // Part B: Funding/Source Leg (Portfolio <-> Cash Sub-account or External Account)
            val fundingLegHubAmount = if (intent.type == TradeType.AssetTrade.BUY || intent.type == TradeType.CashMovement.DEPOSIT) {
                totalImpact
            } else {
                totalImpact.negate()
            }

            val fundingTransferAccountId = when (intent.fundingSource) {
                FundingSource.ACCOUNT -> intent.fundingAccountId
                FundingSource.PORTFOLIO -> currentAccount.children
                    .find { it.type.isCashAccount }?.id
                    ?: run {
                        val cashAccount = Account(
                            label = localizedContext.getString(R.string.account_type_cash),
                            currency = currentAccount.currency,
                            parentId = currentAccount.id,
                            type = repository.findAccountType(AccountType.CASH.name)!!,
                            color = currentAccount.color,
                        )
                        repository.createAccount(cashAccount).id
                    }
                FundingSource.EXTERNAL -> null
            }

            parts.add(
                TransactionEditData(
                    accountId = currentAccount.id,
                    amount = Money.buildWithMajor(portfolioCurrency, fundingLegHubAmount)
                        .getOrThrow(),
                    transferEditData = fundingTransferAccountId?.let { TransferEditData(transferAccountId = it) },
                    comment = if (intent.fundingSource == FundingSource.EXTERNAL) "External" else null,
                    isSplitPart = true,
                    uuid = generateUuid(),
                    categoryId = transferCategory
                )
            )

            // Part C: Fee (Expense)
            if (intent.fee != BigDecimal.ZERO) {
                parts.add(
                    TransactionEditData(
                        accountId = currentAccount.id,
                        amount = Money.buildWithMajor(portfolioCurrency, intent.fee.negate())
                            .getOrThrow(),
                        isSplitPart = true,
                        uuid = generateUuid()
                    )
                )
            }

            // Parent transaction amount is the sum of all parts in Portfolio currency
            val totalPortfolioAmount = parts
                .fold(BigDecimal.ZERO) { acc, part -> acc.add(part.amount.amountMajor) }

            val parent = TransactionEditData(
                id = tradeId ?: 0L,
                accountId = currentAccount.id,
                amount = Money.buildWithMajor(portfolioCurrency, totalPortfolioAmount).getOrThrow(),
                date = intent.date,
                comment = intent.comment,
                uuid = generateUuid(),
                categoryId = SPLIT_CATID,
                splitParts = parts
            )

            if (tradeId != null) {
                repository.updateTransaction(mapTransaction(parent))
            } else {
                repository.createTransaction(mapTransaction(parent))
            }

            // Also update the Price History table for valuation
            if (intent.price > BigDecimal.ZERO) {
                repository.savePrice(
                    portfolioCurrency,
                    intent.targetAsset,
                    intent.date.toLocalDate(),
                    org.totschnig.myexpenses.retrofit.ExchangeRateSource.User,
                    intent.price
                )
            }
        }
    }

    private data class TradePagerInfo(
        val factory: ClearingLastPagingSourceFactory<Int, Trade, *>,
        val flow: Flow<PagingData<Trade>>,
    )

    private val tradePagerCache = mutableMapOf<Long, TradePagerInfo>()

    fun getTrades(account: PageAccount): Flow<PagingData<Trade>> {
        return tradePagerCache.getOrPut(account.id) {
            val factory: ClearingLastPagingSourceFactory<Int, Trade, TradePagingSource> =
                ClearingLastPagingSourceFactory {
                    TradePagingSource(getApplication(), repository, account, pageSize)
                }
            TradePagerInfo(
                factory,
                Pager(
                    config = PagingConfig(
                        pageSize = pageSize,
                        enablePlaceholders = true
                    ),
                    pagingSourceFactory = factory
                ).flow.cachedIn(viewModelScope)
            )
        }.flow
    }
}
