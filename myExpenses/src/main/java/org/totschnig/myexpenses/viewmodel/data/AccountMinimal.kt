package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.adapter.IAccount

data class AccountMinimal(override val id: Long, val label: String, val currency: String): IAccount {
    override fun toString(): String {
        return label
    }
}