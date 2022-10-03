package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.adapter.TransactionPagingSource
import org.totschnig.myexpenses.compose.ExpansionHandler
import org.totschnig.myexpenses.compose.toggle
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionDatabase.SQLiteDowngradeFailedException
import org.totschnig.myexpenses.provider.TransactionDatabase.SQLiteUpgradeFailedException
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.Tag
import org.totschnig.myexpenses.viewmodel.data.Transaction2

const val ERROR_INIT_DOWNGRADE = -1
const val ERROR_INIT_UPGRADE = -2

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ExpansionHandler")

class MyExpensesViewModel(application: Application, private val savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {

    private val hasHiddenAccounts = MutableLiveData<Boolean>()

    fun expansionHandler(key: String) = object : ExpansionHandler {
        val COLLAPSED_IDS = stringSetPreferencesKey(key)
        private val collapsedIds: Flow<Set<String>> = getApplication<MyApplication>().dataStore.data
                .map { preferences ->
                    preferences[COLLAPSED_IDS] ?: emptySet()
                }

        @Composable
        override fun collapsedIds(): State<Set<String>> = collapsedIds.collectAsState(initial = emptySet())

        override fun toggle(id: String) {
            viewModelScope.launch {
                getApplication<MyApplication>().dataStore.edit { settings ->
                    settings[COLLAPSED_IDS] = settings[COLLAPSED_IDS]?.toMutableSet()?.also {
                        it.toggle(id)
                    } ?: setOf(id)
                }
            }
        }
    }

    var selectedTransactionSum: Long
        get() = savedStateHandle["selectedTransactionSum"] ?: 0
        set(value)  { savedStateHandle["selectedTransactionSum"] = value }

    @OptIn(SavedStateHandleSaveableApi::class)
    val selectedAccount: MutableState<Long> = savedStateHandle.saveable("selectedAccount") { mutableStateOf(0L) }

    @OptIn(SavedStateHandleSaveableApi::class)
    val selectionState: MutableState<List<Transaction2>> = savedStateHandle.saveable("selectionState") { mutableStateOf(emptyList()) }

    fun getHasHiddenAccounts(): LiveData<Boolean> {
        return hasHiddenAccounts
    }

    @OptIn(ExperimentalPagerApi::class, SavedStateHandleSaveableApi::class)
    val pagerState = savedStateHandle.saveable("pagerState",
        saver = Saver(
            save = { it.currentPage },
            restore = { PagerState(it) }
        )
    ) {
        PagerState(0) }

    val filterPersistence: Map<Long, FilterPersistence> =
        lazyMap {
            FilterPersistence(
                prefHandler,
                keyTemplate = prefNameForCriteria(accountId = it),
                savedInstanceState = null,
                immediatePersist = true,
                restoreFromPreferences = true
            )
        }

    //TODO Safe mode
/*    if (cursor == null) {
        showSnackBar("Data loading failed", Snackbar.LENGTH_INDEFINITE, new SnackbarAction(R.string.safe_mode, v -> {
            prefHandler.putBoolean(PrefKey.DB_SAFE_MODE, true);
            rebuildAccountProjection();
            mManager.restartLoader(ACCOUNTS_CURSOR, null, this);
        }));
    }*/
    val accountData: StateFlow<List<FullAccount>> = contentResolver.observeQuery(
        uri = ACCOUNTS_URI.buildUpon().appendQueryParameter(
            TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES,
            "1"
        ).build(),
        selection = "$KEY_HIDDEN = 0",
        notifyForDescendants = true
    ).mapToList {
        FullAccount.fromCursor(it, currencyContext)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadData(account: FullAccount): () -> TransactionPagingSource {
        return { TransactionPagingSource(localizedContext, account,
            filterPersistence.getValue(account.id).whereFilterAsFlow, viewModelScope
        ) }
    }

    fun headerData(account: FullAccount): Flow<HeaderData> =
        contentResolver.observeQuery(uri = account.groupingUri()).map { query ->
            withContext(Dispatchers.IO) {
                query.run()?.use { cursor ->
                    HeaderData.fromSequence(account, cursor.asSequence)
                } ?: emptyMap()
            }
        }.combine(dateInfo) { headerData, dateInfo ->
            HeaderData(account.grouping, headerData, dateInfo)
        }

    fun sumInfo(account: FullAccount): Flow<SumInfo> = contentResolver.observeQuery(
        uri = TRANSACTIONS_URI.buildUpon().appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MAPPED_OBJECTS, "1").build(),
        selection = account.selection,
        selectionArgs = account.selectionArgs
    ).mapToOne {
        SumInfoLoaded.fromCursor(it)
    }

    fun initialize(): LiveData<Int> = liveData(context = coroutineContext()) {
        try {
            contentResolver.call(
                TransactionProvider.DUAL_URI,
                TransactionProvider.METHOD_INIT,
                null,
                null
            )
            getApplication<MyApplication>().appComponent.licenceHandler().update()
            Account.updateTransferShortcut()
            emit(0)
        } catch (e: SQLiteDowngradeFailedException) {
            CrashHandler.report(e)
            emit(ERROR_INIT_DOWNGRADE)
        } catch (e: SQLiteUpgradeFailedException) {
            CrashHandler.report(e)
            emit(ERROR_INIT_UPGRADE)
        }
    }

    fun loadHiddenAccountCount() {
        disposable = briteContentResolver.createQuery(
            ACCOUNTS_URI,
            arrayOf("count(*)"), "$KEY_HIDDEN = 1", null, null, false
        )
            .mapToOne { cursor -> cursor.getInt(0) > 0 }
            .subscribe { hasHiddenAccounts.postValue(it) }
    }

    fun persistGrouping(accountId: Long, grouping: Grouping) {
        viewModelScope.launch(context = coroutineContext()) {
            if (accountId == Account.HOME_AGGREGATE_ID) {
                AggregateAccount.persistGroupingHomeAggregate(prefHandler, grouping)
                triggerAccountListRefresh()
            } else {
                contentResolver.update(
                    ContentUris.withAppendedId(TransactionProvider.ACCOUNT_GROUPINGS_URI, accountId)
                        .buildUpon()
                        .appendPath(grouping.name).build(),
                    null, null, null
                )
            }
        }
    }

    fun persistSortDirection(accountId: Long, sortDirection: SortDirection) {
        viewModelScope.launch(context = coroutineContext()) {
            contentResolver.update(
                ContentUris.withAppendedId(Account.CONTENT_URI, accountId).buildUpon()
                    .appendPath(TransactionProvider.URI_SEGMENT_SORT_DIRECTION)
                    .appendPath(sortDirection.name).build(),
                null, null, null
            )
        }
    }

    fun persistSortDirectionAggregate(currency: String, sortDirection: SortDirection) {
        AggregateAccount.persistSortDirectionAggregate(prefHandler, currency, sortDirection)
        triggerAccountListRefresh()
    }

    fun persistSortDirectionHomeAggregate(sortDirection: SortDirection) {
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
                    .appendPath(TransactionProvider.URI_SEGMENT_LINK_TRANSFER)
                    .appendPath(repository.getUuidForTransaction(itemIds[0]))
                    .build(), ContentValues(1).apply {
                    put(KEY_UUID, repository.getUuidForTransaction(itemIds[1]))
                }, null, null
            )
        }
    }

    fun deleteAccounts(accountIds: Array<Long>): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            emit(deleteAccountsInternal(accountIds))
        }

    fun setSealed(accountId: Long, isSealed: Boolean) {
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

    fun balanceAccount(accountId: Long, reset: Boolean): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            emit(runCatching {
                val args = ContentValues()
                args.put(KEY_CR_STATUS, CrStatus.RECONCILED.name)
                Model.cr().update(
                    Transaction.CONTENT_URI,
                    args,
                    "$KEY_ACCOUNTID = ? AND $KEY_PARENTID is null AND $KEY_CR_STATUS = '${CrStatus.CLEARED.name}'",
                    arrayOf(accountId.toString())
                )
                if (reset) {
                    reset(
                        account = Account.getInstanceFromDb(accountId),
                        filter = WhereFilter.empty().put(CrStatusCriterion(arrayOf(CrStatus.RECONCILED))),
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
                TransactionProvider.DUAL_URI,
                TransactionProvider.METHOD_SORT_ACCOUNTS,
                null,
                Bundle(1).apply {
                    putLongArray(KEY_SORT_KEY, sortedIds)
                }
            )
        }
    }

    fun undeleteTransactions(itemId: Long): LiveData<Int> =
        liveData(context = coroutineContext()) {
            emit(try {
                    Transaction.undelete(itemId)
                    1
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(e)
                    0
                }
            )
        }

    private val cloneAndRemapProgressInternal = MutableLiveData<Pair<Int, Int>>()
    val cloneAndRemapProgress: LiveData<Pair<Int, Int>>
        get() = cloneAndRemapProgressInternal

    fun cloneAndRemap(transactionIds: List<Long>, column: String, rowId: Long) {
        viewModelScope.launch(coroutineDispatcher) {
            var successCount = 0
            var failureCount = 0
            for (id in transactionIds) {
                val transaction = Transaction.getInstanceFromDb(id)
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
                        TransactionProvider.AUTHORITY,
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
            contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
        }
    }

    fun addFilterCriteria(c: Criterion<*>, accountId: Long) {
        filterPersistence.getValue(accountId).addCriteria(c)
    }

    /**
     * Removes a given filter
     *
     * @return true if the filter was set and successfully removed, false otherwise
     */
    fun removeFilter(id: Int, accountId: Long) = filterPersistence.getValue(accountId).removeFilter(id)

    fun toggleCrStatus(id: Long) {
        viewModelScope.launch(coroutineDispatcher) {
            contentResolver.update(
                TRANSACTIONS_URI
                    .buildUpon()
                    .appendPath(id.toString())
                    .appendPath(TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS)
                    .build(),
                null, null, null
            )
        }
    }

    companion object {
        fun prefNameForCriteria(accountId: Long) = "filter_%s_${accountId}"
    }
}
