package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.res.Resources
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils.concat
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.database.getLongOrNull
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.adapter.ClearingLastPagingSourceFactory
import org.totschnig.myexpenses.adapter.TransactionPagingSource
import org.totschnig.myexpenses.compose.DataStoreExpansionHandler
import org.totschnig.myexpenses.compose.addToSelection
import org.totschnig.myexpenses.compose.filter.TYPE_COMPLEX
import org.totschnig.myexpenses.compose.toggle
import org.totschnig.myexpenses.compose.transactions.FutureCriterion
import org.totschnig.myexpenses.compose.transactions.SelectionHandler
import org.totschnig.myexpenses.compose.unselect
import org.totschnig.myexpenses.db2.RepositoryTransaction
import org.totschnig.myexpenses.db2.addAttachments
import org.totschnig.myexpenses.db2.calculateSplitSummary
import org.totschnig.myexpenses.db2.createTransaction
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.db2.groupToSplitTransaction
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.loadAttachments
import org.totschnig.myexpenses.db2.loadBanks
import org.totschnig.myexpenses.db2.loadTagsForTransaction
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.saveTagsForTransaction
import org.totschnig.myexpenses.db2.setAccountProperty
import org.totschnig.myexpenses.db2.setGrouping
import org.totschnig.myexpenses.db2.tagMapFlow
import org.totschnig.myexpenses.db2.unarchive
import org.totschnig.myexpenses.db2.undeleteTransaction
import org.totschnig.myexpenses.export.pdf.BalanceSheetPdfGenerator
import org.totschnig.myexpenses.export.pdf.PdfPrinter
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.model.sort.SortDirection
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.preference.ColorSource
import org.totschnig.myexpenses.preference.Mapper
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PreferenceAccessor
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.balanceUri
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.GROUPING_AGGREGATE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.SORT_BY_AGGREGATE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.SORT_DIRECTION_AGGREGATE
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_BUDGET
import org.totschnig.myexpenses.provider.KEY_BUDGETID
import org.totschnig.myexpenses.provider.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.KEY_CATID
import org.totschnig.myexpenses.provider.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.provider.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.KEY_FLAG
import org.totschnig.myexpenses.provider.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SEALED
import org.totschnig.myexpenses.provider.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.KEY_TAGLIST
import org.totschnig.myexpenses.provider.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.KEY_UUID
import org.totschnig.myexpenses.provider.KEY_VISIBLE
import org.totschnig.myexpenses.provider.KEY_YEAR
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DUAL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_REPLACE
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_SAVE_TRANSACTION_TAGS
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_MAPPED_OBJECTS
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES
import org.totschnig.myexpenses.provider.TransactionProvider.SORT_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.UNSPLIT_URI
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_LINK_TRANSFER
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_TRANSFORM_TO_TRANSFER
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_UNLINK_TRANSFER
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID
import org.totschnig.myexpenses.provider.mapToListCatching
import org.totschnig.myexpenses.provider.mapToListWithExtra
import org.totschnig.myexpenses.provider.triggerAccountListRefresh
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.TextUtils.withAmountColor
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_UPDATE_BALANCE
import org.totschnig.myexpenses.viewmodel.data.BalanceAccount
import org.totschnig.myexpenses.viewmodel.data.BalanceAccount.Companion.partitionByAccountType
import org.totschnig.myexpenses.viewmodel.data.BudgetData
import org.totschnig.myexpenses.viewmodel.data.BudgetRow
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount.Companion.fromCursor
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.HeaderDataEmpty
import org.totschnig.myexpenses.viewmodel.data.HeaderDataError
import org.totschnig.myexpenses.viewmodel.data.HeaderDataResult
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Tag
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.time.LocalDate
import kotlin.math.sign

enum class ScrollToCurrentDate { Never, AppLaunch, AccountOpen }

private const val BALANCE_DATE_KEY = "balanceDate"
private const val SELECTED_ACCOUNT_KEY = "selectedAccountId"

open class MyExpensesViewModel(
    application: Application,
    val savedStateHandle: SavedStateHandle,
) : PrintViewModel(application) {

    var showBalanceSheet: Boolean
        get() {
            val get = savedStateHandle.get<Boolean>("showBalanceSheet")
            return get == true
        }
        set(value) {
            savedStateHandle["showBalanceSheet"] = value
        }

    val showStatusHandle by lazy {
        PreferenceAccessor(dataStore, booleanPreferencesKey("showStatusHandle"), defaultValue = true)
    }

    val showEquivalentWorth by lazy {
        PreferenceAccessor(dataStore,
            booleanPreferencesKey("showEquivalentWorth"), defaultValue = false)
    }

    val preferredSearchType by lazy {
        PreferenceAccessor(dataStore,
            intPreferencesKey("preferredSearchType"), defaultValue = TYPE_COMPLEX)
    }

    val balanceSheetShowChart by lazy {
        PreferenceAccessor(dataStore,
            booleanPreferencesKey("balanceSheetShowChart"), defaultValue = false)
    }

    val balanceSheetShowHidden by lazy {
        PreferenceAccessor(dataStore,
            booleanPreferencesKey("balanceSheetShowHidden"), defaultValue = true)
    }

    val balanceSheetShowZero by lazy {
        PreferenceAccessor(dataStore,
            booleanPreferencesKey("balanceSheetShowZero"), defaultValue = true)
    }

    val listState = LazyListState(0, 0)

    suspend fun scrollToAccountIfNeeded(position: Int, key: Long, animate: Boolean) {
        if (!listState.layoutInfo.visibleItemsInfo.any { it.key == key })
            if (animate) {
                listState.animateScrollToItem(position)
            } else {
                listState.requestScrollToItem(position)
            }
    }

    fun expansionHandlerForTransactionGroups(account: PageAccount) =
        if (account.grouping == Grouping.NONE) null else
            expansionHandler(
                "collapsedHeaders_${account.id}_${account.grouping}"
            )

    fun expansionHandler(key: String) = DataStoreExpansionHandler(key, dataStore, viewModelScope)

    val selectedTransactionSum: Long
        get() = selectionState.value.sumOf { it.amount.amountMinor }

    fun actionModeTitle(currencyFormatter: ICurrencyFormatter, currencyUnit: CurrencyUnit, resources: Resources): CharSequence =
        concat(
            selectionState.value.size.toString(),
            " (Σ: ",
            with(selectedTransactionSum) {
                currencyFormatter.convAmount(this, currencyUnit)
                    .withAmountColor(resources, sign)
            },
            ")"
        )

    @Parcelize
    data class SelectionInfo(
        val id: Long,
        val accountId: Long,
        val amount: Money,
        val transferAccount: Long?,
        val isSplit: Boolean,
        val crStatus: CrStatus,
        val accountType: Long?,
    ) : Parcelable {
        constructor(transaction: Transaction2) : this(
            transaction.id,
            transaction.accountId,
            transaction.displayAmount,
            transaction.transferAccount,
            transaction.isSplit,
            transaction.crStatus,
            transaction.accountType
        )

        val isTransfer: Boolean
            get() = transferAccount != null
    }

    private val _selectedAccountId = savedStateHandle.getStateFlow(SELECTED_ACCOUNT_KEY, 0L)
    val selectedAccountId: StateFlow<Long> = _selectedAccountId

    fun selectAccount(accountId: Long) {
        savedStateHandle[SELECTED_ACCOUNT_KEY] = accountId // This updates the StateFlow
        if (scrollToCurrentDatePreference == ScrollToCurrentDate.AccountOpen) {
            scrollToCurrentDate.getValue(accountId).value = true
        }
        prefHandler.putLong(PrefKey.CURRENT_ACCOUNT, accountId)
    }


    val selectAllState: MutableState<Boolean> = mutableStateOf(false)

    @OptIn(SavedStateHandleSaveableApi::class)
    val selectionState: MutableState<List<SelectionInfo>> =
        savedStateHandle.saveable("selectionState") { mutableStateOf(emptyList()) }

    fun clearSelection() {
        selectionState.value = emptyList()
    }

    val selectionHandler = object : SelectionHandler {
        override fun toggle(transaction: Transaction2) {
            selectionState.toggle(SelectionInfo(transaction))
        }

        override fun isSelected(transaction: Transaction2) =
            selectionState.value.contains(SelectionInfo(transaction))

        override fun select(transaction: Transaction2) {
            selectionState.addToSelection(SelectionInfo(transaction))
        }

        override val selectionCount: Int
            get() = selectionState.value.size

        override fun isSelectable(transaction: Transaction2) = !transaction.isArchive

    }

    val showSumDetails: Flow<Boolean> by lazy {
        dataStore.data.map {
            it[prefHandler.getBooleanPreferencesKey(PrefKey.GROUP_HEADER)] != false
        }
    }

    var showFilterDialog by mutableStateOf(false)

    val futureCriterion: Flow<FutureCriterion> by lazy {
        dataStore.data.map {
            enumValueOrDefault(
                it[prefHandler.getStringPreferencesKey(PrefKey.CRITERION_FUTURE)],
                FutureCriterion.EndOfDay
            )
        }
    }

    val accountGrouping: PreferenceAccessor<AccountGrouping<*>, String> by lazy {

        PreferenceAccessor(
            dataStore = dataStore,
            key = prefHandler.getStringPreferencesKey(PrefKey.ACCOUNT_GROUPING),
            defaultValue = AccountGrouping.NONE,
            mapper = object : Mapper<AccountGrouping<*>, String> {
                override fun toPreference(userValue: AccountGrouping<*>): String {
                    return userValue.name
                }

                override fun fromPreference(persistedValue: String): AccountGrouping<*> {
                    // Deserialize from the string name
                    return when (persistedValue) {
                        "TYPE" -> AccountGrouping.TYPE
                        "CURRENCY" -> AccountGrouping.CURRENCY
                        "FLAG" -> AccountGrouping.FLAG
                        "NONE" -> AccountGrouping.NONE
                        else -> AccountGrouping.DEFAULT // Default fallback
                    }
                }
            }
        )
    }

    private val pageSize = 150

    val items: Map<PageAccount, Flow<PagingData<Transaction2>>> = lazyMap {
        Pager(
            PagingConfig(
                initialLoadSize = pageSize,
                pageSize = pageSize,
                prefetchDistance = 40,
                enablePlaceholders = true
            ),
            pagingSourceFactory = pagingSourceFactories.getValue(it)
        )
            .flow.cachedIn(viewModelScope)
    }

    private val sums: Map<PageAccount, Flow<SumInfo>> = lazyMap { account ->
        contentResolver.observeQuery(
            uri = account.uriBuilderForTransactionList(extended = false)
                .appendBooleanQueryParameter(QUERY_PARAMETER_MAPPED_OBJECTS)
                .build()
        ).mapToOne {
            SumInfo.fromCursor(it)
        }.stateIn(viewModelScope, SharingStarted.Lazily, SumInfo.EMPTY)
    }

    private val headerDataV2: Map<PageAccount, StateFlow<HeaderDataResult>> = lazyMap { account ->
        buildHeaderData(account, true)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    private val headerData: Map<PageAccount, StateFlow<HeaderDataResult>> = lazyMap { account ->
        buildHeaderData(account)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildHeaderData(account: PageAccount, v2: Boolean = false) =
        filterPersistence.getValue(account.id).whereFilter.flatMapLatest { filter ->
            val groupingQuery = if (v2) account.groupingQueryV2(filter) else account.groupingQuery(filter)
            contentResolver.observeQuery(
                uri = groupingQuery.first.build(),
                selection = groupingQuery.second,
                selectionArgs = groupingQuery.third
            ).map { query ->
                withContext(Dispatchers.IO) {
                    try {
                        query.run()
                    } catch (e: SQLiteException) {
                        CrashHandler.report(e)
                        null
                    }?.use { cursor ->
                        HeaderData.fromSequence(
                            account.openingBalance,
                            account.grouping,
                            account.currencyUnit,
                            cursor.asSequence
                        )
                    }
                }
            }.combine(dateInfo) { headerData, dateInfo ->
                headerData?.let { HeaderData(account, it, dateInfo, filter != null) }
                    ?: HeaderDataError(account)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, HeaderDataEmpty(account))

    private val pagingSourceFactories: Map<PageAccount, ClearingLastPagingSourceFactory<Int, Transaction2, *>> =
        lazyMap {
            ClearingLastPagingSourceFactory {
                buildTransactionPagingSource(it)
            }
        }

    protected val tags: StateFlow<Map<String, Pair<String, Int?>>> by lazy {
        contentResolver.tagMapFlow
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    }

    open fun buildTransactionPagingSource(account: PageAccount) =
        TransactionPagingSource(
            getApplication(),
            account,
            filterPersistence.getValue(account.id).whereFilter,
            tags,
            currencyContext,
            viewModelScope,
            prefHandler
        )

    val filterPersistence: Map<Long, FilterPersistence> = lazyMap {
        FilterPersistence(
            dataStore,
            prefNameForCriteria(it),
            viewModelScope
        )
    }

    val scrollToCurrentDate: Map<Long, MutableState<Boolean>> =
        lazyMap {
            mutableStateOf(scrollToCurrentDatePreference != ScrollToCurrentDate.Never)
        }

    val scrollToCurrentDatePreference
        get() = prefHandler.enumValueOrDefault(
            PrefKey.SCROLL_TO_CURRENT_DATE,
            ScrollToCurrentDate.Never
        )

    @OptIn(SavedStateHandleSaveableApi::class)
    var balanceDate =
        savedStateHandle.getLiveData<LocalDate>(BALANCE_DATE_KEY, LocalDate.now()).asFlow()

    fun setBalanceDate(date: LocalDate) {
        savedStateHandle[BALANCE_DATE_KEY] = date
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val accountsForBalanceSheet: StateFlow<Pair<LocalDate, List<BalanceAccount>>> =
        balanceDate.flatMapLatest { date ->
            contentResolver.observeQuery(
                balanceUri(if (date == LocalDate.now()) "now" else date.toString(), true),
                selection = "$KEY_EXCLUDE_FROM_TOTALS = 0"
            )
                .mapToList { BalanceAccount.fromCursor(it, currencyContext) }
                .map {
                    date to it
                }
        }.stateIn(viewModelScope, SharingStarted.Lazily, LocalDate.now() to emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val debtSum: StateFlow<Long> = balanceDate.flatMapLatest { date ->
        loadDebts(
            date = date,
            showSealed = true,
            showZero = false,
        ).map {
            it.fold(0L) { sum, debt -> sum + debt.currentEquivalentBalance }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val accountData: StateFlow<Result<List<FullAccount>>?> by lazy {
        contentResolver.observeQuery(
            uri = ACCOUNTS_URI.buildUpon()
                .appendBooleanQueryParameter(QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES)
                .build(),
            selection = "$KEY_VISIBLE = 1",
            notifyForDescendants = true
        )
            .mapToListCatching {
                it.fromCursor(currencyContext)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    }

    fun headerData(account: PageAccount, v2: Boolean) = if (v2)
        headerDataV2.getValue(account)
    else
        headerData.getValue(account)

    fun budgetData(account: PageAccount): Flow<BudgetData?> =
        if (licenceHandler.hasTrialAccessTo(ContribFeature.BUDGET)) {
            contentResolver.observeQuery(
                uri = BaseTransactionProvider.defaultBudgetAllocationUri(
                    account.id,
                    account.grouping
                ),
                projection = arrayOf(
                    KEY_YEAR,
                    KEY_SECOND_GROUP,
                    KEY_BUDGET,
                    KEY_BUDGET_ROLLOVER_PREVIOUS,
                    KEY_ONE_TIME
                ),
                sortOrder = "$KEY_YEAR, $KEY_SECOND_GROUP"
            ).map { it }
                .mapToListWithExtra {
                    BudgetRow(
                        headerId = Grouping.groupId(it.getInt(0), it.getInt(1)),
                        amount = it.getLongOrNull(2),
                        rollOverPrevious = it.getLongOrNull(3),
                        oneTime = it.getInt(4) == 1
                    )
                }.map {
                    BudgetData(it.first.getLong(KEY_BUDGETID), it.second)
                }
        } else emptyFlow()

    fun sumInfo(account: PageAccount) = sums.getValue(account)

    fun persistGrouping(grouping: Grouping) {
        viewModelScope.launch(context = coroutineContext()) {
            if (selectedAccountId.value == DataBaseAccount.HOME_AGGREGATE_ID) {
                prefHandler.putString(GROUPING_AGGREGATE, grouping.name)
                triggerAccountListRefresh()
            } else {
                repository.setGrouping(selectedAccountId.value, grouping)
            }
        }
    }

    fun persistSort(sort: String, direction: SortDirection) {
        viewModelScope.launch(context = coroutineContext()) {
            if (selectedAccountId.value == DataBaseAccount.HOME_AGGREGATE_ID) {
                persistSortDirectionHomeAggregate(sort, direction)
            } else {
                contentResolver.update(
                    ContentUris.withAppendedId(SORT_URI, selectedAccountId.value)
                        .buildUpon()
                        .appendPath(sort)
                        .appendPath(direction.name)
                        .build(),
                    null, null, null
                )
            }
        }
    }

    private fun persistSortDirectionHomeAggregate(sort: String, direction: SortDirection) {
        prefHandler.putString(SORT_BY_AGGREGATE, sort)
        prefHandler.putString(SORT_DIRECTION_AGGREGATE, direction.name)
        triggerAccountListRefresh()
    }

    fun triggerAccountListRefresh() {
        contentResolver.triggerAccountListRefresh()
    }

    fun linkTransfer(itemIds: LongArray) = liveData(context = coroutineContext()) {
        emit(runCatching {
            check(
                contentResolver.update(
                    TRANSACTIONS_URI.buildUpon()
                        .appendPath(URI_SEGMENT_LINK_TRANSFER)
                        .appendPath(repository.getUuidForTransaction(itemIds[0]))
                        .build(),
                    ContentValues(1).apply {
                        put(KEY_UUID, repository.getUuidForTransaction(itemIds[1]))
                    }, null, null
                ) == 2
            )
        })
    }

    fun unlinkTransfer(itemId: Long) = liveData(context = coroutineContext()) {
        emit(runCatching {
            check(
                contentResolver.update(
                    ContentUris.appendId(
                        TRANSACTIONS_URI.buildUpon().appendPath(URI_SEGMENT_UNLINK_TRANSFER),
                        itemId
                    ).build(), null, null, null
                ) == 2
            )
        })
    }

    fun transformToTransfer(transactionId: Long, transferAccount: Long) =
        liveData(context = coroutineContext()) {
            emit(runCatching {
                check(
                    contentResolver.insert(
                        ContentUris.appendId(
                            ContentUris.appendId(TRANSACTIONS_URI.buildUpon(), transactionId)
                                .appendPath(URI_SEGMENT_TRANSFORM_TO_TRANSFER), transferAccount
                        )
                            .build(), null
                    ) != null
                )
            })
        }

    fun deleteAccounts(accountIds: LongArray): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            emit(deleteAccountsInternal(accountIds))
        }

    fun setSealed(accountId: Long, isSealed: Boolean) {
        setAccountProperty(accountId, KEY_SEALED, isSealed)
    }

    fun setExcludeFromTotals(accountId: Long, excludeFromTotals: Boolean) {
        setAccountProperty(accountId, KEY_EXCLUDE_FROM_TOTALS, excludeFromTotals)
    }

    fun setDynamicExchangeRate(accountId: Long, dynamicExchangeRate: Boolean) {
        setAccountProperty(accountId, KEY_DYNAMIC, dynamicExchangeRate)
    }

    fun setFlag(accountId: Long, flagId: Long) {
        setAccountProperty(accountId, KEY_FLAG, flagId)
    }

    private fun setAccountProperty(accountId: Long, column: String, value: Any?) {
        if (DataBaseAccount.isAggregate(accountId)) {
            CrashHandler.report(IllegalStateException("setBooleanProperty for $column called on aggregate account"))
        } else {
            viewModelScope.launch(context = coroutineContext()) {
                repository.setAccountProperty(accountId, column, value)
            }
        }
    }

    fun balanceAccount(accountId: Long, reset: Boolean): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            emit(runCatching {
                val args = ContentValues()
                args.put(KEY_CR_STATUS, CrStatus.RECONCILED.name)
                contentResolver.update(
                    TRANSACTIONS_URI,
                    args,
                    "$KEY_ACCOUNTID = ? AND $KEY_PARENTID is null AND $KEY_CR_STATUS = '${CrStatus.CLEARED.name}'",
                    arrayOf(accountId.toString())
                )
                if (reset) {
                    reset(
                        account = repository.loadAccount(accountId)!!,
                        filter = CrStatusCriterion(listOf(CrStatus.RECONCILED)),
                        handleDelete = EXPORT_HANDLE_DELETED_UPDATE_BALANCE,
                        helperComment = null
                    )
                }
                Unit
            })
        }

    fun undeleteTransactions(itemIds: List<Long>): LiveData<Int> =
        liveData(context = coroutineContext()) {
            emit(itemIds.count {
                try {
                    repository.undeleteTransaction(it) > 0
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(contentResolver, e)
                    false
                }
            })
        }

    private val cloneAndRemapProgressInternal = MutableLiveData<Pair<Int, Int>>()
    val cloneAndRemapProgress: LiveData<Pair<Int, Int>>
        get() = cloneAndRemapProgressInternal

    private fun RepositoryTransaction.clone(): RepositoryTransaction {
        fun Transaction.clone(uuid: String) = copy(id = 0, uuid = uuid)
        val uuid = generateUuid()
        return copy(
            data = data.clone(uuid),
            transferPeer = transferPeer?.clone(uuid),
            splitParts = splitParts?.map {
                it.clone()
            }
        )
    }

    fun cloneAndRemap(transactionIds: List<Long>, column: String, value: Long) {
        viewModelScope.launch(coroutineDispatcher) {
            var successCount = 0
            var failureCount = 0
            for (id in transactionIds) {
                val transaction = repository.loadTransaction(id).clone()
                val clone = repository.createTransaction(transaction)
                val update = remapDo(listOf(clone.id), column, value)

                if (update > 0) {
                    repository.saveTagsForTransaction(
                        repository.loadTagsForTransaction(id).map { it.id }.toLongArray(),
                        clone.id
                    )
                    repository.addAttachments(clone.id, repository.loadAttachments(id))
                    successCount++
                } else {
                    failureCount++
                }
                cloneAndRemapProgressInternal.postValue(Pair(successCount, failureCount))
            }
        }
    }

    fun remap(transactionIds: List<Long>, column: String, value: Long): LiveData<Int> =
        liveData(context = coroutineDispatcher) {
            emit(run {
                remapDo(transactionIds, column, value)
            })
        }

    private fun remapDo(transactionIds: List<Long>, column: String, value: Long): Int {
        val list = transactionIds.joinToString()
        var selection = "$KEY_ROWID IN ($list)"
        if (column == KEY_ACCOUNTID) {
            selection += " OR $KEY_PARENTID IN ($list)"
        }
        if (column == KEY_CATID || column == KEY_DATE) {
            selection += " OR $KEY_TRANSFER_PEER IN ($list)"
        }
        return contentResolver.update(
            TRANSACTIONS_URI,
            ContentValues().apply { put(column, value.takeIf { it != NULL_ITEM_ID }) },
            selection,
            null
        )
    }

    fun tag(transactionIds: List<Long>, tagList: ArrayList<Tag>, replace: Boolean) {
        val tagIds = tagList.map { tag -> tag.id }
        viewModelScope.launch(coroutineDispatcher) {
            for (id in transactionIds) {
                contentResolver.call(DUAL_URI, METHOD_SAVE_TRANSACTION_TAGS, null, Bundle().apply {
                    putBoolean(KEY_REPLACE, replace)
                    putLong(KEY_TRANSACTIONID, id)
                    putLongArray(KEY_TAGLIST, tagIds.toLongArray())
                })
            }
        }
    }

    fun toggleCrStatus(id: Long) {
        selectionState.unselect { it.id == id }
        viewModelScope.launch(coroutineDispatcher) {
            contentResolver.update(
                TRANSACTIONS_URI
                    .buildUpon()
                    .appendPath(id.toString())
                    .appendPath(URI_SEGMENT_TOGGLE_CRSTATUS)
                    .build(),
                null, null, null
            )
        }
    }

    fun split(ids: LongArray) = liveData(context = coroutineContext()) {
        emit(repository.groupToSplitTransaction(ids))
    }

    fun revokeSplit(id: Long) = liveData(context = coroutineContext()) {
        val values = ContentValues(1).apply {
            put(KEY_ROWID, id)
        }
        if (contentResolver.update(
                UNSPLIT_URI,
                values,
                null,
                null
            ) > 0
        ) {
            emit(ResultUnit)
        } else {
            emit(Result.failure(Exception()))
        }
    }

    fun canLinkSelection(): Boolean {
        check(selectionState.value.size == 2)
        val transaction1 = selectionState.value[0]
        val transaction2 = selectionState.value[1]
        return transaction1.accountId != transaction2.accountId && (
                transaction1.amount.amountMinor == -transaction2.amount.amountMinor ||
                        transaction1.amount.currencyUnit.code != transaction2.amount.currencyUnit.code
                )
    }

    override fun onCleared() {
        super.onCleared()
        pagingSourceFactories.forEach {
            it.value.clear()
        }
    }

    fun unarchive(id: Long) {
        viewModelScope.launch(coroutineContext()) {
            repository.unarchive(id)
        }
    }

    suspend fun splitInfo(id: Long): List<Pair<String, String?>>? {
        return withContext(Dispatchers.IO) {
            repository.calculateSplitSummary(id)
        }
    }

    fun print(account: PageAccount, whereFilter: Criterion?) {
        viewModelScope.launch(coroutineContext()) {
            _pdfResult.update {
                AppDirHelper.checkAppDir(getApplication()).mapCatching {
                    val colorSource = enumValueOrDefault(
                        dataStore.data.first()[prefHandler.getStringPreferencesKey(PrefKey.TRANSACTION_AMOUNT_COLOR_SOURCE)],
                        ColorSource.TYPE
                    )
                    PdfPrinter.print(localizedContext, account, it, whereFilter, colorSource)
                }
            }
        }
    }

    fun printBalanceSheet() {
        viewModelScope.launch(coroutineContext()) {
            _pdfResult.update {
                AppDirHelper.checkAppDir(getApplication()).mapCatching {
                    val (date, accounts) = accountsForBalanceSheet.first()
                    BalanceSheetPdfGenerator(localizedContext).generatePdf(
                        destDir = it,
                        data = accounts.partitionByAccountType(),
                        date = date,
                        debts = debtSum.first()
                    )
                }
            }
        }
    }

    val banks: StateFlow<List<Bank>> by lazy {
        repository.loadBanks().stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )
    }

    companion object {
        fun prefNameForCriteriaLegacy(accountId: Long) = "filter_%s_${accountId}"
        fun prefNameForCriteria(accountId: Long) = "filter_${accountId}"
    }
}
