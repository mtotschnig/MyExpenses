package org.totschnig.myexpenses.util

import android.content.Context
import org.totschnig.myexpenses.model.CurrencyUnit
import java.util.*

object TextUtils {
    @JvmStatic
    fun <E : Enum<E>?> joinEnum(enumClass: Class<E>): String {
        val result = StringBuilder()
        val iterator: Iterator<E> = EnumSet.allOf(enumClass).iterator()
        while (iterator.hasNext()) {
            result.append("'").append(iterator.next()!!.name).append("'")
            if (iterator.hasNext()) result.append(",")
        }
        return result.toString()
    }

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
}