package org.totschnig.myexpenses.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.TextViewCompat
import com.evernote.android.state.State
import com.google.android.material.datepicker.MaterialDatePicker
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import org.totschnig.myexpenses.util.getDateTimeFormatter
import org.totschnig.myexpenses.util.ui.UiUtils
import org.totschnig.myexpenses.util.ui.preferredDatePickerBuilder
import org.totschnig.myexpenses.util.ui.resolveThemeColor
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


/**
 * A button that opens DateDialog, and stores the date in its state
 */

class DateButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ButtonWithDialog<MaterialDatePicker<Long>>(context, attrs, defStyleAttr) {

    private var lastTouchDownX: Float? = null
    private val marginTouchWidth = UiUtils.dp2Px(36F, resources)

    @State
    @JvmField
    var date: LocalDate = LocalDate.now()

    private val formatter: DateTimeFormatter = getDateTimeFormatter(context, shortYear = true)

    init {
        this.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_chevron_start,
            0,
            R.drawable.ic_chevron_end,
            0
        )
        compoundDrawablePadding = 0
        TextViewCompat.setCompoundDrawableTintList(
            this,
            ColorStateList.valueOf(
                resolveThemeColor(
                    getContext(),
                    androidx.appcompat.R.attr.colorPrimary
                )
            )
        )
        val horizontalPadding = 0
        setPaddingRelative(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)
        //noinspection ClickableViewAccessibility
        setOnTouchListener { _, motionEvent ->
            if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                lastTouchDownX = motionEvent.x
            }
            false
        }
    }


    override fun onClick() {
        lastTouchDownX?.let {
            when {
                it <= marginTouchWidth -> {
                    previousDay()
                }

                it >= width - marginTouchWidth -> {
                    nextDay()
                }

                else -> {
                    super.onClick()
                }
            }
        } ?: kotlin.run {
            super.onClick()
        }
    }

    private fun previousDay() {
        setDateInternal(date.minusDays(1))
    }

    private fun nextDay() {
        setDateInternal(date.plusDays(1))
    }

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

    override fun buildDialog() = preferredDatePickerBuilder(context)
        .setSelection(
            ZonedDateTime.of(date.atStartOfDay(), ZoneId.of("UTC")).toEpochSecond() * 1000
        )
        .build()

    override fun attachListener(dialogFragment: MaterialDatePicker<Long>) {
        dialogFragment.addOnPositiveButtonClickListener {
            setDateInternal(epochMillis2LocalDate(it, ZoneId.of("UTC")))
            context.injector.prefHandler()
                .putInt(PrefKey.DATE_PICKER_INPUT_MODE, dialogFragment.inputMode)
        }
        dialogFragment.addOnDismissListener {
            dialogShown = false
        }
    }

    override fun update() {
        text = date.format(formatter)
        contentDescription = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
    }

    fun overrideText(text: CharSequence) {
        this.text = text
        setCompoundDrawables(null, null, null, null)
    }
}
