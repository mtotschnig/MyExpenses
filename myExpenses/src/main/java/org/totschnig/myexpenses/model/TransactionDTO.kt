package org.totschnig.myexpenses.model

import android.content.Context
import android.database.Cursor
import androidx.annotation.Keep
import org.apache.commons.lang3.StringUtils
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.enumValueOrDefault
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@Keep
data class TransactionDTO(
    val id: String,
    val dateStr: String,
    val payee: String,
    val amount: BigDecimal,
    val labelMain: String,
    val labelSub: String,
    val fullLabel: String,
    val comment: String,
    val methodLabel: String?,
    val status: CrStatus?,
    val referenceNumber: String?,
    val pictureFileName: String?,
    val tagList: String?,
    val splits: List<TransactionDTO>?
) {

    companion object {
        fun fromCursor(
            context: Context, cursor: Cursor, formatter: SimpleDateFormat,
            currencyUnit: CurrencyUnit, splitCursor: Cursor?, tagList: String?,
            isPart: Boolean = false
        ): TransactionDTO {
            //split transactions take their full_label from the first split part
            val readCat = splitCursor?.takeIf { it.moveToFirst() } ?: cursor
            val transferPeer =
                DbUtils.getLongOrNull(readCat, KEY_TRANSFER_PEER)
            var labelMain = DbUtils.getString(readCat, KEY_LABEL_MAIN)
            var labelSub = ""
            var fullLabel = ""
            if (labelMain.isNotEmpty()) {
                if (transferPeer != null) {
                    fullLabel = "[$labelMain]"
                    labelMain = context.getString(R.string.transfer)
                    labelSub = fullLabel
                } else {
                    labelSub =
                        DbUtils.getString(readCat, KEY_LABEL_SUB)
                    fullLabel = TextUtils.formatQifCategory(labelMain, labelSub)!!
                }
            }
            return TransactionDTO(
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID)).toString(),
                formatter.format(
                    Date(
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(KEY_DATE)
                        ) * 1000
                    )
                ),
                DbUtils.getString(cursor, KEY_PAYEE_NAME),
                Money(currencyUnit, cursor.getLong(cursor.getColumnIndexOrThrow(KEY_AMOUNT)))
                    .amountMajor,
                labelMain,
                labelSub,
                fullLabel,
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
                                    formatter,
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