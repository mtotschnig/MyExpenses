package org.totschnig.myexpenses.next

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.next.core.ui.AmountFormatter

/** The currency as the features of the new UI know it */
fun CurrencyUnit.toInfo() = CurrencyInfo(code, symbol, fractionDigits)

/** The currency of the main module for the parts of the new UI that still work with it */
fun CurrencyInfo.toUnit() = CurrencyUnit(code, symbol, fractionDigits)

/** Formats amounts of the features like the rest of the app */
fun ICurrencyFormatter.toAmountFormatter() = AmountFormatter { amount, currency ->
    convAmount(amount, currency.toUnit())
}

/**
 * The home currency, again whenever the user changes it, see [NextData.homeCurrency]. Reading it may
 * query the currencies of the database, so it is read on [Dispatchers.IO] after the first value.
 */
fun homeCurrencyOf(context: Context, scope: CoroutineScope): StateFlow<CurrencyInfo> {
    val key = context.injector.prefHandler().getKey(PrefKey.HOME_CURRENCY)
    fun current() = context.injector.currencyContext().homeCurrencyUnit.toInfo()
    return callbackFlow {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) trySend(Unit)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
        .map { current() }
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, current())
}
