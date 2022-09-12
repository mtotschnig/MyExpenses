package org.totschnig.myexpenses.model

import org.totschnig.myexpenses.R

/**
 * grouping of accounts in account list
 */
enum class AccountGrouping(val commandId: Int) {
    NONE(R.id.GROUPING_ACCOUNTS_NONE_COMMAND), TYPE(R.id.GROUPING_ACCOUNTS_TYPE_COMMAND), CURRENCY(R.id.GROUPING_ACCOUNTS_CURRENCY_COMMAND);
}