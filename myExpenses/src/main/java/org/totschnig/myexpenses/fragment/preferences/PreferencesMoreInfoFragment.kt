package org.totschnig.myexpenses.fragment.preferences

import android.icu.text.ListFormatter
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.Keep
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.MoreInfoDialogFragment
import org.totschnig.myexpenses.preference.PrefKey
import java.util.Locale

@Keep
class PreferencesMoreInfoFragment : BasePreferenceFragment() {

    override val preferencesResId = R.xml.preferences_more_info

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        val translatorsArrayResId = getTranslatorsArrayResId()
        val translationPreference = requirePreference<Preference>(PrefKey.TRANSLATION)
        if (translatorsArrayResId != 0) {
            val translatorsArray = resources.getStringArray(translatorsArrayResId)
            val translators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ListFormatter.getInstance().format(*translatorsArray) else TextUtils.join(
                ", ",
                translatorsArray
            )
            translationPreference.summary =
                "${getString(R.string.translated_by)}: $translators"

        } else {
            requirePreference<PreferenceCategory>(PrefKey.CATEGORY_TRANSLATION)
                .removePreference(translationPreference)
        }
        requirePreference<Preference>(PrefKey.NEWS).title =
            "${getString(R.string.pref_news_title)} (Mastodon)"
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.MORE_INFO_DIALOG) -> {
            MoreInfoDialogFragment().show(parentFragmentManager, "MORE_INFO")
            true
        }
        else -> false

    }
    private fun getTranslatorsArrayResId(): Int {
        val locale = Locale.getDefault()
        val language = locale.language.lowercase(Locale.US)
        val country = locale.country.lowercase(Locale.US)
        return preferenceActivity.getTranslatorsArrayResId(language, country)
    }
}