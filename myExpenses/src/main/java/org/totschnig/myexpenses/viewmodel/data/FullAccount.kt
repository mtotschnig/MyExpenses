package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.core.content.res.ResourcesCompat
import arrow.core.Tuple4
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.enumValueOrNull
@Immutable
data class FullAccount(
    val id: Long,
    val label: String,
    val description: String?,
    val currency: CurrencyUnit,
    val _color: Int = -1,
    val type: AccountType?,
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
    val hasCleared: Boolean = false,
    val uuid: String? = null,
    val criterion: Long
) {

    //Tuple4 of Uri / projection / selection / selectionArgs
    fun loadingInfo(context: Context): Tuple4<Uri, Array<String>, String, Array<String>?> {
        val builder = Transaction.EXTENDED_URI.buildUpon()
            .appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_SHORTEN_COMMENT)
        if (id < 0) {
            builder.appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_TRANSFERS)
        }
        val uri = builder.build()
        val projection = when {
            !isAggregate -> Transaction2.projection(context)
            isHomeAggregate -> Transaction2.projection(context) +
                    Transaction2.additionalAggregateColumns + Transaction2.additionGrandTotalColumns
            else -> Transaction2.projection(context) + Transaction2.additionalAggregateColumns
        }
        return Tuple4(uri, projection, selection, selectionArgs)
    }

    val selection = when {
        !isAggregate -> "$KEY_ACCOUNTID = ?"
        isHomeAggregate -> "$KEY_ACCOUNTID IN (SELECT $KEY_ROWID from $TABLE_ACCOUNTS WHERE $KEY_EXCLUDE_FROM_TOTALS = 0)"
        else -> "$KEY_ACCOUNTID IN (SELECT $KEY_ROWID from $TABLE_ACCOUNTS WHERE $KEY_CURRENCY = ? AND $KEY_EXCLUDE_FROM_TOTALS = 0)"
    }

    val selectionArgs = when {
        !isAggregate -> arrayOf(id.toString())
        isHomeAggregate -> null
        else -> arrayOf(currency.code)
    }

    fun groupingQuery(whereFilter: WhereFilter): Triple<Uri, String?, Array<String>?> {
        val filter = whereFilter.takeIf { !it.isEmpty }
        val selection = filter?.getSelectionForParts(VIEW_WITH_ACCOUNT)
        val args = filter?.getSelectionArgs(true)
        return Triple(
            Transaction.CONTENT_URI.buildUpon().appendPath(TransactionProvider.URI_SEGMENT_GROUPS)
            .appendPath(grouping.name).apply {
                if (id > 0) {
                    appendQueryParameter(KEY_ACCOUNTID, id.toString())
                } else if (!isHomeAggregate) {
                    appendQueryParameter(KEY_CURRENCY, currency.code)
                }
            }.build(),
            selection,
            args
        )
    }

    val isHomeAggregate get() = id == Account.HOME_AGGREGATE_ID

    val isAggregate get() = id < 0

    fun color(resources: Resources): Int = if (isAggregate)
        ResourcesCompat.getColor(resources, R.color.colorAggregate, null) else _color

    companion object {
        fun fromCursor(cursor: Cursor, currencyContext: CurrencyContext) = FullAccount(
            id = cursor.getLong(KEY_ROWID),
            label = cursor.getString(KEY_LABEL),
            description = cursor.getStringOrNull(KEY_DESCRIPTION),
            currency = currencyContext.get(cursor.getString(KEY_CURRENCY)),
            _color = cursor.getInt(KEY_COLOR),
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
            hasCleared = cursor.getInt(KEY_HAS_CLEARED) > 0,
            uuid = cursor.getStringOrNull(KEY_UUID),
            criterion = cursor.getLong(KEY_CRITERION)
        )
    }
}