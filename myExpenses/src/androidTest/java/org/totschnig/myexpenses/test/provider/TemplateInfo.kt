package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.*

data class TemplateInfo(
    val accountId: Long,
    val title: String,
    val amount: Long,
    val payeeId: Long? = null,
    val planId: Long? = null,
    val parentId: Long? = null
) {
    val contentValues: ContentValues = ContentValues().apply {
        put(KEY_TITLE, title)
        put(KEY_AMOUNT, amount)
        put(KEY_ACCOUNTID, accountId)
        put(KEY_PAYEEID, payeeId)
        put(KEY_PLANID, planId)
        put(KEY_DEFAULT_ACTION, "SAVE")
        put(KEY_PARENTID, parentId)
    }
}