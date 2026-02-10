package org.totschnig.myexpenses.model2

import android.content.Context
import android.database.Cursor
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.sort.SortDirection
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.KEY_ACCOUNT_TYPE_LABEL
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_BANK_ID
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_CRITERION
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.provider.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.KEY_FLAG
import org.totschnig.myexpenses.provider.KEY_FLAG_ICON
import org.totschnig.myexpenses.provider.KEY_FLAG_LABEL
import org.totschnig.myexpenses.provider.KEY_FLAG_SORT_KEY
import org.totschnig.myexpenses.provider.KEY_GROUPING
import org.totschnig.myexpenses.provider.KEY_IS_AGGREGATE
import org.totschnig.myexpenses.provider.KEY_IS_ASSET
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SEALED
import org.totschnig.myexpenses.provider.KEY_SORT_BY
import org.totschnig.myexpenses.provider.KEY_SORT_DIRECTION
import org.totschnig.myexpenses.provider.KEY_SUPPORTS_RECONCILIATION
import org.totschnig.myexpenses.provider.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.KEY_TYPE
import org.totschnig.myexpenses.provider.KEY_TYPE_SORT_KEY
import org.totschnig.myexpenses.provider.KEY_UUID
import org.totschnig.myexpenses.provider.KEY_VISIBLE
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getDoubleIfExists
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongIfExists
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo
import java.io.Serializable

data class Account(
    override val id: Long = 0L,
    val label: String = "",
    val description: String = "",
    val openingBalance: Long = 0L,
    override val currency: String,
    val type: AccountType,
    val flag: AccountFlag = AccountFlag.DEFAULT,
    override val color: Int = DEFAULT_COLOR,
    val criterion: Long? = null,
    val syncAccountName: String? = null,
    val excludeFromTotals: Boolean = false,
    val uuid: String? = null,
    val isSealed: Boolean = false,
    override val sortBy: String = KEY_DATE,
    override val sortDirection: SortDirection = SortDirection.DESC,
    /**
     * describes rate of this accounts minor unit to homeCurrency minor unit
     */
    val exchangeRate: Double = 1.0,
    override val grouping: Grouping = Grouping.NONE,
    val bankId: Long? = null,
    val dynamicExchangeRates: Boolean = false,
    override val accountGrouping: AccountGrouping<*>? =  null
) : DataBaseAccount(), Serializable, DistributionAccountInfo {

    override val flagId = flag.id

    override val typeId = type.id

    fun createIn(repository: Repository) = repository.createAccount(this)

    override fun label(context: Context, currencyContext: CurrencyContext) =
        when {
            isHomeAggregate -> context.getString(R.string.grand_total)
            else -> when(accountGrouping) {
                AccountGrouping.CURRENCY -> currencyContext[currency].title(context)
                AccountGrouping.FLAG -> flag.title(context)
                AccountGrouping.NONE -> context.getString(R.string.grand_total)
                AccountGrouping.TYPE -> type.title(context)
                null -> label
            }
        }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Helper for legacy Java code")
    fun withLabel(label: String) = copy(label = label)

    companion object {

        const val DEFAULT_COLOR = -0xff6978

        fun getProjection(minimal: Boolean) = if (minimal) PROJECTION_MINIMAL else PROJECTION

        val PROJECTION = arrayOf(
            KEY_ROWID,
            KEY_LABEL,
            KEY_DESCRIPTION,
            KEY_OPENING_BALANCE,
            KEY_CURRENCY,
            KEY_COLOR,
            KEY_GROUPING,
            KEY_ACCOUNT_TYPE_LABEL,
            KEY_IS_ASSET,
            KEY_SUPPORTS_RECONCILIATION,
            KEY_TYPE,
            KEY_TYPE_SORT_KEY,
            KEY_FLAG,
            KEY_FLAG_LABEL,
            KEY_VISIBLE,
            KEY_FLAG_SORT_KEY,
            KEY_FLAG_ICON,
            KEY_EXCLUDE_FROM_TOTALS,
            KEY_SYNC_ACCOUNT_NAME,
            KEY_UUID,
            KEY_SORT_BY,
            KEY_SORT_DIRECTION,
            KEY_EXCHANGE_RATE,
            KEY_CRITERION,
            KEY_SEALED,
            KEY_BANK_ID,
            KEY_DYNAMIC
        )

        val PROJECTION_MINIMAL = arrayOf(
            KEY_ROWID,
            KEY_LABEL,
            KEY_CURRENCY,
            KEY_ACCOUNT_TYPE_LABEL,
            KEY_IS_ASSET,
            KEY_SUPPORTS_RECONCILIATION,
            KEY_TYPE,
            KEY_TYPE_SORT_KEY,
            KEY_FLAG,
            KEY_FLAG_LABEL,
            KEY_VISIBLE,
            KEY_FLAG_SORT_KEY,
            KEY_FLAG_ICON,
            "0 AS $KEY_IS_AGGREGATE"
        )

        fun fromCursor(cursor: Cursor): Account {
            val sortBy = cursor.getString(KEY_SORT_BY)
                .takeIf { it == KEY_DATE || it == KEY_AMOUNT }
                ?: KEY_DATE
            return Account(
                id = cursor.getLong(KEY_ROWID),
                label = cursor.getString(KEY_LABEL),
                description = cursor.getString(KEY_DESCRIPTION),
                openingBalance = cursor.getLong(KEY_OPENING_BALANCE),
                currency = cursor.getString(KEY_CURRENCY),
                type = AccountType.fromAccountCursor(cursor),
                flag = AccountFlag.fromAccountCursor(cursor),
                color = cursor.getInt(KEY_COLOR),
                criterion = cursor.getLong(KEY_CRITERION),
                syncAccountName = cursor.getStringOrNull(KEY_SYNC_ACCOUNT_NAME),
                excludeFromTotals = cursor.getBoolean(KEY_EXCLUDE_FROM_TOTALS),
                uuid = cursor.getString(KEY_UUID),
                isSealed = cursor.getBoolean(KEY_SEALED),
                exchangeRate = cursor.getDoubleIfExists(KEY_EXCHANGE_RATE) ?: 1.0,
                grouping = if (sortBy == KEY_DATE) cursor.getEnum(
                    KEY_GROUPING,
                    Grouping.NONE
                ) else Grouping.NONE,
                sortBy = sortBy,
                sortDirection = cursor.getEnum(KEY_SORT_DIRECTION, SortDirection.DESC),
                bankId = cursor.getLongIfExists(KEY_BANK_ID),
                dynamicExchangeRates = cursor.getBoolean(KEY_DYNAMIC)
            )
        }
    }
}