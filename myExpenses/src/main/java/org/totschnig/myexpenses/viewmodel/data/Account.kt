package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import java.io.Serializable

data class Account(
    override val id: Long,
    val label: String,
    val currency: CurrencyUnit,
    val color: Int = -1,
    val type: AccountType,
    val criterion: Long?,
    val isDynamic: Boolean,
    val flag: AccountFlag,
    var currentBalance: Long,
) : IdHolder, Serializable {
    override fun toString(): String {
        return label
    }
}