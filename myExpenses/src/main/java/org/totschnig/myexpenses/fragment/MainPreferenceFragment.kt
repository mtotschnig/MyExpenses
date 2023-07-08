package org.totschnig.myexpenses.fragment

import android.os.Bundle
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey

class MainPreferenceFragment : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_headers, rootKey)
        requirePreference<Preference>(PrefKey.CATEGORY_BACKUP_EXPORT).title =
            getString(R.string.pref_category_title_export) + " / " + getString(R.string.menu_backup)
        unsetIconSpaceReservedRecursive(preferenceScreen)
    }
}