package org.totschnig.myexpenses.ui

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.FragmentManager
import com.google.android.material.timepicker.MaterialTimePicker
import icepick.State
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

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

    override fun showDialog(fragmentManager: FragmentManager) {
        super.showDialog(fragmentManager)
        val picker = MaterialTimePicker.Builder()
            .setHour(time.hour)
            .setMinute(time.minute)
            .build()
        attachListener(picker)
        picker.show(fragmentManager, fragmentTag)
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

    override fun attachListener(dialogFragment: MaterialTimePicker) {
        dialogFragment.addOnPositiveButtonClickListener {
            setTime(LocalTime.of(dialogFragment.hour, dialogFragment.minute))
            host.onValueSet(this)
        }
    }
}