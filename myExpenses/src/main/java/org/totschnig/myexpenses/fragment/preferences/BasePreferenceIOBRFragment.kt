package org.totschnig.myexpenses.fragment.preferences

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PopupMenuPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.enumValueOrNull
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.viewmodel.BaseFunctionalityViewModel

/**
 * Common functionality for IO and BackupRestore fragments
 */
abstract class BasePreferenceIOBRFragment : BasePreferenceFragment() {

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        loadAppDirSummary()
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.APP_DIR) -> {
            val appDirInfo = viewModel.appDirInfo.value?.getOrNull()
            if (appDirInfo?.isDefault == false) {
                (preference as PopupMenuPreference).showPopupMenu(getString(R.string.checkbox_is_default), getString(R.string.select)) {
                    when (it.itemId) {
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
                }
            } else {
                pickAppDir(appDirInfo)
            }
            true
        }

        else -> false
    }

    private fun loadAppDirSummary() {
        viewModel.loadAppDirInfo()
    }

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        if (it != null) {
            requireContext().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefHandler.putString(PrefKey.APP_DIR, it.toString())
            loadAppDirSummary()
        }
    }

    private fun pickAppDir(appDirInfo: SettingsViewModel.AppDirInfo?) {
        try {
            pickFolder.launch(appDirInfo?.documentFile?.uri)
        } catch (e: ActivityNotFoundException) {
            preferenceActivity.showSnackBar(
                "No activity found for picking application directory."
            )
        }
    }

    fun configureShareTargetPreference() {
        with(requirePreference<Preference>(PrefKey.SHARE_TARGET)) {
            summary = getString(R.string.pref_share_target_summary) + " " +
                    BaseFunctionalityViewModel.Scheme.entries.joinToString(
                        separator = ", ", prefix = "(", postfix = ")"
                    ) { it.name.lowercase() }
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val target = newValue as String
                if (target != "") {
                    val uri = BaseFunctionalityViewModel.parseUri(target)
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
                    if (enumValueOrNull<BaseFunctionalityViewModel.Scheme>(scheme.uppercase()) == null) {
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
                                })
                        ) {
                            preferenceActivity.showDialog(R.id.FTP_DIALOG)
                        }
                    }
                }
                true
            }
        }
    }
}