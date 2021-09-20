package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.provider.DatabaseConstants
import java.util.*

data class TransactionInfo @JvmOverloads constructor(
    val comment: String,
    val date: Date,
    val amount: Long,
    val accountId: Long,
    val payeeId: Long,
    val debtId: Long? = null
) {
    val dateAsLong: Long
        get() = date.time / 1000
    val contentValues: ContentValues
        get() {
            val v = ContentValues()
            v.put(DatabaseConstants.KEY_COMMENT, comment)
            v.put(DatabaseConstants.KEY_DATE, dateAsLong)
            v.put(DatabaseConstants.KEY_VALUE_DATE, dateAsLong)
            v.put(DatabaseConstants.KEY_AMOUNT, amount)
            v.put(DatabaseConstants.KEY_PAYEEID, payeeId)
            v.put(DatabaseConstants.KEY_ACCOUNTID, accountId)
            v.put(DatabaseConstants.KEY_CR_STATUS, CrStatus.UNRECONCILED.name)
            v.put(DatabaseConstants.KEY_UUID, Model.generateUuid())
            v.put(DatabaseConstants.KEY_DEBT_ID, debtId)
            return v
        }

}