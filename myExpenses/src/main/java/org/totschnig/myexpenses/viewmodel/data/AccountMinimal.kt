package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.adapter.IdHolder

data class AccountMinimal(override val id: Long, val label: String, val currency: String): IdHolder {
    override fun toString(): String {
        return label
    }
}