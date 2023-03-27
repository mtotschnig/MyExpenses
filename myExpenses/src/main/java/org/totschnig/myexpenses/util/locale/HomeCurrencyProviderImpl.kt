package org.totschnig.myexpenses.util.locale

import android.content.Context
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import java.util.*

open class HomeCurrencyProviderImpl(
    val prefHandler: PrefHandler,
    val context: Context,
    val currencyContext: CurrencyContext
) : HomeCurrencyProvider {
    override val homeCurrencyUnit: CurrencyUnit
        get() = currencyContext[homeCurrencyString]

    override val homeCurrencyString: String
        get() = prefHandler.getString(PrefKey.HOME_CURRENCY, null) ?: localCurrency.currencyCode

    override val localCurrency = Utils.getCountryFromTelephonyManager(context)?.let {
        try {
            Currency.getInstance(Locale("", it))
        } catch (ignore: Exception) {
            null
        }
    } ?: Utils.getSaveDefault()
}