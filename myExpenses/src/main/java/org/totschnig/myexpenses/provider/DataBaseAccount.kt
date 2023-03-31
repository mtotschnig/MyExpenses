package org.totschnig.myexpenses.provider

import android.net.Uri
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants.*

/**
 * groups databaseSpecific information
 */
abstract class DataBaseAccount {
    abstract val id: Long
    abstract val currency: String
    abstract val grouping: Grouping

    val isHomeAggregate get() = isHomeAggregate(id)

    val isAggregate get() = isAggregate(id)


    val selectionInfo: Pair<String, Array<String>?>
        get() = when {
            !isAggregate -> selectionInfo(id)
            isHomeAggregate -> selectionInfoHome
            else -> selectionInfoAggregate(currency)
        }

    fun extendedUriForTransactionList(withType: Boolean = false, shortenComment: Boolean = false): Uri = when {
            !isAggregate -> Companion.extendedUriForTransactionList(shortenComment)
            isHomeAggregate -> extendedUriForTransactionListHome(withType, shortenComment)
            else -> extendedUriForTransactionListAggregate(withType, shortenComment)
        }

    fun extendedProjectionForTransactionList(): Array<String> = when {
        !isAggregate -> getProjectionExtended()
        isHomeAggregate -> getProjectionExtendedHome()
        else -> getProjectionExtendedAggregate()
    }

    val groupingUri: Uri
        get() {
            val baseUri = getGroupingBaseUriBuilder(grouping)
            return when {
                !isAggregate -> baseUri.appendQueryParameter(KEY_ACCOUNTID, id.toString())
                isHomeAggregate -> baseUri
                else -> baseUri.appendQueryParameter(KEY_CURRENCY, currency)
            }.build()
        }

    companion object {

        const val HOME_AGGREGATE_ID = Int.MIN_VALUE.toLong()

        fun isHomeAggregate(id: Long) = id == HOME_AGGREGATE_ID

        fun isAggregate(id: Long) = id < 0

        fun selectionInfo(id: Long) =
            "$KEY_ACCOUNTID = ?" to arrayOf(id.toString())

        fun selectionInfoAggregate(currency: String) =
            "$KEY_ACCOUNTID IN (SELECT $KEY_ROWID from $TABLE_ACCOUNTS WHERE $KEY_CURRENCY = ? AND $KEY_EXCLUDE_FROM_TOTALS = 0)" to arrayOf(
                currency
            )

        val selectionInfoHome =
            "$KEY_ACCOUNTID IN (SELECT $KEY_ROWID from $TABLE_ACCOUNTS WHERE $KEY_EXCLUDE_FROM_TOTALS = 0)" to null

        fun extendedUriForTransactionList(shortenComment: Boolean): Uri {
            return if (shortenComment) Transaction.EXTENDED_URI
                .buildUpon()
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_SHORTEN_COMMENT, "1")
                .build() else Transaction.EXTENDED_URI
        }

        fun extendedUriForTransactionListHome(withType: Boolean, shortenComment: Boolean): Uri =
            extendedUriWithMergeTransfers(withType, true, shortenComment)

        fun extendedUriForTransactionListAggregate(withType: Boolean, shortenComment: Boolean): Uri =
            extendedUriWithMergeTransfers(withType, false, shortenComment)

        private fun extendedUriWithMergeTransfers(withType: Boolean, forHome: Boolean, shortenComment: Boolean) =
            extendedUriForTransactionList(shortenComment).let {
                if (withType) it else
                    it.buildUpon().appendQueryParameter(
                        TransactionProvider.QUERY_PARAMETER_MERGE_TRANSFERS,
                        if (forHome) "2" else "1"
                    )
                        .build()
            }

        protected fun getGroupingBaseUriBuilder(grouping: Grouping): Uri.Builder {
            return Transaction.CONTENT_URI.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_GROUPS).appendPath(grouping.name)
        }
    }
}