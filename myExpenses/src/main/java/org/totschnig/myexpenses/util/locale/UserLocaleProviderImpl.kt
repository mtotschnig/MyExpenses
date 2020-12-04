package org.totschnig.myexpenses.util.locale

import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.requireString
import java.util.*

class UserLocaleProviderImpl(private val prefHandler: PrefHandler, locale: Locale): UserLocaleProvider {
    override var systemLocale: Locale = locale

    override fun getPreferredLanguage(): String {
        return prefHandler.requireString(PrefKey.UI_LANGUAGE, MyApplication.DEFAULT_LANGUAGE)
    }

    override fun getUserPreferredLocale(): Locale {
        return UserLocaleProvider.resolveLocale(getPreferredLanguage(), systemLocale)
    }
}