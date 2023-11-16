package org.totschnig.myexpenses.model2

import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants

interface IAccount {
    val accountId: Long
    val currency: String

    val queryParameter: Pair<String, String>?
        get() = if (accountId != DataBaseAccount.HOME_AGGREGATE_ID) {
            if (accountId < 0) {
                DatabaseConstants.KEY_CURRENCY to currency
            } else {
                DatabaseConstants.KEY_ACCOUNTID to accountId.toString()
            }
        } else null
}