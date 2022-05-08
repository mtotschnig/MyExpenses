package org.totschnig.myexpenses.provider

import android.content.ContentValues
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Model
import java.util.*

data class TransactionInfo @JvmOverloads constructor(
    val comment: String,
    val date: Date,
    val amount: Long,
    val accountId: Long,
    val payeeId: Long,
    val debtId: Long? = null,
    val catId : Long? = null,
    val methodId: Long? = null
) {
    val dateAsLong: Long
        get() = date.time / 1000
    val contentValues: ContentValues
        get() = ContentValues().apply {
            put(DatabaseConstants.KEY_COMMENT, comment)
            put(DatabaseConstants.KEY_DATE, dateAsLong)
            put(DatabaseConstants.KEY_VALUE_DATE, dateAsLong)
            put(DatabaseConstants.KEY_AMOUNT, amount)
            put(DatabaseConstants.KEY_PAYEEID, payeeId)
            put(DatabaseConstants.KEY_ACCOUNTID, accountId)
            put(DatabaseConstants.KEY_CR_STATUS, CrStatus.UNRECONCILED.name)
            put(DatabaseConstants.KEY_UUID, Model.generateUuid())
            put(DatabaseConstants.KEY_DEBT_ID, debtId)
            put(DatabaseConstants.KEY_CATID, catId)
            put(DatabaseConstants.KEY_METHODID, methodId)
        }
}
