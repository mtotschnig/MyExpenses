package org.totschnig.myexpenses.provider

import android.content.ContentValues

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
