package org.totschnig.myexpenses.provider

import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.database.Cursor
import android.widget.Toast
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

class CheckSealedHandler(cr: ContentResolver) : AsyncQueryHandler(cr) {
    interface ResultListener {
        /**
         * @param result true if none of the passed in itemIds are sealed
         */
        fun onResult(result: Boolean)
    }

    private val TOKEN = 1
    fun check(itemIds: LongArray, listener: ResultListener?) {
        startQuery(TOKEN, listener, TransactionProvider.TRANSACTIONS_URI, arrayOf("MAX(" + DatabaseConstants.CHECK_SEALED(DatabaseConstants.VIEW_COMMITTED, DatabaseConstants.TABLE_TRANSACTIONS) + ")"),
                DatabaseConstants.KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(itemIds.size), itemIds.map(Long::toString).toTypedArray(), null)
    }

    override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
        if (token != TOKEN) return
        cursor?.apply {
            moveToFirst()
            val result = getInt(0)
            close()
            (cookie as ResultListener).onResult(result == 0)
        } ?: kotlin.run {
            val errorMessage = "Error while cheching status of transaction"
            CrashHandler.report(errorMessage)
            Toast.makeText(MyApplication.getInstance(), errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}