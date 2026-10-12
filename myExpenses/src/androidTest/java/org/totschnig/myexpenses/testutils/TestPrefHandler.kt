package org.totschnig.myexpenses.testutils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandlerImpl

class TestPrefHandler(
    context: MyApplication,
    sharedPreferences: SharedPreferences,
    private val databaseName: String,
    private val forceEncryptProvider: () -> Boolean = { false }
) : PrefHandlerImpl(context, sharedPreferences) {

    override val encryptDatabase: Boolean
        get() = forceEncryptProvider() || super.encryptDatabase

    override fun setDefaultValues(context: Context, resId: Int) {
        PreferenceManager.setDefaultValues(context, databaseName, Context.MODE_PRIVATE,
            resId, true)
    }

    override fun preparePreferenceFragment(preferenceFragmentCompat: PreferenceFragmentCompat) {
        preferenceFragmentCompat.preferenceManager.sharedPreferencesName = databaseName
    }
}