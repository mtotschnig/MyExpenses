package org.totschnig.myexpenses.model

import android.content.Context
import android.database.Cursor
import androidx.annotation.Keep
import org.apache.commons.lang3.StringUtils
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.*

@Keep
data class TransactionDTO(
    val date: ZonedDateTime,
    val payee: String,
    val amount: BigDecimal,
    val catId: Long?,
    val transferAccount: String?,
    val comment: String,
    val methodLabel: String?,
    val status: CrStatus?,
    val referenceNumber: String?,
    val pictureFileName: String?,
    val tagList: String?,
    val splits: List<TransactionDTO>?
) {



    fun fullLabel(categoryPaths: Map<Long, List<String>>) =
        transferAccount?.let { "[$it]" } ?: catId?.let { cat ->
            categoryPaths[cat]?.joinToString(":") { label ->
                label.replace("/","\\u002F").replace(":","\\u003A")
            }
        }

    companion object {
        fun fromCursor(
            context: Context, cursor: Cursor,
            currencyUnit: CurrencyUnit, splitCursor: Cursor?, tagList: String?,
            isPart: Boolean = false
        ): TransactionDTO {
            //split transactions take their full_label from the first split part
            val readCat = splitCursor?.takeIf { it.moveToFirst() } ?: cursor

            return TransactionDTO(
                epoch2ZonedDateTime(cursor.getLong(
                    cursor.getColumnIndexOrThrow(KEY_DATE))),
                DbUtils.getString(cursor, KEY_PAYEE_NAME),
                Money(currencyUnit, cursor.getLong(cursor.getColumnIndexOrThrow(KEY_AMOUNT)))
                    .amountMajor,
                DbUtils.getLongOrNull(readCat, KEY_CATID),
                readCat.getStringOrNull(KEY_TRANSFER_ACCOUNT_LABEL),
                DbUtils.getString(cursor, KEY_COMMENT),
                if (isPart) null else cursor.getString(cursor.getColumnIndexOrThrow(KEY_METHOD_LABEL)),
                if (isPart) null else
                    enumValueOrDefault(
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_CR_STATUS)),
                        CrStatus.UNRECONCILED
                    ),
                if (isPart) null else DbUtils.getString(cursor, KEY_REFERENCE_NUMBER),
                StringUtils.substringAfterLast(DbUtils.getString(cursor, KEY_PICTURE_URI), "/"),
                tagList,
                splitCursor?.let {
                    sequence {
                        while (!it.isAfterLast) {
                            yield(
                                fromCursor(
                                    context,
                                    it,
                                    currencyUnit,
                                    null,
                                    null,
                                    isPart = true
                                )
                            )
                            it.moveToNext()
                        }
                    }.toList()
                }
            )
        }
    }
}