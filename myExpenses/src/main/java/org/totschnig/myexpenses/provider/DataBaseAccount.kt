package org.totschnig.myexpenses.provider

import android.net.Uri
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.model2.AccountInfoWithGrouping
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

    val isHomeAggregate get() = false

    val isAggregate get() = false

    val sortOrder: String
        get() = "${sortBy.let { if (it == KEY_AMOUNT) "abs($it)" else it }} $sortDirection"

    fun uriForTransactionList(
        shortenComment: Boolean = false,
        extended: Boolean = true,
    ): Uri = uriBuilderForTransactionList(id, currency, null, null,  null, shortenComment, extended).build()

    companion object {

        const val AGGREGATE_HOME_CURRENCY_CODE = "___"

        const val HOME_AGGREGATE_ID = Int.MIN_VALUE.toLong()

        const val GROUPING_AGGREGATE = "AGGREGATE_GROUPING____"
        const val SORT_DIRECTION_AGGREGATE = "AGGREGATE_SORT_DIRECTION____"
        const val SORT_BY_AGGREGATE = "AGGREGATE_SORT_BY____"

        fun isHomeAggregate(id: Long) = id == HOME_AGGREGATE_ID

        fun isAggregate(id: Long) = id < 0

        fun uriBuilderForTransactionList(
            accountId: Long,
            currency: String?,
            type: Long? = null,
            flag: Long? = null,
            accountGrouping: AccountGrouping<*>? = when {
                isHomeAggregate(accountId) -> AccountGrouping.NONE
                isAggregate(accountId) -> AccountGrouping.CURRENCY
                else -> null
            },
            shortenComment: Boolean = false,
            extended: Boolean = true,
        ): Uri.Builder {
            return uriBuilderForTransactionList(shortenComment, extended).apply {
                when (accountGrouping) {
                    null -> appendQueryParameter(KEY_ACCOUNTID, accountId.toString())

                    AccountGrouping.CURRENCY -> appendQueryParameter(KEY_CURRENCY, currency!!)

                    AccountGrouping.TYPE -> appendQueryParameter(KEY_ACCOUNT_TYPE, type!!.toString())
                    AccountGrouping.FLAG -> appendQueryParameter(KEY_FLAG, flag!!.toString())
                    else -> {}
                }
            }
        }

        fun uriBuilderForTransactionList(
            shortenComment: Boolean,
            extended: Boolean = true,
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