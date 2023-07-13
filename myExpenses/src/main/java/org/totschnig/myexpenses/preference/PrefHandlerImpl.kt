package org.totschnig.myexpenses.preference

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.totschnig.myexpenses.R

open class PrefHandlerImpl(
    private val context: Application,
    private val sharedPreferences: SharedPreferences
) : PrefHandler {
    override fun getKey(key: PrefKey): String {
        return if (key.resId == 0) key._key!! else context.getString(key.resId)
    }

    override fun getString(key: PrefKey, defValue: String?): String? {
        return getString(getKey(key), defValue)
    }

    override fun getString(key: String, defValue: String?): String? {
        return sharedPreferences.getString(key, defValue)
    }

    override fun putString(key: PrefKey, value: String?) {
        putString(getKey(key), value)
    }

    override fun putString(key: String, value: String?) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: PrefKey, defValue: Boolean): Boolean {
        return getBoolean(getKey(key), defValue)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defValue)
    }

    override fun putBoolean(key: PrefKey, value: Boolean) {
        putBoolean(getKey(key), value)
    }

    override fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    override fun getInt(key: PrefKey, defValue: Int): Int {
        return getInt(getKey(key), defValue)
    }

    override fun getInt(key: String, defValue: Int): Int {
        return sharedPreferences.getInt(key, defValue)
    }

    override fun putInt(key: PrefKey, value: Int) {
        putInt(getKey(key), value)
    }

    override fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    override fun getLong(key: PrefKey, defValue: Long): Long {
        return getLong(getKey(key), defValue)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return sharedPreferences.getLong(key, defValue)
    }

    override fun putLong(key: PrefKey, value: Long) {
        putLong(getKey(key), value)
    }

    override fun putLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    override fun remove(key: PrefKey) {
        remove(getKey(key))
    }

    override fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    override fun isSet(key: PrefKey): Boolean {
        return isSet(getKey(key))
    }

    override fun isSet(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    override fun matches(key: String, vararg prefKeys: PrefKey): Boolean {
        for (prefKey in prefKeys) {
            if (key == getKey(prefKey)) return true
        }
        return false
    }

    override fun setDefaultValues(context: Context) {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
    }

    override fun preparePreferenceFragment(preferenceFragmentCompat: PreferenceFragmentCompat) {
        //NOOP overridden in test
    }
}