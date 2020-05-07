package org.totschnig.myexpenses.util.bundle

import java.util.*

interface LocaleManager {
    fun requestLocale(locale: Locale, onAvailable: () -> Unit) {
        onAvailable()
    }
}