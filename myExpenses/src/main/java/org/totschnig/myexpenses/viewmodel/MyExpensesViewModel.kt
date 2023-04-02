package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.*
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.adapter.ClearingLastPagingSourceFactory
import org.totschnig.myexpenses.adapter.TransactionPagingSource
import org.totschnig.myexpenses.compose.ExpansionHandler
import org.totschnig.myexpenses.compose.FutureCriterion
import org.totschnig.myexpenses.compose.SelectionHandler
import org.totschnig.myexpenses.compose.select
import org.totschnig.myexpenses.compose.toggle
import org.totschnig.myexpenses.compose.unselect
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider.*
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.toggle
import org.totschnig.myexpenses.viewmodel.data.*
import java.util.*
import javax.inject.Inject

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
    val selectionState: MutableState<List<SelectionInfo>> =
        savedStateHandle.saveable("selectionState") { mutableStateOf(emptyList()) }

    val selectionHandler = object : SelectionHandler {
        override fun toggle(transaction: Transaction2) {
            selectionState.toggle(SelectionInfo(transaction))
        }

        override fun isSelected(transaction: Transaction2) =
            selectionState.value.contains(SelectionInfo(transaction))

        override fun select(transaction: Transaction2) {
            selectionState.select(SelectionInfo(transaction))
        }

        override val selectionCount: Int
            get() = selectionState.value.size

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
        }.also {
            viewModelScope.launch {
                it.drop(1).collect {
                    triggerAccountListRefresh()
                }
            }
        }
    }

    private val pageSize = if (BuildConfig.DEBUG) 1500 else 150

    val items: Map<PageAccount, Flow<PagingData<Transaction2>>> = lazyMap {
        Pager(
            PagingConfig(
                initialLoadSize = pageSize,
                pageSize = pageSize,
                prefetchDistance = 1,
                enablePlaceholders = true
            ),
            pagingSourceFactory = pagingSourceFactories.getValue(it)
        )
            .flow.cachedIn(viewModelScope)
    }

    private val pagingSourceFactories: Map<PageAccount, ClearingLastPagingSourceFactory<Int, Transaction2>> = lazyMap {
        ClearingLastPagingSourceFactory {
            buildTransactionPagingSource(it)
        }
    }

    open fun buildTransactionPagingSource(account: PageAccount) =
        TransactionPagingSource(
            getApplication(),
            account,
            filterPersistence.getValue(account.id).whereFilterAsFlow,
            homeCurrencyProvider,
            currencyContext,
            viewModelScope
        )

    val filterPersistence: Map<Long, FilterPersistence> = lazyMap {
        FilterPersistence(
            prefHandler,
            keyTemplate = prefNameForCriteria(accountId = it),
            savedInstanceState = null,
            immediatePersist = true,
            restoreFromPreferences = true
        )
    }

    val scrollToCurrentDate: Map<Long, MutableState<Boolean>> =
        lazyMap {
            mutableStateOf(
                prefHandler.getBoolean(PrefKey.SCROLL_TO_CURRENT_DATE, false)
            )
        }

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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun headerData(account: PageAccount): Flow<HeaderData> =
        filterPersistence.getValue(account.id).whereFilterAsFlow.flatMapLatest { filter ->
            val groupingQuery = account.groupingQuery(filter)
            contentResolver.observeQuery(
                uri = groupingQuery.first,
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
                        HeaderData.fromSequence(account, cursor.asSequence)
                    } ?: emptyMap()
                }
            }.combine(dateInfo) { headerData, dateInfo ->
                HeaderData(account, headerData, dateInfo, !filter.isEmpty)
            }
        }


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
                        Grouping.groupId(it.getInt(0), it.getInt(1)),
                        it.getLong(2) + it.getLong(3),
                        it.getInt(4) == 1
                    )
                }.map {
                    BudgetData(it.first.getLong(KEY_BUDGETID), it.second)
                }
        } else emptyFlow()

    fun sumInfo(account: FullAccount): Flow<SumInfo> {
        val (selection, selectionArgs) = account.selectionInfo
        return contentResolver.observeQuery(
            uri = TRANSACTIONS_URI.buildUpon()
                .appendBooleanQueryParameter(QUERY_PARAMETER_MAPPED_OBJECTS)
                .build(),
            selection = selection,
            selectionArgs = selectionArgs
        ).mapToOne {
            SumInfoLoaded.fromCursor(it)
        }
    }

    fun persistGrouping(accountId: Long, grouping: Grouping) {
        viewModelScope.launch(context = coroutineContext()) {
            if (accountId == DataBaseAccount.HOME_AGGREGATE_ID) {
                AggregateAccount.persistGroupingHomeAggregate(prefHandler, grouping)
                triggerAccountListRefresh()
            } else {
                contentResolver.update(
                    ContentUris.withAppendedId(ACCOUNT_GROUPINGS_URI, accountId)
                        .buildUpon()
                        .appendPath(grouping.name).build(),
                    null, null, null
                )
            }
        }
    }

    fun persistSortDirection(accountId: Long, sortDirection: SortDirection) {
        viewModelScope.launch(context = coroutineContext()) {
            if (accountId == DataBaseAccount.HOME_AGGREGATE_ID) {
                persistSortDirectionHomeAggregate(sortDirection)
                triggerAccountListRefresh()
            } else {
                contentResolver.update(
                    ContentUris.withAppendedId(SORT_DIRECTION_URI, accountId)
                        .buildUpon()
                        .appendPath(sortDirection.name).build(),
                    null, null, null
                )
            }
        }
    }

    private fun persistSortDirectionHomeAggregate(sortDirection: SortDirection) {
        AggregateAccount.persistSortDirectionHomeAggregate(prefHandler, sortDirection)
        triggerAccountListRefresh()
    }

    fun triggerAccountListRefresh() {
        contentResolver.notifyChange(ACCOUNTS_URI, null, false)
    }

    fun linkTransfer(itemIds: LongArray) {
        viewModelScope.launch(context = coroutineContext()) {
            contentResolver.update(
                TRANSACTIONS_URI.buildUpon()
                    .appendPath(URI_SEGMENT_LINK_TRANSFER)
                    .appendPath(repository.getUuidForTransaction(itemIds[0]))
                    .build(), ContentValues(1).apply {
                    put(KEY_UUID, repository.getUuidForTransaction(itemIds[1]))
                }, null, null
            )
        }
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
                            .put(CrStatusCriterion(arrayOf(CrStatus.RECONCILED))),
                        handleDelete = Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE,
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
                    Transaction.undelete(it) > 0
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(e)
                    false
                }
            })
        }

    private val cloneAndRemapProgressInternal = MutableLiveData<Pair<Int, Int>>()
    val cloneAndRemapProgress: LiveData<Pair<Int, Int>>
        get() = cloneAndRemapProgressInternal

    fun cloneAndRemap(transactionIds: List<Long>, column: String, rowId: Long) {
        viewModelScope.launch(coroutineDispatcher) {
            var successCount = 0
            var failureCount = 0
            for (id in transactionIds) {
                val transaction = Transaction.getInstanceFromDb(id, homeCurrencyProvider.homeCurrencyUnit)
                transaction.prepareForEdit(true, false)
                val ops = transaction.buildSaveOperations(true)
                val newUpdate =
                    ContentProviderOperation.newUpdate(TRANSACTIONS_URI).withValue(column, rowId)
                if (transaction.isSplit) {
                    newUpdate.withSelection("$KEY_ROWID = ?", arrayOf(transaction.id.toString()))
                } else {
                    newUpdate.withSelection(
                        "$KEY_ROWID = ?",
                        arrayOf("")
                    )//replaced by back reference
                        .withSelectionBackReference(0, 0)
                }
                ops.add(newUpdate.build())
                if (contentResolver.applyBatch(
                        AUTHORITY,
                        ops
                    ).size == ops.size
                ) {
                    successCount++
                } else {
                    failureCount++
                }
                cloneAndRemapProgressInternal.postValue(Pair(successCount, failureCount))
            }
        }
    }

    fun remap(transactionIds: List<Long>, column: String, rowId: Long): LiveData<Int> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(run {
                val list = transactionIds.joinToString()
                var selection = "$KEY_ROWID IN ($list)"
                if (column == KEY_ACCOUNTID) {
                    selection += " OR $KEY_PARENTID IN ($list)"
                }
                contentResolver.update(
                    TRANSACTIONS_URI,
                    ContentValues().apply { put(column, rowId) },
                    selection,
                    null
                )
            })
        }

    fun tag(transactionIds: List<Long>, tagList: ArrayList<Tag>, replace: Boolean) {
        val tagIds = tagList.map { tag -> tag.id }
        viewModelScope.launch(coroutineDispatcher) {
            val ops = ArrayList<ContentProviderOperation>()
            for (id in transactionIds) {
                ops.addAll(saveTagLinks(tagIds, id, null, replace))
            }
            contentResolver.applyBatch(AUTHORITY, ops)
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
        val where = KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(count)
        val selectionArgs = ids.map { it.toString() }.toTypedArray()
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
            Transaction.EXTENDED_URI.buildUpon()
                .appendQueryParameter(QUERY_PARAMETER_GROUP_BY, groupBy)
                .appendQueryParameter(QUERY_PARAMETER_DISTINCT, "1")
                .build(),
            projection, where, selectionArgs, null
        )?.use { cursor ->
            if (cursor.count > 1) {
                emit(Result.failure(Exception()))
                null
            } else {
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
                    enumValueOrDefault(cursor.getString(KEY_CR_STATUS), CrStatus.UNRECONCILED)
                SplitTransaction.getNewInstance(accountId, currencyUnit, false).also {
                    it.amount = amount
                    it.date = date
                    it.payeeId = payeeId
                    it.crStatus = crStatus
                }
            }
        }?.let { parent ->
            val operations = parent.buildSaveOperations(false)
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
            emit(ResultUnit)
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

    companion object {
        fun prefNameForCriteria(accountId: Long) = "filter_%s_${accountId}"
    }
}
