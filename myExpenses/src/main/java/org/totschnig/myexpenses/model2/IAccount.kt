package org.totschnig.myexpenses.model2

import android.net.Uri
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.appendQueryParameter
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.isAggregate
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.isHomeAggregate
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.viewmodel.data.Transaction2

interface IAccount {
    val accountId: Long
    val currency: String


    val queryParameter: Pair<String, String>?
        get() = if (accountId != DataBaseAccount.HOME_AGGREGATE_ID) {
            if (accountId < 0) {
                KEY_CURRENCY to currency
            } else {
                KEY_ACCOUNTID to accountId.toString()
            }
        } else null
}

interface AccountInfoWithGrouping : IAccount {
    val grouping: Grouping
    val typeId: Long?
    val flagId: Long?
    val accountGrouping: AccountGrouping<*>?

    val isHomeAggregate get() = isAggregate &&  (isHomeAggregate(accountId) || accountGrouping == AccountGrouping.NONE)

    val isAggregate get() = isAggregate(accountId)

    fun groupingQuery(filter: Criterion?): Triple<Uri.Builder, String?, Array<String>?> {
        if (accountId == 0L) return groupingQueryV2(filter)
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

    override val queryParameter: Pair<String, String>?
        get() = if (accountId != 0L) super.queryParameter else
            DataBaseAccount.queryParameter(accountId, currency, typeId, flagId, accountGrouping)

    fun groupingQueryV2(filter: Criterion?): Triple<Uri.Builder, String?, Array<String>?> {
        val selection = filter?.getSelectionForParts()
        val args = filter?.getSelectionArgs(true)
        return Triple(
            BaseTransactionProvider.groupingUriBuilder(grouping).apply {
                appendQueryParameter(accountId, currency, typeId, flagId, accountGrouping)
            },
            selection,
            args
        )
    }

    //Pair of Uri / projection
    fun loadingInfo(prefHandler: PrefHandler): Pair<Uri, Array<String>> =
        uriBuilderForTransactionList(extended = true).build() to Transaction2.projection(
            isAggregate,
            grouping,
            prefHandler
        )

    fun uriBuilderForTransactionList(
        extended: Boolean,
    ) = DataBaseAccount.uriBuilderForTransactionList(
        accountId = accountId,
        currency = currency,
        type = typeId,
        flag = flagId,
        accountGrouping = accountGrouping,
        shortenComment = true,
        extended = extended
    )
}

interface AccountWithGroupingKey {
    val currencyUnit: CurrencyUnit
    val flag: AccountFlag
    val type: AccountType
}