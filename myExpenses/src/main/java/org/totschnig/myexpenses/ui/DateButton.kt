package org.totschnig.myexpenses.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Build
import android.util.AttributeSet
import android.widget.DatePicker
import androidx.annotation.NonNull
import icepick.State
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.getDateTimeFormatter
import java.util.*


/**
 * A button that opens DateDialog, and stores the date in its state
 */
class DateButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :  ButtonWithDialog(context, attrs, defStyleAttr) {
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
        val context = if (brokenSamsungDevice)
            //https://stackoverflow.com/a/34853067
            object : ContextWrapper(base) {
                private lateinit var wrappedResources: Resources
                override fun getResources(): Resources {
                    val r: Resources = super.getResources()
                    if (!::wrappedResources.isInitialized) {
                        wrappedResources = object : Resources(r.getAssets(), r.getDisplayMetrics(), r.getConfiguration()) {
                            @NonNull
                            @Throws(NotFoundException::class)
                            override fun getString(id: Int, vararg formatArgs: Any?): String {
                                return try {
                                    super.getString(id, formatArgs)
                                } catch (ifce: IllegalFormatConversionException) {
                                    CrashHandler.report(ifce)
                                    var template: String = super.getString(id)
                                    template = template.replace("%" + ifce.conversion, "%s")
                                    java.lang.String.format(getConfiguration().locale, template, formatArgs)
                                }
                            }
                        }
                    }
                    return wrappedResources
                }
            }
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
        val datePickerDialog = DatePickerDialog(context, R.style.ThemeOverlay_MaterialComponents_Dialog,
                mDateSetListener, yearOld, monthOld, dayOld)
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
