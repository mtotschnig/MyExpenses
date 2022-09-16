package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.KEY_DEBT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER_PARENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DbUtils.getLongOr0L
import org.totschnig.myexpenses.provider.DbUtils.getLongOrNull
import org.totschnig.myexpenses.provider.DbUtils.getString
import org.totschnig.myexpenses.provider.FULL_LABEL
import org.totschnig.myexpenses.provider.checkSealedWithAlias
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import java.io.File
import java.math.BigDecimal
import java.time.ZonedDateTime


data class Transaction2(
    val id: Long,
    val accountId: Long,
    val amountRaw: Long,
    val amount: Money,
    val date: ZonedDateTime,
    val valueDate: Long,
    val comment: String?,
    val catId: Long?,
    val payee: String,
    val methodLabel: String?,
    val label: String?,
    val transferPeer: Long?,
    val transferAmount: Money?,
    val hasTransferPeerParent: Boolean,
    val originalAmount: Money?,
    val equivalentAmount: Money?,
    val pictureUri: Uri?,
    val crStatus: CrStatus,
    val referenceNumber: String,
    val originTemplate: Template?,
    val isSealed: Boolean,
    val accountLabel: String,
    val accountType: AccountType,
    val debtLabel: String?,
    val tagList: String? = null
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
            DatabaseConstants.TRANSFER_CURRENCY,
            KEY_ACCOUNTID,
            DatabaseConstants.KEY_METHODID,
            DatabaseConstants.KEY_PARENTID,
            KEY_CR_STATUS,
            KEY_REFERENCE_NUMBER,
            KEY_CURRENCY,
            KEY_PICTURE_URI,
            PaymentMethod.localizedLabelSqlColumn(
                context,
                KEY_METHOD_LABEL
            ) + " AS " + KEY_METHOD_LABEL,
            DatabaseConstants.KEY_STATUS,
            DatabaseConstants.TRANSFER_AMOUNT(DatabaseConstants.VIEW_EXTENDED),
            "${DatabaseConstants.TRANSFER_PEER_PARENT} AS $KEY_TRANSFER_PEER_PARENT",
            KEY_TEMPLATEID,
            DatabaseConstants.KEY_UUID,
            KEY_ORIGINAL_AMOUNT,
            KEY_ORIGINAL_CURRENCY,
            KEY_EQUIVALENT_AMOUNT,
            DatabaseConstants.CATEGORY_ICON,
            checkSealedWithAlias(DatabaseConstants.VIEW_EXTENDED, DatabaseConstants.TABLE_TRANSACTIONS),
            DatabaseConstants.getExchangeRate(DatabaseConstants.VIEW_EXTENDED, KEY_ACCOUNTID) + " AS " + KEY_EXCHANGE_RATE,
            KEY_ACCOUNT_LABEL,
            KEY_ACCOUNT_TYPE,
            BaseTransactionProvider.DEBT_LABEL_EXPRESSION,
            KEY_TAGLIST,
            DatabaseConstants.getYearOfWeekStart() + " AS " + DatabaseConstants.KEY_YEAR_OF_WEEK_START,
            DatabaseConstants.getYearOfMonthStart() + " AS " + DatabaseConstants.KEY_YEAR_OF_MONTH_START,
            DatabaseConstants.YEAR + " AS " + DatabaseConstants.KEY_YEAR,
            DatabaseConstants.getMonth() + " AS " + DatabaseConstants.KEY_MONTH,
            DatabaseConstants.getWeek() + " AS " + DatabaseConstants.KEY_WEEK,
            DatabaseConstants.DAY + " AS " + DatabaseConstants.KEY_DAY,
        )

        fun fromCursor(
            context: Context,
            cursor: Cursor,
            currencyContext: CurrencyContext
        ): Transaction2 {
            val currencyUnit =
                currencyContext.get(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CURRENCY)))
            val amountRaw = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_AMOUNT))
            val money = Money(currencyUnit, amountRaw)
            val transferAccountId = getLongOrNull(cursor, KEY_TRANSFER_ACCOUNT)
            val date: Long = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DATE))
            val transferPeer = getLongOrNull(cursor, KEY_TRANSFER_PEER)
            val homeCurrency = Utils.getHomeCurrency()

            return Transaction2(
                id = getLongOr0L(cursor, KEY_ROWID),
                accountId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ACCOUNTID)),
                amountRaw = amountRaw,
                amount = money,
                date = epoch2ZonedDateTime(date),
                valueDate = getLongOrNull(cursor, KEY_VALUE_DATE) ?: date,
                comment = cursor.getStringOrNull(KEY_COMMENT),
                catId = getLongOrNull(cursor, KEY_CATID),
                payee = getString(cursor, KEY_PAYEE_NAME),
                methodLabel = cursor.getString(cursor.getColumnIndexOrThrow(KEY_METHOD_LABEL)),
                label = cursor.getStringOrNull(KEY_LABEL),
                transferPeer = transferPeer,
                transferAmount = transferAccountId?.let {
                    val transferCurrencyUnit =
                        currencyContext.get(
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(KEY_TRANSFER_CURRENCY)
                            )
                        )
                    Money(
                        transferCurrencyUnit,
                        cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TRANSFER_AMOUNT))
                    )
                },
                originalAmount = getLongOrNull(cursor, KEY_ORIGINAL_AMOUNT)?.let {
                    Money(
                        currencyContext.get(
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    KEY_ORIGINAL_CURRENCY
                                )
                            )
                        ), it
                    )
                },
                equivalentAmount = getLongOrNull(cursor, KEY_EQUIVALENT_AMOUNT)?.let {
                    Money(homeCurrency, it)
                }
                    ?: Money(
                        homeCurrency, money.amountMajor.multiply(
                            BigDecimal(
                                Utils.adjustExchangeRate(
                                    cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_EXCHANGE_RATE)),
                                    currencyUnit
                                )
                            )
                        )
                    ),
                pictureUri = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PICTURE_URI))
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
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_CR_STATUS)),
                    CrStatus.UNRECONCILED
                ),
                referenceNumber = getString(cursor, KEY_REFERENCE_NUMBER),
                originTemplate = getLongOrNull(
                    cursor,
                    KEY_TEMPLATEID
                )?.let { Template.getInstanceFromDb(it) },
                isSealed = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEALED)) > 0,
                accountLabel = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_LABEL)),
                accountType = enumValueOrDefault(
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_TYPE)),
                    AccountType.CASH
                ),
                hasTransferPeerParent = getLongOrNull(
                    cursor,
                    KEY_TRANSFER_PEER_PARENT
                ) != null,
                debtLabel = cursor.getStringOrNull(KEY_DEBT_LABEL),
                tagList = cursor.getStringOrNull(KEY_TAGLIST)
            )
        }
    }
}
