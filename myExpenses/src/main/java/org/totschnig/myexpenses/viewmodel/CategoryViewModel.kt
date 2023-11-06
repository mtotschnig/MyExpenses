package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.cash.copper.Query
import app.cash.copper.flow.observeQuery
import arrow.core.flatMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.deleteCategory
import org.totschnig.myexpenses.db2.ensureCategoryTree
import org.totschnig.myexpenses.db2.moveCategory
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.export.CategoryExporter
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.json.CategoryExport
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.failure
import org.totschnig.myexpenses.util.io.displayName
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.data.BudgetAllocation
import org.totschnig.myexpenses.viewmodel.data.Category
import timber.log.Timber
import java.io.FileNotFoundException

sealed class LoadingState {
    data object Loading: LoadingState()

    sealed class Result: LoadingState()
    class Data(val data: Category): Result()

    class Empty(val hasUnfiltered: Boolean): Result()
}

open class CategoryViewModel(
    application: Application,
    protected val savedStateHandle: SavedStateHandle
) :
    ContentResolvingAndroidViewModel(application) {
    private val _deleteResult: MutableStateFlow<Result<DeleteResult>?> = MutableStateFlow(null)
    private val _moveResult: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private val _importResult: MutableStateFlow<Pair<Int, Int>?> = MutableStateFlow(null)
    private val _exportResult: MutableStateFlow<Result<Pair<Uri, String>>?> = MutableStateFlow(null)
    private val _syncResult: MutableStateFlow<String?> = MutableStateFlow(null)
    val deleteResult: StateFlow<Result<DeleteResult>?> = _deleteResult.asStateFlow()
    val moveResult: StateFlow<Boolean?> = _moveResult.asStateFlow()
    val importResult: StateFlow<Pair<Int, Int>?> = _importResult.asStateFlow()
    val exportResult: StateFlow<Result<Pair<Uri, String>>?> = _exportResult.asStateFlow()
    val syncResult: Flow<String> = _syncResult.asStateFlow().filterNotNull()
    val defaultSort = Sort.USAGES

    sealed class DialogState : java.io.Serializable
    data object NoShow : DialogState()
    data class Show(
        val category: Category? = null,
        val parent: Category? = null,
        val saving: Boolean = false,
        val error: Boolean = false
    ) : DialogState()

    @OptIn(SavedStateHandleSaveableApi::class)
    var dialogState: DialogState by savedStateHandle.saveable { mutableStateOf(NoShow) }

    sealed class DeleteResult {
        class OperationPending(
            val categories: List<Category>,
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
        get() = savedStateHandle[KEY_FILTER]
        set(value) {
            savedStateHandle[KEY_FILTER] = value
        }

    var typeFilter: UByte?
        get() = savedStateHandle.get<Int>(KEY_TYPE_FILTER)?.toUByte()
        set(value) {
            savedStateHandle[KEY_TYPE_FILTER] = value?.toInt()
        }

    fun toggleTypeFilterIsShown() {
        typeFilter = if (typeFilter == null) FLAG_NEUTRAL else null
    }

    val typeFilterLiveData = savedStateHandle.getLiveData<Int?>(KEY_TYPE_FILTER, null).map {
        it?.toUByte()
    }

    fun setSortOrder(sort: Sort) {
        sortOrder.tryEmit(sort)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryTree = combine(
        typeFilterLiveData.asFlow(),
        savedStateHandle.getLiveData(KEY_FILTER, "").asFlow(),
        sortOrder
    ) { type, filter, sort ->
        Timber.d("new emission: $type/$filter/$sort")
        Triple(type, filter, sort)
    }.flatMapLatest { (type, filter, sortOrder) ->
        val (selection, selectionArgs) = joinQueryAndAccountFilter(
            filter,
            savedStateHandle.get<Long>(KEY_ACCOUNTID),
            KEY_LABEL_NORMALIZED, KEY_CATID, "_Tree_"
        )
        categoryTree(
            selection = selection,
            selectionArgs = selectionArgs?.let { it + it } ?: emptyArray(),
            sortOrder = sortOrder.toOrderByWithDefault(defaultSort, collate),
            queryParameter = type?.let { mapOf(KEY_TYPE to type.toString()) } ?: emptyMap(),
            projection = null,
            keepCriteria = null,
            withColors = false
        )
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, LoadingState.Loading)

    val categoryTreeForSelect: Flow<LoadingState>
        get() = categoryTree(sortOrder = sortOrder.value.toOrderByWithDefault(defaultSort, collate))

    fun categoryTree(
        selection: String? = null,
        selectionArgs: Array<String> = emptyArray(),
        sortOrder: String? = null,
        projection: Array<String>? = null,
        additionalSelectionArgs: Array<String>? = null,
        queryParameter: Map<String, String> = emptyMap(),
        keepCriteria: ((Category) -> Boolean)? = null,
        withColors: Boolean = true
    ): Flow<LoadingState.Result> {
        return contentResolver.observeQuery(
            categoryUri(queryParameter),
            projection,
            selection,
            selectionArgs + (additionalSelectionArgs ?: emptyArray()),
            sortOrder ?: KEY_LABEL,
            true
        ).mapToResult(keepCriteria, withColors)
    }

    private fun categoryUri(queryParameter: Map<String, String>): Uri =
        BaseTransactionProvider.CATEGORY_TREE_URI.buildUpon()
            .apply {
                queryParameter.forEach {
                    appendQueryParameter(it.key, it.value)
                }
            }
            .build()

    private fun Flow<Query>.mapToResult(
        keepCriteria: ((Category) -> Boolean)?,
        withColors: Boolean
    ): Flow<LoadingState.Result> = mapNotNull { query ->
        withContext(Dispatchers.IO) {
            query.run()?.use { cursor ->
                if (cursor.moveToFirst())
                    Category(
                        id = 0,
                        parentId = null,
                        level = 0,
                        label = "ROOT",
                        children = ingest(
                            withColors,
                            cursor,
                            null,
                            1
                        )
                    ).pruneNonMatching(keepCriteria)?.let {
                        LoadingState.Data(data = it)
                    } ?: LoadingState.Empty(true)
                else LoadingState.Empty(cursor.extras.getBoolean(KEY_COUNT))
            }
        }
    }

    fun saveCategory(label: String, icon: String?, typeFlags: UByte) {
        viewModelScope.launch(context = coroutineContext()) {
            (dialogState as? Show)?.takeIf { !it.saving }?.let {
                val category = Category(
                    id = it.category?.id ?: 0,
                    label = label,
                    icon = icon,
                    parentId = it.parent?.id,
                    typeFlags = typeFlags
                )
                dialogState = it.copy(saving = true)
                dialogState = if (repository.saveCategory(category) == null) {
                    it.copy(error = true)
                } else {
                    NoShow
                }
            }
        }
    }

    fun deleteCategories(categories: List<Category>) {
        viewModelScope.launch(context = coroutineContext()) {
            mappedObjectQuery(
                arrayOf(KEY_MAPPED_BUDGETS, KEY_HAS_DESCENDANTS),
                categories.map { it.id }, true
            )?.use { cursor ->
                cursor.moveToFirst()
                val mappedBudgets = cursor.getInt(0)
                val hasDescendants = cursor.getInt(1)
                _deleteResult.update {
                    Result.success(
                        DeleteResult.OperationPending(
                            categories,
                            mappedBudgets,
                            hasDescendants
                        )
                    )
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
                    CrashHandler.reportWithDbSchema(contentResolver, e)
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
        _syncResult.update {
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
                            )?.let {
                                Result.success(it)
                            } ?: Result.failure(createFileFailure(context, destDir, fileName))
                        }
                    ).mapCatching {
                        it.uri to it.displayName
                    }
                } else failure(R.string.io_error_appdir_null)
            }
        }
    }

    fun syncCatsExport(accountName: String) {
        viewModelScope.launch(context = coroutineContext()) {
            GenericAccountService.getSyncBackendProvider(localizedContext, accountName)
                .mapCatching { backend ->
                    contentResolver.query(
                        BaseTransactionProvider.CATEGORY_TREE_URI,
                        null,
                        null,
                        null,
                        null,
                    )?.use {
                        fun ingest(
                            cursor: Cursor,
                            parentId: Long?,
                        ): List<CategoryExport> =
                            buildList {
                                while (!cursor.isAfterLast) {
                                    val nextId = cursor.getLong(KEY_ROWID)
                                    val nextParent = cursor.getLongOrNull(KEY_PARENTID)
                                    val nextLabel = cursor.getString(KEY_LABEL)
                                    val nextColor = cursor.getIntOrNull(KEY_COLOR)
                                    val nextIcon = cursor.getStringOrNull(KEY_ICON)
                                    val nextUuid = cursor.getString(KEY_UUID)
                                    if (nextParent == parentId) {
                                        cursor.moveToNext()
                                        add(
                                            CategoryExport(
                                                nextUuid,
                                                nextLabel,
                                                nextIcon,
                                                nextColor,
                                                if (parentId == null) cursor.getInt(KEY_TYPE) else null,
                                                ingest(cursor, nextId)
                                            )
                                        )
                                    } else return@buildList
                                }
                            }
                        if (it.moveToFirst()) {
                            ingest(it, null)
                        } else null
                    }?.let {
                        "${backend.writeCategories(it)} -> $accountName"
                    }
                }.fold(
                    onSuccess = { it },
                    onFailure = {
                        Timber.e(it)
                        getString(R.string.write_fail_reason_cannot_write) + ": " + it.message
                    }
                )?.let { message -> _syncResult.update { message } }
        }
    }

    fun syncCatsImport(accountName: String) {
        viewModelScope.launch(context = coroutineContext()) {
            GenericAccountService.getSyncBackendProvider(getApplication(), accountName)
                .flatMap { it.categories }
                .fold(
                    onSuccess = { list ->
                        "Imported ${list.sumOf { repository.ensureCategoryTree(it, null) }} categories"
                                },
                    onFailure = {
                        if (it !is FileNotFoundException) {
                            Timber.e(it)
                        }
                        it.safeMessage
                    }
                ).let { message -> _syncResult.update { message } }
        }
    }

    companion object {

        const val KEY_TYPE_FILTER = "typeFilter"

        fun ingest(
            withColors: Boolean,
            cursor: Cursor,
            parentId: Long?,
            level: Int
        ): List<Category> =
            buildList {
                if (!cursor.isBeforeFirst) {
                    while (!cursor.isAfterLast) {
                        val nextParent = cursor.getLongOrNull(KEY_PARENTID)
                        val nextId = cursor.getLong(KEY_ROWID)
                        val nextLabel = cursor.getString(KEY_LABEL)
                        val nextPath = cursor.getString(KEY_PATH)
                        val nextColor = if (withColors) cursor.getIntOrNull(KEY_COLOR) else null
                        val nextIcon = cursor.getStringOrNull(KEY_ICON)
                        val nextType = cursor.getIntOrNull(KEY_TYPE)?.toUByte()
                        val nextIsMatching = cursor.getInt(KEY_MATCHES_FILTER) == 1
                        val nextLevel = cursor.getInt(KEY_LEVEL)
                        val nextSum = cursor.getLongIfExistsOr0(KEY_SUM)
                        val nextBudget = cursor.getLongIfExistsOr0(KEY_BUDGET)
                        val nextBudgetRollOverPrevious =
                            cursor.getLongIfExistsOr0(KEY_BUDGET_ROLLOVER_PREVIOUS)
                        val nextBudgetRollOverNext =
                            cursor.getLongIfExistsOr0(KEY_BUDGET_ROLLOVER_NEXT)
                        val nextBudgetOneTime = cursor.getIntIfExistsOr0(KEY_ONE_TIME) != 0
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
                                        cursor,
                                        nextId,
                                        level + 1
                                    ),
                                    nextIsMatching,
                                    nextColor,
                                    nextIcon,
                                    nextSum,
                                    BudgetAllocation(
                                        nextBudget,
                                        nextBudgetRollOverPrevious,
                                        nextBudgetRollOverNext,
                                        nextBudgetOneTime
                                    ),
                                    typeFlags = nextType
                                )
                            )
                        } else return@buildList
                    }
                }
            }
    }
}

