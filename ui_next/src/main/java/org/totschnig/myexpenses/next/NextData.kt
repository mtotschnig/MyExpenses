package org.totschnig.myexpenses.next

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.next.insights.ProviderLedgerSource
import org.totschnig.myexpenses.next.insights.today
import org.totschnig.myexpenses.next.core.model.ledger.LedgerSource

/**
 * The data of the new UI, once per process: the ledger and the facts of the areas, so that they
 * outlive the activity and widgets or notifications can read them without a screen. Created on
 * first use, since the dynamic feature cannot add its types to the app's dependency graph. Its
 * flows run in [scope] while they are collected, see `shareWhileShown`.
 *
 * Each area has one repository here, in the order they build on each other.
 */
class NextData private constructor(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val dataStore = context.injector.preferencesDataStore()

    /** The currency all amounts of the analyses are in, which the user can change */
    val homeCurrency: StateFlow<CurrencyInfo> = homeCurrencyOf(context, scope)

    val ledgerSource: LedgerSource = ProviderLedgerSource(context.contentResolver, dataStore, scope, today(context))

    companion object {
        @Volatile
        private var instance: NextData? = null

        fun of(context: Context): NextData = instance ?: synchronized(this) {
            instance ?: NextData(context.applicationContext).also { instance = it }
        }
    }
}
