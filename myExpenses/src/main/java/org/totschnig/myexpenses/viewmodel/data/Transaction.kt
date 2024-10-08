package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.db2.loadTagsForTransaction
import org.totschnig.myexpenses.db2.localizedLabelSqlColumn
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.DEBT_LABEL_EXPRESSION
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.KEY_DEBT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT_HOME_EQUIVALENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IBAN
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER_PARENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_CURRENCY
import org.totschnig.myexpenses.provider.TRANSFER_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.requireLong
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
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
    override val categoryPath: String?,
    override val transferAccount: String?,
    val transferPeer: Long?,
    val transferAmount: Money?,
    val hasTransferPeerParent: Boolean,
    val originalAmount: Money?,
    val equivalentAmount: Money?,
    val crStatus: CrStatus,
    val referenceNumber: String?,
    val originTemplate: Template?,
    val isSealed: Boolean,
    val accountLabel: String,
    val accountType: AccountType,
    override val debtLabel: String?,
    override val tagList: List<Tag>,
    override val icon: String? = null,
    val iban: String? = null,
    val status: Int = STATUS_NONE,
) : SplitPartRVAdapter.ITransaction {
    val isSameCurrency: Boolean
        get() = transferAmount?.let { amount.currencyUnit == it.currencyUnit } ?: true
    val isSplit
        get() = SPLIT_CATID == catId
    val isArchive
        get() = status == STATUS_ARCHIVE

    companion object {
        fun projection(
            context: Context
        ) = arrayOf(
            KEY_ROWID,
            KEY_DATE,
            KEY_VALUE_DATE,
            KEY_AMOUNT,
            KEY_COMMENT,
            KEY_CATID,
            KEY_PATH,
            TRANSFER_ACCOUNT_LABEL,
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
            localizedLabelSqlColumn(
                context,
                KEY_METHOD_LABEL
            ) + " AS " + KEY_METHOD_LABEL,
            KEY_STATUS,
            KEY_TRANSFER_AMOUNT,
            KEY_TRANSFER_PEER_PARENT,
            KEY_TEMPLATEID,
            KEY_UUID,
            KEY_ORIGINAL_AMOUNT,
            KEY_ORIGINAL_CURRENCY,
            KEY_AMOUNT_HOME_EQUIVALENT,
            KEY_ICON,
            KEY_SEALED,
            KEY_EXCHANGE_RATE,
            KEY_ACCOUNT_LABEL,
            KEY_ACCOUNT_TYPE,
            DEBT_LABEL_EXPRESSION,
            KEY_IBAN
        )

        fun Cursor.readTransaction(
            context: Context,
            currencyContext: CurrencyContext,
            homeCurrency: CurrencyUnit
        ): Transaction {
            val currencyUnit = currencyContext[getString(KEY_CURRENCY)]
            val amountRaw = getLong(KEY_AMOUNT)
            val money = Money(currencyUnit, amountRaw)
            val transferAccountId = getLongOrNull(KEY_TRANSFER_ACCOUNT)
            val date: Long = getLong(KEY_DATE)
            val transferPeer = getLongOrNull(KEY_TRANSFER_PEER)
            val id = requireLong(KEY_ROWID)

            return Transaction(
                id = id,
                accountId = getLong(KEY_ACCOUNTID),
                amountRaw = amountRaw,
                amount = money,
                date = epoch2ZonedDateTime(date),
                valueDate = getLongOrNull(KEY_VALUE_DATE) ?: date,
                comment = getStringOrNull(KEY_COMMENT),
                catId = getLongOrNull(KEY_CATID),
                payee = getString(KEY_PAYEE_NAME),
                methodLabel = getStringOrNull(KEY_METHOD_LABEL),
                categoryPath = getStringOrNull(KEY_PATH, allowEmpty = true),
                transferAccount = getStringOrNull(KEY_TRANSFER_ACCOUNT_LABEL),
                transferPeer = transferPeer,
                transferAmount = transferAccountId?.let {
                    Money(
                        currencyContext[getString(KEY_TRANSFER_CURRENCY)],
                        getLong(KEY_TRANSFER_AMOUNT)
                    )
                },
                originalAmount = getLongOrNull(KEY_ORIGINAL_AMOUNT)?.let {
                    Money(currencyContext[getString(KEY_ORIGINAL_CURRENCY)], it)
                },
                equivalentAmount = Money(homeCurrency, getLong(KEY_AMOUNT_HOME_EQUIVALENT)),
                crStatus = enumValueOrDefault(
                    getStringOrNull(KEY_CR_STATUS),
                    CrStatus.UNRECONCILED
                ),
                referenceNumber = getStringOrNull(KEY_REFERENCE_NUMBER),
                originTemplate = getLongOrNull(KEY_TEMPLATEID)?.let {
                    Template.getInstanceFromDb(context.contentResolver, it)
                },
                isSealed = getInt(KEY_SEALED) > 0,
                accountLabel = getString(KEY_ACCOUNT_LABEL),
                accountType = enumValueOrDefault(
                    getStringOrNull(KEY_ACCOUNT_TYPE),
                    AccountType.CASH
                ),
                hasTransferPeerParent = getLongOrNull(KEY_TRANSFER_PEER_PARENT) != null,
                debtLabel = getStringOrNull(KEY_DEBT_LABEL),
                tagList = context.contentResolver.loadTagsForTransaction(id),
                icon = getStringOrNull(KEY_ICON),
                iban = getStringOrNull(KEY_IBAN),
                status = getInt(KEY_STATUS)
            )
        }
    }
}
