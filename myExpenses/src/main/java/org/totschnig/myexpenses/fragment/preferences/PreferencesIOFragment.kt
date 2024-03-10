package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey

@Keep
class PreferencesIOFragment: BasePreferenceIOBRFragment() {

    override val preferencesResId = R.xml.preferences_io

    override fun setPreferencesFromResource(preferencesResId: Int, key: String?) {
        super.setPreferencesFromResource(preferencesResId, key)
        preferenceScreen.title = ioTitle
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_include_share)

        with(requirePreference<Preference>(PrefKey.IMPORT_QIF)) {
            summary = getString(R.string.pref_import_summary, "QIF")
            title = getString(R.string.pref_import_title, "QIF")
        }

        with(requirePreference<Preference>(PrefKey.IMPORT_CSV)) {
            summary = getString(R.string.pref_import_summary, "CSV")
            title = getString(R.string.pref_import_title, "CSV")
        }

        requirePreference<Preference>(PrefKey.CSV_EXPORT).title =
            getString(R.string.export_to_format, "CSV")

        requirePreference<Preference>(PrefKey.CSV_EXPORT_ORIGINAL_EQUIVALENT_AMOUNTS).summary =
            getString(R.string.menu_original_amount) + " / " + getString(R.string.menu_equivalent_amount)

        configureShareTargetPreference()
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        handleContrib(PrefKey.IMPORT_CSV, ContribFeature.CSV_IMPORT, preference) -> true
        else -> false
    }
}