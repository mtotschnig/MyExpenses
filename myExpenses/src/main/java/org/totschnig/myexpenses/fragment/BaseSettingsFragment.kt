package org.totschnig.myexpenses.fragment

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.util.validateDateFormat
import java.text.SimpleDateFormat

abstract class BaseSettingsFragment: PreferenceFragmentCompat() {
    @SuppressLint("SimpleDateFormat")
    fun validateDateFormatWithFeedback(dateFormat: String) = validateDateFormat(dateFormat)?.let {
        activity().showSnackbar(it, Snackbar.LENGTH_LONG)
        false
    } ?: true

    fun activity() = activity as MyPreferenceActivity
}