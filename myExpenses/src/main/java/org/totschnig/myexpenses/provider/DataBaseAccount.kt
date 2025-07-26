package org.totschnig.myexpenses.provider

import android.net.Uri
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.model2.AccountInfoWithGrouping
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.TransactionProvider.EXTENDED_URI
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_SEARCH
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_SHORTEN_COMMENT
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI

/**
 * groups databaseSpecific information
 */
abstract class DataBaseAccount : AccountInfoWithGrouping {
    abstract val id: Long
    abstract val sortBy: String
    abstract val sortDirection: SortDirection

    override val accountId: Long
        get() = id

    val isHomeAggregate get() = isHomeAggregate(id)

    val isAggregate get() = isAggregate(id)

    val sortOrder: String
        get() = "${sortBy.let { if (it == KEY_AMOUNT) "abs($it)" else it }} $sortDirection"

    fun uriForTransactionList(
        shortenComment: Boolean = false,
        extended: Boolean = true
    ): Uri = uriBuilderForTransactionList(shortenComment, extended).build()

    fun uriBuilderForTransactionList(
        shortenComment: Boolean = false,
        extended: Boolean = true
    ) = uriBuilderForTransactionList(id, currency, shortenComment, extended)

    companion object {

        const val AGGREGATE_HOME_CURRENCY_CODE = "___"

        const val HOME_AGGREGATE_ID = Int.MIN_VALUE.toLong()

        const val GROUPING_AGGREGATE = "AGGREGATE_GROUPING____"
        const val SORT_DIRECTION_AGGREGATE = "AGGREGATE_SORT_DIRECTION____"
        const val SORT_BY_AGGREGATE = "AGGREGATE_SORT_BY____"

        fun isHomeAggregate(id: Long) = id == HOME_AGGREGATE_ID

        fun isAggregate(id: Long) = id < 0

        fun uriBuilderForTransactionList(
            id: Long,
            currency: String?,
            shortenComment: Boolean = false,
            extended: Boolean = true
        ): Uri.Builder {
            val uriBuilder =
                uriBuilderForTransactionList(shortenComment, extended)
            return when {
                !isAggregate(id) -> uriBuilder.apply {
                    appendQueryParameter(KEY_ACCOUNTID, id.toString())
                }

                isHomeAggregate(id) -> uriBuilder

                else -> uriBuilder.apply {
                    appendQueryParameter(KEY_CURRENCY, currency)
                }
            }
        }

        fun uriForTransactionList(shortenComment: Boolean, extended: Boolean = true): Uri =
            uriBuilderForTransactionList(shortenComment, extended).build()

        fun uriBuilderForTransactionList(
            shortenComment: Boolean,
            extended: Boolean = true
        ): Uri.Builder =
            (if (extended) EXTENDED_URI else TRANSACTIONS_URI)
                .buildUpon().apply {
                    if (shortenComment) {
                        appendQueryParameter(
                            QUERY_PARAMETER_SHORTEN_COMMENT,
                            "1"
                        )
                    }
                    appendQueryParameter(
                        QUERY_PARAMETER_SEARCH,
                        "1"
                    )
                }
    }
}