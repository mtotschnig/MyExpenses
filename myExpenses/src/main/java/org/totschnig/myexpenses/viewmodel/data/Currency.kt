package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import android.os.Build
import org.totschnig.myexpenses.model.CurrencyEnum
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.Utils
import java.io.Serializable
import java.util.*

data class Currency(val code: String, val displayName: String) : Serializable {
    fun sortClass(): Int {
        return when (code) {
            "XXX" -> 3
            "XAU", "XPD", "XPT", "XAG" -> 2
            else -> 1
        }
    }

    override fun toString(): String {
        return displayName
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Currency) return false

        if (code != other.code) return false

        return true
    }

    companion object {
        fun create(code: String, context: Context?): Currency {
            return create(code, Utils.localeFromContext(context))
        }

        fun create(code: String, locale: Locale) = Currency(code, findDisplayName(code, locale))

        fun create(cursor: Cursor, locale: Locale) =
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_CODE)).let {
                    Currency(it,
                            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_LABEL))
                                    ?: findDisplayName(it, locale))
                }

        private fun findDisplayName(code: String, locale: Locale): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    return java.util.Currency.getInstance(code).getDisplayName(locale)
                } catch (ignored: IllegalArgumentException) {
                }
            }
            try {
                return CurrencyEnum.valueOf(code).description
            } catch (ignored: IllegalArgumentException) {
            }
            return code
        }
    }
}