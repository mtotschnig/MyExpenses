package org.totschnig.myexpenses.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.NonNull
import icepick.State
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.getDateTimeFormatter
import java.util.*


/**
 * A button that opens DateDialog, and stores the date in its state
 */

class DateButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ButtonWithDialog(context, attrs, defStyleAttr) {
    private var lastTouchDownX: Float = 0F
    private val marginTouchWidth = UiUtils.dp2Px(36F, resources)

    @State
    @JvmField
    var date: LocalDate = LocalDate.now()

    private val formatter: DateTimeFormatter = getDateTimeFormatter(context)

    init {
        UiUtils.setCompoundDrawablesCompatWithIntrinsicBounds(this, R.drawable.ic_chevron_start, 0, R.drawable.ic_chevron_end, 0)
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

    private val isBrokenSamsungDevice: Boolean
        get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true) && Build.VERSION.SDK_INT in Build.VERSION_CODES.LOLLIPOP..Build.VERSION_CODES.LOLLIPOP_MR1

    override fun onCreateDialog(prefHandler: PrefHandler): Dialog {
        val brokenSamsungDevice = isBrokenSamsungDevice
        val context = if (brokenSamsungDevice)
        //https://stackoverflow.com/a/34853067
            object : ContextWrapper(context) {
                private lateinit var wrappedResources: Resources
                override fun getResources(): Resources {
                    val r: Resources = super.getResources()
                    if (!::wrappedResources.isInitialized) {
                        @Suppress("DEPRECATION")
                        wrappedResources = object : Resources(r.assets, r.displayMetrics, r.configuration) {
                            @NonNull
                            @Throws(NotFoundException::class)
                            override fun getString(id: Int, vararg formatArgs: Any?): String {
                                return try {
                                    super.getString(id, formatArgs)
                                } catch (ifce: IllegalFormatConversionException) {
                                    CrashHandler.report(ifce)
                                    var template: String = super.getString(id)
                                    template = template.replace("%" + ifce.conversion, "%s")
                                    java.lang.String.format(configuration.locale, template, formatArgs)
                                }
                            }
                        }
                    }
                    return wrappedResources
                }
            }
        else
            context
        var yearOld = date.year
        var monthOld = date.monthValue - 1
        var dayOld = date.dayOfMonth

        /**
         * listens on changes in the date dialog and sets the date on the button
         */
        val mDateSetListener: DatePickerDialog.OnDateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            if (yearOld != year ||
                    monthOld != month ||
                    dayOld != dayOfMonth) {
                yearOld = year
                monthOld = month
                dayOld = dayOfMonth
                setDateInternal(LocalDate.of(year, month + 1, dayOfMonth))
            }
        }
        val datePickerDialog = DatePickerDialog(context, R.style.ThemeOverlay_MaterialComponents_Dialog,
                mDateSetListener, yearOld, monthOld, dayOld)
        if (prefHandler.isSet(PrefKey.GROUP_WEEK_STARTS)) {
            val startOfWeek = Utils.getFirstDayOfWeekFromPreferenceWithFallbackToLocale(Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                datePickerDialog.datePicker.firstDayOfWeek = startOfWeek
            } else {
                try {
                    setFirstDayOfWeek(datePickerDialog, startOfWeek)
                } catch (e: UnsupportedOperationException) {/*Nothing left to do*/
                }
            }
        }

        return datePickerDialog
    }

    override fun onPrepareDialog(dialog: Dialog) {
        (dialog as? DatePickerDialog)?.updateDate(date.year, date.monthValue - 1, date.dayOfMonth)
    }

    private fun setFirstDayOfWeek(datePickerDialog: DatePickerDialog, startOfWeek: Int) {
        @Suppress("DEPRECATION") val calendarView = datePickerDialog.datePicker.calendarView
        calendarView.firstDayOfWeek = startOfWeek
    }

    private fun setDateInternal(localDate: LocalDate) {
        setDate(localDate)
        host.onValueSet(this)
    }

    fun setDate(localDate: LocalDate) {
        date = localDate
        update()
    }

    override fun update() {
        text = date.format(formatter)
    }

    fun overrideText(text: CharSequence) {
        this.text = text
        setCompoundDrawables(null, null, null, null)
    }
}
