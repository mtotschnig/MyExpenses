package org.totschnig.myexpenses.viewmodel.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.localDate2Epoch
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.sign

data class Debt(
    val id: Long,
    val label: String,
    val description: String,
    val payeeId: Long,
    val amount: Long,
    val currency: CurrencyUnit,
    val date: Long,
    val payeeName: String? = null,
    val isSealed: Boolean = false,
    val sum: Long = 0
) {
    constructor(
        id: Long,
        label: String,
        description: String,
        payeeId: Long,
        amount: BigDecimal,
        currency: CurrencyUnit,
        date: LocalDate
    ) : this(
        id,
        label,
        description,
        payeeId,
        Money(currency, amount).amountMinor,
        currency,
        localDate2Epoch(date)
    )

    fun title(context: Context) = when (val signum = currentBalance.sign) {
        0 -> payeeName!!
        else -> context.getString(
            if (signum == 1) R.string.debt_owes_me else R.string.debt_I_owe,
            payeeName!!
        )
    }

    val currentBalance: Long
        get() = amount - sum

    fun toContentValues() = ContentValues().apply {
        put(KEY_LABEL, label)
        put(KEY_DESCRIPTION, description)
        put(KEY_AMOUNT, amount)
        put(KEY_CURRENCY, currency.code)
        put(KEY_DATE, date)
        if (id == 0L) {
            //the link between debt and payeeId should not be altered
            put(KEY_PAYEEID, payeeId)
        }
    }

    companion object {
        val CONTENT_URI: Uri = TransactionProvider.DEBTS_URI
        fun fromCursor(cursor: Cursor, currencyContext: CurrencyContext) = Debt(
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID)),
            cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL)),
            cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_PAYEEID)),
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_AMOUNT)),
            currencyContext[cursor.getString(cursor.getColumnIndexOrThrow(KEY_CURRENCY))],
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DATE)),
            cursor.getString(cursor.getColumnIndexOrThrow(KEY_PAYEE_NAME)),
            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEALED)) == 1,
            cursor.getColumnIndex(KEY_SUM).takeIf { it != -1 }?.let { cursor.getLong(it) } ?: 0
        )
    }
}