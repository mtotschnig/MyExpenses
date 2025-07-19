package org.totschnig.myexpenses.fragment.preferences

import android.content.Intent
import android.os.Bundle
import androidx.annotation.Keep
import androidx.fragment.app.activityViewModels
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.list.CustomListDialog
import eltos.simpledialogfragment.list.SimpleListDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.SelectDefaultTransferCategoryDialogFragment
import org.totschnig.myexpenses.dialog.SelectDefaultTransferCategoryDialogFragment.Companion.SELECT_CATEGORY_REQUEST
import org.totschnig.myexpenses.feature.Module
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.viewmodel.CategoryViewModel
import timber.log.Timber
import java.io.File
import java.util.Locale

@Keep
class PreferencesAdvancedFragment : BasePreferenceFragment(),
    LocalizedFormatEditTextPreference.OnValidationErrorListener,
    SimpleDialog.OnDialogResultListener {

    private val categoryViewModel: CategoryViewModel by activityViewModels()

    override val preferencesResId = R.xml.preferences_advanced

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(categoryViewModel)
        super.onCreate(savedInstanceState)
        childFragmentManager.setFragmentResultListener(
            SELECT_CATEGORY_REQUEST,
            this
        ) { _, bundle ->
            setDefaultTransferCategoryPath(bundle.getString(DatabaseConstants.KEY_PATH)!!)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        viewModel.dataCorrupted().observe(this) {
            if (it > 0) {
                with(requirePreference<Preference>(PrefKey.DEBUG_REPAIR_987)) {
                    isVisible = true
                    title = "Inspect Corrupted Data ($it)"
                }
            }
        }

        viewModel.shouldOfferCalendarRemoval().observe(this) {
            configureDeleteCalendarPreference((it))
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

        categoryViewModel.defaultTransferCategory().observe(this) {
            setDefaultTransferCategoryPath(it)
        }
    }

    private fun setDefaultTransferCategoryPath(path: String) {
        requirePreference<Preference>(PrefKey.DEFAULT_TRANSFER_CATEGORY).summary = path
    }

    fun configureDeleteCalendarPreference(isVisible: Boolean) {
        requirePreference<Preference>(PrefKey.REMOVE_LOCAL_CALENDAR).isVisible = isVisible
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
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

        matches(preference, PrefKey.DEFAULT_TRANSFER_CATEGORY) -> {
            SelectDefaultTransferCategoryDialogFragment()
                .show(childFragmentManager, "SELECT_DEFAULT")
            true
        }

        matches(preference, PrefKey.REMOVE_LOCAL_CALENDAR) -> {
            ConfirmationDialogFragment.newInstance(Bundle().apply {
                putString(
                    ConfirmationDialogFragment.KEY_MESSAGE,
                    getString(R.string.preferences_calendar_delete_message)
                )
                putInt(
                    ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                    R.string.calendar_delete
                )
                putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                    R.id.DELETE_CALENDAR_COMMAND
                )
            }).show(parentFragmentManager, "CONFIRM_CALENDAR_DELETE")
            true
        }

        else -> false
    }

    private fun configureUninstallPrefs() {
        configureMultiSelectListPref(
            PrefKey.FEATURE_UNINSTALL_FEATURES,
            featureManager.installedModules(requireContext(), prefHandler),
            featureManager::uninstallModules
        ) { Module.print(requireContext(), it) }

        configureMultiSelectListPref(
            PrefKey.FEATURE_UNINSTALL_LANGUAGES,
            featureManager.installedLanguages() - "en",
            featureManager::uninstallLanguages
        ) { language -> Locale(language).let { it.getDisplayName(it) } }
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