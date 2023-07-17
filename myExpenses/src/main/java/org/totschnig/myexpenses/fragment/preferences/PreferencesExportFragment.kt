package org.totschnig.myexpenses.fragment.preferences

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.MultiSelectListPreference
import androidx.preference.MultiSelectListPreferenceDialogFragment2
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity.RESULT_RESTORE_OK
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.preference.AccountPreference
import org.totschnig.myexpenses.preference.PopupMenuPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.enumValueOrNull
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.viewmodel.ShareViewModel
import timber.log.Timber

class PreferencesExportFragment: BasePreferenceFragment(),
    MultiSelectListPreferenceDialogFragment2.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.appDirInfo.observe(this) { result ->
            val pref = requirePreference<Preference>(PrefKey.APP_DIR)
            result.onSuccess { appDirInfo ->
                pref.summary = if (appDirInfo.isWriteable) {
                    appDirInfo.displayName
                } else {
                    getString(R.string.app_dir_not_accessible, appDirInfo.documentFile.uri)
                }
            }.onFailure {
                pref.setSummary(R.string.io_error_appdir_null)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSyncAccountData()
        viewModel.loadAppData()
    }

    private fun loadSyncAccountData() {
        requirePreference<AccountPreference>(PrefKey.AUTO_BACKUP_CLOUD).setData(requireContext())
    }

    override val preferencesResId = R.xml.preferences_export

    override fun setPreferencesFromResource(preferencesResId: Int, key: String?) {
        super.setPreferencesFromResource(preferencesResId, key)
        preferenceScreen.title = exportBackupTitle
        requirePreference<Preference>(PrefKey.CSV_EXPORT).title =
            getString(R.string.export_to_format, "CSV")
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        loadAppDirSummary()

        requirePreference<Preference>(PrefKey.AUTO_BACKUP_CLOUD).onPreferenceChangeListener =
            storeInDatabaseChangeListener

        with(requirePreference<Preference>(PrefKey.AUTO_BACKUP_UNENCRYPTED_INFO)) {
            isVisible = prefHandler.encryptDatabase
            summary = preferenceActivity.unencryptedBackupWarning
        }

        viewModel.appData.observe(this) {
            with(requirePreference<MultiSelectListPreference>(PrefKey.MANAGE_APP_DIR_FILES)) {
                if (it.isEmpty()) {
                    isVisible = false
                } else {
                    isVisible = true
                    entries = it.map { "${it.first} (${Formatter.formatFileSize(requireContext(), it.second)})" }.toTypedArray()
                    entryValues = it.map { it.first }.toTypedArray()
                }
            }
        }

        with(requirePreference<Preference>(PrefKey.SHARE_TARGET)) {
            summary = getString(R.string.pref_share_target_summary) + " " +
                    ShareViewModel.Scheme.values().joinToString(
                        separator = ", ", prefix = "(", postfix = ")"
                    ) { it.name.lowercase() }
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val target = newValue as String
                if (target != "") {
                    val uri = ShareViewModel.parseUri(target)
                    if (uri == null) {
                        preferenceActivity.showSnackBar(
                            getString(
                                R.string.ftp_uri_malformed,
                                target
                            )
                        )
                        return@OnPreferenceChangeListener false
                    }
                    val scheme = uri.scheme
                    if (enumValueOrNull<ShareViewModel.Scheme>(scheme.uppercase()) == null) {
                        preferenceActivity.showSnackBar(
                            getString(
                                R.string.share_scheme_not_supported,
                                scheme
                            )
                        )
                        return@OnPreferenceChangeListener false
                    }
                    if (scheme == "ftp") {
                        if (!Utils.isIntentAvailable(
                                requireActivity(),
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse(target)
                                })) {
                            preferenceActivity.showDialog(R.id.FTP_DIALOG)
                        }
                    }
                }
                true
            }
        }

    }

    //MultiSelectListPreferenceDialogFragmentWithNeutralAction
    override fun onClick(preference: String, values: Set<String>, which: Int) {
        if (values.isNotEmpty() && preference == prefHandler.getKey(PrefKey.MANAGE_APP_DIR_FILES)) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                ConfirmationDialogFragment.newInstance(Bundle().apply {
                    putStringArray(KEY_CHECKED_FILES, values.toTypedArray())
                    putString(
                        ConfirmationDialogFragment.KEY_MESSAGE,
                        resources.getQuantityString(R.plurals.delete_files_confirmation_message, values.size, values.size)
                    )
                    putInt(
                        ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                        R.id.DELETE_FILES_COMMAND
                    )
                    putInt(
                        ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                        R.string.menu_delete
                    )
                })
                    .show(parentFragmentManager, "CONFIRM_DELETE")
            } else if (which == DialogInterface.BUTTON_POSITIVE) {
                val appDir = viewModel.appDirInfo.value?.getOrThrow()!!
                startActivity(Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "text/plain"
                    val arrayList = ArrayList(
                        values.mapNotNull { file ->
                            appDir.documentFile.findFile(file)?.uri?.let {
                                AppDirHelper.ensureContentUri(it, requireContext())
                            }
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

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.APP_DIR) -> {
            val appDirInfo = viewModel.appDirInfo.value?.getOrNull()
            if (appDirInfo?.isDefault == false) {
                (preference as PopupMenuPreference).showPopupMenu(
                    {
                        when(it.itemId) {
                            0 -> {
                                prefHandler.putString(PrefKey.APP_DIR, null)
                                loadAppDirSummary()
                                viewModel.loadAppData()
                                true
                            }
                            1 -> {
                                pickAppDir(appDirInfo)
                                true
                            }
                            else -> false
                        }
                    }, getString(R.string.checkbox_is_default), getString(R.string.select)
                )
            } else {
                pickAppDir(appDirInfo)
            }
            true
        }
        matches(preference, PrefKey.HELP) -> {
            preferenceActivity.startActionView(preference.summary.toString())
            true
        }
        matches(preference, PrefKey.RESTORE) -> {
            restore.launch(preference.intent)
            true
        }
        else -> false
    }

    private val restore = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_RESTORE_OK) {
            requireActivity().setResult(RESULT_RESTORE_OK)
            requireActivity().finish()
        }
    }

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        if (it != null) {
            requireContext().contentResolver.takePersistableUriPermission(it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefHandler.putString(PrefKey.APP_DIR, it.toString())
            loadAppDirSummary()
        }
    }

    private fun pickAppDir(appDirInfo: SettingsViewModel.AppDirInfo?) {
        pickFolder.launch(appDirInfo?.documentFile?.uri)
    }

    private fun loadAppDirSummary() {
        viewModel.loadAppDirInfo()
    }

    companion object {
        const val KEY_CHECKED_FILES = "checkedFiles"
    }
}