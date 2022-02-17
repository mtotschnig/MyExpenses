package org.totschnig.myexpenses.provider

import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

open class CheckSealedHandler(cr: ContentResolver) : AsyncQueryHandler(cr) {
    fun interface ResultListener {
        /**
         * @param result Pair of
         * A: true if none of the passed in itemIds is linked to sealed account
         * B: true if none of the passed in itemIds is linked to sealed debt
         */
        fun onResult(result: Result<Pair<Boolean, Boolean>>)
    }

    open fun check(itemIds: LongArray, listener: ResultListener) {
        startQuery(
            TOKEN,
            listener,
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(
                "MAX(" + checkForSealedAccount(
                    VIEW_COMMITTED,
                    TABLE_TRANSACTIONS
                ) + ")",
                "MAX(${checkForSealedDebt(VIEW_COMMITTED)})"
            ),
            "${DatabaseConstants.KEY_ROWID} IN (${itemIds.joinToString()})",
            null,
            null
        )
    }

    override fun onQueryComplete(token: Int, cookie: Any, cursor: Cursor?) {
        if (token != TOKEN) return
        cursor?.apply {
            moveToFirst()
            val sealedAccount = getInt(0)
            val sealedDebt = getInt(1)
            close()
            (cookie as ResultListener).onResult(
                Result.success((sealedAccount == 0) to (sealedDebt == 0))
            )
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