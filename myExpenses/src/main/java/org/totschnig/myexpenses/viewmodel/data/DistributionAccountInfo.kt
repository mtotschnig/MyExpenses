package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import org.totschnig.myexpenses.model2.IAccount

interface DistributionAccountInfo: IAccount {
    fun label(context: Context): String
    val color: Int
}