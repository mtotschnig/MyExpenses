package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model2.AccountInfoWithGrouping

interface DistributionAccountInfo: AccountInfoWithGrouping {
    fun label(context: Context, currencyContext: CurrencyContext): String
    val color: Int
    override val grouping: Grouping
    override val typeId: Long?
    override val flagId: Long?
    override val accountGrouping: AccountGrouping<*>?
}