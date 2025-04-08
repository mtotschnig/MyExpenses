package org.totschnig.myexpenses.provider

import android.content.ContentValues
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEFAULT_ACTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE

data class TemplateInfo(
    val accountId: Long,
    val amount: Long,
    val title: String,
    val comment: String = "",
    val payeeId: Long? = null,
    val debtId: Long? = null,
    val catId: Long? = null,
    val methodId: Long? = null,
    val planId: Long? = null,
    val parentId: Long? = null
) {
    val contentValues: ContentValues
        get() = ContentValues().apply {
            put(KEY_COMMENT, comment)
            put(KEY_AMOUNT, amount)
            put(KEY_PAYEEID, payeeId)
            put(KEY_ACCOUNTID, accountId)
            put(KEY_TITLE, title)
            put(KEY_DEBT_ID, debtId)
            put(KEY_CATID, catId)
            put(KEY_METHODID, methodId)
            put(KEY_PLANID, planId)
            put(KEY_DEFAULT_ACTION, "SAVE")
            put(KEY_PARENTID, parentId)
        }
}
