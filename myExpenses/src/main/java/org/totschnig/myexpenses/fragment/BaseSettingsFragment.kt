package org.totschnig.myexpenses.fragment

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.feature.Callback
import org.totschnig.myexpenses.feature.ENGINE_TESSERACT
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference.OnValidationErrorListener
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.util.*
import javax.inject.Inject


abstract class BaseSettingsFragment : PreferenceFragmentCompat(), OnValidationErrorListener,
        OnSharedPreferenceChangeListener {

    @Inject
    lateinit var featureManager: FeatureManager

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity().application as MyApplication).appComponent.inject(this)
    }

    override fun onResume() {
        super.onResume()
        requireApplication().settings.registerOnSharedPreferenceChangeListener(this)
        featureManager.registerCallback(object : Callback {
            override fun onLanguageAvailable() {
                rebuildDbConstants()
                activity().recreate()
            }

            override fun onFeatureAvailable() {
                configureTesseractLanguagePref()
            }

            override fun onAsyncStartedLanguage(displayLanguage: String) {
                activity().showSnackbar(getString(R.string.language_download_requested, displayLanguage))
            }

            override fun onError(throwable: Throwable) {
                CrashHandler.report(throwable)
                throwable.message?.let {
                    activity().showSnackbar(it)
                }
            }
        }
        )
    }

    override fun onPause() {
        super.onPause()
        requireApplication().settings.unregisterOnSharedPreferenceChangeListener(this)
        featureManager.unregister()
    }

    override fun onValidationError(messageResId: Int) {
        activity().showSnackbar(messageResId)
    }

    fun activity() = activity as MyPreferenceActivity

    fun configureUninstallPrefs() {
        configureMultiSelectListPref(PrefKey.FEATURE_UNINSTALL_FEATURES, featureManager.installedFeatures()) { featureManager.uninstallFeatures(it) }
        configureMultiSelectListPref(PrefKey.FEATURE_UNINSTALL_LANGUAGES, featureManager.installedLanguages()) { featureManager.uninstallLanguages(it) }
    }

    private fun configureMultiSelectListPref(prefKey: PrefKey, entries: Set<String>, action: (Set<String>) -> Unit) {
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

    fun <T : Preference> requirePreference(prefKey: PrefKey): T {
        return findPreference(prefHandler.getKey(prefKey))
                ?: throw IllegalStateException("Preference not found")
    }

    fun getLocaleArray() =
            requireContext().resources.getStringArray(R.array.pref_ui_language_values)
                    .map(this::getLocaleDisplayName)
                    .toTypedArray()

    private fun getLocaleDisplayName(localeString: CharSequence) =
            if (localeString == "default") {
                requireContext().getString(R.string.pref_ui_language_default)
            } else {
                val localeParts = localeString.split("-")
                val locale = if (localeParts.size == 2)
                    Locale(localeParts[0], localeParts[1])
                else
                    Locale(localeParts[0])
                locale.getDisplayName(locale)
            }

    fun configureTesseractLanguagePref() {
        findPreference<ListPreference>(prefHandler.getKey(PrefKey.TESSERACT_LANGUAGE))?.let {
            if (prefHandler.getString(PrefKey.OCR_ENGINE, null) == ENGINE_TESSERACT)
                activity().ocrViewModel.configureTesseractLanguagePref(it)
            else
                it.isVisible = false
        }
    }

    fun requireApplication(): MyApplication {
        return (requireActivity().getApplication() as MyApplication)
    }

    fun rebuildDbConstants() {
        DatabaseConstants.buildLocalized(userLocaleProvider.getUserPreferredLocale())
        Transaction.buildProjection(requireContext())
        Account.buildProjection()
    }
}