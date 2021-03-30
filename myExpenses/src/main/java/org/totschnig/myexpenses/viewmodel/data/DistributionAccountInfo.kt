package org.totschnig.myexpenses.viewmodel.data

import org.totschnig.myexpenses.model.CurrencyUnit

data class DistributionAccountInfo(val id: Long, val label: String, val currency: CurrencyUnit, val color: Int = -1)