package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.*
import app.cash.copper.Query
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber

class CategoryViewModel(application: Application, private val savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {
    var _deleteResult: MutableStateFlow<Result<DeleteResult>?> = MutableStateFlow(null)
    var deleteResult: StateFlow<Result<DeleteResult>?> = _deleteResult

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

    val categoryTree: Flow<Category> = combine(
        savedStateHandle.getLiveData(KEY_FILTER, "").asFlow(),
        sortOrder
    ) { filter, sort ->
        filter to sort
    }.flatMapLatest { (filter, sortOrder) ->
        val (selection, selectionArgs) = if (filter.isNotBlank()) {
            "$KEY_LABEL_NORMALIZED LIKE ?" to arrayOf(
                "%${Utils.escapeSqlLikeExpression(Utils.normalize(filter))}%"
            )
        } else null to null

        contentResolver.observeQuery(
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
    ): Flow<Category> = transform { query ->
        Timber.d("new emission")
        val value = withContext(dispatcher) {
            query.run()?.use { cursor ->
                cursor.moveToFirst()
                Category(
                    0,
                    0,
                    "ROOT",
                    ingest(getApplication(), cursor, null, 1),
                    true,
                    null as Int?,
                    null
                ).let {
                    if (isFiltered) it.pruneNonMatching() else it
                }
            }
        }
        if (value != null) {
            emit(value)
        }
    }

    fun saveCategory(category: org.totschnig.myexpenses.model.Category) =
        liveData(context = coroutineContext()) {
            emit(category.save())
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

    fun deleteMessageShown() {
        _deleteResult.update {
            null
        }
    }

    companion object {
        fun ingest(context: Context, cursor: Cursor, parentId: Long?, level: Int): List<Category> =
            buildList {
                if (!cursor.isBeforeFirst)
                    while (!cursor.isAfterLast) {
                        val nextParent = cursor.getLongOrNull(KEY_PARENTID)
                        val nextId = cursor.getLong(KEY_ROWID)
                        val nextLabel = cursor.getString(KEY_LABEL)
                        val nextColor = cursor.getIntOrNull(KEY_COLOR)
                        val nextIcon = cursor.getStringOrNull(KEY_ICON)
                        val nextIsMatching = cursor.getInt("matches") == 1
                        val nextLevel = cursor.getInt("level")
                        if (nextParent == parentId) {
                            check(level == nextLevel)
                            cursor.moveToNext()
                            add(
                                Category(
                                    nextId,
                                    nextLevel,
                                    nextLabel,
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

