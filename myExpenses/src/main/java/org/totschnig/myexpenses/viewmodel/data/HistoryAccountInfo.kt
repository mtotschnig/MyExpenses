package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money

data class HistoryAccountInfo(val id: Long, val label: String, val currency: CurrencyUnit, val color: Int = -1, val openingBalance: Money)