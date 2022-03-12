package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.database.Cursor
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import app.cash.copper.Query
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.util.Utils

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
            sortOrder
        ).mapToTree(filter.isNotBlank())
    }

    private fun Flow<Query>.mapToTree(
        isFiltered: Boolean,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Flow<Category> = transform { query ->
        val value = withContext(dispatcher) {
            query.run()?.use { cursor ->
                cursor.moveToFirst()
                Category("ROOT", ingest(getApplication(), cursor, null), true, null as Int?, null).let {
                    if (isFiltered) it.pruneNonMatching() else it
                }
            }
        }
        if (value != null) {
            emit(value)
        }
    }

    companion object {
        fun ingest(context: Context, cursor: Cursor, parentId: Long?): List<Category> = buildList {
            if (!cursor.isBeforeFirst)
                while (!cursor.isAfterLast) {
                    val nextParent =
                        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(KEY_PARENTID))
                    val nextId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID))
                    val nextLabel = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL))
                    val nextColor = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(KEY_COLOR))
                    val nextIcon = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(KEY_ICON))
                    val nextIsMatching = cursor.getInt(cursor.getColumnIndexOrThrow("matches")) == 1
                    if (nextParent == parentId) {
                        cursor.moveToNext()
                        add(
                            Category(
                                nextLabel,
                                ingest(context, cursor, nextId),
                                nextIsMatching,
                                nextColor,
                                nextIcon?.let {
                                    context.resources.getIdentifier(
                                        it,
                                        "drawable",
                                        context.packageName
                                    )
                                }
                            )
                        )
                    } else return@buildList
                }
        }
    }
}

