package org.totschnig.myexpenses.fragment.preferences

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.BaseSettingsFragment
import org.totschnig.myexpenses.preference.PopupMenuPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.SettingsViewModel

class PreferencesExportFragment: BasePreferenceFragment() {


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
        setPreferencesFromResource(R.xml.preferences_export, rootKey)
        unsetIconSpaceReservedRecursive(preferenceScreen)
        loadAppDirSummary()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when {
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
            else -> false
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

    private fun reportException(e: Exception) {
        preferenceActivity.showSnackBar(e.safeMessage)
        CrashHandler.report(e)
    }
}