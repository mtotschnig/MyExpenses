package org.totschnig.myexpenses.fragment

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.getTesseractLanguageDisplayName
import org.totschnig.myexpenses.viewmodel.DownloadViewModel
import java.util.*
import javax.inject.Inject


abstract class BaseSettingsFragment : PreferenceFragmentCompat(), LocalizedFormatEditTextPreference.OnValidationErrorListener {

    lateinit var viewModel: DownloadViewModel

    @Inject
    lateinit var featureManager: FeatureManager

    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity().application as MyApplication).appComponent.inject(this)
        viewModel = ViewModelProvider(requireActivity())[DownloadViewModel::class.java]
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

    fun getTesseractLanguageArray() =
            requireContext().resources.getStringArray(R.array.pref_tesseract_language_values)
                    .map { getTesseractLanguageDisplayName(requireContext(), it)}
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

    fun downloadTessData(language: String) {
        viewModel.tessDataExists(language).observe(this, { if (!it)
            ConfirmationDialogFragment.newInstance(Bundle().apply {
                putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.button_download)
                putString(ConfirmationDialogFragment.KEY_MESSAGE,
                        getString(R.string.tesseract_download_confirmation,
                                getTesseractLanguageDisplayName(requireContext(), language)))
                putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.TESSERACT_DOWNLOAD_COMMAND)
                putSerializable(ConfirmationDialogFragment.KEY_TAG_POSITIVE, language)
            }).show(parentFragmentManager, "DOWNLOAD_TESSDATA")
        })
    }
}