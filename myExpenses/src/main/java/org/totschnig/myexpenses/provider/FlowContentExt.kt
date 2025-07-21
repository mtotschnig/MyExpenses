package org.totschnig.myexpenses.provider

import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.Bundle
import androidx.annotation.CheckResult
import app.cash.copper.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

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
            CrashHandler.report(e)
            Result.failure(e)
        }
    }?.let { emit(it) }
}

@CheckResult
fun <T : Any, E: Any> Flow<Pair<E, Query>>.mapToOne(
    default: T? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    mapper: (E, Cursor) -> T
): Flow<T> = transform { (extra, query) ->
    val item = withContext(dispatcher) {
        query.run()?.use { cursor ->
            if (cursor.moveToNext()) {
                val item = mapper(extra, cursor)
                check(!cursor.moveToNext()) { "Cursor returned more than 1 row" }
                item
            } else {
                default
            }
        }
    }
    if (item != null) {
        emit(item)
    }
}
