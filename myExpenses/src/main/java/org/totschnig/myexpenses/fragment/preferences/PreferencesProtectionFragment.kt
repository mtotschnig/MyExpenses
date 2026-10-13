package org.totschnig.myexpenses.fragment.preferences

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.distrib.DistributionHelper

@Keep
class PreferencesProtectionFragment : BasePreferenceFragment() {

    override val preferencesResId = R.xml.preferences_protection

    override fun setPreferencesFromResource(preferencesResId: Int, key: String?) {
        super.setPreferencesFromResource(preferencesResId, key)
        preferenceScreen.title = protectionTitle
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setProtectionDependentsState()

        requirePreference<Preference>(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN).onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    if (!(requireContext().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardSecure) {
                        preferenceActivity.showDeviceLockScreenWarning()
                        false
                    } else if (prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false)) {
                        showOnlyOneProtectionWarning(true)
                        false
                    } else true
                } else true
            }

        requirePreference<Preference>(PrefKey.CRASHREPORT_ENABLED).summary =
            Utils.getTextWithAppName(
                context,
                R.string.crash_reports_user_info
            )

        requirePreference<PreferenceCategory>(PrefKey.CATEGORY_PRIVACY).isVisible =
            DistributionHelper.distribution.supportsTrackingAndCrashReporting

        with(requirePreference<PreferenceCategory>(PrefKey.CATEGORY_ADS)) {
            if (adHandlerFactory.isAdDisabled)  {
                isVisible = false
            } else {
                lifecycleScope.launch {
                    requirePreference<Preference>(PrefKey.PERSONALIZED_AD_CONSENT).isVisible =
                        adHandlerFactory.isPrivacyOptionsRequired(requireActivity())
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val encryptInfoPref = requirePreference<Preference>(PrefKey.ENCRYPT_DATABASE_INFO)
        val encryptDatabase = prefHandler.encryptDatabase
        encryptInfoPref.isVisible = encryptDatabase
        if (encryptDatabase) {
            viewModel.getDatabaseInfo()
                .observe(viewLifecycleOwner) { (sqliteVersion, cipherVersion) ->
                    if (cipherVersion != null) {
                        encryptInfoPref.setIcon(R.drawable.ic_lock)
                        encryptInfoPref.title = getString(R.string.database_is_encrypted)
                        encryptInfoPref.summary =
                            getString(R.string.database_info_format, sqliteVersion, cipherVersion)
                    } else {
                        val errorColor = ContextCompat.getColor(requireContext(), R.color.colorErrorDialog)
                        encryptInfoPref.setIcon(R.drawable.ic_warning)
                        encryptInfoPref.title = SpannableString("Database is not encrypted").apply {
                            setSpan(ForegroundColorSpan(errorColor), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                }
        }
    }

    private fun setProtectionDependentsState() {
        val isLegacy = prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false)
        val isProtected =
            isLegacy || prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)

        setEnabled(PrefKey.SECURITY_QUESTION, isLegacy)
        setEnabled(PrefKey.PROTECTION_DELAY_SECONDS, isProtected)
        setEnabled(PrefKey.PROTECTION_ALLOW_SCREENSHOT, isProtected)
        setEnabled(PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET, isProtected)
        setEnabled(PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET, isProtected)
        setEnabled(PrefKey.PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET, isProtected)
        setEnabled(PrefKey.PROTECTION_ENABLE_BUDGET_WIDGET, isProtected)

        with(requirePreference<PreferenceCategory>(PrefKey.CATEGORY_PROTECTION)) {
            initialExpandedChildrenCount = preferenceCount - (if (isLegacy) 1 else 2)
        }
    }

    private fun setEnabled(prefKey: PrefKey, enabled: Boolean) {
        requirePreference<Preference>(prefKey).isEnabled = enabled

    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.PERSONALIZED_AD_CONSENT) -> {
            preferenceActivity.checkGdprConsent(true)
            true
        }
        matches(preference, PrefKey.NO_ADS) -> {
            preferenceActivity.contribFeatureRequested(ContribFeature.AD_FREE)
            true
        }

        else -> false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            getKey(PrefKey.PROTECTION_LEGACY), getKey(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN) ->
                setProtectionDependentsState()
        }
    }
}