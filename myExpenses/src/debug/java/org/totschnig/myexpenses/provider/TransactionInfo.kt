package org.totschnig.myexpenses.provider

import android.content.ContentValues
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.generateUuid
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
            put(KEY_UUID, generateUuid())
            put(KEY_DEBT_ID, debtId)
            put(KEY_CATID, catId)
            put(KEY_METHODID, methodId)
            put(KEY_PARENTID, parentId)
            equivalentAmount?.let { put(KEY_EQUIVALENT_AMOUNT, it) }
        }
}
