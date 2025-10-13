package org.totschnig.myexpenses.db2.entities

import android.content.ContentValues
import android.database.Cursor
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.splitStringList

data class Transaction(
    /** Corresponds to KEY_ROWID (integer primary key autoincrement) */
    val id: Long = 0,

    /** Corresponds to KEY_COMMENT (text) */
    val comment: String? = null,

    /** Corresponds to KEY_DATE (datetime not null) */
    val date: Long = System.currentTimeMillis() / 1000,

    /** Corresponds to KEY_VALUE_DATE (datetime not null) */
    val valueDate: Long = date,

    /** Corresponds to KEY_AMOUNT (integer not null) - Amount in minor units (e.g., cents) */
    val amount: Long,

    /** Corresponds to KEY_CATID (integer) - Foreign key to categories table */
    val categoryId: Long? = null,

    /** Corresponds to KEY_ACCOUNTID (integer not null) - Foreign key to accounts table */
    val accountId: Long,

    /** Corresponds to KEY_PAYEEID (integer) - Foreign key to payees table */
    val payeeId: Long? = null,

    /** Corresponds to KEY_TRANSFER_PEER (integer) - Self-referencing key for the other part of a transfer */
    val transferPeerId: Long? = null,

    /** Corresponds to KEY_TRANSFER_ACCOUNT (integer) - The ID of the account on the other side of a transfer */
    val transferAccountId: Long? = null,

    /** Corresponds to KEY_METHODID (integer) - Foreign key to methods table */
    val methodId: Long? = null,

    /** Corresponds to KEY_PARENTID (integer) - Self-referencing key for split transaction parents */
    val parentId: Long? = null,

    /** Corresponds to KEY_CR_STATUS (text not null) - Cleared/reconciled status */
    val crStatus: CrStatus = CrStatus.UNRECONCILED,

    /** Corresponds to KEY_REFERENCE_NUMBER (text) */
    val referenceNumber: String? = null,

    /** Corresponds to KEY_ORIGINAL_AMOUNT (integer) - Amount before currency conversion */
    val originalAmount: Long? = null,

    /** Corresponds to KEY_ORIGINAL_CURRENCY (text) - Currency code before conversion */
    val originalCurrency: String? = null,

    /** equivalent amount is stored in separate table by content provider */
    val equivalentAmount: Long? = null,

    /** Corresponds to KEY_DEBT_ID (integer) - Foreign key to debts table */
    val debtId: Long? = null,

    /** Read-only property holding the full category path, populated from a provider query. */
    val categoryPath: String? = null,

    /** Read-only property holding the list of linked category ids. */
    val tagList: List<Long> = emptyList(),

    /**
     * Read-only property holding the UUID of the transaction.
     */
    val uuid: String? = null,

    /**
     * Read-only property holding the currency of the transaction.
     */
    val currency: String? = null

) {

    fun asContentValues() = ContentValues().apply {
        put(KEY_COMMENT, comment)
        put(KEY_DATE, date)
        put(KEY_VALUE_DATE, valueDate)
        put(KEY_AMOUNT, amount)
        put(KEY_CATID, categoryId)
        put(KEY_ACCOUNTID, accountId)
        put(KEY_PAYEEID, payeeId?.takeIf { it > 0L })
        put(KEY_TRANSFER_ACCOUNT, transferAccountId?.takeIf { it > 0L })
        put(KEY_METHODID, methodId?.takeIf { it > 0L })
        put(KEY_PARENTID, parentId?.takeIf { it > 0L })
        put(KEY_CR_STATUS, crStatus.name)
        put(KEY_REFERENCE_NUMBER, referenceNumber)
        put(KEY_ORIGINAL_AMOUNT, originalAmount)
        put(KEY_ORIGINAL_CURRENCY, originalCurrency)
        put(KEY_EQUIVALENT_AMOUNT, equivalentAmount)
        put(KEY_DEBT_ID, debtId?.takeIf { it > 0L })
        put(KEY_UUID, requireNotNull(uuid))
    }

    val isTransfer: Boolean = transferAccountId != null

    val isSplit: Boolean = categoryId == DatabaseConstants.SPLIT_CATID

    val isSplitPart: Boolean = parentId != null

    companion object {

        val projection = arrayOf(
            KEY_ROWID,
            KEY_COMMENT,
            KEY_DATE,
            KEY_VALUE_DATE,
            KEY_AMOUNT,
            KEY_CATID,
            KEY_ACCOUNTID,
            KEY_PAYEEID,
            KEY_TRANSFER_PEER,
            KEY_TRANSFER_ACCOUNT,
            KEY_METHODID,
            KEY_PARENTID,
            KEY_CR_STATUS,
            KEY_REFERENCE_NUMBER,
            KEY_ORIGINAL_AMOUNT,
            KEY_ORIGINAL_CURRENCY,
            KEY_DEBT_ID,
            KEY_PATH,
            KEY_TAGLIST,
            KEY_UUID,
            KEY_EQUIVALENT_AMOUNT,
            KEY_CURRENCY
        )

        /**
         * Creates a Transaction object from the current row of a Cursor.
         * Assumes the cursor is already positioned at the correct row.
         */
        fun fromCursor(cursor: Cursor) = with(cursor) {
            Transaction(
                id = getLong(KEY_ROWID),
                comment = getString(KEY_COMMENT),
                date = getLong(KEY_DATE),
                valueDate = getLong(KEY_VALUE_DATE),
                amount = getLong(KEY_AMOUNT),
                categoryId = getLongOrNull(KEY_CATID),
                accountId = getLong(KEY_ACCOUNTID),
                payeeId = getLongOrNull(KEY_PAYEEID),
                transferPeerId = getLongOrNull(KEY_TRANSFER_PEER),
                transferAccountId = getLongOrNull(KEY_TRANSFER_ACCOUNT),
                methodId = getLongOrNull(KEY_METHODID),
                parentId = getLongOrNull(KEY_PARENTID),
                crStatus = CrStatus.valueOf(getString(KEY_CR_STATUS)),
                referenceNumber = getStringOrNull(KEY_REFERENCE_NUMBER),
                originalAmount = getLongOrNull(KEY_ORIGINAL_AMOUNT),
                originalCurrency = getStringOrNull(KEY_ORIGINAL_CURRENCY),
                debtId = getLongOrNull(KEY_DEBT_ID),
                categoryPath = getStringOrNull(KEY_PATH),
                tagList = splitStringList(KEY_TAGLIST).map { it.toLong() },
                uuid = getStringOrNull(KEY_UUID),
                equivalentAmount = getLongOrNull(KEY_EQUIVALENT_AMOUNT),
                currency = getStringOrNull(KEY_CURRENCY)
            )
        }
    }
}
