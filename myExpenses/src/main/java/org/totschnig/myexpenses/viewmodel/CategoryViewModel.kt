package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.database.Cursor
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import app.cash.copper.Query
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.getCategoryPath
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.KEY_BUDGET
import org.totschnig.myexpenses.provider.KEY_BUDGET_ROLLOVER_NEXT
import org.totschnig.myexpenses.provider.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_COUNT
import org.totschnig.myexpenses.provider.KEY_ICON
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_LEVEL
import org.totschnig.myexpenses.provider.KEY_MATCHES_FILTER
import org.totschnig.myexpenses.provider.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_PATH
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SUM
import org.totschnig.myexpenses.provider.KEY_TYPE
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getIntIfExistsOr0
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongIfExistsOr0
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.viewmodel.data.BudgetAllocation
import org.totschnig.myexpenses.viewmodel.data.Category

sealed class LoadingState {
    data object Loading : LoadingState()

    sealed class Result : LoadingState()
    class Data(val data: Category) : Result()

    class Empty(val hasUnfiltered: Boolean) : Result()
}

open class CategoryViewModel(
    application: Application,
    protected val savedStateHandle: SavedStateHandle,
) : ContentResolvingAndroidViewModel(application) {

    protected val _syncResult: MutableStateFlow<String?> = MutableStateFlow(null)
    val syncResult: Flow<String> = _syncResult.asStateFlow().filterNotNull()

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
        val effectiveOrder = sortOrder ?: KEY_LABEL
        return contentResolver.observeQuery(
            categoryUri(queryParameter),
            projection,
            selection,
            selectionArgs + (additionalSelectionArgs ?: emptyArray()),
            effectiveOrder,
            true
        ).mapToResult(keepCriterion, withColors, idMapper)
            .map {
                if (it is LoadingState.Data && effectiveOrder?.startsWith(KEY_LABEL) == true) {
                    LoadingState.Data(it.data.sortChildrenByLabelNaturalRecursive())
                } else it
            }
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

    open fun messageShown() {
        _syncResult.update { null }
    }

    fun defaultTransferCategory() = liveData(context = coroutineContext()) {
        emit(
            prefHandler.defaultTransferCategory?.let {
                repository.getCategoryPath(it) ?: "Category $it not found"
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

