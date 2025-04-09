package org.totschnig.myexpenses.provider

import android.content.ContentValues
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.util.toEpoch
import java.time.LocalDateTime

data class TransactionInfo @JvmOverloads constructor(
    val accountId: Long,
    val amount: Long,
    val date: LocalDateTime = LocalDateTime.now(),
    val comment: String = "",
    val payeeId: Long? = null,
    val debtId: Long? = null,
    val catId: Long? = null,
    val methodId: Long? = null,
    val parentId: Long? = null,
    val crStatus: CrStatus = CrStatus.UNRECONCILED,
    val equivalentAmount: Long? = null
) {
    val dateAsLong: Long
        get() = date.toEpoch()
    val contentValues: ContentValues
        get() = ContentValues().apply {
            put(KEY_COMMENT, comment)
            put(KEY_DATE, dateAsLong)
            put(KEY_VALUE_DATE, dateAsLong)
            put(KEY_AMOUNT, amount)
            put(KEY_PAYEEID, payeeId)
            put(KEY_ACCOUNTID, accountId)
            put(KEY_CR_STATUS, crStatus.name)
            put(KEY_UUID, Model.generateUuid())
            put(KEY_DEBT_ID, debtId)
            put(KEY_CATID, catId)
            put(KEY_METHODID, methodId)
            put(KEY_PARENTID, parentId)
            equivalentAmount?.let { put(KEY_EQUIVALENT_AMOUNT, it) }
        }
}
