package org.totschnig.myexpenses.model2

import android.net.Uri
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.Criterion

interface IAccount {
    val accountId: Long
    val currency: String

    val queryParameter: Pair<String, String>?
        get() = if (accountId != DataBaseAccount.HOME_AGGREGATE_ID) {
            if (accountId < 0) {
                DatabaseConstants.KEY_CURRENCY to currency
            } else {
                DatabaseConstants.KEY_ACCOUNTID to accountId.toString()
            }
        } else null
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