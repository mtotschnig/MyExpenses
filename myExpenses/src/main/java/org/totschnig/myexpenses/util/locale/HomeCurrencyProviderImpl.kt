package org.totschnig.myexpenses.util.locale

import android.content.Context
import org.totschnig.myexpenses.util.Utils
import java.util.*

open class HomeCurrencyProviderImpl : HomeCurrencyProvider {

    override fun getLocalCurrency(context: Context) =
        Utils.getCountryFromTelephonyManager(context)?.let {
            try {
                Currency.getInstance(Locale("", it))
            } catch (ignore: Exception) {
                null
            }
        } ?: Utils.getSaveDefault()
}