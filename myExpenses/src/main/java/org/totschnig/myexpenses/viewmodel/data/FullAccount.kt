package org.totschnig.myexpenses.viewmodel.data

import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.content.res.ResourcesCompat
import arrow.core.Tuple4
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.WhereFilter
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
    val sortBy: String = KEY_DATE,
    val sortDirection: SortDirection = SortDirection.DESC,
    val syncAccountName: String? = null,
    val reconciledTotal: Long = 0L,
    val clearedTotal: Long = 0L,
    val hasCleared: Boolean = false,
    val uuid: String? = null,
    val criterion: Long?,
    val total: Long? = null,
    val excludeFromTotals: Boolean = false,
    val lastUsed: Long = 0L,
    val bankId: Long? = null,
    val blz: String? = null
) : BaseAccount() {

    override val currency: String = currencyUnit.code

    val toPageAccount: PageAccount
        get() = PageAccount(
            id, type, sortBy, sortDirection, grouping, currencyUnit, sealed, openingBalance, _color
        )

    companion object {

        fun fromCursor(cursor: Cursor, currencyContext: CurrencyContext): FullAccount {
            val sortBy = cursor.getString(KEY_SORT_BY)
                .takeIf { it == KEY_DATE || it == KEY_AMOUNT }
                ?: KEY_DATE
            return FullAccount(
                id = cursor.getLong(KEY_ROWID),
                label = cursor.getString(KEY_LABEL),
                description = cursor.getStringOrNull(KEY_DESCRIPTION),
                currencyUnit = currencyContext.get(cursor.getString(KEY_CURRENCY)),
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
                bankId = cursor.getLongOrNull(KEY_BANK_ID),
                blz = cursor.getStringOrNull(KEY_BLZ)
            )
        }
    }
}

@Immutable
data class PageAccount(
    override val id: Long,
    val type: AccountType?,
    val sortBy: String,
    val sortDirection: SortDirection,
    override val grouping: Grouping,
    val currencyUnit: CurrencyUnit,
    val sealed: Boolean,
    val openingBalance: Long,
    override val _color: Int
) : BaseAccount() {
    override val currency: String = currencyUnit.code
    fun groupingQuery(whereFilter: WhereFilter): Triple<Uri, String?, Array<String>?> {
        val filter = whereFilter.takeIf { !it.isEmpty }
        val selection = filter?.getSelectionForParts(CTE_TRANSACTION_GROUPS)
        val args = filter?.getSelectionArgs(true)
        return Triple(
            Transaction.CONTENT_URI.buildUpon().appendPath(TransactionProvider.URI_SEGMENT_GROUPS)
                .appendPath(grouping.name).apply {
                    if (id > 0) {
                        appendQueryParameter(KEY_ACCOUNTID, id.toString())
                    } else if (!isHomeAggregate(id)) {
                        appendQueryParameter(KEY_CURRENCY, currency)
                    }
                }.build(),
            selection,
            args
        )
    }

    //Tuple4 of Uri / projection / selection / selectionArgs
    fun loadingInfo(homeCurrency: String): Tuple4<Uri, Array<String>, String, Array<String>?> {
        val uri = extendedUriForTransactionList(shortenComment = true)
        val projection = Transaction2.projection(id, grouping, homeCurrency)
        val (selection, selectionArgs) = selectionInfo
        return Tuple4(uri, projection, selection, selectionArgs)
    }

}