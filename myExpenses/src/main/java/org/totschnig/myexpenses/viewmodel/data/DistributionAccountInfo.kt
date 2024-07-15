package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.IAccount
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants

interface DistributionAccountInfo: IAccount {
    fun label(context: Context): String
    val color: Int
}