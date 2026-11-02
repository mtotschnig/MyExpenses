package org.totschnig.myexpenses.next

import android.content.Context
import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.PREDEFINED_NAME_BANK
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CCARD
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.next.core.common.R

/**
 * Account types that are shown together in a single "Daily accounts" section of the overview,
 * when accounts are grouped by type.
 */
private val dailyAccountTypes = setOf(PREDEFINED_NAME_CASH, PREDEFINED_NAME_BANK, PREDEFINED_NAME_CCARD)

/**
 * Cash, bank and credit card accounts: where the money for daily life comes from and goes to
 */
val FullAccount.isDailyAccount: Boolean
    get() = isDailyAccountType(type.name)

/** Whether accounts of the type named [typeName] are daily accounts, see [isDailyAccount] */
fun isDailyAccountType(typeName: String) = typeName in dailyAccountTypes

/**
 * Groups [accounts] into titled sections for the overview, following the user's [grouping].
 * With grouping by type, cash, bank and credit card accounts are merged into one section
 * that comes first, all other types keep their own section.
 * Within a section, accounts are in the custom order the user arranged them in the overview,
 * independent of the sort order configured for the classic account list.
 */
fun groupAccountsForOverview(
    accounts: List<FullAccount>,
    grouping: AccountGrouping<*>,
    context: Context,
): List<Pair<String, List<FullAccount>>> {
    val sorted = accounts.sortedBy { it.sortKey }
    val (daily, others) = if (grouping == AccountGrouping.TYPE)
        sorted.partition { it.isDailyAccount }
    else emptyList<FullAccount>() to sorted

    @Suppress("UNCHECKED_CAST")
    val comparator = grouping.comparator as Comparator<in AccountGroupingKey>
    val grouped = others.groupBy { grouping.getGroupKey(it) }
    val sections = grouped.keys.sortedWith(comparator).map { it.title(context) to grouped.getValue(it) }

    return if (daily.isEmpty()) sections
    else listOf(context.getString(R.string.next_daily_accounts) to daily) + sections
}

/**
 * The accounts of the overview in their groups, see [groupAccountsForOverview]
 *
 * @param formatAmount formats a balance in minor units of its currency
 */
fun overviewSections(
    accounts: List<FullAccount>,
    grouping: AccountGrouping<*>,
    context: Context,
    resources: Resources,
    formatAmount: (Long, CurrencyUnit) -> String,
) = groupAccountsForOverview(accounts, grouping, context).map { (title, groupAccounts) ->
    OverviewSection(
        title = title,
        accounts = groupAccounts.map {
            OverviewAccount(
                id = it.id,
                label = it.label,
                balance = formatAmount(it.effectiveBalance, it.currencyUnit),
                isNegative = it.effectiveBalance < 0,
                color = Color(it.color(resources)),
                bankId = it.bankId
            )
        }
    )
}

