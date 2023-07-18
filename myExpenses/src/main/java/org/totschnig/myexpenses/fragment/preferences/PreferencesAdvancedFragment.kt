package org.totschnig.myexpenses.fragment.preferences

import android.content.Intent
import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.list.CustomListDialog
import eltos.simpledialogfragment.list.SimpleListDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import timber.log.Timber
import java.io.File
import java.util.Locale

@Keep
class PreferencesAdvancedFragment : BasePreferenceFragment(),
    LocalizedFormatEditTextPreference.OnValidationErrorListener,
    SimpleDialog.OnDialogResultListener {
    override val preferencesResId = R.xml.preferences_advanced

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        requirePreference<Preference>(PrefKey.CRASHLYTICS_USER_ID).let {
            if (DistributionHelper.isGithub ||
                !prefHandler.getBoolean(PrefKey.CRASHREPORT_ENABLED, false)
            ) {
                requirePreference<PreferenceCategory>(PrefKey.DEBUG_SCREEN).removePreference(it)
            } else {
                it.summary =
                    prefHandler.getString(PrefKey.CRASHLYTICS_USER_ID, null).toString()
            }
        }

        viewModel.dataCorrupted().observe(this) {
            if (it > 0) {
                with(requirePreference<Preference>(PrefKey.DEBUG_REPAIR_987)) {
                    isVisible = true
                    title = "Inspect Corrupted Data ($it)"
                }
            }
        }

        if (featureManager.allowsUninstall()) {
            configureUninstallPrefs()
        } else {
            requirePreference<Preference>(PrefKey.FEATURE_UNINSTALL).isVisible = false
        }

        requirePreference<LocalizedFormatEditTextPreference>(PrefKey.CUSTOM_DECIMAL_FORMAT)
            .onValidationErrorListener = this

        requirePreference<LocalizedFormatEditTextPreference>(PrefKey.CUSTOM_DATE_FORMAT)
            .onValidationErrorListener = this
    }

    override fun onPreferenceTreeClick(preference: Preference)= when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.DEBUG_REPAIR_987) -> {
            viewModel.prettyPrintCorruptedData(currencyFormatter).observe(this) { message ->
                MessageDialogFragment.newInstance(
                    "Inspect Corrupted Data",
                    message,
                    MessageDialogFragment.okButton(),
                    null,
                    null
                )
                    .show(parentFragmentManager, "INSPECT")
            }
            true
        }
        matches(preference, PrefKey.DEBUG_LOG_SHARE) -> {
            viewModel.logData().observe(this) {
                SimpleListDialog.build().choiceMode(CustomListDialog.MULTI_CHOICE)
                    .title(R.string.pref_debug_logging_share_summary)
                    .items(it)
                    .neg()
                    .pos(android.R.string.ok)
                    .show(this, DIALOG_SHARE_LOGS)
            }
            true
        }
        else -> false
    }

    private fun configureUninstallPrefs() {
        configureMultiSelectListPref(
            PrefKey.FEATURE_UNINSTALL_FEATURES,
            featureManager.installedFeatures(requireContext(), prefHandler),
            featureManager::uninstallFeatures
        ) { module ->
            Feature.fromModuleName(module)?.let { getString(it.labelResId) } ?: module
        }
        configureMultiSelectListPref(
            PrefKey.FEATURE_UNINSTALL_LANGUAGES,
            featureManager.installedLanguages(),
            featureManager::uninstallLanguages
        ) { language ->
            Locale(language).let { it.getDisplayName(it) }
        }
    }

    private fun configureMultiSelectListPref(
        prefKey: PrefKey,
        entries: Set<String>,
        action: (Set<String>) -> Unit,
        prettyPrint: (String) -> String
    ) {
        (requirePreference(prefKey) as? MultiSelectListPreference)?.apply {
            if (entries.isEmpty()) {
                isEnabled = false
            } else {
                setOnPreferenceChangeListener { _, newValue ->
                    @Suppress("UNCHECKED_CAST")
                    (newValue as? Set<String>)?.let { action(it) }
                    false
                }
                setEntries(entries.map(prettyPrint).toTypedArray())
                entryValues = entries.toTypedArray()
            }
        }
    }

    override fun onValidationError(message: String) {
        preferenceActivity.showSnackBar(message)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        when (dialogTag) {

            DIALOG_SHARE_LOGS -> {
                if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
                    val logDir = File(requireContext().getExternalFilesDir(null), "logs")
                    startActivity(Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.support_email)))
                        putExtra(Intent.EXTRA_SUBJECT, "[${getString(R.string.app_name)}]: Logs")
                        type = "text/plain"
                        val arrayList = ArrayList(
                            extras.getStringArrayList(SimpleListDialog.SELECTED_LABELS)!!.map {
                                AppDirHelper.getContentUriForFile(
                                    requireContext(),
                                    File(logDir, it)
                                )
                            })
                        Timber.d("ATTACHMENTS" + arrayList.joinToString())
                        putParcelableArrayListExtra(
                            Intent.EXTRA_STREAM,
                            arrayList
                        )
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    })
                }
            }
        }
        return true
    }

    companion object {
        const val DIALOG_SHARE_LOGS = "shareLogs"
    }
}