package org.totschnig.myexpenses.provider

import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

class CheckTransferAccountOfSplitPartsHandler(cr: ContentResolver) : AsyncQueryHandler(cr) {
    fun interface ResultListener {
        /**
         * @param result list of all transfer accounts of parts of the passed in split transactions
         */
        fun onResult(result: Result<List<Long>>)
    }

    fun check(itemIds: List<Long>, listener: ResultListener) {
        if (itemIds.isEmpty()) {
            listener.onResult(Result.success(emptyList()))
        } else {
            startQuery(TOKEN, listener, TransactionProvider.TRANSACTIONS_URI, arrayOf("distinct $KEY_TRANSFER_ACCOUNT"),
                "$KEY_TRANSFER_ACCOUNT is not null AND ${DatabaseConstants.KEY_PARENTID} IN (${itemIds.joinToString()})", null, null)
        }
    }

    override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
        if (token != TOKEN) return
        cursor?.apply {
            val ids = mutableListOf<Long>()
            if (moveToFirst()) {
                do {
                    ids.add(cursor.getLong(0))
                } while (moveToNext())
            }
            close()
            (cookie as ResultListener).onResult(Result.success(ids))
        } ?: kotlin.run {
            val error = Exception("Error while checking transfer account of split parts")
            CrashHandler.report(error)
            (cookie as ResultListener).onResult(Result.failure(error))
        }
    }

    companion object {
        private const val TOKEN = 1
    }
}