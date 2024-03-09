package org.totschnig.myexpenses.model2

import android.content.Context
import android.database.Cursor
import androidx.annotation.Keep
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DISPLAY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.splitStringList
import org.totschnig.myexpenses.util.ICurrencyFormatter
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
    val party: Long,
    val category: Long?,
    val tags: List<Long>,
    val comment: String,
    val method: Long,
    val number: String,
    val displayHtml: String,
    val transferPeer: Long?
) {
    companion object {
        fun fromCursor(
            context: Context,
            cursor: Cursor,
            account: Long,
            currencyUnit: CurrencyUnit,
            currencyFormatter: ICurrencyFormatter,
            dateFormat: DateFormat
        ): Transaction {
            val date = cursor.getLong(KEY_DATE)
            val dateTime = epoch2ZonedDateTime(date)
            val comment = cursor.getString(KEY_COMMENT)
            val amount = cursor.getLong(KEY_DISPLAY_AMOUNT)
            val money = Money(currencyUnit, amount)
            val number = cursor.getString(KEY_REFERENCE_NUMBER)
            val category = cursor.getLongOrNull(KEY_CATID)
            val transferPeer = cursor.getLongOrNull(KEY_TRANSFER_PEER)
            return Transaction(
                id = cursor.getLong(KEY_ROWID),
                account = account,
                amount = money.amountMajor.toFloat(),
                amountFormatted = currencyFormatter.formatMoney(money),
                date = dateTime.toLocalDate(),
                time = dateTime.toLocalTime(),
                dateFormatted = Utils.convDateTime(date, dateFormat),
                valueDate = epoch2LocalDate(cursor.getLong(KEY_VALUE_DATE)),
                party = cursor.getLong(KEY_PAYEEID),
                category = category,
                tags = emptyList(),
                comment = comment,
                method = cursor.getLong(KEY_METHODID),
                number = number,
                displayHtml = if (category == SPLIT_CATID)
                    context.getString(R.string.split_transaction)
                else
                    (number.takeIf { it.isNotEmpty() }?.let { "($it) " } ?: "")
                            + buildList {
                        cursor.getString(KEY_PATH).takeIf { it.isNotEmpty() }?.let {
                            val transferIndicator = if (transferPeer == null) "" else {
                                Transfer.getIndicatorPrefixForLabel(amount)
                            }
                            add("<span>$transferIndicator$it</span>")
                        }
                        comment.takeIf { it.isNotEmpty() }?.let {
                            add("<span class ='italic'>$it</span>")
                        }
                        cursor.getString(KEY_PAYEE_NAME).takeIf { it.isNotEmpty() }?.let {
                            add("<span class='underline'>$it</span>")
                        }
                        cursor.splitStringList(KEY_TAGLIST).takeIf { it.isNotEmpty() }?.let {
                            add("<span class='font-semibold'>${it.joinToString()}</span>")
                        }
                    }.joinToString(separator = " / "),
                transferPeer = transferPeer
            )
        }
    }
}