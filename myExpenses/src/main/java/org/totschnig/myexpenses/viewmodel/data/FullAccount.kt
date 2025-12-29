package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.content.res.ResourcesCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.SIGMA
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.SortDirection
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
import kotlin.math.absoluteValue
import kotlin.math.roundToLong
import kotlin.math.sign

abstract class BaseAccount : DataBaseAccount() {
    abstract val currencyUnit: CurrencyUnit
    abstract val label: String
    abstract val type: AccountType?
    abstract val flag: AccountFlag?
    abstract fun toPageAccount(context: Context): PageAccount

    open fun labelV2(context: Context) = label
}

@Stable
data class AggregateAccount(
    override val id: Long = 0,
    override val currencyUnit: CurrencyUnit,
    override val type: AccountType?,
    override val flag: AccountFlag?,
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
    val reconciledTotal: Long = 0L,
    val clearedTotal: Long = 0L,
    val hasCleared: Boolean = false,
    val total: Long? = null,
    val equivalentTotal: Long? = null,
    val accountGrouping: AccountGrouping
): BaseAccount() {
    override val currency: String = currencyUnit.code

    override val label: String
        get() = throw UnsupportedOperationException("Use labelV2")

    override fun labelV2(context: Context): String = when(accountGrouping) {
        AccountGrouping.TYPE -> type!!.localizedName(context) + " ($SIGMA)"
        AccountGrouping.CURRENCY -> currencyUnit.code + " ($SIGMA)"
        AccountGrouping.FLAG -> flag!!.localizedLabel(context) + " ($SIGMA)"
        AccountGrouping.NONE -> context.getString(R.string.grand_total)
    }

    override fun toPageAccount(context: Context) = PageAccount(
            id = id,
            type = type,
            flag = flag,
            sortBy = sortBy,
            sortDirection = sortDirection,
            grouping = grouping,
            currencyUnit = currencyUnit,
            sealed = false,
            openingBalance = openingBalance,
            currentBalance = currentBalance,
            color = ResourcesCompat.getColor(context.resources, R.color.colorAggregate, null),
            label = labelV2(context),
            accountGrouping = accountGrouping
        )
}

@Stable
data class FullAccount(
    override val id: Long,
    override val label: String,
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
    val total: Long? = null,
    val equivalentTotal: Long? = null,
    val excludeFromTotals: Boolean = false,
    val lastUsed: Long = 0L,
    val bankId: Long? = null,
    val initialExchangeRate: Double? = null,
    val latestExchangeRate: Pair<LocalDate, Double>? = null,
    val dynamic: Boolean = false,
) : BaseAccount() {

    override val currency: String = currencyUnit.code

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

    override fun toPageAccount(context: Context) = toPageAccount

    companion object {

        fun fromCursor(
            cursor: Cursor,
            currencyContext: CurrencyContext,
        ): FullAccount {
            val sortBy = cursor.getString(KEY_SORT_BY)
                .takeIf { it == KEY_DATE || it == KEY_AMOUNT }
                ?: KEY_DATE
            val id = cursor.getLong(KEY_ROWID)
            return FullAccount(
                id = id.absoluteValue,
                label = cursor.getString(KEY_LABEL),
                description = cursor.getStringOrNull(KEY_DESCRIPTION),
                currencyUnit = currencyContext[cursor.getString(KEY_CURRENCY)],
                color = cursor.getInt(KEY_COLOR),
                type = AccountType.fromAccountCursor(cursor),
                flag = AccountFlag.fromAccountCursor(cursor),
                sealed = cursor.getInt(KEY_SEALED) == 1,
                openingBalance = cursor.getLong(KEY_OPENING_BALANCE),
                currentBalance = cursor.getLong(KEY_CURRENT_BALANCE),
                sumIncome = cursor.getLong(KEY_SUM_INCOME),
                sumExpense = cursor.getLong(KEY_SUM_EXPENSES),
                sumTransfer = cursor.getLong(KEY_SUM_TRANSFERS),
                grouping = if (sortBy == KEY_DATE) cursor.getEnum(
                    KEY_GROUPING,
                    Grouping.NONE
                ) else Grouping.NONE,
                sortBy = sortBy,
                sortDirection = cursor.getEnum(KEY_SORT_DIRECTION, SortDirection.DESC),
                syncAccountName = cursor.getStringOrNull(KEY_SYNC_ACCOUNT_NAME),
                reconciledTotal = cursor.getLong(KEY_RECONCILED_TOTAL),
                clearedTotal = cursor.getLong(KEY_CLEARED_TOTAL),
                hasCleared = cursor.getInt(KEY_HAS_CLEARED) > 0,
                uuid = cursor.getStringOrNull(KEY_UUID),
                criterion = cursor.getLong(KEY_CRITERION).takeIf { it != 0L },
                total = if (cursor.getBoolean(KEY_HAS_FUTURE)) cursor.getLong(KEY_TOTAL) else null,
                equivalentTotal = if (cursor.getBoolean(KEY_HAS_FUTURE)) cursor.getDouble(KEY_EQUIVALENT_TOTAL).roundToLong() else null,
                excludeFromTotals = cursor.getBoolean(KEY_EXCLUDE_FROM_TOTALS),
                lastUsed = cursor.getLong(KEY_LAST_USED),
                bankId = cursor.getLongOrNull(KEY_BANK_ID),
                equivalentOpeningBalance = cursor.getLong(KEY_EQUIVALENT_OPENING_BALANCE),
                equivalentCurrentBalance = cursor.getDouble(KEY_EQUIVALENT_CURRENT_BALANCE).roundToLong(),
                equivalentSumIncome = cursor.getLong(KEY_EQUIVALENT_INCOME),
                equivalentSumExpense = cursor.getLong(KEY_EQUIVALENT_EXPENSES),
                equivalentSumTransfer = cursor.getLong(KEY_EQUIVALENT_TRANSFERS),
                initialExchangeRate = cursor.getDoubleOrNull(KEY_EXCHANGE_RATE),
                latestExchangeRate = cursor.getDoubleOrNull(KEY_LATEST_EXCHANGE_RATE)?.let {
                    cursor.getLocalDate(KEY_LATEST_EXCHANGE_RATE_DATE) to it
                },
                dynamic = cursor.getBoolean(KEY_DYNAMIC),
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
    val accountGrouping: AccountGrouping? = null,
    val color: Int = -1
) : DataBaseAccount() {
    override val currency: String = currencyUnit.code

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