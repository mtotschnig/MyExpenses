package org.totschnig.myexpenses.testutils

import android.annotation.TargetApi
import android.icu.text.DateFormat
import android.icu.text.DisplayContext
import android.icu.util.TimeZone
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import java.util.Date
import java.util.Locale

const val UTC = "UTC"

//copied from material-components-android
fun getMonthDayOfWeekDay(timeInMillis: Long, locale: Locale?): String? {
    if (VERSION.SDK_INT >= VERSION_CODES.N) {
        return getMonthWeekdayDayFormat(locale).format(Date(timeInMillis))
    }
    return getFullFormat(locale).format(Date(timeInMillis))
}

@TargetApi(VERSION_CODES.N)
fun getMonthWeekdayDayFormat(locale: Locale?): DateFormat {
    return getAndroidFormat(DateFormat.MONTH_WEEKDAY_DAY, locale)
}

@TargetApi(VERSION_CODES.N)
private fun getAndroidFormat(pattern: String?, locale: Locale?): DateFormat {
    val format =
        DateFormat.getInstanceForSkeleton(pattern, locale)
    format.setTimeZone(getUtcAndroidTimeZone())
    format.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE)
    return format
}

@TargetApi(VERSION_CODES.N)
private fun getUtcAndroidTimeZone(): TimeZone? {
    return TimeZone.getTimeZone(UTC)
}

fun getFullFormat(locale: Locale?): java.text.DateFormat {
    return getFormat(java.text.DateFormat.FULL, locale)
}

private fun getFormat(style: Int, locale: Locale?): java.text.DateFormat {
    val format = java.text.DateFormat.getDateInstance(style, locale)
    format.setTimeZone(getTimeZone())
    return format
}

private fun getTimeZone(): java.util.TimeZone? {
    return java.util.TimeZone.getTimeZone(UTC)
}
