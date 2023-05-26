package org.totschnig.myexpenses.ui

import android.content.Context
import android.text.format.DateFormat.is24HourFormat
import android.util.AttributeSet
import com.evernote.android.state.State
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import org.totschnig.myexpenses.injector
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class TimeButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ButtonWithDialog<MaterialTimePicker>(context, attrs, defStyleAttr) {

    @JvmField
    @State
    var time: LocalTime

    init {
        time = LocalTime.now()
    }

    private var timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    override fun buildDialog() = MaterialTimePicker.Builder()
        .setTimeFormat(if (is24HourFormat(context)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
        .apply {
            context.injector.prefHandler().getInt(KEY_INPUT_MODE, -1).takeIf { it != -1 }?.let {
                setInputMode(it)
            }
        }
        .setHour(time.hour)
        .setMinute(time.minute)
        .build()

    override fun update() {
        text = time.format(timeFormatter)
    }

    fun setTime(time: LocalTime) {
        this.time = time
        update()
    }

    override val fragmentTag: String
        get() = "date_button"

    override fun attachListener(dialogFragment: MaterialTimePicker) {
        dialogFragment.addOnPositiveButtonClickListener {
            context.injector.prefHandler().putInt(KEY_INPUT_MODE, dialogFragment.inputMode)
            setTime(LocalTime.of(dialogFragment.hour, dialogFragment.minute))
        }
        dialogFragment.addOnDismissListener {
            dialogShown = false
        }
    }

    companion object {
        const val KEY_INPUT_MODE = "timePickerInputMode"
    }
}
