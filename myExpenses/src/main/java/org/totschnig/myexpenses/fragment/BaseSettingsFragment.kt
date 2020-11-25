package org.totschnig.myexpenses.fragment

import android.os.Bundle
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import java.lang.IllegalStateException
import javax.inject.Inject

abstract class BaseSettingsFragment: PreferenceFragmentCompat(), LocalizedFormatEditTextPreference.OnValidationErrorListener {
    @Inject
    lateinit var featureManager: FeatureManager
    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity().application as MyApplication).appComponent.inject(this)
    }

    override fun onValidationError(messageResId: Int) {
        activity().showSnackbar(messageResId)
    }

    fun activity() = activity as MyPreferenceActivity

    fun configureUninstallPrefs() {
        configureMultiSelectListPref(PrefKey.FEATURE_UNINSTALL_FEATURES, featureManager.installedFeatures()) { featureManager.uninstallFeatures(it) }
        configureMultiSelectListPref(PrefKey.FEATURE_UNINSTALL_LANGUAGES, featureManager.installedLanguages()) { featureManager.uninstallLanguages(it) }
    }

    private fun configureMultiSelectListPref(prefKey: PrefKey, entries: Set<String>, action: (Set<String>) -> Unit ) {
        (requirePreference(prefKey) as? MultiSelectListPreference)?.apply {
            if (entries.isEmpty()) {
                isEnabled = false
            } else {
                setOnPreferenceChangeListener { _, newValue ->
                    @Suppress("UNCHECKED_CAST")
                    (newValue as? Set<String>)?.let { action(it) }
                    false
                }
                entries.toTypedArray<CharSequence>().let {
                    setEntries(it)
                    entryValues = it
                }
            }
        }
    }

    fun <T: Preference> requirePreference(prefKey: PrefKey): T {
        return findPreference(prefHandler.getKey(prefKey)) ?: throw IllegalStateException("Preference not found")
    }
}