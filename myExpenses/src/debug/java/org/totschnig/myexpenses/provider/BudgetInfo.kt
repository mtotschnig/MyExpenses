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
        put(KEY_TITLE, title)
        put(KEY_DESCRIPTION, description)
        put(KEY_GROUPING, grouping.name)
        put(KEY_BUDGET, amount)
        put(KEY_ACCOUNTID, accountId)
        putNull(KEY_CURRENCY)
        put(KEY_START, start)
        put(KEY_END, end)
    }
}