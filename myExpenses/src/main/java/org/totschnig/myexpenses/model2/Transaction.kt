package org.totschnig.myexpenses.model2

import android.database.Cursor
import androidx.annotation.Keep
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.util.formatMoney
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalTime

@Keep
data class Transaction(
    val id: Long?,
    val account: Long,
    val amount: Float,
    val amountFormatted: String,
    val date: LocalDate,
    val time: LocalTime?,
    val dateFormatted: String,
    val valueDate: LocalDate,
    val payee: String,
    val category: Long,
    val tags: List<Long>,
    val comment: String,
    val method: Long,
    val number: String,
    val displayHtml: String
) {
    companion object {
        fun fromCursor(
            cursor: Cursor,
            account: Long,
            currencyUnit: CurrencyUnit,
            currencyFormatter: CurrencyFormatter,
            dateFormat: DateFormat
        ): Transaction {
            val date = cursor.getLong(KEY_DATE)
            val dateTime = epoch2ZonedDateTime(date)
            val payee = cursor.getString(KEY_PAYEE_NAME)
            val comment = cursor.getString(KEY_COMMENT)
            val money = Money(currencyUnit, cursor.getLong(KEY_AMOUNT))
            val number = cursor.getString(KEY_REFERENCE_NUMBER)
            return Transaction(
                id = cursor.getLong(KEY_ROWID),
                account = account,
                amount = money.amountMajor.toFloat(),
                amountFormatted = currencyFormatter.formatMoney(money),
                date = dateTime.toLocalDate(),
                time = dateTime.toLocalTime(),
                dateFormatted = Utils.convDateTime(date, dateFormat),
                valueDate = epoch2LocalDate(cursor.getLong(KEY_VALUE_DATE)),
                payee = payee,
                category = cursor.getLong(KEY_CATID),
                tags = emptyList(),
                comment = comment,
                method = cursor.getLong(KEY_METHODID),
                number = number,
                displayHtml = (number.takeIf { it.isNotEmpty() }?.let { "($it) " } ?: "")
                        + buildList {
                    cursor.getString(KEY_LABEL).takeIf { it.isNotEmpty() }?.let {
                        add("<span>$it</span>")
                    }
                    comment.takeIf { it.isNotEmpty() }?.let {
                        add("<span class ='italic'>$it</span>")
                    }
                    payee.takeIf { it.isNotEmpty() }?.let {
                        add("<span class='underline'>$it</span>")
                    }
                    cursor.getString(KEY_TAGLIST).takeIf { it.isNotEmpty() }?.let {
                        add("<span class='font-semibold'>$it</span>")
                    }
                }.joinToString(separator = " / ")
            )
        }
    }
}