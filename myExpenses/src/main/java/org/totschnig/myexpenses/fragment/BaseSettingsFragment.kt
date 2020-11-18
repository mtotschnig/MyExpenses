package org.totschnig.myexpenses.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.validateDateFormat
import javax.inject.Inject

abstract class BaseSettingsFragment: PreferenceFragmentCompat() {
    @Inject
    lateinit var featureManager: FeatureManager
    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity().application as MyApplication).appComponent.inject(this)
    }

    @SuppressLint("SimpleDateFormat")
    fun validateDateFormatWithFeedback(dateFormat: String) = validateDateFormat(dateFormat)?.let {
        activity().showSnackbar(it)
        false
    } ?: true

    fun activity() = activity as MyPreferenceActivity

    fun configureUninstallPrefs() {
        configureMultiSelectListPref(PrefKey.FEATURE_UNINSTALL_FEATURES, featureManager.installedFeatures()) { featureManager.uninstallFeatures(it) }
        configureMultiSelectListPref(PrefKey.FEATURE_UNINSTALL_LANGUAGES, featureManager.installedLanguages()) { featureManager.uninstallLanguages(it) }
    }

    private fun configureMultiSelectListPref(prefKey: PrefKey, entries: Set<String>, action: (Set<String>) -> Unit ) {
        (findPreference(prefKey) as? MultiSelectListPreference)?.apply {
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

    fun findPreference(prefKey: PrefKey): Preference? {
        return findPreference(prefHandler.getKey(prefKey))
    }
}