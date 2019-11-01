package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants

data class BudgetInfo(val accountId: Long, val title: String, val description: String?,
                      val amount: Long, val grouping: Grouping) {
    val contentValues: ContentValues = ContentValues().apply {
        put(DatabaseConstants.KEY_TITLE, title)
        put(DatabaseConstants.KEY_DESCRIPTION, description)
        put(DatabaseConstants.KEY_GROUPING, grouping.name)
        put(DatabaseConstants.KEY_BUDGET, amount)
        put(DatabaseConstants.KEY_ACCOUNTID, accountId)
        putNull(DatabaseConstants.KEY_CURRENCY)
        putNull(DatabaseConstants.KEY_START)
        putNull(DatabaseConstants.KEY_END)
    }
}