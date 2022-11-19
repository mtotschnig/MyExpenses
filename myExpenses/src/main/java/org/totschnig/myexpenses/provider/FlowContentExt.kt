package org.totschnig.myexpenses.provider

import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.Bundle
import app.cash.copper.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import timber.log.Timber

fun <T> Flow<Query>.mapToListWithExtra(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    mapper: (Cursor) -> T
): Flow<Pair<Bundle, List<T>>> = transform { query ->
    withContext(dispatcher) {
        query.run()?.use { cursor ->
            cursor.extras to buildList {
                while (cursor.moveToNext()) {
                    this.add(mapper(cursor))
                }
            }
        }
    }?.let { emit(it) }
}

fun <T> Flow<Query>.mapToListCatchingWithExtra(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    mapper: (Cursor) -> T
): Flow<Result<Pair<Bundle, List<T>>>> = transform { query ->
    withContext(dispatcher) {
        try {
            query.run()?.use { cursor ->
                cursor.extras to buildList {
                    while (cursor.moveToNext()) {
                        this.add(mapper(cursor))
                    }
                }
            }?.let { Result.success(it) }
        } catch (e: SQLiteException) {
            Timber.e(e)
            Result.failure(e)
        }
    }?.let { emit(it) }
}

fun Flow<Query>.mapToStringMap(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Flow<Map<String, String?>> = transform { query ->
    val map = withContext(dispatcher) {
        query.run()?.use { cursor ->
            val items = mutableMapOf<String, String?>()
            while (cursor.moveToNext()) {
                cursor.getString(0)?.let {
                    items.put(it, cursor.getString(1))
                }
            }
            items
        }
    }
    if (map != null) {
        emit(map)
    }
}