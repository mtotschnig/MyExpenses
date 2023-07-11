package org.totschnig.myexpenses.fragment

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.FontSizeDialogFragmentCompat
import org.totschnig.myexpenses.preference.FontSizeDialogPreference
import org.totschnig.myexpenses.preference.PrefKey
import java.util.Locale

class PreferenceUiFragment: BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_ui, rootKey)
        unsetIconSpaceReservedRecursive(preferenceScreen)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is FontSizeDialogPreference) {
            FontSizeDialogFragmentCompat.newInstance(preference.key).also {
                it.setTargetFragment(this, 0)
                it.show(
                        parentFragmentManager,
                        "android.support.v7.preference.PreferenceFragment.DIALOG"
                    )
            }
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        //we configure language picker here, so that we can override the outdated value
        //that might get set in ListPreference onRestoreInstanceState, when activity is recreated
        //due to user changing app language in Android 13 system settings
        findPreference<ListPreference>(PrefKey.UI_LANGUAGE)?.apply {
            entries = getLocaleArray()
            value = AppCompatDelegate.getApplicationLocales()[0]?.language ?: MyApplication.DEFAULT_LANGUAGE
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val newLocale = newValue as String
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && newLocale != MyApplication.DEFAULT_LANGUAGE) {
                        featureManager.requestLocale(newLocale)
                    } else {
                        preferenceActivity.setLanguage(newLocale)
                    }
                    value = newValue
                    false
                }
        }
    }


    private fun getLocaleArray() =
        requireContext().resources.getStringArray(R.array.pref_ui_language_values)
            .map(this::getLocaleDisplayName)
            .toTypedArray()

    private fun getLocaleDisplayName(localeString: CharSequence) =
        if (localeString == "default") {
            requireContext().getString(R.string.pref_ui_language_default)
        } else {
            val localeParts = localeString.split("-")
            val locale = if (localeParts.size == 2)
                Locale(localeParts[0], localeParts[1])
            else
                Locale(localeParts[0])
            locale.getDisplayName(locale)
        }
}