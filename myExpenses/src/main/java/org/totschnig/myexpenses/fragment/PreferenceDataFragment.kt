package org.totschnig.myexpenses.fragment

import android.os.Bundle
import org.totschnig.myexpenses.R

class PreferenceDataFragment: BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_data, rootKey)
        unsetIconSpaceReservedRecursive(preferenceScreen)
    }
}