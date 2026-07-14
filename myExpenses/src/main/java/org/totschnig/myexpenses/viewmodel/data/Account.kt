package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_CRITERION
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import java.io.Serializable

data class Account(
    override val id: Long,
    val label: String,
    val currency: CurrencyUnit,
    val color: Int = -1,
    val type: AccountType,
    val criterion: Long?,
    val isDynamic: Boolean,
    val flag: AccountFlag,
    var currentBalance: Long,
) : IdHolder, Serializable {
    override fun toString(): String {
        return label
    }

    companion object {
        fun fromCursor(cursor: Cursor, currencyContext: CurrencyContext): Account {
            val currency =
                currencyContext[cursor.getString(KEY_CURRENCY)]
            return Account(
                id = cursor.getLong(KEY_ROWID),
                label = cursor.getString(KEY_LABEL),
                currency,
                color = cursor.getInt(KEY_COLOR),
                type = AccountType.fromAccountCursor(cursor),
                criterion = cursor.getLongOrNull(KEY_CRITERION),
                isDynamic = cursor.getBoolean(KEY_DYNAMIC),
                flag = AccountFlag.fromAccountCursor(cursor),
                currentBalance = cursor.getLong(KEY_CURRENT_BALANCE)
            )
        }
    }
}