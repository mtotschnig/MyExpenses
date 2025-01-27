package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.localizedQuote

@Keep
class PreferencesSyncFragment : BasePreferenceFragment() {

    override val preferencesResId = R.xml.preferences_sync

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        requirePreference<Preference>(PrefKey.MANAGE_SYNC_BACKENDS).summary = (getString(
            R.string.pref_manage_sync_backends_summary,
            BackendService.allAvailable(requireContext()).joinToString { it.label }
        ) +
                " " + ContribFeature.SYNCHRONIZATION.buildRequiresString(requireActivity()))

        requirePreference<Preference>(PrefKey.SYNC_NOTIFICATION).onPreferenceChangeListener =
            storeInDatabaseChangeListener

        requirePreference<Preference>(PrefKey.SYNC_WIFI_ONLY).onPreferenceChangeListener =
            storeInDatabaseChangeListener

        requirePreference<Preference>(PrefKey.SYNC_NOW_ALL).summary =
            getString(
                R.string.pref_sync_now_all_summary,
                preferenceActivity.localizedQuote(getString(R.string.menu_sync_now))
            )
        requirePreference<Preference>(PrefKey.SYNC_FREQUCENCY).summary =
            TextUtils.concatResStrings(requireContext(), R.string.pref_sync_frequency_summary, R.string.pref_sync_frequency_summary_0)
    }
}