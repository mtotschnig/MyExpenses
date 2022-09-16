package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.cash.copper.Query
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.CategoryExporter
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.failure
import org.totschnig.myexpenses.util.io.FileUtils
import org.totschnig.myexpenses.viewmodel.data.BudgetAllocation
import org.totschnig.myexpenses.viewmodel.data.Category
import timber.log.Timber

open class CategoryViewModel(
    application: Application,
    protected val savedStateHandle: SavedStateHandle
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

    sealed class DialogState: java.io.Serializable
    object NoShow : DialogState()
    data class Show(
        val id: Long? = null,
        val parentId: Long? = null,
        val label: String? = null,
        val icon: String? = null,
        val saving: Boolean = false,
        val error: Boolean = false
    ): DialogState()

    @OptIn(SavedStateHandleSaveableApi::class)
    var dialogState: DialogState by savedStateHandle.saveable { mutableStateOf(NoShow) }

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

    var filter: String?
        get() = savedStateHandle.get<String>(KEY_FILTER)
        set(value) { savedStateHandle[KEY_FILTER] = value }

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
        val (selection, selectionArgs) = if (filter.isNotBlank()) {
            val selectionArgs =
                arrayOf("%${Utils.escapeSqlLikeExpression(Utils.normalize(filter))}%")
            //The filter is applied twice in the CTE
            "$KEY_LABEL_NORMALIZED LIKE ?" to selectionArgs + selectionArgs
        } else null to emptyArray()
        categoryTree(
            selection = selection,
            selectionArgs = selectionArgs,
            sortOrder = sortOrder.toOrderByWithDefault(defaultSort),
            projection = null,
            keepCriteria = null,
            withColors = false
        )
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, Category.LOADING)

    val categoryTreeForSelect: Flow<Category>
        get() = categoryTree(sortOrder = sortOrder.value.toOrderByWithDefault(defaultSort))

    fun categoryTree(
        selection: String? = null,
        selectionArgs: Array<String> = emptyArray(),
        sortOrder: String? = null,
        projection: Array<String>? = null,
        additionalSelectionArgs: Array<String>? = null,
        queryParameter: Map<String, String> = emptyMap(),
        keepCriteria: ((Category) -> Boolean)? = null,
        withColors: Boolean = true
    ): Flow<Category> {
        return contentResolver.observeQuery(
            categoryUri(queryParameter),
            projection,
            selection,
            selectionArgs + (additionalSelectionArgs ?: emptyArray()),
            sortOrder ?: KEY_LABEL,
            true
        ).mapToTree(keepCriteria, withColors)
    }

    private fun categoryUri(queryParameter: Map<String, String>): Uri =
        TransactionProvider.CATEGORIES_URI.buildUpon()
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_HIERARCHICAL, "1")
            .apply {
                queryParameter.forEach {
                    appendQueryParameter(it.key, it.value)
                }
            }
            .build()

    private fun Flow<Query>.mapToTree(
        keepCriteria: ((Category) -> Boolean)?,
        withColors: Boolean
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
                        withColors,
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

    fun saveCategory(label: String, icon: String?) {
        viewModelScope.launch(context = coroutineContext()) {
            dialogState.let {
                if (it is Show) {
                    val category = Category(
                        id = it.id ?: 0,
                        label = label,
                        icon = icon,
                        parentId = it.parentId
                    )
                    dialogState = it.copy(saving = true)
                    dialogState = if (repository.saveCategory(category) == null) {
                        it.copy(error = true)
                    } else {
                        NoShow
                    }
                } else {
                    throw java.lang.IllegalStateException("SaveCategory called without dialogState")
                }
            }
        }
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
        viewModelScope.launch(context = coroutineContext()) {
            _moveResult.update {
                repository.moveCategory(source, target)
            }
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
                )?.getSerializable(TransactionProvider.KEY_RESULT) as? Pair<Int, Int> ?: (0 to 0)
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
                            AppDirHelper.timeStampedFile(
                                destDir,
                                fileName,
                                ExportFormat.QIF.mimeType, "qif"
                            ) ?.let {
                                Result.success(it)
                            } ?: Result.failure(createFileFailure(context, destDir, fileName))
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
            withColors: Boolean,
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
                        val nextColor = if (withColors) cursor.getIntOrNull(KEY_COLOR) else null
                        val nextIcon = cursor.getStringOrNull(KEY_ICON)
                        val nextIsMatching = cursor.getInt(KEY_MATCHES_FILTER) == 1
                        val nextLevel = cursor.getInt(KEY_LEVEL)
                        val nextSum = cursor.getColumnIndex(KEY_SUM).takeIf { it != -1 }
                            ?.let { cursor.getLong(it) } ?: 0L
                        val nextBudget = cursor.getColumnIndex(KEY_BUDGET).takeIf { it != -1 }
                            ?.let { cursor.getLong(it) } ?: 0L
                        val nextBudgetRollOverPrevious = cursor.getColumnIndex(
                            KEY_BUDGET_ROLLOVER_PREVIOUS).takeIf { it != -1 }
                            ?.let { cursor.getLong(it) } ?: 0L
                        val nextBudgetRollOverNext = cursor.getColumnIndex(KEY_BUDGET_ROLLOVER_NEXT).takeIf { it != -1 }
                            ?.let { cursor.getLong(it) } ?: 0L
                        val nextBudgetOneTime = cursor.getColumnIndex(KEY_ONE_TIME).takeIf { it != -1 }
                            ?.let { cursor.getInt(it) != 0 } ?: false
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
                                        withColors,
                                        context,
                                        cursor,
                                        nextId,
                                        level + 1
                                    ),
                                    nextIsMatching,
                                    nextColor,
                                    nextIcon,
                                    nextSum,
                                    BudgetAllocation(nextBudget, nextBudgetRollOverPrevious, nextBudgetRollOverNext, nextBudgetOneTime)
                                )
                            )
                            index++
                        } else return@buildList
                    }
                }
            }
    }
}

