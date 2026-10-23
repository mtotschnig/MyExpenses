package org.totschnig.myexpenses.preference

import androidx.preference.PreferenceDataStore

class PrefHandlerDataStore(
    private val prefHandler: PrefHandler
) : PreferenceDataStore() {

    override fun putString(key: String, value: String?) {
        prefHandler.putString(key, value)
    }

    override fun getString(key: String, defValue: String?): String? {
        return prefHandler.getString(key, defValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefHandler.putBoolean(key, value)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return prefHandler.getBoolean(key, defValue)
    }

    override fun putInt(key: String, value: Int) {
        prefHandler.putInt(key, value)
    }

    override fun getInt(key: String, defValue: Int): Int {
        return prefHandler.getInt(key, defValue)
    }

    override fun putLong(key: String, value: Long) {
        prefHandler.putLong(key, value)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return prefHandler.getLong(key, defValue)
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return prefHandler.getFloat(key, defValue)
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        prefHandler.putStringSet(key, values ?: emptySet())
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        return prefHandler.getStringSet(key) ?: defValues
    }
}
