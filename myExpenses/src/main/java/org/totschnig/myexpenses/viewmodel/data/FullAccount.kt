package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.content.res.ResourcesCompat
import arrow.core.Tuple4
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.accounts.SIGMA
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.BalanceType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.SortDirection
import org.totschnig.myexpenses.model2.AccountWithGroupingKey
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_BALANCE_TYPE
import org.totschnig.myexpenses.provider.KEY_BANK_ID
import org.totschnig.myexpenses.provider.KEY_CLEARED_TOTAL
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_CRITERION
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.provider.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_EXPENSES
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_INCOME
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_OPENING_BALANCE
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_TRANSFERS
import org.totschnig.myexpenses.provider.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.KEY_GROUPING
import org.totschnig.myexpenses.provider.KEY_HAS_CLEARED
import org.totschnig.myexpenses.provider.KEY_HAS_FUTURE
import org.totschnig.myexpenses.provider.KEY_IS_PORTFOLIO
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_LAST_USED
import org.totschnig.myexpenses.provider.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.KEY_PARENTID
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
import org.totschnig.myexpenses.provider.KEY_VISIBLE
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getBooleanIfExists
import org.totschnig.myexpenses.provider.getDouble
import org.totschnig.myexpenses.provider.getDoubleOrNull
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getEnumIfExists
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongIfExists
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import timber.log.Timber
import java.time.LocalDate
import kotlin.math.roundToLong
import kotlin.math.sign

sealed class BaseAccount : DataBaseAccount() {
    abstract val currencyUnit: CurrencyUnit
    abstract val type: AccountType
    abstract val flag: AccountFlag
    abstract fun toPageAccount(context: Context): PageAccount
    abstract fun color(resources: Resources): Int
    abstract val total: Long?
    abstract val currentBalance: Long?
    abstract val equivalentTotal: Long?
    abstract fun labelV2(context: Context): String
    fun aggregateColor(resources: Resources) =
        ResourcesCompat.getColor(resources, R.color.colorAggregate, null)

    abstract val balanceType: BalanceType
    override val flagId: Long?
        get() = flag.id
    override val typeId: Long?
        get() = type.id
    abstract val isSingleCurrency: Boolean
}

@Stable
data class AggregateAccount(
    override val currencyUnit: CurrencyUnit,
    override val type: AccountType,
    override val flag: AccountFlag,
    override val isSingleCurrency: Boolean,
    val openingBalance: Long? = null,
    override val currentBalance: Long? = null,
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
    override val equivalentTotal: Long? = total,
    val parentId: Long? = null,
    val isPortfolio: Boolean = false,
    override val accountGrouping: AccountGrouping<*>,
    override val balanceType: BalanceType = BalanceType.CURRENT,
) : BaseAccount() {
    override val id: Long = 0
    override val currency: String = currencyUnit.code

    override fun labelV2(context: Context): String = when (accountGrouping) {
        AccountGrouping.TYPE -> type.title(context) + " ($SIGMA)"
        AccountGrouping.CURRENCY -> currencyUnit.code + " ($SIGMA)"
        AccountGrouping.FLAG -> flag.title(context) + " ($SIGMA)"
        AccountGrouping.NONE -> context.getString(R.string.grand_total)
    }

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
        parentId = null,
        isPortfolio = false,
        accountGrouping = accountGrouping,
        isSingCurrency = isSingleCurrency
    )
}

data class BalanceData(
    val assets: List<BalanceSection>,
    val totalAssets: Long,
    val liabilities: List<BalanceSection>,
    val totalLiabilities: Long,
)

data class BalanceSection(
    val type: AccountType,
    val total: Long,
    val accounts: List<FullAccount>,
)

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
    override val currentBalance: Long = 0,
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
    override val balanceType: BalanceType = BalanceType.CURRENT,
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
    val parentId: Long? = null,
    val isPortfolio: Boolean = false,
    val isVisible: Boolean = true,
    val children: List<FullAccount> = emptyList(),
    override val isSingleCurrency: Boolean = true,
    val childrenValuation: Long = 0,
    val equivalentChildrenValuation: Long = childrenValuation,
) : BaseAccount(), AccountWithGroupingKey {

    override val accountGrouping: AccountGrouping<*>? = null

    override val currency: String = currencyUnit.code

    override fun color(resources: Resources) = if (isAggregate) aggregateColor(resources) else color

    override fun labelV2(context: Context) = label

    val effectiveBalance
        get() = currentBalance + childrenValuation
    val equivalentEffectiveBalance
        get() = equivalentCurrentBalance + equivalentChildrenValuation

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
            parentId = parentId,
            isPortfolio = isPortfolio,
            accountGrouping = null,
            isSingCurrency = isSingleCurrency
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

    val reconciliationAvailable = type.supportsReconciliation && !sealed

    override fun toPageAccount(context: Context) = toPageAccount

    companion object {

        fun Cursor.fromCursor(
            currencyContext: CurrencyContext,
        ): FullAccount {
            val sortBy = getString(KEY_SORT_BY)
                .takeIf { it == KEY_DATE || it == KEY_AMOUNT }
                ?: KEY_DATE
            val id = getLong(KEY_ROWID)
            return FullAccount(
                id = id,
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
                excludeFromTotals = getBoolean(KEY_EXCLUDE_FROM_TOTALS),
                lastUsed = getLong(KEY_LAST_USED),
                bankId = getLongOrNull(KEY_BANK_ID),
                equivalentOpeningBalance = getDouble(KEY_EQUIVALENT_OPENING_BALANCE).roundToLong(),
                equivalentSumIncome = getLong(KEY_EQUIVALENT_INCOME),
                equivalentSumExpense = getLong(KEY_EQUIVALENT_EXPENSES),
                equivalentSumTransfer = getLong(KEY_EQUIVALENT_TRANSFERS),
                initialExchangeRate = getDoubleOrNull(KEY_EXCHANGE_RATE),
                dynamic = getBoolean(KEY_DYNAMIC),
                balanceType = getEnumIfExists(KEY_BALANCE_TYPE, BalanceType.CURRENT),
                parentId = getLongIfExists(KEY_PARENTID),
                isVisible = getBooleanIfExists(KEY_VISIBLE) ?: true,
                isPortfolio = getBooleanIfExists(KEY_IS_PORTFOLIO) ?: false,
                //in V2 this is only called for real accounts, in V1 we need to set isSingleCurrency to false for home aggregate
                isSingleCurrency = !isHomeAggregate(id)
            )
        }

        fun List<FullAccount>.partitionByAccountType(): BalanceData = groupBy { it.type }
            .map { entry ->
                BalanceSection(
                    type = entry.key,
                    total = entry.value.sumOf { it.equivalentCurrentBalance },
                    accounts = entry.value
                )
            }
            .partition { it.type.isAsset }.let { pair ->
                BalanceData(
                    totalAssets = pair.first.sumOf { it.total },
                    assets = pair.first,
                    totalLiabilities = pair.second.sumOf { it.total },
                    liabilities = pair.second
                )
            }


        fun List<FullAccount>.nest(): List<FullAccount> {
            val accountMap = associateBy { it.id }
            val roots = mutableListOf<FullAccount>()
            val childrenMap = mutableMapOf<Long, MutableList<FullAccount>>()

            forEach { account ->
                val parentId = account.parentId
                if (parentId != null && accountMap.containsKey(parentId)) {
                    childrenMap.getOrPut(parentId) { mutableListOf() }.add(account)
                } else {
                    roots.add(account)
                }
            }

            return roots.map { root ->
                root.copy(children = childrenMap[root.id] ?: emptyList())
            }
        }

    }

    fun enrich(
        priceMap: Map<Pair<String, String>, Double>,
        homeCurrency: CurrencyUnit,
    ): FullAccount {
        val enrichedChildren = children.map { it.enrich(priceMap, currencyUnit) }

        val rateToHome = when {
            currencyUnit == homeCurrency -> 1.0

            dynamic -> priceMap[currencyUnit.code to homeCurrency.code]
            else -> null
        } ?: (latestExchangeRate?.second ?: initialExchangeRate ?: 1.0)

        val childrenValuation = if (isPortfolio) {
            enrichedChildren.sumOf { child ->
                val directPrice = priceMap[child.currencyUnit.code to this.currencyUnit.code]
                if (directPrice != null) {
                    (child.currentBalance.toDouble() * directPrice).roundToLong()
                } else {
                    // Cross-rate fallback: childValInHome / thisRateToHome //TODO check
                    (child.equivalentCurrentBalance.toDouble() / rateToHome).roundToLong()
                }
            }
        } else 0L

        val equivalentChildrenValuation = (childrenValuation * rateToHome).roundToLong()

        // 4. Update the account instance with calculated valuations and equivalents
        return this.copy(
            children = enrichedChildren,
            equivalentCurrentBalance = (currentBalance * rateToHome).roundToLong(),
            childrenValuation = childrenValuation,
            equivalentChildrenValuation = equivalentChildrenValuation
        )
    }
}

@Immutable
data class PageAccount(
    override val id: Long = 0,
    override val type: AccountType = AccountType.CASH,
    override val flag: AccountFlag = AccountFlag.DEFAULT,
    override val sortBy: String = KEY_DATE,
    override val sortDirection: SortDirection = SortDirection.DESC,
    override val grouping: Grouping = Grouping.NONE,
    override val currencyUnit: CurrencyUnit,
    val sealed: Boolean = false,
    val openingBalance: Long = 0,
    val currentBalance: Long = 0,
    val label: String,
    val parentId: Long? = null,
    val isPortfolio: Boolean = false,
    //not null for aggregate accounts
    override val accountGrouping: AccountGrouping<*>? = null,
    val color: Int = -1,
    val isSingCurrency: Boolean = true,
) : DataBaseAccount(), AccountWithGroupingKey {
    override val currency: String = currencyUnit.code

    override val flagId = flag.id
    override val typeId = type.id

    val stableId: Any
        get() = if (id != 0L) id
        else accountGrouping to accountGrouping?.getGroupKey(this)

    val queryKey: Any
        get() = Tuple4(stableId, sortBy, sortDirection, grouping)
}