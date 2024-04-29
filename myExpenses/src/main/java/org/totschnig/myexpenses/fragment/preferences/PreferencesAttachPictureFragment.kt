package org.totschnig.myexpenses.fragment.preferences

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault

@Keep
class PreferencesAttachPictureFragment : BasePreferenceFragment() {

    override val preferencesResId = R.xml.preferences_attach_picture

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        configureQualityPreference()
        requirePreference<Preference>(PrefKey.OPTIMIZE_PICTURE_MAX_SIZE).title =
            getString(R.string.maximum_size) + " (" + getString(R.string.pixels) + ")"
    }

    private fun configureQualityPreference() {
        requirePreference<Preference>(PrefKey.OPTIMIZE_PICTURE_QUALITY).isEnabled =
            prefHandler.enumValueOrDefault(
                PrefKey.OPTIMIZE_PICTURE_FORMAT,
                Bitmap.CompressFormat.WEBP
            ) != Bitmap.CompressFormat.PNG
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when(key) {
            getKey(PrefKey.OPTIMIZE_PICTURE_FORMAT) -> configureQualityPreference()
        }
    }
}