package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.XmlRes
import androidx.fragment.app.activityViewModels
import androidx.preference.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PreferenceActivity
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.*
import org.totschnig.myexpenses.preference.PreferenceDataStore
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import javax.inject.Inject

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var featureManager: FeatureManager

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var preferenceDataStore: PreferenceDataStore

    @Inject
    lateinit var licenceHandler: LicenceHandler

    @get:XmlRes
    abstract val preferencesResId: Int

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(preferencesResId, rootKey)
        unsetIconSpaceReservedRecursive(preferenceScreen)
    }

    val preferenceActivity get() = requireActivity() as PreferenceActivity

    val viewModel: SettingsViewModel by activityViewModels()

    val storeInDatabaseChangeListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
            preferenceActivity.showSnackBarIndefinite(R.string.saving)
            viewModel.storeSetting(preference.key, newValue.toString())
                .observe(this@BasePreferenceFragment) { result ->
                    preferenceActivity.dismissSnackBar()
                    if ((!result)) preferenceActivity.showSnackBar("ERROR")
                }
            true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        with(requireActivity().injector) {
            inject(this@BasePreferenceFragment)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val key = preference.key
        if (matches(preference, PrefKey.AUTO_BACKUP_CLOUD)) {
            if ((preference as ListPreference).entries.size == 1) {
                preferenceActivity.showSnackBar(R.string.no_sync_backends)
                return
            }
        }
        val fragment = when {
            preference is TimePreference -> TimePreferenceDialogFragmentCompat.newInstance(key)
            preference is FontSizeDialogPreference -> FontSizeDialogFragmentCompat.newInstance(key)
            matches(preference, PrefKey.MANAGE_APP_DIR_FILES) ->
                MultiSelectListPreferenceDialogFragment2.newInstance(key)
            else -> null
        }

        if (fragment != null) {
            fragment.setTargetFragment(this, 0)
            fragment.show(
                parentFragmentManager,
                "android.support.v7.preference.PreferenceFragment.DIALOG"
            )
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    fun <T : Preference> findPreference(prefKey: PrefKey): T? =
        findPreference(prefHandler.getKey(prefKey))

    fun <T : Preference> requirePreference(prefKey: PrefKey): T {
        return findPreference(prefKey)
            ?: throw IllegalStateException("Preference not found")
    }

    fun matches(preference: Preference, vararg prefKey: PrefKey) =
        prefKey.any { prefHandler.getKey(it) == preference.key }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        trackPreferenceClick(preference)
        return super.onPreferenceTreeClick(preference)
    }

    private fun trackPreferenceClick(preference: Preference) {
        val bundle = Bundle()
        bundle.putString(Tracker.EVENT_PARAM_ITEM_ID, preference.key)
        preferenceActivity.logEvent(Tracker.EVENT_PREFERENCE_CLICK, bundle)
    }

    fun unsetIconSpaceReservedRecursive(preferenceGroup: PreferenceGroup) {
        for (i in 0 until preferenceGroup.preferenceCount) {
            val preference = preferenceGroup.getPreference(i)
            if (preference is PreferenceCategory) {
                unsetIconSpaceReservedRecursive(preference)
            }
            preference.isIconSpaceReserved = false
        }
    }

    fun handleContrib(prefKey: PrefKey, feature: ContribFeature, preference: Preference) =
        if (matches(preference, prefKey)) {
            if (licenceHandler.hasAccessTo(feature)) {
                preferenceActivity.contribFeatureCalled(feature, null)
            } else {
                preferenceActivity.showContribDialog(feature, null)
            }
            true
        } else false

}