package org.totschnig.myexpenses.provider

import android.content.ContentValues
import org.totschnig.myexpenses.model.Grouping

data class BudgetInfo(
    val accountId: Long,
    val title: String,
    val amount: Long,
    val grouping: Grouping,
    val description: String = "",
    val start: String? = null,
    val end: String? = null
) {
    val contentValues: ContentValues = ContentValues().apply {
        put(DatabaseConstants.KEY_TITLE, title)
        put(DatabaseConstants.KEY_DESCRIPTION, description)
        put(DatabaseConstants.KEY_GROUPING, grouping.name)
        put(DatabaseConstants.KEY_BUDGET, amount)
        put(DatabaseConstants.KEY_ACCOUNTID, accountId)
        putNull(DatabaseConstants.KEY_CURRENCY)
        put(DatabaseConstants.KEY_START, start)
        put(DatabaseConstants.KEY_END, end)
    }
}