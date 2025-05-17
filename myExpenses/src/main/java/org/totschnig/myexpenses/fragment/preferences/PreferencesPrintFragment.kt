package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.annotation.Keep
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.FloatSeekBarPreference
import org.totschnig.myexpenses.preference.PrefKey
import java.text.NumberFormat

@Keep
class PreferencesPrintFragment : BasePreferenceFragment() {

    override val preferencesResId = R.xml.preferences_print

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        val percentFormat = NumberFormat.getPercentInstance().also {
            it.setMinimumFractionDigits(1)
        }
        configureMarginPreference(PrefKey.PRINT_MARGIN_LEFT, percentFormat)
        configureMarginPreference(PrefKey.PRINT_MARGIN_RIGHT, percentFormat)
        configureMarginPreference(PrefKey.PRINT_MARGIN_TOP, percentFormat)
        configureMarginPreference(PrefKey.PRINT_MARGIN_BOTTOM, percentFormat)
    }

    private fun configureMarginPreference(prefKey: PrefKey, percentFormat: NumberFormat) {
        requirePreference<FloatSeekBarPreference>(prefKey).formatter = {
            percentFormat.format(it)
        }
    }
}