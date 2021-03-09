package org.totschnig.myexpenses.testutils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandlerImpl

class TestPrefHandler(context: MyApplication, sharedPreferences: SharedPreferences, private val databaseName: String) : PrefHandlerImpl(context, sharedPreferences) {
    override fun setDefaultValues(context: Context) {
        PreferenceManager.setDefaultValues(context, databaseName, Context.MODE_PRIVATE,
                R.xml.preferences, true)
    }

    override fun preparePreferenceFragment(preferenceFragmentCompat: PreferenceFragmentCompat) {
        preferenceFragmentCompat.preferenceManager.sharedPreferencesName = databaseName
    }
}