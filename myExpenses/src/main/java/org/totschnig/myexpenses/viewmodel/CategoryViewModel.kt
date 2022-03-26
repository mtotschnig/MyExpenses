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
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.failure
import org.totschnig.myexpenses.util.io.FileUtils
import org.totschnig.myexpenses.viewmodel.data.Category2
import timber.log.Timber
import java.io.IOException
import java.io.OutputStreamWriter

open class CategoryViewModel(application: Application, private val savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {
    private var _deleteResult: MutableStateFlow<Result<DeleteResult>?> = MutableStateFlow(null)
    private var _moveResult: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private var _importResult: MutableStateFlow<Pair<Int, Int>?> = MutableStateFlow(null)
    private var _exportResult: MutableStateFlow<Result<Pair<Uri, String>>?> = MutableStateFlow(null)
    var deleteResult: StateFlow<Result<DeleteResult>?> = _deleteResult
    var moveResult: StateFlow<Boolean?> = _moveResult
    var importResult: StateFlow<Pair<Int, Int>?> = _importResult
    var exportResult: StateFlow<Result<Pair<Uri, String>>?> = _exportResult

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

    private val sortOrder = MutableStateFlow<String?>(null)

    fun setFilter(filter: String) {
        savedStateHandle[KEY_FILTER] = filter
    }

    fun getFilter() = savedStateHandle.get<String>(KEY_FILTER)

    fun setSortOrder(sort: String) {
        sortOrder.tryEmit(sort)
    }

    val categoryTree = categoryTree()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun categoryTree(projection: Array<String>? = null, keepCriteria: ((Category2) -> Boolean)? = null): StateFlow<Category2> = combine(
        savedStateHandle.getLiveData(KEY_FILTER, "").asFlow(),
        sortOrder
    ) { filter, sort ->
        Timber.d("new emission: $filter/$sort")
        filter to sort
    }.flatMapLatest { (filter, sortOrder) -> categoryTree(filter, sortOrder, projection, keepCriteria) }
        .stateIn(viewModelScope, SharingStarted.Lazily, Category2.EMPTY)

    val categoryTreeForSelect = categoryTree("", sortOrder.value)

    fun categoryTree(
        filter: String?,
        sortOrder: String?,
        projection: Array<String>? = null,
        keepCriteria: ((Category2) -> Boolean)? = null
    ): Flow<Category2> {
        val (selection, selectionArgs) = if (filter?.isNotBlank() == true) {
            val selectionArgs =
                arrayOf("%${Utils.escapeSqlLikeExpression(Utils.normalize(filter))}%")
            //The filter is applied twice in the CTE
            "$KEY_LABEL_NORMALIZED LIKE ?" to selectionArgs + selectionArgs
        } else null to null

        return contentResolver.observeQuery(
            TransactionProvider.CATEGORIES_URI.buildUpon()
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_HIERARCHICAL, "1")
                .build(),
            projection,
            selection,
            selectionArgs,
            sortOrder,
            true
        ).mapToTree(keepCriteria)
    }

    private fun Flow<Query>.mapToTree(
        keepCriteria: ((Category2) -> Boolean)?
    ): Flow<Category2> = transform { query ->
        Timber.d("new emission")
        val value = withContext(Dispatchers.IO) {
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
                ).pruneNonMatching(keepCriteria)
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
            _exportResult.update {
                val appDir = AppDirHelper.getAppDir(getApplication())
                if (appDir == null) {
                    failure(R.string.external_storage_unavailable)
                } else {
                    val mainLabel =
                        "CASE WHEN $KEY_PARENTID THEN (SELECT $KEY_LABEL FROM $TABLE_CATEGORIES parent WHERE parent.$KEY_ROWID = $TABLE_CATEGORIES.$KEY_PARENTID) ELSE $KEY_LABEL END"
                    val subLabel = "CASE WHEN $KEY_PARENTID THEN $KEY_LABEL END"

                    //sort sub categories immediately after their main category
                    val sort = "CASE WHEN parent_id then parent_id else _id END"
                    val fileName = "categories"
                    contentResolver.query(
                        TransactionProvider.CATEGORIES_URI, arrayOf(mainLabel, subLabel),
                        null, null, sort
                    )?.use { c ->
                        if (c.count == 0) {
                            failure(R.string.no_categories)
                        } else {
                            val outputFile = AppDirHelper.timeStampedFile(
                                appDir,
                                fileName,
                                ExportFormat.QIF.mimeType, "qif"
                            )
                            if (outputFile == null) {
                                failure(R.string.external_storage_unavailable)
                            } else {
                                try {
                                    @Suppress("BlockingMethodInNonBlockingContext")
                                    OutputStreamWriter(
                                        contentResolver.openOutputStream(outputFile.uri),
                                        encoding
                                    ).use { out ->
                                        out.write("!Type:Cat")
                                        c.moveToFirst()
                                        while (c.position < c.count) {
                                            val sb = StringBuilder()
                                            sb.append("\nN")
                                                .append(
                                                    TextUtils.formatQifCategory(
                                                        c.getString(0),
                                                        c.getString(1)
                                                    )
                                                )
                                                .append("\n^")
                                            out.write(sb.toString())
                                            c.moveToNext()
                                        }
                                    }
                                    Result.success<Pair<Uri, String>>(
                                        outputFile.uri to FileUtils.getPath(
                                            getApplication(),
                                            outputFile.uri
                                        )
                                    )
                                } catch (e: IOException) {
                                    failure(
                                        R.string.export_sdcard_failure,
                                        appDir.name,
                                        e.message
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
                        val nextSum = cursor.getColumnIndex(KEY_SUM).takeIf { it != -1 }?.let { cursor.getLong(it) } ?: 0L
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
                                    nextIcon,
                                    nextSum
                                )
                            )
                        } else return@buildList
                    }
            }
    }
}

