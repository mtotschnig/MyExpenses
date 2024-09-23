package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import androidx.compose.runtime.Immutable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_SUM
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM
import org.totschnig.myexpenses.provider.getLongOrNull
import kotlin.math.sign

@Immutable
data class DisplayDebt(
    val id: Long,
    val label: String,
    val description: String,
    val payeeId: Long,
    val amount: Long,
    val currency: CurrencyUnit,
    val date: Long,
    val payeeName: String,
    val isSealed: Boolean = false,
    val sum: Long = 0,
    val equivalentAmount: Long? = null,
    val equivalentSum: Long? = null
) {
    val currentBalance: Long
        get() = amount - sum

    val currentEquivalentBalance: Long
        get() = (equivalentAmount ?: amount) - (equivalentSum ?: sum)

    fun title(context: Context) = when (val signum = currentBalance.sign) {
        0 -> payeeName
        else -> context.getString(
            if (signum == 1) R.string.debt_owes_me else R.string.debt_I_owe,
            payeeName
        )
    }

    companion object {
        fun fromCursor(cursor: Cursor, currencyContext: CurrencyContext) = DisplayDebt(
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID)),
            cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL)),
            cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_PAYEEID)),
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_AMOUNT)),
            currencyContext[cursor.getString(cursor.getColumnIndexOrThrow(KEY_CURRENCY))],
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DATE)),
            cursor.getString(cursor.getColumnIndexOrThrow(KEY_PAYEE_NAME)),
            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEALED)) == 1,
            cursor.getColumnIndex(KEY_SUM).takeIf { it != -1 }?.let { cursor.getLong(it) } ?: 0,
            cursor.getLongOrNull(KEY_EQUIVALENT_AMOUNT),
            cursor.getColumnIndex(KEY_EQUIVALENT_SUM).takeIf { it != -1 }?.let { cursor.getLong(it) } ?: 0

        )
    }
}