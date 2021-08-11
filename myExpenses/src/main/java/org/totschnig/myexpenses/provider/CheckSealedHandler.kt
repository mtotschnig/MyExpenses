package org.totschnig.myexpenses.provider

import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.database.Cursor
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

class CheckSealedHandler(cr: ContentResolver) : AsyncQueryHandler(cr) {
    interface ResultListener {
        /**
         * @param result true if none of the passed in itemIds are sealed
         */
        fun onResult(result: Result<Boolean>)
    }

    fun check(itemIds: LongArray, listener: ResultListener) {
        startQuery(TOKEN, listener, TransactionProvider.TRANSACTIONS_URI, arrayOf("MAX(" + DatabaseConstants.CHECK_SEALED(DatabaseConstants.VIEW_COMMITTED, DatabaseConstants.TABLE_TRANSACTIONS) + ")"),
            "${DatabaseConstants.KEY_ROWID} IN (${itemIds.joinToString()})", null, null)
    }

    override fun onQueryComplete(token: Int, cookie: Any, cursor: Cursor?) {
        if (token != TOKEN) return
        cursor?.apply {
            moveToFirst()
            val result = getInt(0)
            close()
            (cookie as ResultListener).onResult(Result.success(result == 0))
        } ?: kotlin.run {
            val error = Exception("Error while checking status of transaction")
            CrashHandler.report(error)
            (cookie as ResultListener).onResult(Result.failure(error))
        }
    }

    companion object {
        private const val TOKEN = 1
    }
}