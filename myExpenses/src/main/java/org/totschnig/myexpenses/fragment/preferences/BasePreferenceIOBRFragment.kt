package org.totschnig.myexpenses.fragment.preferences

import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.enumValueOrNull
import org.totschnig.myexpenses.viewmodel.BaseFunctionalityViewModel

/**
 * Common functionality for IO and BackupRestore fragments
 */
abstract class BasePreferenceIOBRFragment : BasePreferenceFragment() {

    fun configureShareTargetPreference() {
        requirePreference<Preference>(PrefKey.PERFORM_SHARE).onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                updateShareTargetCredentialsEnabled(
                    performShare = newValue as Boolean,
                )
                true
            }

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
                updateShareTargetCredentialsEnabled(shareTarget = target.trim())
                true
            }
        }

        updateShareTargetCredentialsEnabled()
    }

    private fun updateShareTargetCredentialsEnabled(
        performShare: Boolean = prefHandler.getBoolean(PrefKey.PERFORM_SHARE, defValue = false),
        shareTarget: String = prefHandler.requireString(PrefKey.SHARE_TARGET, "").trim()
    ) {
        val enabled = performShare && isHttpUrlWithoutCredentials(shareTarget)
        requirePreference<Preference>(PrefKey.SHARE_TARGET_USERNAME).isEnabled = enabled
        requirePreference<Preference>(PrefKey.SHARE_TARGET_PASSWORD).isEnabled = enabled
    }

    companion object {
        fun isHttpUrlWithoutCredentials(target: String): Boolean {
            if (target.isBlank()) return false
            val uri = BaseFunctionalityViewModel.parseUri(target) ?: return false
            val scheme = uri.scheme ?: return false
            val schemeEnum = enumValueOrNull<BaseFunctionalityViewModel.Scheme>(scheme.uppercase())
            val isHttp = (schemeEnum == BaseFunctionalityViewModel.Scheme.HTTP ||
                    schemeEnum == BaseFunctionalityViewModel.Scheme.HTTPS)
            return isHttp && uri.userInfo.isNullOrEmpty()
        }
    }
}