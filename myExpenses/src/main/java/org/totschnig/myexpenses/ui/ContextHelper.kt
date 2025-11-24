package org.totschnig.myexpenses.ui

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.RequiresApi
import java.util.Locale

//https://stackoverflow.com/a/40849142/1199911
object ContextHelper {
    fun wrap(context: Context, newLocale: Locale) =
        context.resources?.configuration?.let {
            buildContext24(context, newLocale, it)
        } ?: context

    private fun buildContext24(
        context: Context,
        newLocale: Locale,
        configuration: Configuration
    ): Context {
        val localeList = LocaleList(newLocale)
        LocaleList.setDefault(localeList)
        return context.createConfigurationContext(configuration.apply {
            //noinspection AppBundleLocaleChanges
            setLocales(localeList)
        })
    }
}