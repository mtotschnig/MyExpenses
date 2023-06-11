package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.db2.localizedLabelSqlColumn
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.DEBT_LABEL_EXPRESSION
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.KEY_DEBT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.calculateRealExchangeRate
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import java.io.File
import java.math.BigDecimal
import java.time.ZonedDateTime


data class Transaction(
    override val id: Long,
    val accountId: Long,
    override val amountRaw: Long,
    val amount: Money,
    val date: ZonedDateTime,
    val valueDate: Long,
    override val comment: String?,
    val catId: Long?,
    val payee: String,
    val methodLabel: String?,
    override val label: String?,
    val transferPeer: Long?,
    val transferAmount: Money?,
    val hasTransferPeerParent: Boolean,
    val originalAmount: Money?,
    val equivalentAmount: Money?,
    val pictureUri: Uri?,
    val crStatus: CrStatus,
    val referenceNumber: String?,
    val originTemplate: Template?,
    val isSealed: Boolean,
    val accountLabel: String,
    val accountType: AccountType,
    override val debtLabel: String?,
    override val tagList: String? = null,
    override val icon: String? = null
) : SplitPartRVAdapter.ITransaction {
    val isSameCurrency: Boolean
        get() = transferAmount?.let { amount.currencyUnit == it.currencyUnit } ?: true
    override val isTransfer
        get() = transferPeer != null
    val isSplit
        get() = SPLIT_CATID == catId

    companion object {
        fun projection(context: Context, homeCurrency: String) = arrayOf(
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
            TRANSFER_CURRENCY,
            KEY_ACCOUNTID,
            KEY_METHODID,
            KEY_PARENTID,
            KEY_CR_STATUS,
            KEY_REFERENCE_NUMBER,
            KEY_CURRENCY,
            KEY_PICTURE_URI,
            localizedLabelSqlColumn(
                context,
                KEY_METHOD_LABEL
            ) + " AS " + KEY_METHOD_LABEL,
            KEY_STATUS,
            TRANSFER_AMOUNT(VIEW_EXTENDED),
            "$TRANSFER_PEER_PARENT AS $KEY_TRANSFER_PEER_PARENT",
            KEY_TEMPLATEID,
            KEY_UUID,
            KEY_ORIGINAL_AMOUNT,
            KEY_ORIGINAL_CURRENCY,
            KEY_EQUIVALENT_AMOUNT,
            KEY_ICON,
            checkSealedWithAlias(VIEW_EXTENDED, TABLE_TRANSACTIONS),
            getExchangeRate(VIEW_EXTENDED, KEY_ACCOUNTID, homeCurrency) + " AS " + KEY_EXCHANGE_RATE,
            KEY_ACCOUNT_LABEL,
            KEY_ACCOUNT_TYPE,
            DEBT_LABEL_EXPRESSION,
            KEY_TAGLIST
        )

        fun fromCursor(
            context: Context,
            cursor: Cursor,
            currencyContext: CurrencyContext,
            homeCurrency: CurrencyUnit
        ): Transaction {
            val currencyUnit =
                currencyContext.get(cursor.getString(KEY_CURRENCY))
            val amountRaw = cursor.getLong(KEY_AMOUNT)
            val money = Money(currencyUnit, amountRaw)
            val transferAccountId = cursor.getLongOrNull(KEY_TRANSFER_ACCOUNT)
            val date: Long = cursor.getLong(KEY_DATE)
            val transferPeer = cursor.getLongOrNull(KEY_TRANSFER_PEER)

            return Transaction(
                id = cursor.requireLong(KEY_ROWID),
                accountId = cursor.getLong(KEY_ACCOUNTID),
                amountRaw = amountRaw,
                amount = money,
                date = epoch2ZonedDateTime(date),
                valueDate = cursor.getLongOrNull(KEY_VALUE_DATE) ?: date,
                comment = cursor.getStringOrNull(KEY_COMMENT),
                catId = cursor.getLongOrNull(KEY_CATID),
                payee = cursor.getString(KEY_PAYEE_NAME),
                methodLabel = cursor.getStringOrNull(KEY_METHOD_LABEL),
                label = cursor.getStringOrNull(KEY_LABEL),
                transferPeer = transferPeer,
                transferAmount = transferAccountId?.let {
                    val transferCurrencyUnit =
                        currencyContext.get(
                            cursor.getString(KEY_TRANSFER_CURRENCY)
                        )
                    Money(
                        transferCurrencyUnit,
                        cursor.getLong(KEY_TRANSFER_AMOUNT)
                    )
                },
                originalAmount = cursor.getLongOrNull(KEY_ORIGINAL_AMOUNT)?.let {
                    Money(
                        currencyContext.get(
                            cursor.getString(KEY_ORIGINAL_CURRENCY)
                        ), it
                    )
                },
                equivalentAmount = cursor.getLongOrNull(KEY_EQUIVALENT_AMOUNT)?.let {
                    Money(homeCurrency, it)
                }
                    ?: Money(
                        homeCurrency, money.amountMajor.multiply(
                            BigDecimal(
                                calculateRealExchangeRate(
                                    cursor.getDouble(KEY_EXCHANGE_RATE),
                                    currencyUnit, homeCurrency
                                )
                            )
                        )
                    ),
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
                    cursor.getStringOrNull(KEY_CR_STATUS),
                    CrStatus.UNRECONCILED
                ),
                referenceNumber = cursor.getStringOrNull(KEY_REFERENCE_NUMBER),
                originTemplate = cursor.getLongOrNull(KEY_TEMPLATEID)?.let {
                    Template.getInstanceFromDb(it)
                },
                isSealed = cursor.getInt(KEY_SEALED) > 0,
                accountLabel = cursor.getString(KEY_ACCOUNT_LABEL),
                accountType = enumValueOrDefault(
                    cursor.getStringOrNull(KEY_ACCOUNT_TYPE),
                    AccountType.CASH
                ),
                hasTransferPeerParent = cursor.getLongOrNull(KEY_TRANSFER_PEER_PARENT) != null,
                debtLabel = cursor.getStringOrNull(KEY_DEBT_LABEL),
                tagList = cursor.splitStringList(KEY_TAGLIST).joinToString(),
                icon = cursor.getStringOrNull(KEY_ICON)
            )
        }
    }
}
