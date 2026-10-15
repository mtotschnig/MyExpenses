package org.totschnig.myexpenses.model.sort

import androidx.annotation.StringRes
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_DATE

enum class TransactionSort(val commandId: Int) {
    DATE_DESC(R.id.SORT_BY_DATE_DESCENDING_COMMAND),
    DATE_ASC(R.id.SORT_BY_DATE_ASCENDING_COMMAND),
    AMOUNT_DESC(R.id.SORT_BY_AMOUNT_DESCENDING_COMMAND),
    AMOUNT_ASC(R.id.SORT_BY_AMOUNT_ASCENDING_COMMAND);

    val column: String
        get() = when (this) {
            DATE_ASC, DATE_DESC -> KEY_DATE
            AMOUNT_ASC, AMOUNT_DESC -> KEY_AMOUNT
        }

    val sortDirection: SortDirection
        get() = when (this) {
            AMOUNT_DESC, DATE_DESC -> SortDirection.DESC
            AMOUNT_ASC, DATE_ASC -> SortDirection.ASC
        }

    @get:StringRes
    val label: Int
        get() = when (this) {
            DATE_ASC, DATE_DESC -> R.string.date
            AMOUNT_ASC, AMOUNT_DESC -> R.string.amount
        }

    companion object {
        fun fromCommandId(id: Int): TransactionSort? = TransactionSort.entries.find { it.commandId == id }
    }
}