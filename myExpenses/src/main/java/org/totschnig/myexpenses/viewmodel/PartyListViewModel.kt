package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.newDelete
import android.content.ContentProviderOperation.newUpdate
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.cleanupUnusedParties
import org.totschnig.myexpenses.db2.unsetParentId
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.ACCOUNTS_MINIMAL_URI_WITH_AGGREGATES
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_PAYEEID
import org.totschnig.myexpenses.provider.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.KEY_PAYEE_NAME_NORMALIZED
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SEALED
import org.totschnig.myexpenses.provider.TABLE_BUDGETS
import org.totschnig.myexpenses.provider.TABLE_PAYEES
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.AUTHORITY
import org.totschnig.myexpenses.provider.TransactionProvider.BUDGETS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.CHANGES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DEBTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.PAYEES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_COUNT_UNUSED
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_HIERARCHICAL
import org.totschnig.myexpenses.provider.TransactionProvider.TEMPLATES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.filter.AndCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.NotCriterion
import org.totschnig.myexpenses.provider.filter.OrCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.replace
import org.totschnig.myexpenses.viewmodel.data.DisplayDebt
import org.totschnig.myexpenses.viewmodel.data.Party
import timber.log.Timber

const val KEY_EXPANDED_ITEM = "expandedItem"

enum class MergeStrategy(@param:StringRes val description: Int) {
    DELETE(R.string.merge_parties_strategy_delete),
    GROUP(R.string.merge_parties_strategy_group)
}

class PartyListViewModel(
    application: Application,
    val savedStateHandle: SavedStateHandle
) : ContentResolvingAndroidViewModel(application) {

    private lateinit var debts: Map<Long, List<DisplayDebt>>

    var filter: String?
        get() = savedStateHandle[KEY_FILTER]
        set(value) {
            savedStateHandle[KEY_FILTER] = value
        }

    var expandedItem: Long?
        get() = savedStateHandle[KEY_EXPANDED_ITEM]
        set(value) {
            savedStateHandle[KEY_EXPANDED_ITEM] = value
        }

    fun getDebts(partyId: Long): List<DisplayDebt>? =
        if (::debts.isInitialized) debts[partyId] else null

    val hasDebts: Boolean
        get() = if (::debts.isInitialized) debts.isNotEmpty() else false

    val unusedCount: StateFlow<Int> by lazy {
        contentResolver.observeQuery(
            PAYEES_URI.buildUpon().appendBooleanQueryParameter(QUERY_PARAMETER_COUNT_UNUSED)
                .build(),
            notifyForDescendants = true
        ).mapToOne {
            it.getInt(0)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun parties(hierarchical: Boolean): Flow<List<Party>> =
        savedStateHandle.getLiveData(KEY_FILTER, "")
            .asFlow()
            .distinctUntilChanged()
            .flatMapLatest { filter ->
                val (selection, selectionArgs) = joinQueryAndAccountFilter(
                    filter,
                    savedStateHandle,
                    KEY_PAYEE_NAME_NORMALIZED, KEY_PAYEEID, TABLE_PAYEES
                )
                contentResolver.observeQuery(
                    PAYEES_URI.buildUpon().also {
                        if (hierarchical) it.appendBooleanQueryParameter(QUERY_PARAMETER_HIERARCHICAL)
                    }.build(),
                    null, selection, selectionArgs, null, true
                ).mapToList { cursor -> Party.fromCursor(cursor) } // Let Copper handle Dispatchers.IO and cursor closing
            }
            .map { allParties ->
                if (!hierarchical) return@map allParties

                // Grouping logic: Collect children into their respective parents
                val result = mutableListOf<Party>()
                var currentParent: Party? = null
                val children = mutableListOf<Party>()

                fun flush() {
                    currentParent?.let {
                        result.add(it.copy(duplicates = ArrayList(children)))
                    }
                    children.clear()
                }

                for (party in allParties) {
                    if (party.isDuplicate) {
                        children.add(party)
                    } else {
                        flush()
                        currentParent = party
                    }
                }
                flush()
                result
            }
            .combine(
                savedStateHandle.getLiveData<Long?>(KEY_EXPANDED_ITEM, null).asFlow()
            ) { parties, expandedId ->
                val sorted = parties.sortedWith(compareBy(getNaturalComparator()) { it.name })
                if (expandedId == null) sorted else sorted.flatMap { parent ->
                    if (parent.id == expandedId) {
                        listOf(parent) + parent.duplicates
                    } else {
                        listOf(parent)
                    }
                }
            }
            .distinctUntilChanged()

    fun loadDebts(): LiveData<Unit> = liveData(context = coroutineContext()) {
        contentResolver.observeQuery(DEBTS_URI, notifyForDescendants = true)
            .mapToList {
                DisplayDebt.fromCursor(it, currencyContext)
            }.collect { list ->
                this@PartyListViewModel.debts = list.groupBy { it.payeeId }
                emit(Unit)
            }
    }

    fun deleteParty(id: Long): LiveData<Result<Int>> = liveData(context = coroutineContext()) {
        try {
            emit(
                Result.success(
                    contentResolver.delete(
                        ContentUris.withAppendedId(
                            PAYEES_URI,
                            id
                        ), null, null
                    )
                )
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private suspend fun updatePartyFilters(old: Set<Long>, new: Long) {
        contentResolver.query(ACCOUNTS_MINIMAL_URI_WITH_AGGREGATES, null, null, null, null)
            ?.use { cursor ->
                updateFilterHelper(old, new, cursor, MyExpensesViewModel::prefNameForCriteria)
            }
    }


    private suspend fun updatePartyBudgets(old: Set<Long>, new: Long) {
        contentResolver.query(
            BUDGETS_URI,
            arrayOf("$TABLE_BUDGETS.$KEY_ROWID"),
            null,
            null,
            null
        )?.use { cursor ->
            updateFilterHelper(old, new, cursor, BudgetViewModel::prefNameForCriteria)
        }
    }

    private fun updateCriterion(
        criterion: Criterion,
        old: Set<Long>,
        new: Long,
    ): Criterion {
        return when (criterion) {
            is PayeeCriterion -> {
                val oldSet = criterion.values.toSet()
                val newSet: Set<Long> = oldSet.replace(old, new)
                if (oldSet != newSet) {
                    val labelList = mutableListOf<String>()
                    contentResolver.query(
                        PAYEES_URI, arrayOf(KEY_PAYEE_NAME),
                        "$KEY_ROWID IN (${newSet.joinToString()})", null, null
                    )?.use {
                        it.moveToFirst()
                        while (!it.isAfterLast) {
                            labelList.add(it.getString(0))
                            it.moveToNext()
                        }
                    }
                    PayeeCriterion(
                        labelList.joinToString(","),
                        *newSet.toLongArray()
                    )
                } else criterion
            }

            is NotCriterion -> NotCriterion(updateCriterion(criterion.criterion, old, new))
            is AndCriterion -> AndCriterion(criterion.criteria.map { updateCriterion(it, old, new) }
                .toSet())

            is OrCriterion -> OrCriterion(criterion.criteria.map { updateCriterion(it, old, new) }
                .toSet())

            else -> criterion
        }
    }

    private suspend fun updateFilterHelper(
        old: Set<Long>,
        new: Long,
        cursor: Cursor,
        prefNameCreator: (Long) -> String
    ) {
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val id = cursor.getLong(KEY_ROWID)
            val filterKey = prefNameCreator(id)
            val filterPersistence = FilterPersistence(dataStore, filterKey)
            filterPersistence.getValue()?.let {
                val newValue = updateCriterion(it, old, new)
                if (newValue != it) {
                    Timber.i("updating parties in filter %s: %s -> %s", filterKey, it, newValue)
                    filterPersistence.persist(newValue)
                }
            }
            cursor.moveToNext()
        }
    }

    fun mergeParties(
        duplicateIds: Set<Long>,
        keepId: Long,
        mergeStrategy: MergeStrategy
    ) {
        viewModelScope.launch(context = coroutineContext()) {
            check(!duplicateIds.contains(keepId))

            val operations = ArrayList<ContentProviderOperation>().apply {
                when (mergeStrategy) {
                    MergeStrategy.DELETE -> {
                        val joined = duplicateIds.joinToString()
                        val contentValues = ContentValues(1).apply {
                            put(KEY_PAYEEID, keepId)
                        }

                        val where =
                            "$KEY_PAYEEID IN ($joined, (select $KEY_ROWID from $TABLE_PAYEES where $KEY_PARENTID IN ($joined)))"
                        add(
                            newUpdate(ACCOUNTS_URI).withValue(KEY_SEALED, -1)
                                .withSelection("$KEY_SEALED = 1", null).build()
                        )
                        add(
                            newUpdate(DEBTS_URI).withValue(KEY_SEALED, -1)
                                .withSelection("$KEY_SEALED = 1", null).build()
                        )
                        add(
                            newUpdate(DEBTS_URI).withValues(contentValues)
                                .withSelection(where, null).build()
                        )
                        add(
                            newUpdate(TRANSACTIONS_URI).withValues(contentValues)
                                .withSelection(where, null).build()
                        )
                        add(
                            newUpdate(TEMPLATES_URI).withValues(contentValues)
                                .withSelection(where, null).build()
                        )
                        add(
                            newUpdate(CHANGES_URI).withValues(contentValues)
                                .withSelection(where, null).build()
                        )
                        add(
                            newDelete(PAYEES_URI).withSelection(
                                "$KEY_ROWID IN ($joined)",
                                null
                            ).build()
                        )
                        add(
                            newUpdate(ACCOUNTS_URI).withValue(KEY_SEALED, 1)
                                .withSelection("$KEY_SEALED = -1", null).build()
                        )
                        add(
                            newUpdate(DEBTS_URI).withValue(KEY_SEALED, 1)
                                .withSelection("$KEY_SEALED = -1", null).build()
                        )
                    }

                    MergeStrategy.GROUP -> {
                        duplicateIds.forEach {
                            add(
                                newUpdate(ContentUris.withAppendedId(PAYEES_URI, it))
                                    .withValues(ContentValues(1).apply {
                                        put(KEY_PARENTID, keepId)
                                    })
                                    .build()
                            )
                        }
                    }
                }
            }
            val size =
                contentResolver.applyBatch(AUTHORITY, operations).size
            if (size == operations.size) {
                updatePartyFilters(duplicateIds, keepId)
                updatePartyBudgets(duplicateIds, keepId)
            } else {
                CrashHandler.report(Exception("Unexpected result while merging Parties, result size is : $size"))
            }
        }
    }

    fun removeDuplicateFromGroup(id: Long) {
        repository.unsetParentId(id)
    }

    fun cleanup() {
        repository.cleanupUnusedParties()
    }
}