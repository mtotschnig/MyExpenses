package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.model.AccountType

data class AccountMinimal(override val id: Long, val label: String, val currency: String, val type: AccountType?): IdHolder {
    override fun toString(): String {
        return label
    }
}