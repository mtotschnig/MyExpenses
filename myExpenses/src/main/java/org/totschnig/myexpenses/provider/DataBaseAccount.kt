package org.totschnig.myexpenses.provider

import android.net.Uri
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.getProjectionExtended
import org.totschnig.myexpenses.provider.DatabaseConstants.getProjectionExtendedAggregate
import org.totschnig.myexpenses.provider.DatabaseConstants.getProjectionExtendedHome

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

    fun extendedUriForTransactionList(withType: Boolean = false, shortenComment: Boolean = false) =
        uriForTransactionList(id, withType, shortenComment)

    val extendedProjectionForTransactionList: Array<String>
        get() = when {
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

        const val AGGREGATE_HOME_CURRENCY_CODE = "___"

        const val HOME_AGGREGATE_ID = Int.MIN_VALUE.toLong()

        const val GROUPING_AGGREGATE = "AGGREGATE_GROUPING____"
        const val SORT_DIRECTION_AGGREGATE = "AGGREGATE_SORT_DIRECTION____"
        const val SORT_BY_AGGREGATE = "AGGREGATE_SORT_BY____"

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

        fun uriForTransactionList(id: Long, withType: Boolean = false, shortenComment: Boolean = false, extended: Boolean = true): Uri = when {
            !isAggregate(id) -> uriForTransactionList(shortenComment, extended)
            isHomeAggregate(id) -> uriForTransactionListHome(withType, shortenComment, extended)
            else -> uriForTransactionListAggregate(withType, shortenComment, extended)
        }

        fun uriForTransactionList(shortenComment: Boolean, extended: Boolean = true): Uri =
            uriBuilderForTransactionList(shortenComment, extended).build()

        fun uriBuilderForTransactionList(shortenComment: Boolean, extended: Boolean = true): Uri.Builder =
            Transaction.CONTENT_URI.buildUpon().apply {
                if (extended) {
                    appendQueryParameter(TransactionProvider.QUERY_PARAMETER_EXTENDED, "1")
                }
                if (shortenComment) {
                    appendQueryParameter(TransactionProvider.QUERY_PARAMETER_SHORTEN_COMMENT, "1")
                }
            }

        private fun uriForTransactionListHome(
            withType: Boolean,
            shortenComment: Boolean,
            extended: Boolean
        ): Uri =
            uriWithMergeTransfers(withType, true, shortenComment, extended)

        private fun uriForTransactionListAggregate(
            withType: Boolean,
            shortenComment: Boolean,
            extended: Boolean
        ): Uri =
            uriWithMergeTransfers(withType, false, shortenComment, extended)

        private fun uriWithMergeTransfers(
            withType: Boolean,
            forHome: Boolean,
            shortenComment: Boolean,
            extended: Boolean
        ) =
            uriForTransactionList(shortenComment, extended).let {
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