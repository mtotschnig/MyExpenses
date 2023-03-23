package org.totschnig.myexpenses.util.locale

import android.content.Context
import org.totschnig.myexpenses.util.Utils
import java.util.*

open class UserLocaleProviderImpl(override var systemLocale: Locale): UserLocaleProvider {

    override fun getUserPreferredLocale(): Locale = Locale.getDefault()

    override fun wrapContext(context: Context): Context = context

    override fun getLocalCurrency(context: Context) =
        Utils.getCountryFromTelephonyManager(context)?.let {
            try {
                Currency.getInstance(Locale("", it))
            } catch (ignore: Exception) {
                null
            }
        } ?: Utils.getSaveDefault()
}