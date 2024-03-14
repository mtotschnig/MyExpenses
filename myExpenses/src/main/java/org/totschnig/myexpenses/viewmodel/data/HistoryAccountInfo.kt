package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model2.AccountInfoWithGrouping

data class HistoryAccountInfo(
    override val accountId: Long,
    val label: String,
    val currencyUnit: CurrencyUnit,
    val color: Int,
    val openingBalance: Money,
    override val grouping: Grouping
): AccountInfoWithGrouping {
    override val currency: String = currencyUnit.code
}