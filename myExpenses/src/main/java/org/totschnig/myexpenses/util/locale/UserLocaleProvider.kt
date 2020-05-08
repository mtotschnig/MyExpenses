package org.totschnig.myexpenses.util.locale

import org.totschnig.myexpenses.MyApplication.DEFAULT_LANGUAGE
import java.util.*

interface UserLocaleProvider {
    fun getDefaultLanguage(): String
    fun getUserPreferredLocale(): Locale
    var systemLocale: Locale

    companion object {
        fun resolveLocale(language: String, systemLocale: Locale): Locale = if (language == DEFAULT_LANGUAGE) {
            systemLocale
        } else if (language.contains("-")) {
            val parts = language.split("-").toTypedArray()
            Locale(parts[0], parts[1])
        } else {
            Locale(language)
        }
    }
}