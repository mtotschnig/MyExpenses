package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey

@Keep
class PreferencesFeedbackFragment : BasePreferenceFragment() {
    override val preferencesResId = R.xml.preferences_feedback

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        requirePreference<Preference>(PrefKey.NEWS).title =
            "${getString(R.string.pref_news_title)} (Mastodon)"
    }


    override fun onPreferenceTreeClick(preference: Preference)= when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.RATE) -> {
            prefHandler.putLong(PrefKey.NEXT_REMINDER_RATE, -1)
            preferenceActivity.dispatchCommand(R.id.RATE_COMMAND, null)
            true
        }
        matches(preference, PrefKey.SEND_FEEDBACK) -> {
            preferenceActivity.dispatchCommand(R.id.FEEDBACK_COMMAND, null)
            true
        }
        else -> false
    }
}