package org.totschnig.myexpenses.model2

import android.net.Uri
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.filter.Criterion

interface IAccount {
    val accountId: Long
    val currency: String
    /**
     * if accountGrouping is not null,
     * the account is an aggregate account for
     * a given currency,
     * a given type,
     * a given flag.
     * If accountGrouping is AccountGrouping.NONE, the account is the Grand Total account
     */
    val accountGrouping: AccountGrouping?
        get() = null


    val queryParameter: Pair<String, String>?
        get() = when(accountGrouping) {
            AccountGrouping.TYPE -> TODO()
            AccountGrouping.CURRENCY -> KEY_CURRENCY to currency
            AccountGrouping.FLAG -> TODO()
            AccountGrouping.NONE -> TODO()
            null -> KEY_ACCOUNTID to accountId.toString()
        }
}

interface AccountInfoWithGrouping: IAccount {
    val grouping: Grouping

    fun groupingQuery(filter: Criterion?): Triple<Uri.Builder, String?, Array<String>?> {
        val selection = filter?.getSelectionForParts()
        val args = filter?.getSelectionArgs(true)
        return Triple(
            BaseTransactionProvider.groupingUriBuilder(grouping).apply {
                queryParameter?.let {
                    appendQueryParameter(it.first, it.second)
                }
            },
            selection,
            args
        )
    }
}