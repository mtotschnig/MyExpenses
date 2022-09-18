package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants.DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DAY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MONTH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER_PARENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_PEER_PARENT
import org.totschnig.myexpenses.provider.DatabaseConstants.YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.getMonth
import org.totschnig.myexpenses.provider.DatabaseConstants.getWeek
import org.totschnig.myexpenses.provider.DbUtils.getLongOr0L
import org.totschnig.myexpenses.provider.DbUtils.getString
import org.totschnig.myexpenses.provider.FULL_LABEL
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import java.io.File
import java.time.ZonedDateTime


data class Transaction2(
    val id: Long,
    val date: ZonedDateTime,
    val valueDate: ZonedDateTime,
    val amount: Money,
    val comment: String?,
    val catId: Long?,
    val label: String?,
    val payee: String?,
    val transferPeer: Long?,
    val transferAccount: Long?,
    val accountId: Long,
    val methodId: Long?,
    val methodLabel: String?,
    val crStatus: CrStatus,
    val referenceNumber: String,
    val currency: CurrencyUnit,
    val pictureUri: Uri?,
    val color: Int,
    val transferPeerParent: Long?,
    val status: Int,
    val accountLabel: String,
    val accountType: AccountType,
    val tagList: String? = null,
    val year: Int,
    val month: Int,
    val week: Int,
    val day: Int
) {

    companion object {
        fun projection(context: Context) = arrayOf(
            KEY_ROWID,
            KEY_DATE,
            KEY_VALUE_DATE,
            KEY_AMOUNT,
            KEY_COMMENT,
            KEY_CATID,
            FULL_LABEL,
            KEY_PAYEE_NAME,
            KEY_TRANSFER_PEER,
            KEY_TRANSFER_ACCOUNT,
            KEY_ACCOUNTID,
            KEY_METHODID,
            PaymentMethod.localizedLabelSqlColumn(
                context, KEY_METHOD_LABEL
            ) + " AS " + KEY_METHOD_LABEL,
            KEY_CR_STATUS,
            KEY_REFERENCE_NUMBER,
            KEY_CURRENCY,
            KEY_PICTURE_URI,
            KEY_COLOR,
            "$TRANSFER_PEER_PARENT AS $KEY_TRANSFER_PEER_PARENT",
            KEY_STATUS,
            KEY_ACCOUNT_LABEL,
            KEY_ACCOUNT_TYPE,
            KEY_TAGLIST,
            KEY_PARENTID,
            "$YEAR AS $KEY_YEAR",
            "${getMonth()} AS $KEY_MONTH",
            "${getWeek()} AS $KEY_WEEK",
            "$DAY AS $KEY_DAY"
        )

        fun fromCursor(
            context: Context,
            cursor: Cursor,
            currencyContext: CurrencyContext
        ): Transaction2 {
            val currencyUnit = currencyContext.get(cursor.getString(KEY_CURRENCY))
            val amountRaw = cursor.getLong(KEY_AMOUNT)
            val money = Money(currencyUnit, amountRaw)
            val date: Long = cursor.getLong(KEY_DATE)
            val valueDate: Long = cursor.getLong(KEY_VALUE_DATE)
            val transferPeer = cursor.getLongOrNull(KEY_TRANSFER_PEER)

            return Transaction2(
                id = getLongOr0L(cursor, KEY_ROWID),
                amount = money,
                date = epoch2ZonedDateTime(date),
                valueDate = epoch2ZonedDateTime(valueDate),
                comment = cursor.getStringOrNull(KEY_COMMENT),
                catId = cursor.getLongOrNull( KEY_CATID),
                payee = cursor.getStringOrNull(KEY_PAYEE_NAME),
                methodLabel = cursor.getStringOrNull(KEY_METHOD_LABEL),
                label = cursor.getStringOrNull(KEY_LABEL),
                transferPeer = transferPeer,
                transferAccount = cursor.getLongOrNull(KEY_TRANSFER_ACCOUNT),
                accountId = cursor.getLong(KEY_ACCOUNTID),
                methodId = cursor.getLongOrNull(KEY_METHODID),
                currency = currencyUnit,
                pictureUri = cursor.getStringOrNull(KEY_PICTURE_URI)
                    ?.let { uri ->
                        var parsedUri = Uri.parse(uri)
                        if ("file" == parsedUri.scheme) { // Upgrade from legacy uris
                            parsedUri.path?.let {
                                try {
                                    parsedUri = AppDirHelper.getContentUriForFile(context, File(it))
                                } catch (ignored: IllegalArgumentException) {
                                }
                            }
                        }
                        parsedUri
                    },
                crStatus = enumValueOrDefault(
                    cursor.getString(KEY_CR_STATUS),
                    CrStatus.UNRECONCILED
                ),
                referenceNumber = getString(cursor, KEY_REFERENCE_NUMBER),
                accountLabel = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_LABEL)),
                accountType = enumValueOrDefault(
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_TYPE)),
                    AccountType.CASH
                ),
                transferPeerParent = cursor.getLongOrNull(KEY_TRANSFER_PEER_PARENT),
                tagList = cursor.getStringOrNull(KEY_TAGLIST),
                color = cursor.getInt(KEY_COLOR),
                status = cursor.getInt(KEY_STATUS),
                year = cursor.getInt(KEY_YEAR),
                month = cursor.getInt(KEY_MONTH),
                week = cursor.getInt(KEY_WEEK),
                day = cursor.getInt(KEY_DAY)
            )
        }
    }
}
