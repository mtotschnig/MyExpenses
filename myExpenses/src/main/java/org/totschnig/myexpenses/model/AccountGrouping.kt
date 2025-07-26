package org.totschnig.myexpenses.model

import androidx.annotation.StringRes
import org.totschnig.myexpenses.R

/**
 * grouping of accounts in account list
 */
enum class AccountGrouping(@param:StringRes val title: Int) {
    TYPE(R.string.type),
    CURRENCY(R.string.currency),
    NONE(R.string.grouping_none)
    ;
}