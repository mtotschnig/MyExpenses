package org.totschnig.myexpenses.provider

import android.net.Uri
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.model2.AccountInfoWithGrouping
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY

/**
 * groups databaseSpecific information
 */
abstract class DataBaseAccount : AccountInfoWithGrouping {
    abstract val id: Long
    abstract override val currency: String
    abstract override val grouping: Grouping
    abstract val sortDirection: SortDirection

    override val accountId: Long
        get() = id

    val isHomeAggregate get() = isHomeAggregate(id)

    val isAggregate get() = isAggregate(id)

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
            currency: String,
            shortenComment: Boolean = false,
            extended: Boolean = true
        ): Uri.Builder = when {
            !isAggregate(id) -> uriBuilderForTransactionList(shortenComment, extended).apply {
                appendQueryParameter(KEY_ACCOUNTID, id.toString())
            }

            isHomeAggregate(id) -> uriBuilderForTransactionList(
                shortenComment,
                extended
            )

            else -> uriBuilderForTransactionList(shortenComment, extended).apply {
                appendQueryParameter(KEY_CURRENCY, currency)
            }
        }

        fun uriForTransactionList(shortenComment: Boolean, extended: Boolean = true): Uri =
            uriBuilderForTransactionList(shortenComment, extended).build()

        fun uriBuilderForTransactionList(
            shortenComment: Boolean,
            extended: Boolean = true
        ): Uri.Builder =
            (if (extended) TransactionProvider.EXTENDED_URI else TransactionProvider.TRANSACTIONS_URI)
                .buildUpon().apply {
                    if (shortenComment) {
                        appendQueryParameter(
                            TransactionProvider.QUERY_PARAMETER_SHORTEN_COMMENT,
                            "1"
                        )
                    }
                }
    }
}