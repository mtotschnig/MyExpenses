package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import org.totschnig.myexpenses.model.CurrencyUnit

interface DistributionAccountInfo {
    val id: Long
    fun label(context: Context): String
    val currency: CurrencyUnit
    val color: Int
}