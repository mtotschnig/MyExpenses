package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.content.res.ResourcesCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.accounts.SIGMA
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.SortDirection
import org.totschnig.myexpenses.model2.AccountWithGroupingKey
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_BANK_ID
import org.totschnig.myexpenses.provider.KEY_CLEARED_TOTAL
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_CRITERION
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.provider.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_EXPENSES
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_INCOME
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_OPENING_BALANCE
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_TOTAL
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_TRANSFERS
import org.totschnig.myexpenses.provider.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.KEY_GROUPING
import org.totschnig.myexpenses.provider.KEY_HAS_CLEARED
import org.totschnig.myexpenses.provider.KEY_HAS_FUTURE
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_LAST_USED
import org.totschnig.myexpenses.provider.KEY_LATEST_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.KEY_LATEST_EXCHANGE_RATE_DATE
import org.totschnig.myexpenses.provider.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.KEY_RECONCILED_TOTAL
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SEALED
import org.totschnig.myexpenses.provider.KEY_SORT_BY
import org.totschnig.myexpenses.provider.KEY_SORT_DIRECTION
import org.totschnig.myexpenses.provider.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.KEY_SUM_INCOME
import org.totschnig.myexpenses.provider.KEY_SUM_TRANSFERS
import org.totschnig.myexpenses.provider.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.KEY_TOTAL
import org.totschnig.myexpenses.provider.KEY_UUID
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getDouble
import org.totschnig.myexpenses.provider.getDoubleOrNull
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLocalDate
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import java.time.LocalDate
import kotlin.math.roundToLong
import kotlin.math.sign

sealed class BaseAccount : DataBaseAccount() {
    abstract val currencyUnit: CurrencyUnit
    abstract val type: AccountType?
    abstract val flag: AccountFlag?
    abstract fun toPageAccount(context: Context): PageAccount
    abstract fun color(resources: Resources): Int
    abstract val total: Long?
    abstract val equivalentTotal: Long?
    abstract fun labelV2(context: Context): String
    fun aggregateColor(resources: Resources) = ResourcesCompat.getColor(resources, R.color.colorAggregate, null)
    override val flagId: Long?
        get() = flag?.id
    override val typeId: Long?
        get() = type?.id
}

@Stable
data class AggregateAccount(
    override val currencyUnit: CurrencyUnit,
    override val type: AccountType?,
    override val flag: AccountFlag?,
    val openingBalance: Long? = null,
    val currentBalance: Long? = null,
    val sumIncome: Long? = null,
    val sumExpense: Long? = null,
    val sumTransfer: Long? = null,
    val equivalentOpeningBalance: Long = openingBalance ?: 0,
    val equivalentCurrentBalance: Long = currentBalance ?: 0,
    val equivalentSumIncome: Long = sumIncome ?: 0,
    val equivalentSumExpense: Long = sumExpense ?: 0,
    val equivalentSumTransfer: Long = sumTransfer ?: 0,
    override val grouping: Grouping = Grouping.NONE,
    override val sortBy: String = KEY_DATE,
    override val sortDirection: SortDirection = SortDirection.DESC,
    val reconciledTotal: Long = 0L,
    val clearedTotal: Long = 0L,
    val hasCleared: Boolean = false,
    override val total: Long? = null,
    override val equivalentTotal: Long = total ?: 0,
    override val accountGrouping: AccountGrouping<*>
): BaseAccount() {
    override val id: Long = 0
    override val currency: String = currencyUnit.code

    override fun labelV2(context: Context): String = when(accountGrouping) {
        AccountGrouping.TYPE -> type!!.title(context) + " ($SIGMA)"
        AccountGrouping.CURRENCY -> currencyUnit.code + " ($SIGMA)"
        AccountGrouping.FLAG -> flag!!.title(context) + " ($SIGMA)"
        AccountGrouping.NONE -> context.getString(R.string.grand_total)
    }

    @Deprecated("Used only on legacy Main Screen")
    override val isHomeAggregate = accountGrouping == AccountGrouping.NONE

    @Deprecated("Used only on legacy Main Screen")
    override val isAggregate = true

    override fun color(resources: Resources) = aggregateColor(resources)

    override fun toPageAccount(context: Context) = PageAccount(
            id = id,
            type = type,
            flag = flag,
            sortBy = sortBy,
            sortDirection = sortDirection,
            grouping = grouping,
            currencyUnit = currencyUnit,
            sealed = false,
            openingBalance = openingBalance ?: equivalentOpeningBalance,
            currentBalance = currentBalance ?: equivalentCurrentBalance,
            color = color(context.resources),
            label = labelV2(context),
            accountGrouping = accountGrouping
        )
}

@Stable
data class FullAccount(
    override val id: Long,
    val label: String,
    val description: String? = null,
    override val currencyUnit: CurrencyUnit,
    val color: Int = -1,
    override val type: AccountType,
    override val flag: AccountFlag = AccountFlag.DEFAULT,
    val sealed: Boolean = false,
    val openingBalance: Long = 0,
    val currentBalance: Long = 0,
    val sumIncome: Long = 0,
    val sumExpense: Long = 0,
    val sumTransfer: Long = 0L,
    val equivalentOpeningBalance: Long = openingBalance,
    val equivalentCurrentBalance: Long = currentBalance,
    val equivalentSumIncome: Long = sumIncome,
    val equivalentSumExpense: Long = sumExpense,
    val equivalentSumTransfer: Long = sumTransfer,
    override val grouping: Grouping = Grouping.NONE,
    override val sortBy: String = KEY_DATE,
    override val sortDirection: SortDirection = SortDirection.DESC,
    val syncAccountName: String? = null,
    val reconciledTotal: Long = 0L,
    val clearedTotal: Long = 0L,
    val hasCleared: Boolean = false,
    val uuid: String? = null,
    val criterion: Long? = 0,
    override val total: Long? = null,
    override val equivalentTotal: Long? = null,
    val excludeFromTotals: Boolean = false,
    val lastUsed: Long = 0L,
    val bankId: Long? = null,
    val initialExchangeRate: Double? = null,
    val latestExchangeRate: Pair<LocalDate, Double>? = null,
    val dynamic: Boolean = false,
) : BaseAccount(), AccountWithGroupingKey {

    override val accountGrouping: AccountGrouping<*>? = null

    override val currency: String = currencyUnit.code

    override fun color(resources: Resources) = if (isAggregate) aggregateColor(resources) else color

    override fun labelV2(context: Context) = label

    val toPageAccount: PageAccount
        get() = PageAccount(
            id = id,
            type = type,
            flag = flag,
            sortBy = sortBy,
            sortDirection = sortDirection,
            grouping = grouping,
            currencyUnit = currencyUnit,
            sealed = sealed,
            openingBalance = openingBalance,
            currentBalance = currentBalance,
            color = color,
            label = label,
            accountGrouping = null
        )

    /**
     * pair of criterion sign (saving goal or credit limit) and progress in percent
     */
    val progress: Pair<Int, Float>?
        get() = criterion?.let {
            it.sign to
                    if (it > 0 == currentBalance > 0) {
                        (currentBalance * 100F / it)
                    } else 0f
        }

    val visible: Boolean
        get() = flag.isVisible

    override fun toPageAccount(context: Context) = toPageAccount

    companion object {

        fun Cursor.fromCursor(
            currencyContext: CurrencyContext,
        ): FullAccount {
            val sortBy = getString(KEY_SORT_BY)
                .takeIf { it == KEY_DATE || it == KEY_AMOUNT }
                ?: KEY_DATE
            return FullAccount(
                id = getLong(KEY_ROWID),
                label = getString(KEY_LABEL),
                description = getStringOrNull(KEY_DESCRIPTION),
                currencyUnit = currencyContext[getString(KEY_CURRENCY)],
                color = getInt(KEY_COLOR),
                type = AccountType.fromAccountCursor(this),
                flag = AccountFlag.fromAccountCursor(this),
                sealed = getInt(KEY_SEALED) == 1,
                openingBalance = getLong(KEY_OPENING_BALANCE),
                currentBalance = getLong(KEY_CURRENT_BALANCE),
                sumIncome = getLong(KEY_SUM_INCOME),
                sumExpense = getLong(KEY_SUM_EXPENSES),
                sumTransfer = getLong(KEY_SUM_TRANSFERS),
                grouping = if (sortBy == KEY_DATE) getEnum(
                    KEY_GROUPING,
                    Grouping.NONE
                ) else Grouping.NONE,
                sortBy = sortBy,
                sortDirection = getEnum(KEY_SORT_DIRECTION, SortDirection.DESC),
                syncAccountName = getStringOrNull(KEY_SYNC_ACCOUNT_NAME),
                reconciledTotal = getLong(KEY_RECONCILED_TOTAL),
                clearedTotal = getLong(KEY_CLEARED_TOTAL),
                hasCleared = getInt(KEY_HAS_CLEARED) > 0,
                uuid = getStringOrNull(KEY_UUID),
                criterion = getLong(KEY_CRITERION).takeIf { it != 0L },
                total = if (getBoolean(KEY_HAS_FUTURE)) getLong(KEY_TOTAL) else null,
                equivalentTotal = if (getBoolean(KEY_HAS_FUTURE)) getDouble(KEY_EQUIVALENT_TOTAL).roundToLong() else null,
                excludeFromTotals = getBoolean(KEY_EXCLUDE_FROM_TOTALS),
                lastUsed = getLong(KEY_LAST_USED),
                bankId = getLongOrNull(KEY_BANK_ID),
                equivalentOpeningBalance = getLong(KEY_EQUIVALENT_OPENING_BALANCE),
                equivalentCurrentBalance = getDouble(KEY_EQUIVALENT_CURRENT_BALANCE).roundToLong(),
                equivalentSumIncome = getLong(KEY_EQUIVALENT_INCOME),
                equivalentSumExpense = getLong(KEY_EQUIVALENT_EXPENSES),
                equivalentSumTransfer = getLong(KEY_EQUIVALENT_TRANSFERS),
                initialExchangeRate = getDoubleOrNull(KEY_EXCHANGE_RATE),
                latestExchangeRate = getDoubleOrNull(KEY_LATEST_EXCHANGE_RATE)?.let {
                    getLocalDate(KEY_LATEST_EXCHANGE_RATE_DATE) to it
                },
                dynamic = getBoolean(KEY_DYNAMIC),
            )
        }
    }
}

@Immutable
data class PageAccount(
    override val id: Long = 0,
    val type: AccountType? = AccountType.CASH,
    val flag: AccountFlag? = AccountFlag.DEFAULT,
    override val sortBy: String = KEY_DATE,
    override val sortDirection: SortDirection = SortDirection.DESC,
    override val grouping: Grouping = Grouping.NONE,
    val currencyUnit: CurrencyUnit,
    val sealed: Boolean = false,
    val openingBalance: Long = 0,
    val currentBalance: Long = 0,
    val label: String,
    //not null for aggregate accounts
    override val accountGrouping: AccountGrouping<*>? = null,
    val color: Int = -1
) : DataBaseAccount() {
    override val currency: String = currencyUnit.code

    override val flagId = flag?.id
    override val typeId = type?.id


    //Pair of Uri / projection
    fun loadingInfo(prefHandler: PrefHandler): Pair<Uri, Array<String>> =
        uriBuilderForTransactionList(extended = true).build() to Transaction2.projection(
            isAggregate,
            grouping,
            prefHandler
        )

    fun uriBuilderForTransactionList(
        extended: Boolean,
    ) = uriBuilderForTransactionList(
        accountId = id,
        currency = currency,
        type = type?.id,
        flag = flag?.id,
        accountGrouping = accountGrouping,
        shortenComment = true,
        extended = extended
    )
}