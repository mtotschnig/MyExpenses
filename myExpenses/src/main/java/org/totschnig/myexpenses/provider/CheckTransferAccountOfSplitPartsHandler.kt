package org.totschnig.myexpenses.provider

import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.database.Cursor
import android.widget.Toast
import org.jetbrains.annotations.Nullable
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

class CheckTransferAccountOfSplitPartsHandler(cr: ContentResolver) : AsyncQueryHandler(cr) {
    interface ResultListener {
        /**
         * @param result list of all transfer accounts of parts of the passed in split transactions
         */
        fun onResult(result: List<Long>)
    }

    private val TOKEN = 1
    fun check(itemIds: MutableList<Long>, listener: @Nullable ResultListener) {
        if (itemIds.size == 0) {
            listener.onResult(emptyList())
        } else {
            startQuery(TOKEN, listener, TransactionProvider.TRANSACTIONS_URI, arrayOf("distinct $KEY_TRANSFER_ACCOUNT"),
                    KEY_TRANSFER_ACCOUNT + " is not null AND " +
                            DatabaseConstants.KEY_PARENTID + " " + WhereFilter.Operation.IN.getOp(itemIds.size), itemIds.map(Long::toString).toTypedArray(), null)
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
            (cookie as ResultListener).onResult(ids)
        } ?: kotlin.run {
            val errorMessage = "Error while cheching transfer account of split parts"
            CrashHandler.report(errorMessage)
            Toast.makeText(MyApplication.getInstance(), errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}