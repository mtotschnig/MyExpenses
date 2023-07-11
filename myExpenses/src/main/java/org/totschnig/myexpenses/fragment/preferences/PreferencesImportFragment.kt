package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey

class PreferencesImportFragment: BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_import, rootKey)
        unsetIconSpaceReservedRecursive(preferenceScreen)

        with(requirePreference<Preference>(PrefKey.IMPORT_QIF)) {
            summary = getString(R.string.pref_import_summary, "QIF")
            title = getString(R.string.pref_import_title, "QIF")
        }

        with(requirePreference<Preference>(PrefKey.IMPORT_CSV)) {
            summary = getString(R.string.pref_import_summary, "CSV")
            title = getString(R.string.pref_import_title, "CSV")
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when {
            super.onPreferenceTreeClick(preference) -> true
            handleContrib(PrefKey.IMPORT_CSV, ContribFeature.CSV_IMPORT, preference) -> true
            else -> false
        }
    }
}