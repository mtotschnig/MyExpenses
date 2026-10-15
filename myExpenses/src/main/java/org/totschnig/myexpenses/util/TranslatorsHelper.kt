package org.totschnig.myexpenses.util

import android.annotation.SuppressLint
import android.content.Context
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.preferences.PreferenceUiFragment.Companion.getLocaleDisplayName
import java.util.Locale

object TranslatorsHelper {

    @SuppressLint("DiscouragedApi")
    fun getTranslatorsArrayResId(context: Context, language: String, country: String?): Int {
        var result = 0
        val prefix = "translators_"
        val resources = context.resources
        val packageName = context.packageName
        if (language.isNotEmpty()) {
            if (!country.isNullOrEmpty()) {
                result = resources.getIdentifier(
                    prefix + language + "_" + country,
                    "array", packageName
                )
            }
            if (result == 0) {
                result = resources.getIdentifier(
                    prefix + language,
                    "array", packageName
                )
            }
        }
        return result
    }

    fun Context.buildTranslationCredits() =
        resources.getStringArray(R.array.pref_ui_language_values)
            .map { lang ->
                val parts = lang.split("-".toRegex()).toTypedArray()
                val resId = getTranslatorsArrayResId(
                    this,
                    parts[0],
                    if (parts.size == 2) parts[1].lowercase(Locale.ROOT) else null
                )
                lang to resId
            }
            .filter { it.second != 0 }
            .map {
                "${getLocaleDisplayName(it.first)}: ${
                    resources.getStringArray(it.second).joinToString(", ")
                }"
            }
}
