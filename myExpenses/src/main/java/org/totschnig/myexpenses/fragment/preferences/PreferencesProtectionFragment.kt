package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey

class PreferencesProtectionFragment : BasePreferenceFragment() {

    override val preferencesResId = R.xml.preferences_protection

    override fun setPreferencesFromResource(preferencesResId: Int, key: String?) {
        super.setPreferencesFromResource(preferencesResId, key)
        preferenceScreen.title = protectionTitle
        requirePreference<Preference>(PrefKey.PROTECTION_LEGACY).title =
            getString(R.string.pref_protection_password_title) + " (" + getString(R.string.feature_deprecated)  + ")"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
    }
}