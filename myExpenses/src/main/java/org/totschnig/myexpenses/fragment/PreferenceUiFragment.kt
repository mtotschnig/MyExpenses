package org.totschnig.myexpenses.fragment

import android.os.Bundle
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.FontSizeDialogFragmentCompat
import org.totschnig.myexpenses.preference.FontSizeDialogPreference

class PreferenceUiFragment: BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_ui, rootKey)
        unsetIconSpaceReservedRecursive(preferenceScreen)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is FontSizeDialogPreference) {
            FontSizeDialogFragmentCompat.newInstance(preference.key).also {
                it.setTargetFragment(this, 0)
                it.show(
                        parentFragmentManager,
                        "android.support.v7.preference.PreferenceFragment.DIALOG"
                    )
            }
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}