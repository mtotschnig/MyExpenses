package org.totschnig.myexpenses.viewmodel.data

import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.content.res.ResourcesCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CLEARED_TOTAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CRITERION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_CLEARED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_FUTURE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_BY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_DIRECTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TOTAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.enumValueOrNull

abstract class BaseAccount : DataBaseAccount() {
    abstract val _color: Int
    fun color(resources: Resources): Int = if (isAggregate)
        ResourcesCompat.getColor(resources, R.color.colorAggregate, null) else _color
}

@Stable
data class FullAccount(
    override val id: Long,
    val label: String,
    val description: String?,
    val currencyUnit: CurrencyUnit,
    override val _color: Int = -1,
    val type: AccountType?,
    val sealed: Boolean = false,
    val openingBalance: Long,
    val currentBalance: Long,
    val sumIncome: Long,
    val sumExpense: Long,
    val sumTransfer: Long = 0L,
    override val grouping: Grouping = Grouping.NONE,
    override val sortBy: String = KEY_DATE,
    override val sortDirection: SortDirection = SortDirection.DESC,
    val syncAccountName: String? = null,
    val reconciledTotal: Long = 0L,
    val clearedTotal: Long = 0L,
    val hasCleared: Boolean = false,
    val uuid: String? = null,
    val criterion: Long?,
    val total: Long? = null,
    val excludeFromTotals: Boolean = false,
    val lastUsed: Long = 0L,
    val bankId: Long? = null
) : BaseAccount() {

    override val currency: String = currencyUnit.code

    val toPageAccount: PageAccount
        get() = PageAccount(
            id, type, sortBy, sortDirection, grouping, currencyUnit, sealed, openingBalance, _color
        )

    val progress: Float?
        get() = criterion?.let {
            if (it > 0 == currentBalance > 0) {
                (currentBalance * 100F / it)
            } else 0f
        }

    companion object {

        fun fromCursor(cursor: Cursor, currencyContext: CurrencyContext): FullAccount {
            val sortBy = cursor.getString(KEY_SORT_BY)
                .takeIf { it == KEY_DATE || it == KEY_AMOUNT }
                ?: KEY_DATE
            return FullAccount(
                id = cursor.getLong(KEY_ROWID),
                label = cursor.getString(KEY_LABEL),
                description = cursor.getStringOrNull(KEY_DESCRIPTION),
                currencyUnit = currencyContext[cursor.getString(KEY_CURRENCY)],
                _color = cursor.getInt(KEY_COLOR),
                type = enumValueOrNull<AccountType>(cursor.getStringOrNull(KEY_TYPE)),
                sealed = cursor.getInt(KEY_SEALED) == 1,
                openingBalance = cursor.getLong(KEY_OPENING_BALANCE),
                currentBalance = cursor.getLong(KEY_CURRENT_BALANCE),
                sumIncome = cursor.getLong(KEY_SUM_INCOME),
                sumExpense = cursor.getLong(KEY_SUM_EXPENSES),
                sumTransfer = cursor.getLong(KEY_SUM_TRANSFERS),
                grouping = if (sortBy == KEY_DATE) cursor.getEnum(KEY_GROUPING, Grouping.NONE) else Grouping.NONE,
                sortBy = sortBy,
                sortDirection = cursor.getEnum(KEY_SORT_DIRECTION, SortDirection.DESC),
                syncAccountName = cursor.getStringOrNull(KEY_SYNC_ACCOUNT_NAME),
                reconciledTotal = cursor.getLong(KEY_RECONCILED_TOTAL),
                clearedTotal = cursor.getLong(KEY_CLEARED_TOTAL),
                hasCleared = cursor.getInt(KEY_HAS_CLEARED) > 0,
                uuid = cursor.getStringOrNull(KEY_UUID),
                criterion = cursor.getLong(KEY_CRITERION).takeIf { it != 0L },
                total = if (cursor.getBoolean(KEY_HAS_FUTURE)) cursor.getLong(KEY_TOTAL) else null,
                excludeFromTotals = cursor.getBoolean(KEY_EXCLUDE_FROM_TOTALS),
                lastUsed = cursor.getLong(KEY_LAST_USED),
                bankId = cursor.getLongOrNull(KEY_BANK_ID)
            )
        }
    }
}

@Immutable
data class PageAccount(
    override val id: Long,
    val type: AccountType?,
    override val sortBy: String,
    override val sortDirection: SortDirection,
    override val grouping: Grouping,
    val currencyUnit: CurrencyUnit,
    val sealed: Boolean,
    val openingBalance: Long,
    override val _color: Int
) : BaseAccount() {
    override val currency: String = currencyUnit.code

    //Tuple4 of Uri / projection / selection / selectionArgs
    fun loadingInfo(prefHandler: PrefHandler): Pair<Uri, Array<String>> =
        uriForTransactionList(shortenComment = true) to Transaction2.projection(
            id,
            grouping,
            prefHandler
        )
}