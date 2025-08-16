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

fun <T> Flow<Query>.mapToListCatching(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    mapper: (Cursor) -> T
): Flow<Result<List<T>>> = transform { query ->
    withContext(dispatcher) {
        try {
            query.run()?.use { cursor ->
                buildList {
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
