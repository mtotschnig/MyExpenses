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