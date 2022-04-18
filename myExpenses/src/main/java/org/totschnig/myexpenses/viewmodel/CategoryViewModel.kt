package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.Query
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.CategoryExporter
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.failure
import org.totschnig.myexpenses.util.io.FileUtils
import org.totschnig.myexpenses.viewmodel.data.Category
import timber.log.Timber

open class CategoryViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) :
    ContentResolvingAndroidViewModel(application) {
    private val _deleteResult: MutableStateFlow<Result<DeleteResult>?> = MutableStateFlow(null)
    private val _moveResult: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private val _importResult: MutableStateFlow<Pair<Int, Int>?> = MutableStateFlow(null)
    private val _exportResult: MutableStateFlow<Result<Pair<Uri, String>>?> = MutableStateFlow(null)
    val deleteResult: StateFlow<Result<DeleteResult>?> = _deleteResult
    val moveResult: StateFlow<Boolean?> = _moveResult
    val importResult: StateFlow<Pair<Int, Int>?> = _importResult
    val exportResult: StateFlow<Result<Pair<Uri, String>>?> = _exportResult
    val defaultSort = Sort.USAGES

    sealed class DeleteResult {
        class OperationPending(
            val ids: List<Long>,
            val mappedToBudgets: Int,
            val hasDescendants: Int
        ) : DeleteResult()

        class OperationComplete(
            val deleted: Int,
            val mappedToTransactions: Int,
            val mappedToTemplates: Int
        ) : DeleteResult()
    }

    val sortOrder = MutableStateFlow(Sort.LABEL)

    fun setFilter(filter: String) {
        savedStateHandle[KEY_FILTER] = filter
    }

    fun getFilter() = savedStateHandle.get<String>(KEY_FILTER)

    fun setSortOrder(sort: Sort) {
        sortOrder.tryEmit(sort)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryTree = combine(
        savedStateHandle.getLiveData(KEY_FILTER, "").asFlow(),
        sortOrder
    ) { filter, sort ->
        Timber.d("new emission: $filter/$sort")
        filter to sort
    }.flatMapLatest { (filter, sortOrder) ->
        categoryTree(
            filter = filter,
            sortOrder = sortOrder.toOrderByWithDefault(defaultSort),
            projection = null,
            keepCriteria = null
        )
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, Category.LOADING)

    val categoryTreeForSelect: Flow<Category>
        get() = categoryTree("", sortOrder.value.toOrderByWithDefault(defaultSort))

    fun categoryTree(
        filter: String?,
        sortOrder: String? = null,
        projection: Array<String>? = null,
        additionalSelectionArgs: Array<String>? = null,
        queryParameter: String? = null,
        keepCriteria: ((Category) -> Boolean)? = null
    ): Flow<Category> {
        val (selection, selectionArgs) = if (filter?.isNotBlank() == true) {
            val selectionArgs =
                arrayOf("%${Utils.escapeSqlLikeExpression(Utils.normalize(filter))}%")
            //The filter is applied twice in the CTE
            "$KEY_LABEL_NORMALIZED LIKE ?" to selectionArgs + selectionArgs
        } else null to emptyArray()

        return contentResolver.observeQuery(
            categoryUri(queryParameter),
            projection,
            selection,
            selectionArgs + (additionalSelectionArgs ?: emptyArray()),
            sortOrder ?: KEY_LABEL,
            true
        ).mapToTree(keepCriteria)
    }

    private fun categoryUri(queryParameter: String?): Uri =
        TransactionProvider.CATEGORIES_URI.buildUpon()
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_HIERARCHICAL, "1")
            .apply {
                queryParameter?.let {
                    appendQueryParameter(it, "1")
                }
            }
            .build()

    private fun Flow<Query>.mapToTree(
        keepCriteria: ((Category) -> Boolean)?
    ): Flow<Category> = transform { query ->
        val value = withContext(Dispatchers.IO) {
            query.run()?.use { cursor ->
                cursor.moveToFirst()
                Category(
                    id = 0,
                    parentId = null,
                    level = 0,
                    label = "ROOT",
                    children = ingest(
                        getApplication(),
                        cursor,
                        null,
                        1
                    ),
                    isMatching = true,
                    color = null as Int?,
                    icon = null
                ).pruneNonMatching(keepCriteria)
            }
        }
        if (value != null) {
            emit(value)
        }
    }

    fun saveCategory(category: Category) =
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

    private fun <T> failure(
        @StringRes resId: Int,
        vararg formatArgs: Any?
    ) = Result.failure<T>(getApplication(), resId, formatArgs)

    fun deleteCategoriesDo(ids: List<Long>) {
        viewModelScope.launch(context = coroutineContext()) {
            _deleteResult.update {
                try {
                    mappedObjectQuery(
                        arrayOf(KEY_ROWID, KEY_MAPPED_TRANSACTIONS, KEY_MAPPED_TEMPLATES),
                        ids, false
                    ).use { cursor ->
                        if (cursor == null) {
                            failure(R.string.db_error_cursor_null)
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
                                        if (repository.deleteCategory(cursor.getLong(0))) {
                                            deleted++
                                        }
                                    }
                                    cursor.moveToNext()
                                }
                                Result.success(
                                    DeleteResult.OperationComplete(
                                        deleted,
                                        mappedToTransaction,
                                        mappedToTemplate
                                    )
                                )
                            } else {
                                failure(R.string.db_error_cursor_empty)
                            }
                        }
                    }
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(e)
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
        _importResult.update {
            null
        }
    }

    fun moveCategory(source: Long, target: Long?) {
        _moveResult.update {
            repository.moveCategory(source, target)
        }
    }

    fun importCats() {
        viewModelScope.launch(context = coroutineContext()) {
            _importResult.update {
                contentResolver.call(
                    TransactionProvider.DUAL_URI,
                    TransactionProvider.METHOD_SETUP_CATEGORIES,
                    null,
                    null
                )?.getSerializable(TransactionProvider.KEY_RESULT) as? Pair<Int, Int> ?: 0 to 0
            }
        }
    }

    fun exportCats(encoding: String) {
        viewModelScope.launch(context = coroutineContext()) {
            val context = getApplication<MyApplication>()
            val destDir = AppDirHelper.getAppDir(context)
            val fileName = "categories"
            _exportResult.update {
                if (destDir != null) {
                    CategoryExporter.export(getApplication(), encoding,
                        lazy {
                            Result.success(
                                AppDirHelper.timeStampedFile(destDir,
                                    fileName,
                                    ExportFormat.QIF.mimeType, "qif"
                                )  ?: throw createFileFailure(context, destDir, fileName)
                            )
                        }
                    ).mapCatching {
                        it to FileUtils.getPath(context, it)
                    }
                } else failure(R.string.external_storage_unavailable)
            }
        }
    }

    companion object {

        fun ingest(
            context: Context,
            cursor: Cursor,
            parentId: Long?,
            level: Int
        ): List<Category> =
            buildList {
                if (!cursor.isBeforeFirst) {
                    var index = 0
                    while (!cursor.isAfterLast) {
                        val nextParent = cursor.getLongOrNull(KEY_PARENTID)
                        val nextId = cursor.getLong(KEY_ROWID)
                        val nextLabel = cursor.getString(KEY_LABEL)
                        val nextPath = cursor.getString(KEY_PATH)
                        val nextColor = cursor.getIntOrNull(KEY_COLOR)
                        val nextIcon = cursor.getStringOrNull(KEY_ICON)
                        val nextIsMatching = cursor.getInt(KEY_MATCHES_FILTER) == 1
                        val nextLevel = cursor.getInt(KEY_LEVEL)
                        val nextSum = cursor.getColumnIndex(KEY_SUM).takeIf { it != -1 }
                            ?.let { cursor.getLong(it) } ?: 0L
                        val nextBudget = cursor.getColumnIndex(KEY_BUDGET).takeIf { it != -1 }
                            ?.let { cursor.getLong(it) } ?: 0L
                        if (nextParent == parentId) {
                            check(level == nextLevel)
                            cursor.moveToNext()
                            add(
                                Category(
                                    nextId,
                                    parentId,
                                    nextLevel,
                                    nextLabel,
                                    nextPath,
                                    ingest(
                                        context,
                                        cursor,
                                        nextId,
                                        level + 1
                                    ),
                                    nextIsMatching,
                                    nextColor,
                                    nextIcon,
                                    nextSum,
                                    nextBudget
                                )
                            )
                            index++
                        } else return@buildList
                    }
                }
            }
    }
}

