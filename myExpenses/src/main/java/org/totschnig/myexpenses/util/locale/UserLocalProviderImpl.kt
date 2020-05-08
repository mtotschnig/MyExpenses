package org.totschnig.myexpenses.util.locale

import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import java.util.*
import javax.inject.Inject

class UserLocalProviderImpl(private val prefHandler: PrefHandler): UserLocaleProvider {
    override var systemLocale: Locale = Locale.getDefault()

    fun getDefaultLanguage(): String {
        return prefHandler.getString(PrefKey.UI_LANGUAGE, MyApplication.DEFAULT_LANGUAGE)
    }

    override fun getUserPreferredLocale(): Locale {
        return resolveLocale(getDefaultLanguage())
    }

    override fun resolveLocale(language: String): Locale = UserLocaleProvider.resolveLocale(language, systemLocale)
}