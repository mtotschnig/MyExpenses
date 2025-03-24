package org.totschnig.myexpenses.util

import android.content.Context
import android.content.res.Resources
import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.ConfigurationCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.licence.LicenceStatus
import java.util.EnumSet
import java.util.Locale

object TextUtils {
    @JvmStatic
    fun <E : Enum<E>> joinEnum(enumClass: Class<E>): String =
        EnumSet.allOf(enumClass).joinToString(",") { "'${it.name}'" }

    @JvmStatic
    fun concatResStrings(ctx: Context, vararg resIds: Int): String =
        concatResStrings(ctx, " ", *resIds)

    @JvmStatic
    fun concatResStrings(ctx: Context, separator: String, vararg resIds: Int): String =
        resIds.joinToString(separator) { ctx.getString(it) }

    @JvmStatic
    fun appendCurrencySymbol(context: Context, resId: Int, currency: CurrencyUnit): String {
        return appendText(context, resId, currency.symbol)
    }

    fun appendCurrencyDescription(context: Context, resId: Int, currency: CurrencyUnit): String {
        return appendText(context, resId, currency.description)
    }

    private fun appendText(context: Context, resId: Int, symbol: String?): String {
        return String.format(Locale.ROOT, "%s (%s)", context.getString(resId), symbol)
    }

    fun formatQifCategory(mainLabel: String?, subLabel: String?): String? {
        val mainLabelSanitized = sanitizeQifCategory(mainLabel)
        val subLabelSanitized = sanitizeQifCategory(subLabel)
        return if (subLabelSanitized.isNullOrEmpty()) mainLabelSanitized else String.format(
            "%s:%s",
            mainLabelSanitized,
            subLabelSanitized
        )
    }

    private fun sanitizeQifCategory(label: String?): String? {
        val substitute = '|'
        return label?.replace('/', substitute)?.replace(':', substitute)
    }

    fun String.withAmountColor(resources: Resources, sign: Int): CharSequence =
        if (sign == 0) this else
            SpannableString(this).apply {
                setSpan(
                    ForegroundColorSpan(
                        ResourcesCompat.getColor(
                            resources,
                            if (sign > 0) R.color.colorIncome else R.color.colorExpense,
                            null
                        )
                    ), 0, length, 0
                )
            }

    fun getContribFeatureLabelsAsList(ctx: Context, type: LicenceStatus) =
        ContribFeature.entries.filter { feature: ContribFeature -> feature.licenceStatus === type }
            .map { feature: ContribFeature -> ctx.getText(feature.labelResId) }
}

fun getDisplayNameForScript(context: Context, script: String) =
    getDisplayNameForScript(Utils.localeFromContext(context), script)

fun getDisplayNameForScript(locale: Locale, script: String): String =
    when (script) {
        "Han" -> Locale.CHINESE.getDisplayLanguage(locale)
        else -> Locale.Builder().setScript(script).build().getDisplayScript(locale)
    }

fun Context.getLocale(): Locale =
    ConfigurationCompat.getLocales(resources.configuration).get(0) ?: Locale.getDefault()

val Context.quotationStart: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        LocaleData.getInstance(ULocale.forLocale(getLocale()))
            .getDelimiter(LocaleData.QUOTATION_START) else "\""
val Context.quotationEnd: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        LocaleData.getInstance(ULocale.forLocale(getLocale()))
            .getDelimiter(LocaleData.QUOTATION_END) else "\""

fun Context.localizedQuote(input: String) = "$quotationStart$input$quotationEnd"

/**
 * surrounds text with Unicode isolate characters in order to prevent it from having affect on surrounding text
 *
 */
fun isolateText(text: String) = "\u2068$text\u2069"
