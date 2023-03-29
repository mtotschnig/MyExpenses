package org.totschnig.myexpenses.model2

import android.database.Cursor
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString

data class Account(
    val id: Long = 0L,
    val label: String,
    val description: String = "",
    val openingBalance: Long = 0L,
    val currency: String,
    val type: AccountType = AccountType.CASH,
    val color: Int = DEFAULT_COLOR,
    val criterion: Long? = null,
    val syncAccountName: String? = null,
    val excludeFromTotals: Boolean = false,
    val uuid: String? = null,
    val isSealed: Boolean = false,
    val exchangeRate: Double = 1.0,
    val grouping: Grouping = Grouping.NONE
) {

    fun createIn(repository: Repository) = repository.createAccount(this)

    companion object {

        const val DEFAULT_COLOR = -0xff6978
        val PROJECTION = arrayOf(
            "$TABLE_ACCOUNTS.$KEY_ROWID AS $KEY_ROWID",
            KEY_LABEL,
            "$TABLE_ACCOUNTS.$KEY_DESCRIPTION AS $KEY_DESCRIPTION",
            KEY_OPENING_BALANCE,
            "$TABLE_ACCOUNTS.$KEY_CURRENCY AS $KEY_CURRENCY",
            KEY_COLOR,
            "$TABLE_ACCOUNTS.$KEY_GROUPING AS $KEY_GROUPING",
            KEY_TYPE,
            KEY_SORT_KEY,
            KEY_EXCLUDE_FROM_TOTALS,
            KEY_SYNC_ACCOUNT_NAME,
            KEY_UUID,
            KEY_SORT_DIRECTION,
            KEY_EXCHANGE_RATE,
            KEY_CRITERION,
            KEY_SEALED
        )

        fun fromCursor(cursor: Cursor) =
            Account(
                id = cursor.getLong(KEY_ROWID),
                openingBalance = cursor.getLong(KEY_OPENING_BALANCE),
                currency = cursor.getString(KEY_CURRENCY),
                label = cursor.getString(KEY_LABEL),
                description = cursor.getString(KEY_DESCRIPTION),
                uuid = cursor.getString(KEY_UUID),
                isSealed = cursor.getBoolean(KEY_SEALED),
                grouping = cursor.getEnum(KEY_GROUPING, Grouping.NONE)
            )
    }
}