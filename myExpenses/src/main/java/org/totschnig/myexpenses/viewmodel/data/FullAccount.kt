package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.getDouble
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.enumValueOrNull

data class FullAccount(
    val id: Long,
    val label: String,
    val description: String,
    val currency: CurrencyUnit,
    val color: Int = -1,
    val type: AccountType? = null,
    val exchangeRate: Double = 1.0,
    val sealed: Boolean = false,
    val openingBalance: Long,
    val currentBalance: Long,
    val sumIncome: Long,
    val sumExpense: Long,
    val sumTransfer: Long = 0L,
    val grouping: Grouping = Grouping.NONE,
    val sortDirection: SortDirection = SortDirection.DESC,
    val syncAccountName: String? = null,
    val reconciledTotal: Long = 0L,
    val clearedTotal: Long = 0L,
    val hasCleared: Boolean = false

) {
    companion object {
        fun fromCursor(cursor: Cursor, currencyContext: CurrencyContext) = FullAccount(
            id = cursor.getLong(KEY_ROWID),
            label = cursor.getString(KEY_LABEL),
            description = cursor.getString(KEY_DESCRIPTION),
            currency = currencyContext.get(cursor.getString(KEY_CURRENCY)),
            color = cursor.getInt(KEY_COLOR),
            type = enumValueOrNull<AccountType>(cursor.getStringOrNull(KEY_TYPE)),
            exchangeRate = cursor.getDouble(KEY_EXCHANGE_RATE),
            sealed = cursor.getInt(KEY_SEALED) == 1,
            openingBalance = cursor.getLong(KEY_OPENING_BALANCE),
            currentBalance = cursor.getLong(KEY_CURRENT_BALANCE),
            sumIncome = cursor.getLong(KEY_SUM_INCOME),
            sumExpense = cursor.getLong(KEY_SUM_EXPENSES),
            sumTransfer = cursor.getLong(KEY_SUM_TRANSFERS),
            grouping = enumValueOrDefault(
                cursor.getString(KEY_GROUPING),
                Grouping.NONE
            ),
            sortDirection = enumValueOrDefault(
                cursor.getString(KEY_SORT_DIRECTION),
                SortDirection.DESC
            ),
            syncAccountName = cursor.getStringOrNull(KEY_SYNC_ACCOUNT_NAME),
            reconciledTotal = cursor.getLong(KEY_RECONCILED_TOTAL),
            clearedTotal = cursor.getLong(KEY_CLEARED_TOTAL),
            hasCleared = cursor.getInt(KEY_HAS_CLEARED) > 0
        )
    }
}