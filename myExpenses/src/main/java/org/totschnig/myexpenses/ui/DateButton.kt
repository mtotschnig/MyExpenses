package org.totschnig.myexpenses.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.DatePicker
import icepick.State
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.getDateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * A button that opens DateDialog, and stores the date in its state
 */
class DateButton(context: Context, attrs: AttributeSet?) : ButtonWithDialog(context, attrs) {
    @State @JvmField
    var date: LocalDate = LocalDate.now()

    private val formatter : DateTimeFormatter

    init {
        formatter = getDateTimeFormatter(context)
    }

    private val isBrokenSamsungDevice: Boolean
        get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true) && isBetweenAndroidVersions(
                Build.VERSION_CODES.LOLLIPOP,
                Build.VERSION_CODES.LOLLIPOP_MR1)

    override fun onCreateDialog(): Dialog {
        val brokenSamsungDevice = isBrokenSamsungDevice
        val base = context
        @SuppressLint("InlinedApi")
        val context = if (brokenSamsungDevice)
            ContextThemeWrapper(base,
                    if (base is ProtectedFragmentActivity && base.themeType == ProtectedFragmentActivity.ThemeType.dark)
                        android.R.style.Theme_Holo_Dialog
                    else
                        android.R.style.Theme_Holo_Light_Dialog)
        else
            base
        var yearOld = date.year
        var monthOld = date.monthValue - 1
        var dayOld = date.dayOfMonth
        /**
         * listens on changes in the date dialog and sets the date on the button
         */
        val mDateSetListener: DatePickerDialog.OnDateSetListener = object: DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
                if (yearOld != year ||
                        monthOld != month ||
                        dayOld != dayOfMonth) {
                    yearOld = year
                    monthOld = month
                    dayOld = dayOfMonth
                    setDate(LocalDate.of(year, month + 1, dayOfMonth))
                    host.onValueSet(this@DateButton)
                }
            }
        }
        val datePickerDialog = DatePickerDialog(context, mDateSetListener,
                yearOld, monthOld, dayOld)
        if (brokenSamsungDevice) {
            datePickerDialog.setTitle("")
            datePickerDialog.updateDate(yearOld, monthOld, dayOld)
        }
        if (PrefKey.GROUP_WEEK_STARTS.isSet) {
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

    private fun setFirstDayOfWeek(datePickerDialog: DatePickerDialog, startOfWeek: Int) {
        val calendarView = datePickerDialog.datePicker.calendarView
        calendarView.firstDayOfWeek = startOfWeek
    }

    private fun isBetweenAndroidVersions(min: Int, max: Int): Boolean {
        return Build.VERSION.SDK_INT >= min && Build.VERSION.SDK_INT <= max
    }

    fun setDate(localDate: LocalDate) {
        date = localDate
        update()
    }

    override fun update() {
        text = date.format(formatter)
    }
}
