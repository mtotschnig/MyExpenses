package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.model.CurrencyEnum
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_COMMODITY_TYPE
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_SYMBOL
import org.totschnig.myexpenses.provider.KEY_USAGES
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getIntIfExistsOr0
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.Utils
import java.util.Locale

@Parcelize
data class Currency(
    val code: String,
    val displayName: String,
    val symbol: String = "¤",
    val usages: Int = 0,
    val fractionDigits: Int = 2,
    val commodityType: CommodityType = CommodityType.FIAT
) : Parcelable {
    @IgnoredOnParcel
    val sortClass = when (code) {
        "XXX" -> 3
        "XAU", "XPD", "XPT", "XAG" -> 2
        else -> 1
    }

    override fun toString() = displayName

    override fun hashCode() = code.hashCode()

    override fun equals(other: Any?) = when {
        this === other -> true
        other !is Currency -> false
        code != other.code -> false
        else -> true
    }

    companion object {
        fun create(code: String, context: Context) = create(code, Utils.localeFromContext(context))

        fun create(code: String, locale: Locale) = Currency(code, findDisplayName(code, locale))

        fun create(cursor: Cursor, locale: Locale): Currency {
            val code = cursor.getString(KEY_CODE)
            val label = cursor.getStringOrNull(KEY_LABEL)
            val usages = cursor.getIntIfExistsOr0(KEY_USAGES)
            val commodityType = cursor.getEnum(KEY_COMMODITY_TYPE, CommodityType.FIAT)
            val symbol = cursor.getString(KEY_SYMBOL)
            return Currency(
                code = code,
                displayName = label ?: findDisplayName(code, locale),
                symbol = symbol,
                usages = usages,
                commodityType = commodityType
            )
        }

        private fun findDisplayName(code: String, locale: Locale) = try {
            java.util.Currency.getInstance(code).getDisplayName(locale)
        } catch (_: IllegalArgumentException) {
            try {
                CurrencyEnum.valueOf(code).description
            } catch (_: IllegalArgumentException) {
                code
            }
        }
    }
}