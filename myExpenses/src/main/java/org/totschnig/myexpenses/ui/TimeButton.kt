package org.totschnig.myexpenses.ui

import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateFormat
import android.util.AttributeSet
import androidx.fragment.app.DialogFragment
import icepick.State
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.totschnig.myexpenses.R

class TimeButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ButtonWithDialog<TimeButton.PlatformTimePicker>(context, attrs, defStyleAttr) {

    @JvmField
    @State
    var time: LocalTime

    init {
        time = LocalTime.now()
    }

    private var timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    override fun buildDialog() = PlatformTimePicker().apply {
        arguments = Bundle().apply {
            putSerializable(KEY_TIME, time)
        }
    }

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

    override val fragmentTag: String
        get() = "date_button"

    override fun attachListener(dialogFragment: PlatformTimePicker) {
        dialogFragment.onTimeSetListener =
            PlatformTimePicker.OnTimeSetListener { newTime: LocalTime ->
                if (time != newTime) {
                    setTime(newTime)
                    host.onValueSet(this)
                }
            }
        dialogFragment.onDismissListener = PlatformTimePicker.OnDismissListener {
            dialogShown = false
        }
    }

    class PlatformTimePicker : DialogFragment() {
        fun interface OnTimeSetListener {
            fun onTimeSet(time: LocalTime)
        }

        fun interface OnDismissListener {
            fun onDismiss()
        }

        var onTimeSetListener: OnTimeSetListener? = null
        var onDismissListener: OnDismissListener? = null

        override fun onCreateDialog(savedInstanceState: Bundle?): TimePickerDialog {
            val time = requireArguments().getSerializable(KEY_TIME) as LocalTime
            return TimePickerDialog(
                context, R.style.ThemeOverlay_MaterialComponents_Dialog,
                { _, hourOfDay, minute ->
                    onTimeSetListener?.onTimeSet(
                        LocalTime.of(
                            hourOfDay,
                            minute
                        )
                    )
                },
                time.hour,
                time.minute,
                DateFormat.is24HourFormat(context)
            )
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            onDismissListener?.onDismiss()
        }
    }

    companion object {
        const val KEY_TIME = "time"
    }
}
