package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.database.Cursor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.Query
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.util.Utils
import timber.log.Timber

class CategoryViewModel(application: Application, private val savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {

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
            org.totschnig.myexpenses.model.Category.CONTENT_URI.buildUpon()
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

    companion object {
        fun ingest(context: Context, cursor: Cursor, parentId: Long?, level: Int): List<Category> = buildList {
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

