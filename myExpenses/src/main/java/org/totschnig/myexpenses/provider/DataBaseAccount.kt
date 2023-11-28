package org.totschnig.myexpenses.provider

import android.net.Uri
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model2.IAccount
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.groupingUriBuilder
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
abstract class DataBaseAccount : IAccount {
    abstract val id: Long
    abstract override val currency: String
    abstract val grouping: Grouping

    override val accountId: Long
        get() = id

    val isHomeAggregate get() = isHomeAggregate(id)

    val isAggregate get() = isAggregate(id)

    fun uriForTransactionList(
        withType: Boolean = false,
        shortenComment: Boolean = false,
        extended: Boolean = true
    ): Uri = uriBuilderForTransactionList(withType, shortenComment, extended).build()

    fun uriBuilderForTransactionList(
        withType: Boolean = false,
        shortenComment: Boolean = false,
        extended: Boolean = true
    ) =
        uriBuilderForTransactionList(id, currency, withType, shortenComment, extended)

    val extendedProjectionForTransactionList: Array<String>
        get() = when {
            !isAggregate -> getProjectionExtended()
            isHomeAggregate -> getProjectionExtendedHome()
            else -> getProjectionExtendedAggregate()
        }

    val groupingUri: Uri
        get() = groupingUriBuilder(grouping).apply {
            queryParameter?.let {
                appendQueryParameter(it.first, it.second)
            }
        }.build()

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
            withType: Boolean = false,
            shortenComment: Boolean = false,
            extended: Boolean = true
        ): Uri.Builder = when {
            !isAggregate(id) -> uriBuilderForTransactionList(shortenComment, extended).apply {
                appendQueryParameter(KEY_ACCOUNTID, id.toString())
            }

            isHomeAggregate(id) -> uriForTransactionListHome(withType, shortenComment, extended)
            else -> uriForTransactionListAggregate(withType, shortenComment, extended).apply {
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

        private fun uriForTransactionListHome(
            withType: Boolean,
            shortenComment: Boolean,
            extended: Boolean
        ): Uri.Builder =
            uriWithMergeTransfers(withType, true, shortenComment, extended)

        private fun uriForTransactionListAggregate(
            withType: Boolean,
            shortenComment: Boolean,
            extended: Boolean
        ): Uri.Builder =
            uriWithMergeTransfers(withType, false, shortenComment, extended)

        private fun uriWithMergeTransfers(
            withType: Boolean,
            forHome: Boolean,
            shortenComment: Boolean,
            extended: Boolean
        ) =
            uriBuilderForTransactionList(shortenComment, extended).apply {
                if (!withType)
                    appendQueryParameter(
                        TransactionProvider.QUERY_PARAMETER_MERGE_TRANSFERS,
                        if (forHome) "2" else "1"
                    )
            }
    }
}