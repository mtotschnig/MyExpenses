package org.totschnig.myexpenses.fragment.preferences

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.Keep
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
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

        with(requirePreference<PreferenceCategory>(PrefKey.CATEGORY_PRIVACY)) {
            if (!DistributionHelper.distribution.supportsTrackingAndCrashReporting) {
                removePreference(requirePreference(PrefKey.TRACKING))
                removePreference(requirePreference(PrefKey.CRASHREPORT_ENABLED))
                removePreference(requirePreference(PrefKey.CRASHREPORT_USEREMAIL))
            }
            lifecycleScope.launch {
                val adConsentPref = requirePreference<Preference>(PrefKey.PERSONALIZED_AD_CONSENT)
                if (adHandlerFactory.isPrivacyOptionsRequired()) {
                    adConsentPref.isVisible = true
                } else {
                    removePreference(adConsentPref)
                    if (preferenceCount == 0) {
                        preferenceScreen.removePreference(this@with)
                    }
                }
            }
        }

        requirePreference<Preference>(PrefKey.ENCRYPT_DATABASE_INFO).isVisible =
            prefHandler.encryptDatabase
    }

    private fun setProtectionDependentsState() {
        val isLegacy = prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false)
        val isProtected =
            isLegacy || prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)
        requirePreference<Preference>(PrefKey.SECURITY_QUESTION).isEnabled = isLegacy
        requirePreference<Preference>(PrefKey.PROTECTION_DELAY_SECONDS).isEnabled = isProtected
        requirePreference<Preference>(PrefKey.PROTECTION_ALLOW_SCREENSHOT).isEnabled = isProtected
        requirePreference<Preference>(PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET).isEnabled =
            isProtected
        requirePreference<Preference>(PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET).isEnabled =
            isProtected
        requirePreference<Preference>(PrefKey.PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET).isEnabled =
            isProtected
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.PERSONALIZED_AD_CONSENT) -> {
            preferenceActivity.checkGdprConsent(true)
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