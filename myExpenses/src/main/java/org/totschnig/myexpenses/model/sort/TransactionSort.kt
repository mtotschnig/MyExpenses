package org.totschnig.myexpenses.model.sort

import androidx.annotation.StringRes
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_DATE

enum class TransactionSort(val sortDirection: SortDirection) {
    DATE_DESC(SortDirection.DESC),
    DATE_ASC(SortDirection.ASC),
    AMOUNT_DESC(SortDirection.DESC),
    AMOUNT_ASC(SortDirection.ASC);

    val column: String
        get() = when (this) {
            DATE_ASC, DATE_DESC -> KEY_DATE
            AMOUNT_ASC, AMOUNT_DESC -> KEY_AMOUNT
        }

    @get:StringRes
    val label: Int
        get() = when (this) {
            DATE_ASC, DATE_DESC -> R.string.date
            AMOUNT_ASC, AMOUNT_DESC -> R.string.amount
        }
}