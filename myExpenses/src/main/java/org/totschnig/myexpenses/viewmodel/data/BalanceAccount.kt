package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.AccountWithGroupingKey
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_VISIBLE
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getDouble
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import kotlin.math.roundToLong

data class BalanceData(
    val assets: List<BalanceSection>,
    val totalAssets: Long,
    val liabilities: List<BalanceSection>,
    val totalLiabilities: Long
)

data class BalanceSection(
    val type: AccountType,
    val total: Long,
    val accounts: List<BalanceAccount>,
)

data class BalanceAccount(
    val id: Long = 0,
    val label: String,
    val currentBalance: Long,
    override val type: AccountType = AccountType.CASH,
    override val flag: AccountFlag = AccountFlag.DEFAULT,
    val color: Int = 0,
    override val currencyUnit: CurrencyUnit = CurrencyUnit.DebugInstance,
    val equivalentCurrentBalance: Long = currentBalance,
    val isVisible: Boolean = true,
): AccountWithGroupingKey {
    companion object {

        fun fromCursor(
            cursor: Cursor,
            currencyContext: CurrencyContext,
        ) = BalanceAccount(
            id = cursor.getLong(KEY_ROWID),
            label = cursor.getString(KEY_LABEL),
            type = AccountType.fromAccountCursor(cursor),
            flag = AccountFlag.fromAccountCursor(cursor),
            color = cursor.getInt(KEY_COLOR),
            currentBalance = cursor.getLong(KEY_CURRENT_BALANCE),
            equivalentCurrentBalance = cursor.getDouble(KEY_EQUIVALENT_CURRENT_BALANCE)
                .roundToLong(),
            currencyUnit = currencyContext[cursor.getString(KEY_CURRENCY)],
            isVisible = cursor.getBoolean(KEY_VISIBLE)
        )

        fun List<BalanceAccount>.partitionByAccountType(): BalanceData
                = groupBy { it.type }
            .map { entry ->
                BalanceSection(
                    type = entry.key,
                    total = entry.value.sumOf { it.equivalentCurrentBalance },
                    accounts = entry.value)
            }
            .partition { it.type.isAsset }.let { pair ->
                BalanceData(
                    totalAssets = pair.first.sumOf { it.total },
                    assets = pair.first,
                    totalLiabilities = pair.second.sumOf { it.total },
                    liabilities = pair.second
                )
            }
    }
}
