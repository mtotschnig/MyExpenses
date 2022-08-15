package org.totschnig.myexpenses.util.locale

import android.content.Context
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.requireString
import org.totschnig.myexpenses.ui.ContextHelper
import org.totschnig.myexpenses.util.Utils
import java.util.*

open class UserLocaleProviderImpl(private val prefHandler: PrefHandler, override var systemLocale: Locale): UserLocaleProvider {
    private var currentPreferredLocale: Locale? = null
    override fun invalidate() {
        currentPreferredLocale = null
    }

    override fun getPreferredLanguage() =
        prefHandler.requireString(PrefKey.UI_LANGUAGE, MyApplication.DEFAULT_LANGUAGE)

    override fun getUserPreferredLocale() = currentPreferredLocale ?:
        UserLocaleProvider.resolveLocale(getPreferredLanguage(), systemLocale).also {
            currentPreferredLocale = it
        }

    override fun wrapContext(context: Context): Context =
        ContextHelper.wrap(context, getUserPreferredLocale())

    override fun getLocalCurrency(context: Context) =
        Utils.getCountryFromTelephonyManager(context)?.let {
            try {
                Currency.getInstance(Locale("", it))
            } catch (ignore: Exception) {
                null
            }
        } ?: Utils.getSaveDefault()
}