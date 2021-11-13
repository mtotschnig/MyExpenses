package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.DEBT_LABEL_EXPRESSION
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.KEY_DEBT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.CATEGORY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.FULL_LABEL
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
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED
import org.totschnig.myexpenses.provider.DatabaseConstants.getExchangeRate
import org.totschnig.myexpenses.provider.DbUtils.getLongOr0L
import org.totschnig.myexpenses.provider.DbUtils.getLongOrNull
import org.totschnig.myexpenses.provider.DbUtils.getString
import org.totschnig.myexpenses.provider.checkSealedWithAlias
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.Utils
import java.io.File
import java.math.BigDecimal


data class Transaction(
    override val id: Long,
    val accountId: Long,
    override val amountRaw: Long,
    val amount: Money,
    val date: Long,
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
    val referenceNumber: String,
    val originTemplate: Template?,
    val isSealed: Boolean,
    val accountLabel: String,
    val accountType: AccountType,
    override val debtLabel: String?
) : SplitPartRVAdapter.ITransaction {
    val isSameCurrency: Boolean
        get() = transferAmount?.let { amount.currencyUnit == it.currencyUnit } ?: true
    override val isTransfer
        get() = transferPeer != null
    val isSplit
        get() = SPLIT_CATID == catId

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
            TRANSFER_CURRENCY,
            KEY_ACCOUNTID,
            KEY_METHODID,
            KEY_PARENTID,
            KEY_CR_STATUS,
            KEY_REFERENCE_NUMBER,
            KEY_CURRENCY,
            KEY_PICTURE_URI,
            PaymentMethod.localizedLabelSqlColumn(
                context,
                KEY_METHOD_LABEL
            ) + " AS " + KEY_METHOD_LABEL,
            KEY_STATUS,
            TRANSFER_AMOUNT(VIEW_EXTENDED),
            KEY_TEMPLATEID,
            KEY_UUID,
            KEY_ORIGINAL_AMOUNT,
            KEY_ORIGINAL_CURRENCY,
            KEY_EQUIVALENT_AMOUNT,
            CATEGORY_ICON,
            checkSealedWithAlias(VIEW_EXTENDED, TABLE_TRANSACTIONS),
            getExchangeRate(VIEW_EXTENDED, KEY_ACCOUNTID) + " AS " + KEY_EXCHANGE_RATE,
            KEY_ACCOUNT_LABEL,
            KEY_ACCOUNT_TYPE,
            DEBT_LABEL_EXPRESSION
        )

        fun fromCursor(cursor: Cursor, currencyContext: CurrencyContext): Transaction {
            val currencyUnit =
                currencyContext.get(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CURRENCY)))
            val amountRaw = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_AMOUNT))
            val money = Money(currencyUnit, amountRaw)
            val transferAccountId = getLongOrNull(cursor, KEY_TRANSFER_ACCOUNT)
            val date: Long = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DATE))
            val transferPeer = getLongOrNull(cursor, KEY_TRANSFER_PEER)
            val homeCurrency = Utils.getHomeCurrency()

            return Transaction(
                id = getLongOr0L(cursor, KEY_ROWID),
                accountId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ACCOUNTID)),
                amountRaw = amountRaw,
                amount = money,
                date = date,
                valueDate = getLongOrNull(cursor, KEY_VALUE_DATE) ?: date,
                comment = cursor.getStringOrNull(KEY_COMMENT),
                catId = getLongOrNull(cursor, KEY_CATID),
                payee = getString(cursor, KEY_PAYEE_NAME),
                methodLabel = cursor.getString(cursor.getColumnIndexOrThrow(KEY_METHOD_LABEL)),
                label = cursor.getStringOrNull(KEY_LABEL),
                transferPeer = transferPeer,
                transferAmount = transferAccountId?.let {
                    Money(
                        Account.getInstanceFromDb(it).currencyUnit,
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
                pictureUri = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PICTURE_URI))?.let { uri ->
                    var parsedUri = Uri.parse(uri)
                    if ("file" == parsedUri.scheme) { // Upgrade from legacy uris
                        parsedUri.path?.let {
                            try {
                                parsedUri = AppDirHelper.getContentUriForFile(File(it))
                            } catch (ignored: IllegalArgumentException) {
                            }
                        }
                    }
                    parsedUri
                },
                crStatus = try {
                    CrStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CR_STATUS)))
                } catch (ex: IllegalArgumentException) {
                    CrStatus.UNRECONCILED
                },
                referenceNumber = getString(cursor, KEY_REFERENCE_NUMBER),
                originTemplate = getLongOrNull(
                    cursor,
                    KEY_TEMPLATEID
                )?.let { Template.getInstanceFromDb(it) },
                isSealed = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEALED)) > 0,
                accountLabel = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACCOUNT_LABEL)),
                accountType = try {
                    AccountType.valueOf(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                KEY_ACCOUNT_TYPE
                            )
                        )
                    )
                } catch (ex: IllegalArgumentException) {
                    AccountType.CASH
                },
                hasTransferPeerParent = org.totschnig.myexpenses.model.Transaction.hasParent(
                    transferPeer
                ),
                debtLabel = cursor.getStringOrNull(KEY_DEBT_LABEL)
            )
        }
    }
}
