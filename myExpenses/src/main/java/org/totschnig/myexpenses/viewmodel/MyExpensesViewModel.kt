package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.os.Parcelable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.database.getLongOrNull
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.adapter.ClearingLastPagingSourceFactory
import org.totschnig.myexpenses.adapter.TransactionPagingSource
import org.totschnig.myexpenses.compose.ExpansionHandler
import org.totschnig.myexpenses.compose.FutureCriterion
import org.totschnig.myexpenses.compose.SelectionHandler
import org.totschnig.myexpenses.compose.addToSelection
import org.totschnig.myexpenses.compose.toggle
import org.totschnig.myexpenses.compose.unselect
import org.totschnig.myexpenses.db2.addAttachments
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.loadAttachments
import org.totschnig.myexpenses.db2.loadBanks
import org.totschnig.myexpenses.db2.loadTagsForTransaction
import org.totschnig.myexpenses.db2.saveTagsForTransaction
import org.totschnig.myexpenses.db2.setGrouping
import org.totschnig.myexpenses.db2.tagMapFlow
import org.totschnig.myexpenses.db2.unarchive
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.GROUPING_AGGREGATE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.SORT_BY_AGGREGATE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.SORT_DIRECTION_AGGREGATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGETID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.AUTHORITY
import org.totschnig.myexpenses.provider.TransactionProvider.DUAL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.EXTENDED_URI
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_REPLACE
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_SAVE_TRANSACTION_TAGS
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_SORT_ACCOUNTS
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_DISTINCT
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_GROUP_BY
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_MAPPED_OBJECTS
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_TRANSACTION_ID_LIST
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_WITH_HIDDEN_ACCOUNT_COUNT
import org.totschnig.myexpenses.provider.TransactionProvider.SORT_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_LINK_TRANSFER
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_TRANSFORM_TO_TRANSFER
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_UNLINK_TRANSFER
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_UNSPLIT
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.FilterPersistenceV2
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.mapToListCatchingWithExtra
import org.totschnig.myexpenses.provider.mapToListWithExtra
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.toggle
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_UPDATE_BALANCE
import org.totschnig.myexpenses.viewmodel.data.BudgetData
import org.totschnig.myexpenses.viewmodel.data.BudgetRow
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.HeaderDataEmpty
import org.totschnig.myexpenses.viewmodel.data.HeaderDataError
import org.totschnig.myexpenses.viewmodel.data.HeaderDataResult
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import org.totschnig.myexpenses.viewmodel.data.Tag
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.util.Locale

enum class ScrollToCurrentDate { Never, AppLaunch, AccountOpen }

open class MyExpensesViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : ContentResolvingAndroidViewModel(application) {

    private val hiddenAccountsInternal: MutableStateFlow<Int> = MutableStateFlow(0)
    val hasHiddenAccounts: StateFlow<Int> = hiddenAccountsInternal

    private val showStatusHandlePrefKey = booleanPreferencesKey("showStatusHandle")

    fun showStatusHandle() =
        dataStore.data.map { preferences ->
            preferences[showStatusHandlePrefKey] ?: true
        }

    suspend fun persistShowStatusHandle(showStatus: Boolean) {
        dataStore.edit { preference ->
            preference[showStatusHandlePrefKey] = showStatus
        }
    }

    val listState = LazyListState(0, 0)

    suspend fun scrollToAccountIfNeeded(position: Int, key: Long) {
        if (!listState.layoutInfo.visibleItemsInfo.any { it.key == key })
            listState.animateScrollToItem(position)
    }

    fun expansionHandlerForTransactionGroups(account: PageAccount) =
        if (account.grouping == Grouping.NONE) null else
            expansionHandler("collapsedHeaders_${account.id}_${account.grouping}")

    fun expansionHandler(key: String) = object : ExpansionHandler {
        val collapsedIdsPrefKey = stringSetPreferencesKey(key)
        override val collapsedIds: Flow<Set<String>> = dataStore.data.map { preferences ->
            preferences[collapsedIdsPrefKey] ?: emptySet()
        }

        override fun toggle(id: String) {
            viewModelScope.launch {
                dataStore.edit { settings ->
                    settings[collapsedIdsPrefKey] =
                        settings[collapsedIdsPrefKey]?.toMutableSet()?.also {
                            it.toggle(id)
                        } ?: setOf(id)
                }
            }
        }
    }

    val selectedTransactionSum: Long
        get() = selectionState.value.sumOf { it.amount.amountMinor }

    @Parcelize
    data class SelectionInfo(
        val id: Long,
        val accountId: Long,
        val amount: Money,
        val transferAccount: Long?,
        val isSplit: Boolean,
        val crStatus: CrStatus,
        val accountType: AccountType?
    ) : Parcelable {
        constructor(transaction: Transaction2) : this(
            transaction.id,
            transaction.accountId,
            transaction.amount,
            transaction.transferAccount,
            transaction.isSplit,
            transaction.crStatus,
            transaction.accountType
        )

        val isTransfer: Boolean
            get() = transferAccount != null
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    var selectedAccountId by savedStateHandle.saveable {
        mutableLongStateOf(0L)
    }

    fun selectAccount(accountId: Long) {
        selectedAccountId = accountId
        if (scrollToCurrentDatePreference == ScrollToCurrentDate.AccountOpen) {
            scrollToCurrentDate.getValue(accountId).value = true
        }
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    val selectionState: MutableState<List<SelectionInfo>> =
        savedStateHandle.saveable("selectionState") { mutableStateOf(emptyList()) }

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

    val futureCriterion: Flow<FutureCriterion> by lazy {
        dataStore.data.map {
            enumValueOrDefault(
                it[prefHandler.getStringPreferencesKey(PrefKey.CRITERION_FUTURE)],
                FutureCriterion.EndOfDay
            )
        }.distinctUntilChanged().also {
            viewModelScope.launch {
                it.drop(1).collect {
                    triggerAccountListRefresh()
                }
            }
        }
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
            SumInfoLoaded.fromCursor(it)
        }.stateIn(viewModelScope, SharingStarted.Lazily, SumInfoUnknown)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val headerData: Map<PageAccount, StateFlow<HeaderDataResult>> = lazyMap { account ->
        filterPersistence.getValue(account.id).whereFilterAsFlow.flatMapLatest { filter ->
            val groupingQuery = account.groupingQuery(filter)
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
    }

    private val pagingSourceFactories: Map<PageAccount, ClearingLastPagingSourceFactory<Int, Transaction2>> =
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
            filterPersistence.getValue(account.id).whereFilterAsFlow,
            tags,
            currencyContext,
            viewModelScope,
            prefHandler
        )

    val filterPersistence: Map<Long, FilterPersistenceV2> = lazyMap {
        FilterPersistenceV2(
            prefHandler,
            prefNameForCriteria(it)
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

    val accountData: StateFlow<Result<List<FullAccount>>?> = contentResolver.observeQuery(
        uri = ACCOUNTS_URI.buildUpon()
            .appendBooleanQueryParameter(QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES)
            .appendBooleanQueryParameter(QUERY_PARAMETER_WITH_HIDDEN_ACCOUNT_COUNT)
            .build(),
        selection = "$KEY_HIDDEN = 0",
        notifyForDescendants = true
    )
        .mapToListCatchingWithExtra {
            FullAccount.fromCursor(it, currencyContext)
        }.onEach { result ->
            result.onSuccess { pair ->
                hiddenAccountsInternal.value = pair.first.getInt(KEY_COUNT)
            }
        }
        .map { result -> result.map { it.second } }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun headerData(account: PageAccount) = headerData.getValue(account)

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

    fun persistGrouping(accountId: Long, grouping: Grouping) {
        viewModelScope.launch(context = coroutineContext()) {
            if (accountId == DataBaseAccount.HOME_AGGREGATE_ID) {
                prefHandler.putString(GROUPING_AGGREGATE, grouping.name)
                triggerAccountListRefresh()
            } else {
                repository.setGrouping(accountId, grouping)
            }
        }
    }

    fun persistSortDirection(accountId: Long, sort: Pair<String, SortDirection>) {
        viewModelScope.launch(context = coroutineContext()) {
            if (accountId == DataBaseAccount.HOME_AGGREGATE_ID) {
                persistSortDirectionHomeAggregate(sort)
                triggerAccountListRefresh()
            } else {
                contentResolver.update(
                    ContentUris.withAppendedId(SORT_URI, accountId)
                        .buildUpon()
                        .appendPath(sort.first)
                        .appendPath(sort.second.name)
                        .build(),
                    null, null, null
                )
            }
        }
    }

    private fun persistSortDirectionHomeAggregate(sort: Pair<String, SortDirection>) {
        prefHandler.putString(SORT_BY_AGGREGATE, sort.first)
        prefHandler.putString(SORT_DIRECTION_AGGREGATE, sort.second.name)
        triggerAccountListRefresh()
    }

    fun triggerAccountListRefresh() {
        contentResolver.notifyChange(ACCOUNTS_URI, null, false)
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
        if (DataBaseAccount.isAggregate(accountId)) {
            CrashHandler.report(IllegalStateException("setSealed called on aggregate account"))
        } else {
            viewModelScope.launch(context = coroutineContext()) {
                contentResolver.update(
                    ContentUris.withAppendedId(ACCOUNTS_URI, accountId),
                    ContentValues(1).apply {
                        put(KEY_SEALED, isSealed)
                    },
                    null,
                    null
                )
            }
        }
    }

    fun setExcludeFromTotals(accountId: Long, excludeFromTotals: Boolean) {
        if (DataBaseAccount.isAggregate(accountId)) {
            CrashHandler.report(IllegalStateException("setSealed called on aggregate account"))
        } else {
            viewModelScope.launch(context = coroutineContext()) {
                contentResolver.update(
                    ContentUris.withAppendedId(ACCOUNTS_URI, accountId),
                    ContentValues(1).apply {
                        put(KEY_EXCLUDE_FROM_TOTALS, excludeFromTotals)
                    },
                    null,
                    null
                )
            }
        }
    }

    fun balanceAccount(accountId: Long, reset: Boolean): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            emit(runCatching {
                val args = ContentValues()
                args.put(KEY_CR_STATUS, CrStatus.RECONCILED.name)
                contentResolver.update(
                    Transaction.CONTENT_URI,
                    args,
                    "$KEY_ACCOUNTID = ? AND $KEY_PARENTID is null AND $KEY_CR_STATUS = '${CrStatus.CLEARED.name}'",
                    arrayOf(accountId.toString())
                )
                if (reset) {
                    reset(
                        account = repository.loadAccount(accountId)!!,
                        filter = WhereFilter.empty()
                            .put(CrStatusCriterion(listOf(CrStatus.RECONCILED))),
                        handleDelete = EXPORT_HANDLE_DELETED_UPDATE_BALANCE,
                        helperComment = null
                    )
                }
                Unit
            })
        }

    fun setAccountVisibility(hidden: Boolean, vararg itemIds: Long) {
        viewModelScope.launch(context = coroutineContext()) {
            contentResolver.update(
                ACCOUNTS_URI,
                ContentValues().apply { put(KEY_HIDDEN, hidden) },
                "$KEY_ROWID ${WhereFilter.Operation.IN.getOp(itemIds.size)}",
                itemIds.map { it.toString() }.toTypedArray()
            )
        }
    }

    fun sortAccounts(sortedIds: LongArray) {
        viewModelScope.launch(context = coroutineContext()) {
            contentResolver.call(
                DUAL_URI,
                METHOD_SORT_ACCOUNTS,
                null,
                Bundle(1).apply {
                    putLongArray(KEY_SORT_KEY, sortedIds)
                }
            )
        }
    }

    fun undeleteTransactions(itemIds: List<Long>): LiveData<Int> =
        liveData(context = coroutineContext()) {
            emit(itemIds.count {
                try {
                    Transaction.undelete(contentResolver, it) > 0
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(contentResolver, e)
                    false
                }
            })
        }

    private val cloneAndRemapProgressInternal = MutableLiveData<Pair<Int, Int>>()
    val cloneAndRemapProgress: LiveData<Pair<Int, Int>>
        get() = cloneAndRemapProgressInternal

    fun cloneAndRemap(transactionIds: List<Long>, column: String, value: Long) {
        viewModelScope.launch(coroutineDispatcher) {
            var successCount = 0
            var failureCount = 0
            for (id in transactionIds) {
                val transaction = Transaction.getInstanceFromDb(
                    contentResolver,
                    id,
                    currencyContext.homeCurrencyUnit
                )
                transaction.prepareForEdit(contentResolver, true, false)
                val ops = transaction.buildSaveOperations(contentResolver, true)
                val newUpdate =
                    ContentProviderOperation.newUpdate(TRANSACTIONS_URI).withValue(column, value)
                if (transaction.isSplit) {
                    var selection = "$KEY_ROWID = ${transaction.id}"
                    if (column == KEY_ACCOUNTID) {
                        selection += " OR $KEY_PARENTID = ${transaction.id}"
                    }
                    newUpdate.withSelection(selection, null)
                } else {
                    var selection = "$KEY_ROWID = ?"
                    val updateTransferPeer = column == KEY_CATID || column == KEY_DATE
                    if (updateTransferPeer) {
                        selection += " OR $KEY_TRANSFER_PEER = ?"
                    }
                    newUpdate.withSelection(
                        selection,
                        arrayOfNulls(if (updateTransferPeer) 2 else 1)
                    )//replaced by back reference
                        .withSelectionBackReference(0, 0)
                    if (updateTransferPeer) {
                        newUpdate.withSelectionBackReference(1, 0)
                    }
                }
                ops.add(newUpdate.build())
                val results = contentResolver.applyBatch(
                    AUTHORITY,
                    ops
                )
                if (results.size == ops.size
                ) {
                    transaction.updateFromResult(results)
                    contentResolver.saveTagsForTransaction(
                        contentResolver.loadTagsForTransaction(id).map { it.id }.toLongArray(),
                        transaction.id
                    )
                    repository.addAttachments(transaction.id, repository.loadAttachments(id))
                    successCount++
                } else {
                    failureCount++
                }
                cloneAndRemapProgressInternal.postValue(Pair(successCount, failureCount))
            }
        }
    }

    fun remap(transactionIds: List<Long>, column: String, value: Long): LiveData<Int> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(run {
                val list = transactionIds.joinToString()
                var selection = "$KEY_ROWID IN ($list)"
                if (column == KEY_ACCOUNTID) {
                    selection += " OR $KEY_PARENTID IN ($list)"
                }
                if (column == KEY_CATID || column == KEY_DATE) {
                    selection += " OR $KEY_TRANSFER_PEER IN ($list)"
                }
                contentResolver.update(
                    TRANSACTIONS_URI,
                    ContentValues().apply { put(column, value) },
                    selection,
                    null
                )
            })
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
        val count = ids.size
        val projection = arrayOf(
            KEY_ACCOUNTID,
            KEY_CURRENCY,
            KEY_PAYEEID,
            KEY_CR_STATUS,
            "avg($KEY_DATE) AS $KEY_DATE",
            "sum($KEY_AMOUNT) AS $KEY_AMOUNT"
        )

        val groupBy = String.format(
            Locale.ROOT,
            "%s, %s, %s, %s",
            KEY_ACCOUNTID,
            KEY_CURRENCY,
            KEY_PAYEEID,
            KEY_CR_STATUS
        )
        contentResolver.query(
            EXTENDED_URI.buildUpon()
                .appendQueryParameter(QUERY_PARAMETER_TRANSACTION_ID_LIST, ids.joinToString())
                .appendQueryParameter(QUERY_PARAMETER_GROUP_BY, groupBy)
                .appendQueryParameter(QUERY_PARAMETER_DISTINCT, "1")
                .build(),
            projection, null, null, null
        )?.use { cursor ->
            emit(when (cursor.count) {
                1 -> {
                    cursor.moveToFirst()
                    val accountId = cursor.getLong(KEY_ACCOUNTID)
                    val currencyUnit = currencyContext[cursor.getString(KEY_CURRENCY)]
                    val amount = Money(
                        currencyUnit,
                        cursor.getLong(KEY_AMOUNT)
                    )
                    val payeeId = cursor.getLongOrNull(KEY_PAYEEID)
                    val date = cursor.getLong(KEY_DATE)
                    val crStatus =
                        enumValueOrDefault(
                            cursor.getString(KEY_CR_STATUS),
                            CrStatus.UNRECONCILED
                        )
                    val parent = SplitTransaction.getNewInstance(
                        contentResolver,
                        accountId,
                        currencyUnit,
                        false
                    ).also {
                        it.amount = amount
                        it.date = date
                        it.payeeId = payeeId
                        it.crStatus = crStatus
                    }
                    val operations = parent.buildSaveOperations(contentResolver, false)
                    val where = KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(count)
                    val selectionArgs = ids.map { it.toString() }.toTypedArray()
                    operations.add(
                        ContentProviderOperation.newUpdate(TRANSACTIONS_URI)
                            .withValues(ContentValues().apply {
                                put(KEY_CR_STATUS, CrStatus.UNRECONCILED.name)
                                put(KEY_DATE, parent.date)
                                putNull(KEY_PAYEEID)
                            })
                            .withValueBackReference(KEY_PARENTID, 0)
                            .withSelection(where, selectionArgs)
                            .withExpectedCount(count)
                            .build()
                    )
                    contentResolver.applyBatch(AUTHORITY, operations)
                    Result.success(true)
                }

                0 -> Result.failure(IllegalStateException().also {
                    CrashHandler.report(it)
                })

                else -> Result.success(false)
            }
            )
        }
    }

    fun revokeSplit(id: Long) = liveData(context = coroutineContext()) {
        val values = ContentValues(1).apply {
            put(KEY_ROWID, id)
        }
        if (contentResolver.update(
                TRANSACTIONS_URI.buildUpon().appendPath(URI_SEGMENT_UNSPLIT).build(),
                values,
                null,
                null
            ) == 1
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
        repository.unarchive(id)
    }

    val banks: StateFlow<List<Bank>> by lazy {
        repository.loadBanks().stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )
    }

    companion object {
        fun prefNameForCriteria(accountId: Long) = "filter_v2_%s_${accountId}"
    }
}
