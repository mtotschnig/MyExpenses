package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getDouble
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.enumValueOrDefault
import kotlin.math.roundToLong

data class BalanceAccount(
    val id: Long = 0,
    val label: String,
    val type: AccountType,
    val currentBalance: Long,
    val color: Int = 0,
    val currency: CurrencyUnit = CurrencyUnit.DebugInstance,
    val equivalentCurrentBalance: Long = currentBalance,
    val isHidden: Boolean = false,
) {
    companion object {

        fun fromCursor(
            cursor: Cursor,
            currencyContext: CurrencyContext,
        ) = BalanceAccount(
            id = cursor.getLong(KEY_ROWID),
            label = cursor.getString(KEY_LABEL),
            type = enumValueOrDefault(cursor.getStringOrNull(KEY_TYPE), AccountType.CASH),
            color = cursor.getInt(KEY_COLOR),
            currentBalance = cursor.getLong(KEY_CURRENT_BALANCE),
            equivalentCurrentBalance = cursor.getDouble(KEY_EQUIVALENT_CURRENT_BALANCE)
                .roundToLong(),
            currency = currencyContext[cursor.getString(KEY_CURRENCY)],
            isHidden = cursor.getBoolean(KEY_HIDDEN)
        )
    }
}
