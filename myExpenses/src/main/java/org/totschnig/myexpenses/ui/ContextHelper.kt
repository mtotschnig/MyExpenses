package org.totschnig.myexpenses.ui

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

//https://stackoverflow.com/a/40849142/1199911
object ContextHelper {
    fun wrap(context: Context, newLocale: Locale) =
        context.resources?.configuration?.let {
            if (Build.VERSION.SDK_INT >= 24) {
                buildContext24(context, newLocale, it)
            } else {
                buildContext17(context, newLocale, it)
            }
        } ?: context

    private fun buildContext17(
        context: Context,
        newLocale: Locale,
        configuration: Configuration
    ) = context.createConfigurationContext(configuration.apply {
        //noinspection AppBundleLocaleChanges
        setLocale(newLocale)
    })

    @TargetApi(Build.VERSION_CODES.N)
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