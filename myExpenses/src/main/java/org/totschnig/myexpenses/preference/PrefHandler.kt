package org.totschnig.myexpenses.preference

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.preference.PreferenceFragmentCompat
import kotlinx.serialization.json.Json
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.dialog.valueOf
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.toDayOfWeek
import org.totschnig.myexpenses.viewmodel.Account
import org.totschnig.myexpenses.viewmodel.Amount
import org.totschnig.myexpenses.viewmodel.Category
import org.totschnig.myexpenses.viewmodel.ColumnFeed
import org.totschnig.myexpenses.viewmodel.CombinedField
import org.totschnig.myexpenses.viewmodel.Date
import org.totschnig.myexpenses.viewmodel.Notes
import org.totschnig.myexpenses.viewmodel.OriginalAmount
import org.totschnig.myexpenses.viewmodel.Payee
import org.totschnig.myexpenses.viewmodel.Position
import org.totschnig.myexpenses.viewmodel.ReferenceNumber
import org.totschnig.myexpenses.viewmodel.Tags
import java.util.Calendar
import java.util.Locale

interface PrefHandler {
    fun getKey(key: PrefKey): String


    fun getString(key: String, defValue: String? = null): String?
    fun putString(key: String, value: String?)
    fun getString(key: PrefKey, defValue: String? = null): String? =
        getString(getKey(key), defValue)

    fun putString(key: PrefKey, value: String?) {
        putString(getKey(key), value)
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: PrefKey, defValue: Boolean): Boolean = getBoolean(getKey(key), defValue)
    fun putBoolean(key: PrefKey, value: Boolean) {
        putBoolean(getKey(key), value)
    }

    fun getInt(key: String, defValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getInt(key: PrefKey, defValue: Int): Int = getInt(getKey(key), defValue)
    fun putInt(key: PrefKey, value: Int) {
        putInt(getKey(key), value)
    }

    fun getLong(key: String, defValue: Long): Long
    fun putLong(key: String, value: Long)
    fun getLong(key: PrefKey, defValue: Long): Long = getLong(getKey(key), defValue)
    fun putLong(key: PrefKey, value: Long) {
        putLong(getKey(key), value)
    }

    fun getFloat(key: String, defValue: Float): Float
    fun getFloat(key: PrefKey, defValue: Float): Float = getFloat(getKey(key), defValue)


    fun getOrderedStringSet(key: String, separator: Char = ':'): Set<String>?
    fun getOrderedStringSet(key: PrefKey, separator: Char = ':') =
        getOrderedStringSet(getKey(key), separator)

    /**
     * @param separator no item in value must contain separator
     */
    fun putOrderedStringSet(key: String, value: Set<String>, separator: Char = ':')
    fun putOrderedStringSet(key: PrefKey, value: Set<String>, separator: Char = ':') {
        putOrderedStringSet(getKey(key), value, separator)
    }

    fun getStringSet(key: String): Set<String>?
    fun getStringSet(key: PrefKey) = getStringSet(getKey(key))
    fun putStringSet(key: String, value: Set<String>)
    fun putStringSet(key: PrefKey, value: Set<String>) {
        putStringSet(getKey(key), value)
    }

    fun remove(key: String)
    fun remove(key: PrefKey) {
        remove(getKey(key))
    }

    fun isSet(key: String): Boolean
    fun isSet(key: PrefKey): Boolean = isSet(getKey(key))

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
        } catch (_: NumberFormatException) {
            null
        } ?: 1

    val weekStart
        get() = try {
            getString(PrefKey.GROUP_WEEK_STARTS)?.toInt()
        } catch (_: NumberFormatException) {
            null
        }.takeIf { it in Calendar.SUNDAY..Calendar.SATURDAY }

    fun weekStartWithFallback(locale: Locale = Locale.getDefault()) =
        weekStart ?: Utils.getFirstDayOfWeek(locale)

    val weekStartAsDayOfWeek
        get() = weekStartWithFallback().toDayOfWeek

    fun uiMode(context: Context) = getString(
        PrefKey.UI_THEME,
        context.getString(R.string.pref_ui_theme_default)
    )

    val isProtected
        get() = getBoolean(PrefKey.PROTECTION_LEGACY, false) ||
                getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)

    val shouldSecureWindow
        get() = isProtected && !getBoolean(PrefKey.PROTECTION_ALLOW_SCREENSHOT, false)

    val defaultTransferCategory: Long?
        get() = getLong(PrefKey.DEFAULT_TRANSFER_CATEGORY, -1L).takeIf { it != -1L }

    val mainMenu: List<MenuItem>
        get() = getOrderedStringSet(PrefKey.CUSTOMIZE_MAIN_MENU)
            ?.let { stored ->
                stored.mapNotNull {
                    try {
                        MenuItem.valueOf(it)
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            }
            ?: MenuItem.defaultConfiguration

    val shouldDebug: Boolean
        get() = getBoolean(PrefKey.DEBUG_LOGGING, BuildConfig.DEBUG)

    val cloudStorage: String?
        get() = getString(PrefKey.AUTO_BACKUP_CLOUD)
            ?.takeIf { it != AccountPreference.SYNCHRONIZATION_NONE }

    companion object {
        const val AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX =
            "automatic_exchange_rate_download_"
        const val SERVICE_DEACTIVATED = "no"
    }
}

inline fun <reified T : Enum<T>> PrefHandler.enumValueOrDefault(prefKey: PrefKey, default: T): T =
    org.totschnig.myexpenses.util.enumValueOrDefault(getStringSafe(prefKey, default.name), default)

inline fun <reified T : Enum<T>> PrefHandler.enumValueOrDefault(prefKey: String, default: T): T =
    org.totschnig.myexpenses.util.enumValueOrDefault(getStringSafe(prefKey, default.name), default)

fun PrefHandler.getStringSafe(prefKey: PrefKey, default: String) = try {
    getString(prefKey, default)
} catch (_: ClassCastException) {
    default
}

fun PrefHandler.getStringSafe(prefKey: String, default: String) = try {
    getString(prefKey, default)
} catch (_: ClassCastException) {
    default
}

fun PrefHandler.saveIntList(key: PrefKey, list: List<Int>) {
    putString(key, list.joinToString(","))
}

fun PrefHandler.loadIntList(key: PrefKey) = getString(key, null)?.let { value ->
    value.split(",").mapNotNull { it.toIntOrNull() }
}

val printLayoutDefault = listOf(
    Date,
    ColumnFeed,
    Account, Category, Tags,
    ColumnFeed,
    Payee, CombinedField(listOf(ReferenceNumber, Notes)),
    ColumnFeed,
    OriginalAmount, Amount
)

val printLayoutDefaultColumnsWidths = listOf(150, 350, 350, 200)

var PrefHandler.printLayout: List<Position>
    get() = getString(PrefKey.PRINT_LAYOUT, null)?.let { Json.decodeFromString(it) }
        ?: printLayoutDefault
    set(value) {
        putString(PrefKey.PRINT_LAYOUT, Json.encodeToString(value))
    }

var PrefHandler.printLayoutColumnWidths: List<Int>
    get() = loadIntList(PrefKey.PRINT_LAYOUT_COLUMN_WIDTH)
        ?: printLayoutDefaultColumnsWidths
    set(value) {
        saveIntList(PrefKey.PRINT_LAYOUT_COLUMN_WIDTH, value)
    }

enum class ColorSource {
    TYPE, SIGN, TYPE_WITH_SIGN;

    fun transformType(type: Byte) = type.takeIf { it == FLAG_NEUTRAL } ?: when (this) {
        TYPE -> type
        TYPE_WITH_SIGN -> type.takeIf { it == FLAG_TRANSFER }
        SIGN -> null
    }
}