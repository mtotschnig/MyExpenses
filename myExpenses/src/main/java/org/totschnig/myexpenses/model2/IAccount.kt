package org.totschnig.myexpenses.model2

import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants

interface IAccount {
    val id: Long
    val currency: String

    val queryParameter: Pair<String, String>?
        get() = if (id != DataBaseAccount.HOME_AGGREGATE_ID) {
            if (id < 0) {
                DatabaseConstants.KEY_CURRENCY to currency
            } else {
                DatabaseConstants.KEY_ACCOUNTID to id.toString()
            }
        } else null
}