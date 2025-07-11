package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.BundleCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
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
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.deleteCategory
import org.totschnig.myexpenses.db2.ensureCategoryTree
import org.totschnig.myexpenses.db2.getCategoryPath
import org.totschnig.myexpenses.db2.mergeCategories
import org.totschnig.myexpenses.db2.moveCategory
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.export.CategoryExporter
import org.totschnig.myexpenses.export.createFileFailure
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model2.CategoryExport
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.ACCOUNTS_MINIMAL_URI_WITH_AGGREGATES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_NEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_DESCENDANTS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LEVEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_BUDGETS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TEMPLATES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MATCHES_FILTER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BUDGETS
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.BUDGETS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.CATEGORIES_URI
import org.totschnig.myexpenses.provider.filter.AndCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.filter.NotCriterion
import org.totschnig.myexpenses.provider.filter.Operation
import org.totschnig.myexpenses.provider.filter.OrCriterion
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getIntIfExistsOr0
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongIfExistsOr0
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.failure
import org.totschnig.myexpenses.util.io.displayName
import org.totschnig.myexpenses.util.replace
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.data.BudgetAllocation
import org.totschnig.myexpenses.viewmodel.data.Category
import timber.log.Timber
import java.io.FileNotFoundException

sealed class LoadingState {
    data object Loading : LoadingState()

    sealed class Result : LoadingState()
    class Data(val data: Category) : Result()

    class Empty(val hasUnfiltered: Boolean) : Result()
}

open class CategoryViewModel(
    application: Application,
    protected val savedStateHandle: SavedStateHandle,
) :
    ContentResolvingAndroidViewModel(application) {
    private val _deleteResult: MutableStateFlow<Result<DeleteResult>?> = MutableStateFlow(null)
    private val _moveResult: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private val _importResult: MutableStateFlow<Pair<Int, Int>?> = MutableStateFlow(null)
    private val _exportResult: MutableStateFlow<Result<Pair<Uri, String>>?> = MutableStateFlow(null)
    protected val _syncResult: MutableStateFlow<String?> = MutableStateFlow(null)
    private val _mergeResult: MutableStateFlow<Unit?> = MutableStateFlow(null)
    val deleteResult: StateFlow<Result<DeleteResult>?> = _deleteResult.asStateFlow()
    val moveResult: StateFlow<Boolean?> = _moveResult.asStateFlow()
    val importResult: StateFlow<Pair<Int, Int>?> = _importResult.asStateFlow()
    val exportResult: StateFlow<Result<Pair<Uri, String>>?> = _exportResult.asStateFlow()
    val syncResult: Flow<String> = _syncResult.asStateFlow().filterNotNull()
    val mergeResult: Flow<Unit?> = _mergeResult.asStateFlow()
    val defaultSort = Sort.USAGES

    sealed class DialogState : Parcelable

    @Parcelize
    data object NoShow : DialogState()

    @Parcelize
    data class Edit(
        val category: Category? = null,
        val parent: Category? = null,
        val saving: Boolean = false,
        val error: Boolean = false,
    ) : DialogState() {
        val isNew: Boolean
            get() = category == null || category.id == 0L
    }

    @Parcelize
    data class Merge(
        val categories: List<Category>,
        val saving: Boolean = false,
    ) : DialogState()

    @OptIn(SavedStateHandleSaveableApi::class)
    var dialogState: DialogState by savedStateHandle.saveable { mutableStateOf(NoShow) }

    sealed class DeleteResult {
        class OperationPending(
            val categories: List<Category>,
            val mappedToBudgets: Int,
            val hasDescendants: Int,
        ) : DeleteResult()

        class OperationComplete(
            val deleted: Int,
            val mappedToTransactions: Int,
            val mappedToTemplates: Int,
        ) : DeleteResult()
    }

    val sortOrder = MutableStateFlow(Sort.LABEL)

    var filter: String?
        get() = savedStateHandle[KEY_FILTER]
        set(value) {
            savedStateHandle[KEY_FILTER] = value
        }

    var typeFilter: Byte?
        get() = savedStateHandle.get<Byte>(KEY_TYPE)
        set(value) {
            savedStateHandle[KEY_TYPE] = value
        }

    fun toggleTypeFilterIsShown() {
        typeFilter = if (typeFilter == null) FLAG_NEUTRAL else null
    }

    val typeFilterLiveData = savedStateHandle.getLiveData<Byte?>(KEY_TYPE, null)

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
            null,
            savedStateHandle.get<Long>(KEY_ACCOUNTID),
            KEY_LABEL_NORMALIZED, KEY_CATID, "_Tree_"
        )
        categoryTree(
            selection = selection,
            selectionArgs = selectionArgs?.let { it + it } ?: emptyArray(),
            sortOrder = sortOrder.toOrderByWithDefault(defaultSort, collate),
            queryParameter = type?.let { mapOf(KEY_TYPE to type.toString()) } ?: emptyMap(),
            projection = null,
            keepCriterion = { it.label.contains(filter, ignoreCase = true) },
            withColors = false
        )
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, LoadingState.Loading)

    val categoryTreeForSelect: Flow<LoadingState> by lazy {
        categoryTree(sortOrder = sortOrder.value.toOrderByWithDefault(defaultSort, collate))
    }

    fun categoryTree(
        selection: String? = null,
        selectionArgs: Array<String> = emptyArray(),
        sortOrder: String? = null,
        projection: Array<String>? = null,
        additionalSelectionArgs: Array<String>? = null,
        queryParameter: Map<String, String> = emptyMap(),
        keepCriterion: ((Category) -> Boolean)? = null,
        withColors: Boolean = true,
        idMapper: (Long) -> Long = { it },
    ): Flow<LoadingState.Result> {
        return contentResolver.observeQuery(
            categoryUri(queryParameter),
            projection,
            selection,
            selectionArgs + (additionalSelectionArgs ?: emptyArray()),
            sortOrder ?: KEY_LABEL,
            true
        ).mapToResult(keepCriterion, withColors, idMapper)
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
        keepCriterion: ((Category) -> Boolean)?,
        withColors: Boolean,
        idMapper: (Long) -> Long,
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
                            1,
                            idMapper
                        )
                    )
                        .pruneNonMatching()
                        ?.pruneByCriterion(keepCriterion)
                        ?.let {
                            LoadingState.Data(data = it)
                        } ?: LoadingState.Empty(true)
                else LoadingState.Empty(cursor.extras.getBoolean(KEY_COUNT))
            }
        }
    }

    fun saveCategory(label: String, icon: String?, typeFlags: Byte) {
        viewModelScope.launch(context = coroutineContext()) {
            (dialogState as? Edit)?.takeIf { !it.saving }?.let { edit ->
                val category = org.totschnig.myexpenses.model2.Category(
                    id = edit.category?.id?.takeIf { it != 0L },
                    label = label,
                    icon = icon,
                    parentId = edit.parent?.id ?: edit.category?.parentId,
                    type = typeFlags
                )
                dialogState = edit.copy(saving = true)
                dialogState = if (repository.saveCategory(category) == null) {
                    edit.copy(error = true)
                } else {
                    NoShow
                }
            }
        }
    }

    fun mergeCategories(keepIndex: Int) {
        viewModelScope.launch(context = coroutineContext()) {
            (dialogState as? Merge)?.takeIf { !it.saving }?.let { merge ->
                dialogState = merge.copy(saving = true)
                val idList = merge.categories.map { it.id }.toMutableList()
                val kept = idList.removeAt(keepIndex)
                repository.mergeCategories(idList, kept)
                updateCategoryFilters(idList.toSet(), kept)
                updateCategoryBudgets(idList.toSet(), kept)
                dialogState = NoShow
                _mergeResult.update { }
            }
        }
    }

    private suspend fun updateCategoryFilters(old: Set<Long>, new: Long) {
        contentResolver.query(ACCOUNTS_MINIMAL_URI_WITH_AGGREGATES, null, null, null, null)
            ?.use { cursor ->
                updateFilterHelper(old, new, cursor, MyExpensesViewModel::prefNameForCriteria)
            }
    }


    private suspend fun updateCategoryBudgets(old: Set<Long>, new: Long) {
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
            is CategoryCriterion -> {
                val oldSet = criterion.values.toSet()
                val newSet: Set<Long> = oldSet.replace(old, new)
                if (oldSet != newSet) {
                    val labelList = mutableListOf<String>()
                    contentResolver.query(
                        CATEGORIES_URI, arrayOf(KEY_LABEL),
                        "$KEY_ROWID IN (${newSet.joinToString()})", null, null
                    )?.use {
                        it.moveToFirst()
                        while (!it.isAfterLast) {
                            labelList.add(it.getString(0))
                            it.moveToNext()
                        }
                    }
                    CategoryCriterion(
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
        prefNameCreator: (Long) -> String,
    ) {
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val id = cursor.getLong(KEY_ROWID)
            val filterKey = prefNameCreator(id)
            val filterPersistence = FilterPersistence(dataStore, filterKey)
            filterPersistence.getValue()?.let {
                val newValue = updateCriterion(it, old, new)
                if (newValue != it) {
                    Timber.i("updating categories in filter %s: %s -> %s", filterKey, it, newValue)
                    filterPersistence.persist(newValue)
                }
            }
            cursor.moveToNext()
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
            CATEGORIES_URI.buildUpon()
                .appendQueryParameter(
                    TransactionProvider.QUERY_PARAMETER_MAPPED_OBJECTS,
                    if (aggregate) "2" else "1"
                )
                .build(),
            projection,
            "$KEY_ROWID ${Operation.IN.getOp(ids.size)}",
            ids.map { it.toString() }.toTypedArray(), null
        )

    private fun <T> failure(
        @StringRes resId: Int,
        vararg formatArgs: Any?,
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
        _deleteResult.update { null }
        _moveResult.update { null }
        _importResult.update { null }
        _syncResult.update { null }
        _mergeResult.update { null }
        _exportResult.update { null }
    }

    fun moveCategory(source: Long, target: Long?) {
        viewModelScope.launch(context = coroutineContext()) {
            _moveResult.update {
                repository.moveCategory(source, target)
            }
        }
    }

    fun checkImportableCategories(): LiveData<Category> = liveData(context = coroutineContext()) {
        contentResolver.call(
            TransactionProvider.DUAL_URI,
            TransactionProvider.METHOD_SETUP_CATEGORIES_DRY_RUN,
            null,
            null
        )!!.let {
            emit(
                BundleCompat.getParcelable(
                    it,
                    TransactionProvider.KEY_RESULT,
                    Category::class.java
                )!!
            )
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
            val fileName = "categories"
            _exportResult.update {
                AppDirHelper.getAppDir(context).mapCatching { destDir ->
                    CategoryExporter.export(
                        getApplication(), encoding,
                        lazy {
                            AppDirHelper.timeStampedFile(
                                destDir,
                                fileName,
                                ExportFormat.QIF.mimeType, "qif"
                            )?.let {
                                Result.success(it)
                            } ?: Result.failure(createFileFailure(context, destDir, fileName))
                        }
                    ).getOrThrow()
                }.mapCatching {
                    it.uri to it.displayName
                }
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
                                    val nextType =
                                        if (parentId == null) cursor.getIntOrNull(KEY_TYPE) else null
                                    if (nextParent == parentId) {
                                        cursor.moveToNext()
                                        add(
                                            CategoryExport(
                                                nextUuid,
                                                nextLabel,
                                                nextIcon,
                                                nextColor,
                                                nextType,
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
                        CrashHandler.report(it)
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
                        try {
                            "Imported ${list.sumOf { repository.ensureCategoryTree(it) }} categories"
                        } catch (e: Exception) {
                            e.safeMessage
                        }
                    },
                    onFailure = {
                        if (it !is FileNotFoundException) {
                            CrashHandler.report(it)
                        }
                        it.safeMessage
                    }
                ).let { message -> _syncResult.update { message } }
        }
    }

    fun defaultTransferCategory() = liveData(context = coroutineContext()) {
        emit(
            prefHandler.defaultTransferCategory?.let {
                repository.getCategoryPath(it)
            } ?: getString(R.string.unmapped)
        )
    }

    companion object {

        fun ingest(
            withColors: Boolean,
            cursor: Cursor,
            parentId: Long?,
            level: Int,
            idMapper: (Long) -> Long = { it },
        ): List<Category> =
            buildList {
                if (!cursor.isBeforeFirst) {
                    while (!cursor.isAfterLast) {
                        val nextParent = cursor.getLongOrNull(KEY_PARENTID)?.let(idMapper)
                        val nextId = idMapper(cursor.getLong(KEY_ROWID))
                        val nextLabel = cursor.getString(KEY_LABEL)
                        val nextPath = cursor.getString(KEY_PATH)
                        val nextColor = if (withColors) cursor.getIntOrNull(KEY_COLOR) else null
                        val nextIcon = cursor.getStringOrNull(KEY_ICON)
                        val nextType = cursor.getIntOrNull(KEY_TYPE)?.toByte() ?: FLAG_NEUTRAL
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
                                        false,
                                        cursor,
                                        nextId,
                                        level + 1,
                                        idMapper
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

