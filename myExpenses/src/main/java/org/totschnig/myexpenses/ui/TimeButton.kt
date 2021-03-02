package org.totschnig.myexpenses.ui

import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.widget.TimePicker
import icepick.State
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.threeten.bp.temporal.ChronoField
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler

class TimeButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ButtonWithDialog(context, attrs, defStyleAttr) {
    @JvmField
    @State
    var time: LocalTime

    init {
        time = LocalTime.now()
    }

    private var timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    override fun update() {
        text = time.format(timeFormatter)
    }

    fun getTime(): LocalTime {
        return time
    }

    fun setTime(time: LocalTime) {
        this.time = time
        update()
    }

    override fun onCreateDialog(prefHandler: PrefHandler): Dialog {
        val timeSetListener = OnTimeSetListener { view: TimePicker?, hourOfDay: Int, minute: Int ->
            if (time[ChronoField.HOUR_OF_DAY] != hourOfDay ||
                    time[ChronoField.MINUTE_OF_HOUR] != minute) {
                setTime(LocalTime.of(hourOfDay, minute))
                host.onValueSet(this)
            }
        }
        return TimePickerDialog(context, R.style.ThemeOverlay_MaterialComponents_Dialog,
                timeSetListener,
                time.hour,
                time.minute,
                DateFormat.is24HourFormat(context)
        )
    }

    override fun onPrepareDialog(dialog: Dialog) {
        (dialog as? TimePickerDialog)?.updateTime(time.hour, time.minute)
    }
}