package org.totschnig.myexpenses.fragment.preferences

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.preference.*
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.MoreInfoDialogFragment
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PopupMenuPreference
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import java.util.Locale

class MainPreferenceFragment : BasePreferenceFragment(),
    MultiSelectListPreferenceDialogFragment2.OnClickListener {

    lateinit var adapter: PreferenceGroupAdapter

    var isSlideable: Boolean = true
        set(value) {
            field = value
            val preferenceAdapterPosition = highlightedKey?.let {
                adapter.getPreferenceAdapterPosition(
                    it
                )
            }
            if (!value) {
                view?.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.settings_two_pane_background_color,
                        null
                    )
                )

                if (preferenceAdapterPosition != null) {
                    adapter.notifyItemChanged(preferenceAdapterPosition)
                }
            }
        }

    @State
    var highlightedKey: String? = null

    fun onLoadPreference(key: String) {
        val oldPosition = highlightedKey?.let { adapter.getPreferenceAdapterPosition(it) }
        val newPosition = adapter.getPreferenceAdapterPosition(key)
        highlightedKey = key
        if (oldPosition != null) {
            adapter.notifyItemChanged(oldPosition)
        }
        adapter.notifyItemChanged(newPosition)
    }

    override fun overrideTheme() = R.style.MyPreferenceHeaderTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            StateSaver.restoreInstanceState(this, it)
        }
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
        viewModel.loadAppDirInfo()
        viewModel.loadAppData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    override val preferencesResId = R.xml.preference_headers

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        requirePreference<Preference>(PrefKey.CATEGORY_IO).title = ioTitle
        requirePreference<Preference>(PrefKey.CATEGORY_BACKUP_RESTORE).title = backupRestoreTitle
        requirePreference<Preference>(PrefKey.CATEGORY_SECURITY).title = protectionTitle
        with(requirePreference<Preference>(PrefKey.BANKING_FINTS)) {
            summary = "FinTS (${Locale.GERMANY.displayCountry})"
            isVisible = preferenceActivity.bankingFeature != BankingFeature
        }

        viewModel.appData.observe(this) {
            with(requirePreference<MultiSelectListPreference>(PrefKey.MANAGE_APP_DIR_FILES)) {
                if (it.isEmpty()) {
                    isVisible = false
                } else {
                    isVisible = true
                    entries = it.map { it.format(requireContext()) }.toTypedArray()
                    entryValues = it.map { it.name }.toTypedArray()
                }
            }
        }

        loadAppDirSummary()
    }


    //MultiSelectListPreferenceDialogFragmentWithNeutralAction
    override fun onClick(preference: String, values: Set<String>, which: Int) {
        if (values.isNotEmpty() && preference == prefHandler.getKey(PrefKey.MANAGE_APP_DIR_FILES)) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                ConfirmationDialogFragment.newInstance(Bundle().apply {
                    putStringArray(
                        PreferencesBackupRestoreFragment.KEY_CHECKED_FILES,
                        values.toTypedArray()
                    )
                    putString(
                        ConfirmationDialogFragment.KEY_MESSAGE,
                        resources.getQuantityString(
                            R.plurals.delete_files_confirmation_message,
                            values.size,
                            values.size
                        )
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
                    putParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        arrayList
                    )
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                })
            }
        }
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen) =
        object : PreferenceGroupAdapter(preferenceScreen) {

            override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                if (!isSlideable && getItem(position)?.key == highlightedKey) {
                    holder.itemView.setBackgroundColor(
                        ResourcesCompat.getColor(resources, R.color.activatedBackground, null)
                    )
                }
            }
        }.also {
            adapter = it
        }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        matches(preference, PrefKey.CONTRIB_PURCHASE) && !licenceHandler.hasAnyLicence -> {
            preferenceActivity.dispatchCommand(R.id.CONTRIB_INFO_COMMAND, null)
            true
        }

        matches(preference, PrefKey.MORE_INFO_DIALOG) -> {
            MoreInfoDialogFragment().show(parentFragmentManager, "MORE_INFO")
            true
        }

        matches(preference, PrefKey.APP_DIR) -> {
            val appDirInfo = viewModel.appDirInfo.value?.getOrNull()
            if (appDirInfo?.isDefault == false) {
                (preference as PopupMenuPreference).showPopupMenu(
                    getString(R.string.checkbox_is_default), getString(R.string.select)
                ) {
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

        super.onPreferenceTreeClick(preference) -> true

        handleContrib(PrefKey.BANKING_FINTS, ContribFeature.BANKING, preference) -> true
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
        } catch (_: ActivityNotFoundException) {
            preferenceActivity.showSnackBar(
                "Could not open directory picker. This feature may not be supported on your device.",
            )
        }
    }
}