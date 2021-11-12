package org.totschnig.myexpenses.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.datepicker.MaterialDatePicker
import icepick.State
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import org.totschnig.myexpenses.util.getDateTimeFormatter


/**
 * A button that opens DateDialog, and stores the date in its state
 */

class DateButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ButtonWithDialog<MaterialDatePicker<Long>>(context, attrs, defStyleAttr) {

    private var lastTouchDownX: Float = 0F
    private val marginTouchWidth = UiUtils.dp2Px(36F, resources)

    @State
    @JvmField
    var date: LocalDate = LocalDate.now()

    private val formatter: DateTimeFormatter = getDateTimeFormatter(context)

    init {
        this.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_chevron_start,
            0,
            R.drawable.ic_chevron_end,
            0
        )
        setPaddingRelative(0, paddingTop, 0, paddingBottom)
        compoundDrawablePadding = UiUtils.dp2Px(-6F, resources)
        //noinspection ClickableViewAccessibility
        setOnTouchListener { _, motionEvent ->
            if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                lastTouchDownX = motionEvent.x
            }
            false
        }
    }

    override fun onClick() {
        when {
            lastTouchDownX <= marginTouchWidth -> {
                previousDay()
            }
            lastTouchDownX >= width - marginTouchWidth -> {
                nextDay()
            }
            else -> {
                super.onClick()
            }
        }
    }

    private fun previousDay() {
        setDateInternal(date.minusDays(1))
    }

    private fun nextDay() {
        setDateInternal(date.plusDays(1))
    }

    //TODO setFirstDayOfWeek https://github.com/material-components/material-components-android/issues/1949

    private fun setDateInternal(localDate: LocalDate) {
        setDate(localDate)
        host.onValueSet(this)
    }

    fun setDate(localDate: LocalDate) {
        date = localDate
        update()
    }

    override val fragmentTag: String
        get() = "date_button"

    override fun buildDialog() = MaterialDatePicker.Builder.datePicker()
        .setSelection(
            ZonedDateTime.of(date.atStartOfDay(), ZoneId.of("UTC")).toEpochSecond() * 1000
        )
        .build()

    override fun attachListener(dialogFragment: MaterialDatePicker<Long>) {
        dialogFragment.addOnPositiveButtonClickListener {
            setDateInternal(epochMillis2LocalDate(it, ZoneId.of("UTC")))
        }
        dialogFragment.addOnDismissListener {
            dialogShown = false
        }
    }

    override fun update() {
        text = date.format(formatter)
    }

    fun overrideText(text: CharSequence) {
        this.text = text
        setCompoundDrawables(null, null, null, null)
    }
}
