package org.totschnig.myexpenses.provider

import android.net.Uri
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.sort.SortDirection
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

    @Deprecated("Used only on legacy Main Screen")
    open val isHomeAggregate get() = isHomeAggregate(id)

    @Deprecated("Used only on legacy Main Screen")
    open val isAggregate get() = isAggregate(id)

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

        fun isAggregate(id: Long) = id <= 0

        fun uriBuilderForTransactionList(
            accountId: Long,
            currency: String?,
            type: Long? = null,
            flag: Long? = null,
            accountGrouping: AccountGrouping<*>? = null,
            shortenComment: Boolean = false,
            extended: Boolean = true,
        ): Uri.Builder = uriBuilderForTransactionList(shortenComment, extended)
            .appendQueryParameter(accountId, currency, type, flag, accountGrouping)

        fun queryParameter(
            accountId: Long,
            currency: String?,
            type: Long? = null,
            flag: Long? = null,
            accountGrouping: AccountGrouping<*>? = null,
        ) : Pair<String, String>? = when (accountGrouping ?: when {
            isHomeAggregate(accountId) -> AccountGrouping.NONE
            isAggregate(accountId) -> AccountGrouping.CURRENCY
            else -> null
        }) {
            null -> KEY_ACCOUNTID to accountId.toString()

            AccountGrouping.CURRENCY -> KEY_CURRENCY to currency!!

            AccountGrouping.TYPE -> KEY_ACCOUNT_TYPE to type!!.toString()
            AccountGrouping.FLAG -> KEY_FLAG to flag!!.toString()
            else -> null
        }

        fun Uri.Builder.appendQueryParameter(
            accountId: Long,
            currency: String?,
            type: Long? = null,
            flag: Long? = null,
            accountGrouping: AccountGrouping<*>? = null,
        ): Uri.Builder {
            queryParameter(accountId, currency, type, flag, accountGrouping)?.let {
                appendQueryParameter(it.first, it.second)
            }
            return this
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