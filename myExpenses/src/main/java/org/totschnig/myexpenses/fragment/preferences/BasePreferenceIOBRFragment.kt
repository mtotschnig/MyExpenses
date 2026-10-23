package org.totschnig.myexpenses.fragment.preferences

import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.crypt.PrefKeyStoreEncryption
import org.totschnig.myexpenses.util.enumValueOrNull
import org.totschnig.myexpenses.viewmodel.BaseFunctionalityViewModel

/**
 * Common functionality for IO and BackupRestore fragments
 */
abstract class BasePreferenceIOBRFragment : BasePreferenceFragment() {

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
                }
                true
            }
        }

        with(requirePreference<EditTextPreference>(PrefKey.SHARE_TARGET_USERNAME)) {
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val username = newValue as? String ?: ""
                if (username.isNotEmpty()) {
                    val encrypted = PrefKeyStoreEncryption.encrypt(username)
                    if (encrypted == null) {
                        preferenceActivity.showSnackBar(getString(R.string.encryption_error))
                        return@OnPreferenceChangeListener false
                    }
                }
                true
            }
        }

        with(requirePreference<EditTextPreference>(PrefKey.SHARE_TARGET_PASSWORD)) {
            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
                if (preference.text.isNullOrEmpty()) {
                    getString(androidx.preference.R.string.not_set)
                } else {
                    "••••••••"
                }
            }
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val password = newValue as? String ?: ""
                if (password.isNotEmpty()) {
                    val encrypted = PrefKeyStoreEncryption.encrypt(password)
                    if (encrypted == null) {
                        preferenceActivity.showSnackBar(getString(R.string.encryption_error))
                        return@OnPreferenceChangeListener false
                    }
                }
                true
            }
        }
    }
}