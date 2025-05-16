package org.totschnig.myexpenses.preference

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.totschnig.myexpenses.R
import androidx.core.content.edit

open class PrefHandlerImpl(
    private val context: Application,
    private val sharedPreferences: SharedPreferences,
) : PrefHandler {
    override fun getKey(key: PrefKey) =
        if (key.resId == 0) key._key!! else context.getString(key.resId)

    override fun getString(key: String, defValue: String?) =
        sharedPreferences.getString(key, defValue)
    override fun putString(key: String, value: String?) {
        sharedPreferences.edit { putString(key, value) }
    }

    override fun getBoolean(key: String, defValue: Boolean) =
        sharedPreferences.getBoolean(key, defValue)
    override fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    override fun getInt(key: String, defValue: Int) = sharedPreferences.getInt(key, defValue)
    override fun putInt(key: String, value: Int) {
        sharedPreferences.edit { putInt(key, value) }
    }

    override fun getLong(key: String, defValue: Long) =
        sharedPreferences.getLong(key, defValue)
    override fun putLong(key: String, value: Long) {
        sharedPreferences.edit { putLong(key, value) }
    }

    override fun getFloat(key: String, defValue: Float) =
        sharedPreferences.getFloat(key, defValue)

    override fun getOrderedStringSet(key: String, separator: Char) =
        sharedPreferences.getString(key, null)?.let {
            LinkedHashSet(it.split(separator))
        }

    override fun putOrderedStringSet(key: String, value: Set<String>, separator: Char) {
        require(value.none { it.contains(separator) }) { "Cannot marshall set if any value contains '$separator'" }
        sharedPreferences.edit { putString(key, value.joinToString(separator.toString())) }
    }

    override fun getStringSet(key: String): Set<String>? =  sharedPreferences.getStringSet(key, null)
    override fun putStringSet(key: String, value: Set<String>) {
        sharedPreferences.edit { putStringSet(key, value) }
    }

    override fun remove(key: String) {
        sharedPreferences.edit { remove(key) }
    }

    override fun isSet(key: String) = sharedPreferences.contains(key)

    override fun matches(key: String, vararg prefKeys: PrefKey) =
        prefKeys.any { key == getKey(it) }

    override fun setDefaultValues(context: Context) {
        setDefaultValues(context, R.xml.preferences_advanced)
        setDefaultValues(context, R.xml.preferences_attach_picture)
        setDefaultValues(context, R.xml.preferences_backup_restore)
        setDefaultValues(context, R.xml.preferences_data)
        setDefaultValues(context, R.xml.preferences_feedback)
        setDefaultValues(context, R.xml.preferences_include_share)
        setDefaultValues(context, R.xml.preferences_io)
        setDefaultValues(context, R.xml.preferences_ocr)
        setDefaultValues(context, R.xml.preferences_print)
        setDefaultValues(context, R.xml.preferences_protection)
        setDefaultValues(context, R.xml.preferences_sync)
        setDefaultValues(context, R.xml.preferences_ui)
        setDefaultValues(context, R.xml.preferences_web_ui)
    }

    open fun setDefaultValues(context: Context, resId: Int) {
        PreferenceManager.setDefaultValues(context, resId, true)
    }

    override fun preparePreferenceFragment(preferenceFragmentCompat: PreferenceFragmentCompat) {
        //NOOP overridden in test
    }
}