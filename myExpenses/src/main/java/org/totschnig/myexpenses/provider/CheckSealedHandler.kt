package org.totschnig.myexpenses.provider

import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_SEALED_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_SEALED_ACCOUNT_WITH_TRANSFER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_SEALED_DEBT
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

    open fun check(itemIds: List<Long>, withTransfer: Boolean, listener: ResultListener) {
        startQuery(
            TOKEN,
            listener,
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(
                if (withTransfer) KEY_HAS_SEALED_ACCOUNT_WITH_TRANSFER else KEY_HAS_SEALED_ACCOUNT,
                KEY_HAS_SEALED_DEBT
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