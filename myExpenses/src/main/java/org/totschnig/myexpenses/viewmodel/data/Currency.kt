package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import org.totschnig.myexpenses.model.CurrencyEnum
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.getIntIfExistsOr0
import org.totschnig.myexpenses.util.Utils
import java.io.Serializable
import java.util.*

data class Currency(val code: String, val displayName: String, val usages: Int = 0) : Serializable {
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
            val code = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_CODE))
            val label = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_LABEL))
            val usages = cursor.getIntIfExistsOr0(DatabaseConstants.KEY_USAGES)
            return Currency(code, label ?: findDisplayName(code, locale), usages)
        }

        private fun findDisplayName(code: String, locale: Locale) = try {
            java.util.Currency.getInstance(code).getDisplayName(locale)
        } catch (ignored: IllegalArgumentException) {
            try {
                CurrencyEnum.valueOf(code).description
            } catch (ignored: IllegalArgumentException) {
                code
            }
        }
    }
}