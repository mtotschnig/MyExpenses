package org.totschnig.myexpenses.model2

import android.content.Context
import android.database.Cursor
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import java.io.Serializable

data class Account(
    override val id: Long = 0L,
    val label: String  = "",
    val description: String = "",
    val openingBalance: Long = 0L,
    override val currency: String,
    val type: AccountType = AccountType.CASH,
    val color: Int = DEFAULT_COLOR,
    val criterion: Long? = null,
    val syncAccountName: String? = null,
    val excludeFromTotals: Boolean = false,
    val uuid: String? = null,
    val isSealed: Boolean = false,
    /**
     * describes rate of this accounts minor unit to homeCurrency minor unit
     */
    val exchangeRate: Double = 1.0,
    override val grouping: Grouping = Grouping.NONE
): DataBaseAccount(), Serializable {

    fun createIn(repository: Repository) = repository.createAccount(this)

    fun getLabelForScreenTitle(context: Context) =
        if (isHomeAggregate) context.getString(R.string.grand_total) else label

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Helper for legacy Java code")
    fun withLabel(label: String) = copy(label = label)

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
                label = cursor.getString(KEY_LABEL),
                description = cursor.getString(KEY_DESCRIPTION),
                openingBalance = cursor.getLong(KEY_OPENING_BALANCE),
                currency = cursor.getString(KEY_CURRENCY),
                type = cursor.getEnum(KEY_TYPE, AccountType.CASH),
                color = cursor.getInt(KEY_COLOR),
                criterion = cursor.getLong(KEY_CRITERION),
                syncAccountName= cursor.getStringOrNull(KEY_SYNC_ACCOUNT_NAME),
                excludeFromTotals = cursor.getBoolean(KEY_EXCLUDE_FROM_TOTALS),
                uuid = cursor.getString(KEY_UUID),
                isSealed = cursor.getBoolean(KEY_SEALED),
                exchangeRate = cursor.getDoubleIfExists(KEY_EXCHANGE_RATE) ?: 1.0,
                grouping = cursor.getEnum(KEY_GROUPING, Grouping.NONE)
            )

    }
}