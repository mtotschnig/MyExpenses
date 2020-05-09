package org.totschnig.myexpenses.fragment

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import java.text.SimpleDateFormat

abstract class BaseSettingsFragment: PreferenceFragmentCompat() {
    @SuppressLint("SimpleDateFormat")
    fun validateDateFormat(dateFormat: String) = when {
        TextUtils.isEmpty(dateFormat) -> null
        dateFormat.matches(Regex("[^\\p{P}Mdy]")) -> "Only Day (d), Month (M), and Year (y) can be used."
        !(dateFormat.contains("d") && dateFormat.contains("M") && dateFormat.contains("y")) -> "You need to use Day (d), Month (M), and Year (y)"
        else -> try {
            SimpleDateFormat(dateFormat)
            null
        } catch (e: IllegalArgumentException) {
            requireContext().getString(R.string.date_format_illegal)
        }
    }?.let {
        activity().showSnackbar(it, Snackbar.LENGTH_LONG)
        false
    } ?: true

    fun activity() = activity as MyPreferenceActivity
}