package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.database.Cursor
import androidx.core.database.getLongOrNull
import app.cash.copper.Query
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider

class CategoryViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    fun loadCategoryTree(): Flow<Category> = contentResolver.observeQuery(
        org.totschnig.myexpenses.model.Category.CONTENT_URI.buildUpon()
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_HIERARCHICAL, "1")
            .build(),
        null,
        null,
        null,
        null
    ).mapToTree()
    companion object {
        fun ingest(cursor: Cursor, parentId: Long?): List<Category> = buildList {
            check(!cursor.isBeforeFirst)
            while (!cursor.isAfterLast) {
                val nextParent = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(KEY_PARENTID))
                val nextLabel = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL))
                val nextId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID))
                if (nextParent == parentId) {
                    cursor.moveToNext()
                    add(Category(nextLabel, ingest(cursor, nextId)))
                } else return@buildList
            }
        }

        private fun Flow<Query>.mapToTree(
            dispatcher: CoroutineDispatcher = Dispatchers.IO
        ): Flow<Category> = transform { query ->
            val value = withContext(dispatcher) {
                query.run()?.use { cursor ->
                    cursor.moveToFirst()
                    Category("ROOT", ingest(cursor, null))
                }
            }
            if (value != null) {
                emit(value)
            }
        }
    }
}

