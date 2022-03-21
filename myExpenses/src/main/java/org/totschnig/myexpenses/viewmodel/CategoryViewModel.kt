package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.*
import app.cash.copper.Query
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Category2
import timber.log.Timber

class CategoryViewModel(application: Application, private val savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {
    private var _deleteResult: MutableStateFlow<Result<DeleteResult>?> = MutableStateFlow(null)
    private var _moveResult: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    var deleteResult: StateFlow<Result<DeleteResult>?> = _deleteResult
    var moveResult: StateFlow<Boolean?> = _moveResult

    sealed class DeleteResult {
        class OperationPending(val ids: List<Long>, val mappedToBudgets: Int, val hasDescendants: Int): DeleteResult()
        class OperationComplete(val deleted: Int, val mappedToTransactions: Int, val mappedToTemplates: Int): DeleteResult()
    }

    private val sortOrder = MutableStateFlow<String?>(null)

    fun setFilter(filter: String) {
        savedStateHandle[KEY_FILTER] = filter
    }

    fun getFilter() = savedStateHandle.get<String>(KEY_FILTER)

    fun setSortOrder(sort: String) {
        viewModelScope.launch {
            sortOrder.emit(sort)
        }
    }

    val categoryTree: StateFlow<Category2> = combine(
        savedStateHandle.getLiveData(KEY_FILTER, "").asFlow(),
        sortOrder
    ) { filter, sort ->
        filter to sort
    }.flatMapLatest { (filter, sortOrder) -> categoryTree(filter, sortOrder) }
        .stateIn(viewModelScope, SharingStarted.Lazily, Category2.EMPTY)

    val categoryTreeForSelect = categoryTree("", sortOrder.value)

    private fun categoryTree(filter: String, sortOrder: String?): Flow<Category2> {
        val (selection, selectionArgs) = if (filter.isNotBlank()) {
            "$KEY_LABEL_NORMALIZED LIKE ?" to arrayOf(
                "%${Utils.escapeSqlLikeExpression(Utils.normalize(filter))}%"
            )
        } else null to null

        return contentResolver.observeQuery(
            TransactionProvider.CATEGORIES_URI.buildUpon()
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_HIERARCHICAL, "1")
                .build(),
            null,
            selection,
            selectionArgs,
            sortOrder,
            true
        ).mapToTree(filter.isNotBlank())
    }

    private fun Flow<Query>.mapToTree(
        isFiltered: Boolean,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Flow<Category2> = transform { query ->
        Timber.d("new emission")
        val value = withContext(dispatcher) {
            query.run()?.use { cursor ->
                cursor.moveToFirst()
                Category2(
                    id = 0,
                    parentId = null,
                    level = 0,
                    label = "ROOT",
                    children = ingest(getApplication(), cursor, null, 1),
                    isMatching = true,
                    color = null as Int?,
                    icon = null
                ).let {
                    if (isFiltered) it.pruneNonMatching() else it
                }
            }
        }
        if (value != null) {
            emit(value)
        }
    }

    fun saveCategory(category: Category2) =
        liveData(context = coroutineContext()) {
            emit(repository.saveCategory(category))
        }

    fun deleteCategories(ids: List<Long>) {
        viewModelScope.launch(context = coroutineContext()) {
            mappedObjectQuery(
                arrayOf(KEY_MAPPED_BUDGETS, KEY_HAS_DESCENDANTS),
                ids, true
            )?.use { cursor ->
                cursor.moveToFirst()
                val mappedBudgets = cursor.getInt(0)
                val hasDescendants = cursor.getInt(1)
                if (mappedBudgets == 0 && hasDescendants == 0) {
                    deleteCategoriesDo(ids)
                } else {
                    _deleteResult.update {
                        Result.success(
                            DeleteResult.OperationPending(
                                ids,
                                mappedBudgets,
                                hasDescendants
                            )
                        )
                    }
                }
            }
        }
    }

    private fun mappedObjectQuery(projection: Array<String>, ids: List<Long>, aggregate: Boolean) =
        contentResolver.query(
            TransactionProvider.CATEGORIES_URI.buildUpon()
                .appendQueryParameter(
                    TransactionProvider.QUERY_PARAMETER_MAPPED_OBJECTS,
                    if (aggregate) "2" else "1"
                )
                .build(),
            projection,
            "$KEY_ROWID ${WhereFilter.Operation.IN.getOp(ids.size)}",
            ids.map { it.toString() }.toTypedArray(), null
        )

    fun deleteCategoriesDo(ids: List<Long>) {
        viewModelScope.launch(context = coroutineContext()) {
            try {
                mappedObjectQuery(
                    arrayOf(KEY_ROWID, KEY_MAPPED_TRANSACTIONS, KEY_MAPPED_TEMPLATES),
                    ids, false
                ).use { cursor ->
                        if (cursor == null) {
                            _deleteResult
                            _deleteResult.failure(R.string.db_error_cursor_null)
                        } else {
                            var deleted = 0
                            var mappedToTransaction = 0
                            var mappedToTemplate = 0
                            if (cursor.moveToFirst()) {
                                while (!cursor.isAfterLast) {
                                    var deletable = true
                                    if (cursor.getInt(1) > 0) {
                                        deletable = false
                                        mappedToTransaction++
                                    }
                                    if (cursor.getInt(2) > 0) {
                                        deletable = false
                                        mappedToTemplate++
                                    }
                                    if (deletable) {
                                        org.totschnig.myexpenses.model.Category.delete(
                                            cursor.getLong(
                                                0
                                            )
                                        )
                                        deleted++
                                    }
                                    cursor.moveToNext()
                                }
                                _deleteResult.update {
                                    Result.success(DeleteResult.OperationComplete(deleted, mappedToTransaction, mappedToTemplate))
                                }
                            } else {
                                _deleteResult.failure(R.string.db_error_cursor_empty)
                            }
                        }
                    }
            } catch (e: SQLiteConstraintException) {
                CrashHandler.reportWithDbSchema(e)
                _deleteResult.update {
                    Result.failure(e)
                }
            }
        }
    }

    fun messageShown() {
        _deleteResult.update {
            null
        }
        _moveResult.update {
            null
        }
    }

    fun moveCategory(source: Long, target: Long?) {
        _moveResult.update {
            repository.moveCategory(source, target)
            org.totschnig.myexpenses.model.Category.move(source, target)
        }
    }

    companion object {
        fun ingest(context: Context, cursor: Cursor, parentId: Long?, level: Int): List<Category2> =
            buildList {
                if (!cursor.isBeforeFirst)
                    while (!cursor.isAfterLast) {
                        val nextParent = cursor.getLongOrNull(KEY_PARENTID)
                        val nextId = cursor.getLong(KEY_ROWID)
                        val nextLabel = cursor.getString(KEY_LABEL)
                        val nextPath = cursor.getString(KEY_PATH)
                        val nextColor = cursor.getIntOrNull(KEY_COLOR)
                        val nextIcon = cursor.getStringOrNull(KEY_ICON)
                        val nextIsMatching = cursor.getInt(KEY_MATCHES_FILTER) == 1
                        val nextLevel = cursor.getInt(KEY_LEVEL)
                        if (nextParent == parentId) {
                            check(level == nextLevel)
                            cursor.moveToNext()
                            add(
                                Category2(
                                    nextId,
                                    parentId ?: 0L,
                                    nextLevel,
                                    nextLabel,
                                    nextPath,
                                    ingest(context, cursor, nextId, level + 1),
                                    nextIsMatching,
                                    nextColor,
                                    nextIcon
                                )
                            )
                        } else return@buildList
                    }
            }
    }
}

