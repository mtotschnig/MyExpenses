package org.totschnig.myexpenses.preference

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import org.totschnig.myexpenses.util.ui.preferredTimePickerBuilder
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

class TimePreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private val hour: Int
        get() = getPersistedInt(DEFAULT_VALUE) / 100
    private val minute: Int
        get() {
            val hm = getPersistedInt(DEFAULT_VALUE)
            val h = hm / 100
            return hm - 100 * h
        }

    override fun getSummary(): String = LocalTime.of(hour, minute)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

    override fun onClick() {
        val picker = preferredTimePickerBuilder(context)
            .setHour(hour)
            .setMinute(minute)
            .build()

        picker.addOnPositiveButtonClickListener {
            val value = 100 * picker.hour + picker.minute
            if (callChangeListener(value)) {
                persistInt(value)
                notifyChanged()
            }
        }

        picker.show((context as FragmentActivity).supportFragmentManager, "timepicker")
    }

    companion object {
        const val DEFAULT_VALUE = 700

        /**
         * @return offset from current time until scheduled time in milliseconds
         */
        fun getScheduledTime(
            prefHandler: PrefHandler,
            prefKey: PrefKey,
            clock: Clock = Clock.systemDefaultZone()
        ): Long {
            val value = prefHandler.getInt(prefKey, DEFAULT_VALUE)
            val hh = value / 100
            val mm = value - 100 * hh
            return getScheduledTime(hh, mm, clock)
        }

        fun getScheduledTime(
            hh: Int,
            mm: Int,
            clock: Clock = Clock.systemDefaultZone()
        ): Long {
            val now = clock.instant()
            val scheduledTime = ZonedDateTime.of(LocalDate.now(), LocalTime.of(hh, mm), ZoneId.systemDefault()).toInstant().let {
                if (now > it) it.plus(1, ChronoUnit.DAYS) else it
            }
            return Duration.between(now, scheduledTime).toMillis()
        }
    }
}
