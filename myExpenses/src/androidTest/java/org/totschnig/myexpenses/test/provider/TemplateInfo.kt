package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import org.totschnig.myexpenses.provider.DatabaseConstants

data class TemplateInfo(val accountId: Long, val title: String,
                        val amount: Long, val payeeId: Long, val planId: Long) {
    val contentValues: ContentValues = ContentValues().apply {
        put(DatabaseConstants.KEY_TITLE, title)
        put(DatabaseConstants.KEY_AMOUNT, amount)
        put(DatabaseConstants.KEY_ACCOUNTID, accountId)
        put(DatabaseConstants.KEY_PAYEEID, payeeId)
        put(DatabaseConstants.KEY_PLANID, planId)
        put(DatabaseConstants.KEY_DEFAULT_ACTION, "SAVE")
    }
}