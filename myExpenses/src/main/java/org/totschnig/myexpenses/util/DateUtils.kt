package org.totschnig.myexpenses.util

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.text.format.DateFormat.is24HourFormat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Locale

fun epoch2LocalDate(
    epochSecond: Long,
    zoneId: ZoneId? = ZoneId.systemDefault(),
): LocalDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), zoneId)
    .toLocalDate()

fun epoch2LocalDateTime(
    epochSecond: Long,
    zoneId: ZoneId? = ZoneId.systemDefault(),
): LocalDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), zoneId)
    .toLocalDateTime()

fun epochMillis2LocalDate(
    epochMillis: Long,
    zoneId: ZoneId? = ZoneId.systemDefault(),
): LocalDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId)
        .toLocalDate()

fun epoch2ZonedDateTime(
    epoch: Long,
    zoneId: ZoneId? = ZoneId.systemDefault(),
): ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch), zoneId)

fun LocalDateTime.toEpoch() =
    ZonedDateTime.of(this, ZoneId.systemDefault()).toEpochSecond()

fun LocalDate.toEpoch(atTime: LocalTime = LocalTime.NOON) =
    ZonedDateTime.of(this, atTime, ZoneId.systemDefault()).toEpochSecond()

fun LocalDateTime.toEpochMillis() = toEpoch() * 1000

fun getDateTimeFormatter(context: Context, shortYear: Boolean = false): DateTimeFormatter = ((
        if (shortYear)
            Utils.ensureDateFormatWithShortYear(context)
        else
            Utils.getDateFormatSafe(context)
        )  as? SimpleDateFormat)?.let {
        try {
            DateTimeFormatter.ofPattern(it.toPattern())
        } catch (e: Exception) {
            Timber.w("Unable to get DateTimeFormatter from %s", it.toPattern())
            CrashHandler.report(e)
            null
        }
    } ?: DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

fun LocalDate.toStartOfDayEpoch(): Long = atTime(LocalTime.MIN).toEpoch()
fun LocalDate.toEndOfDayEpoch(): Long = atTime(LocalTime.MAX).toEpoch()

@SuppressLint("SimpleDateFormat")
fun validateDateFormat(context: Context, dateFormat: String) = when {
    TextUtils.isEmpty(dateFormat) -> null
    Regex("[^\\p{P}Mdy]").containsMatchIn(dateFormat) -> context.getString(R.string.date_format_unsupported_character)
    !(dateFormat.contains("d") && dateFormat.contains("M") && dateFormat.contains("y")) ->
        context.getString(R.string.date_format_specifier_missing)

    else -> try {
        DateTimeFormatter.ofPattern(SimpleDateFormat(dateFormat).toPattern())
        null
    } catch (e: IllegalArgumentException) {
        context.getString(R.string.date_format_illegal) + " (${e.safeMessage})"
    }
}

val Int.toDayOfWeek: DayOfWeek
    get() = when(this) {
        Calendar.SUNDAY -> DayOfWeek.SUNDAY
        Calendar.MONDAY -> DayOfWeek.MONDAY
        Calendar.TUESDAY -> DayOfWeek.TUESDAY
        Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
        Calendar.THURSDAY -> DayOfWeek.THURSDAY
        Calendar.FRIDAY -> DayOfWeek.FRIDAY
        Calendar.SATURDAY -> DayOfWeek.SATURDAY
        else -> throw IllegalArgumentException()
    }

/**
 * Returns the correct time format pattern string based on both the user's Locale
 * and their 24-hour system setting.
 */
val Context.timeFormatterPattern: String
    get() {

    val localizedPattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        null, // No date part
        FormatStyle.SHORT,
        IsoChronology.INSTANCE,
        resources.configuration.locales[0] ?: Locale.getDefault()
    )

    val is24h = is24HourFormat(this)

    // 4. Return the pattern, adjusting for 24-hour format if necessary.
    return when {
        // If the system is 24h and the locale pattern is 12h, convert it.
        is24h && localizedPattern.contains("h") ->
            localizedPattern.replace("h", "H").replace(" a", "").trim()
        // If the system is 12h and the locale pattern is 24h, convert it.
        !is24h && localizedPattern.contains("H") ->
            localizedPattern.replace("H", "h") + " a"
        // Otherwise, the pattern already matches the system setting.
        else -> localizedPattern
    }
}


val Context.timeFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern(timeFormatterPattern)