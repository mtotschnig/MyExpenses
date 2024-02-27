package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity.Companion.RESULT_RESTORE_OK
import org.totschnig.myexpenses.preference.AccountPreference
import org.totschnig.myexpenses.preference.PrefKey

@Keep
class PreferencesBackupRestoreFragment: BasePreferenceIOBRFragment() {

    override fun onResume() {
        super.onResume()
        loadSyncAccountData()
    }

    private fun loadSyncAccountData() {
        requirePreference<AccountPreference>(PrefKey.AUTO_BACKUP_CLOUD).setData(requireContext())
    }

    override val preferencesResId = R.xml.preferences_backup_restore

    override fun setPreferencesFromResource(preferencesResId: Int, key: String?) {
        super.setPreferencesFromResource(preferencesResId, key)
        preferenceScreen.title = backupRestoreTitle
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_include_share)

        requirePreference<Preference>(PrefKey.AUTO_BACKUP_CLOUD).onPreferenceChangeListener =
            storeInDatabaseChangeListener

        with(requirePreference<Preference>(PrefKey.AUTO_BACKUP_UNENCRYPTED_INFO)) {
            isVisible = prefHandler.encryptDatabase
            summary = preferenceActivity.unencryptedBackupWarning
        }
        configureShareTargetPreference()
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.RESTORE) -> {
            restore.launch(preference.intent)
            true
        }
        else -> false
    }

    private val restore = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_RESTORE_OK) {
            with(preferenceActivity) {
                setResult(RESULT_RESTORE_OK)
                finish()
            }
        }
    }

    companion object {
        const val KEY_CHECKED_FILES = "checkedFiles"
    }
}