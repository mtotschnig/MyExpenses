package org.totschnig.myexpenses.preference

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.TimePicker
import androidx.preference.PreferenceDialogFragmentCompat

class TimePreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {
    private lateinit var timePicker: TimePicker
    override fun onCreateDialogView(context: Context): View {
        timePicker = super.onCreateDialogView(context) as TimePicker
        timePicker.setIs24HourView(DateFormat.is24HourFormat(context))
        return timePicker
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = preference as TimePreference
            timePicker.currentHour = preference.hour
            timePicker.currentMinute = preference.minute
        } else {
            timePicker.currentHour = savedInstanceState.getInt(KEY_HOUR)
            timePicker.currentMinute = savedInstanceState.getInt(KEY_MINUTE)
        }
        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_HOUR, timePicker.currentHour)
        outState.putInt(KEY_MINUTE, timePicker.currentMinute)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            timePicker.clearFocus()
            (preference as TimePreference).value =
                100 * timePicker.currentHour + timePicker.currentMinute
        }
    }

    companion object {
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        @JvmStatic
        fun newInstance(key: String?): TimePreferenceDialogFragmentCompat {
            val fragment = TimePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }
    }
}