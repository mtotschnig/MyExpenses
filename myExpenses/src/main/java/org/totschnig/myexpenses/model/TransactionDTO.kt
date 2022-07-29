package org.totschnig.myexpenses.model

import android.content.Context
import android.database.Cursor
import org.apache.commons.lang3.StringUtils
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import java.math.BigDecimal
import java.time.ZonedDateTime

data class TransactionDTO(
    val uuid: String,
    val date: ZonedDateTime,
    val payee: String?,
    val amount: BigDecimal,
    val catId: Long?,
    val transferAccount: String?,
    val comment: String?,
    val methodLabel: String?,
    val status: CrStatus?,
    val referenceNumber: String?,
    val pictureFileName: String?,
    val tagList: List<String>?,
    val splits: List<TransactionDTO>?
) {

    fun fullLabel(categoryPaths: Map<Long, List<String>>) =
        transferAccount?.let { "[$it]" } ?: categoryPath(categoryPaths)

    fun categoryPath(categoryPaths: Map<Long, List<String>>) = catId?.let { cat ->
        categoryPaths[cat]?.joinToString(":") { label ->
            label.replace("/","\\u002F").replace(":","\\u003A")
        }
    }

    companion object {
        fun fromCursor(
            context: Context,
            cursor: Cursor,
            projection: Array<String>,
            currencyUnit: CurrencyUnit,
            isPart: Boolean = false
        ): TransactionDTO {
            val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID)).toString()
            val catId = DbUtils.getLongOrNull(cursor, KEY_CATID)
            val isSplit = SPLIT_CATID == catId
            val splitCursor = if (isSplit) context.contentResolver.query(
                Transaction.CONTENT_URI,
                projection,
                "$KEY_PARENTID = ?",
                arrayOf(rowId),
                null
            ) else null
            val readCat = splitCursor?.takeIf { it.moveToFirst() } ?: cursor

            val tagList = context.contentResolver.query(
                TransactionProvider.TRANSACTIONS_TAGS_URI,
                arrayOf(KEY_LABEL),
                "$KEY_TRANSACTIONID = ?",
                arrayOf(rowId),
                null
            )?.use { tagCursor -> tagCursor.asSequence.map { it.getString(0) }.toList() }?.takeIf { it.isNotEmpty() }

            val transactionDTO = TransactionDTO(
                cursor.getString(KEY_UUID),
                epoch2ZonedDateTime(
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(KEY_DATE)
                    )
                ),
                cursor.getStringOrNull(KEY_PAYEE_NAME),
                Money(currencyUnit, cursor.getLong(cursor.getColumnIndexOrThrow(KEY_AMOUNT)))
                    .amountMajor,
                DbUtils.getLongOrNull(readCat, KEY_CATID),
                readCat.getStringOrNull(KEY_TRANSFER_ACCOUNT_LABEL),
                cursor.getStringOrNull(KEY_COMMENT)?.takeIf { it.isNotEmpty() },
                if (isPart) null else cursor.getString(cursor.getColumnIndexOrThrow(KEY_METHOD_LABEL)),
                if (isPart) null else
                    enumValueOrDefault(
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_CR_STATUS)),
                        CrStatus.UNRECONCILED
                    ),
                if (isPart) null else cursor.getStringOrNull(KEY_REFERENCE_NUMBER)
                    ?.takeIf { it.isNotEmpty() },
                StringUtils.substringAfterLast(cursor.getStringOrNull(KEY_PICTURE_URI), "/"),
                tagList,
                splitCursor?.let {
                    sequence {
                        while (!it.isAfterLast) {
                            yield(
                                fromCursor(
                                    context,
                                    it,
                                    projection,
                                    currencyUnit,
                                    isPart = true
                                )
                            )
                            it.moveToNext()
                        }
                    }.toList()
                }
            )
            splitCursor?.close()
            return transactionDTO
        }
    }
}