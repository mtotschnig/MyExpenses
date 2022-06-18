package org.totschnig.myexpenses.model2

import android.database.Cursor
import androidx.annotation.Keep
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import java.time.LocalDate
import java.time.LocalTime

@Keep
data class Transaction(
    val id: Long?,
    val account: Long,
    val amount: Float,
    val date: LocalDate,
    val time: LocalTime?,
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
        fun fromCursor(cursor: Cursor, account: Long): Transaction {
            val dateTime = epoch2ZonedDateTime(cursor.getLong(KEY_DATE))
            val payee = cursor.getString(KEY_PAYEE_NAME)
            val comment = cursor.getString(KEY_COMMENT)
            return Transaction(
                id = cursor.getLong(KEY_ROWID),
                account = account,
                amount = cursor.getLong(KEY_AMOUNT).toFloat(),
                date = dateTime.toLocalDate(),
                time = dateTime.toLocalTime(),
                valueDate = epoch2LocalDate(cursor.getLong(KEY_VALUE_DATE)),
                payee = payee,
                category = cursor.getLong(KEY_CATID),
                tags = emptyList(),
                comment = comment,
                method = cursor.getLong(KEY_METHODID),
                number = cursor.getString(KEY_REFERENCE_NUMBER),
                displayHtml = buildList {
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