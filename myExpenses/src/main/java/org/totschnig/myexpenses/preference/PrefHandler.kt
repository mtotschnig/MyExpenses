package org.totschnig.myexpenses.preference

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.preference.PreferenceFragmentCompat
import org.totschnig.myexpenses.util.Utils
import java.util.Calendar
import java.util.Locale

interface PrefHandler {
    fun getKey(key: PrefKey): String
    fun getString(key: PrefKey, defValue: String? = null): String?
    fun getString(key: String, defValue: String? = null): String?
    fun putString(key: PrefKey, value: String?)
    fun putString(key: String, value: String?)
    fun getBoolean(key: PrefKey, defValue: Boolean): Boolean
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun putBoolean(key: PrefKey, value: Boolean)
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: PrefKey, defValue: Int): Int
    fun getInt(key: String, defValue: Int): Int
    fun putInt(key: PrefKey, value: Int)
    fun putInt(key: String, value: Int)
    fun getLong(key: PrefKey, defValue: Long): Long
    fun getLong(key: String, defValue: Long): Long
    fun putLong(key: PrefKey, value: Long)
    fun putLong(key: String, value: Long)
    fun remove(key: PrefKey)
    fun remove(key: String)
    fun isSet(key: PrefKey): Boolean
    fun isSet(key: String): Boolean
    fun matches(key: String, vararg prefKeys: PrefKey): Boolean
    fun setDefaultValues(context: Context)
    fun preparePreferenceFragment(preferenceFragmentCompat: PreferenceFragmentCompat)

    fun getStringPreferencesKey(key: PrefKey) = stringPreferencesKey(getKey(key))
    fun getBooleanPreferencesKey(key: PrefKey) = booleanPreferencesKey(getKey(key))

    fun requireString(key: PrefKey, defaultValue: String) =
        getString(key, defaultValue)!!

    fun requireString(key: String, defaultValue: String) =
        getString(key, defaultValue)!!

    val encryptDatabase
        get() = getBoolean(PrefKey.ENCRYPT_DATABASE, false)

    val collate
        get() = if (encryptDatabase) "NOCASE" else "LOCALIZED"

    val monthStart
        get() = try {
            requireString((PrefKey.GROUP_MONTH_STARTS), "1").toInt()
                .takeIf { it in 1..31 }
        } catch (e: NumberFormatException) {
            null
        } ?: 1

    val weekStart
        get() = try {
            getString(PrefKey.GROUP_WEEK_STARTS)?.toInt()
        } catch (e: NumberFormatException) {
            null
        }.takeIf { it in Calendar.SUNDAY..Calendar.SATURDAY }

    fun weekStartWithFallback(locale: Locale) = weekStart ?: Utils.getFirstDayOfWeek(locale)

}

inline fun <reified T : Enum<T>> PrefHandler.enumValueOrDefault(prefKey: PrefKey, default: T): T =
    org.totschnig.myexpenses.util.enumValueOrDefault(getString(prefKey, default.name), default)

inline fun <reified T : Enum<T>> PrefHandler.enumValueOrDefault(prefKey: String, default: T): T =
    org.totschnig.myexpenses.util.enumValueOrDefault(getString(prefKey, default.name), default)