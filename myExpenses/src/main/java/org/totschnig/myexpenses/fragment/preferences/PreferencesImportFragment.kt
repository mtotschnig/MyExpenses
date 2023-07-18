package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey

@Keep
class PreferencesImportFragment: BasePreferenceFragment() {

    override val preferencesResId = R.xml.preferences_import

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        with(requirePreference<Preference>(PrefKey.IMPORT_QIF)) {
            summary = getString(R.string.pref_import_summary, "QIF")
            title = getString(R.string.pref_import_title, "QIF")
        }

        with(requirePreference<Preference>(PrefKey.IMPORT_CSV)) {
            summary = getString(R.string.pref_import_summary, "CSV")
            title = getString(R.string.pref_import_title, "CSV")
        }
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        handleContrib(PrefKey.IMPORT_CSV, ContribFeature.CSV_IMPORT, preference) -> true
        else -> false
    }
}