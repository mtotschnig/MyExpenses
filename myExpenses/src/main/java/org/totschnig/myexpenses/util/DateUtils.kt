package org.totschnig.myexpenses.util

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar

fun epoch2LocalDate(
    epochSecond: Long,
    zoneId: ZoneId? = ZoneId.systemDefault()
) = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), zoneId)
    .toLocalDate()

fun epochMillis2LocalDate(
    epochMillis: Long,
    zoneId: ZoneId? = ZoneId.systemDefault()
) = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId)
        .toLocalDate()

fun epoch2ZonedDateTime(
    epoch: Long,
    zoneId: ZoneId? = ZoneId.systemDefault()
) = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch), zoneId)

fun LocalDateTime.toEpoch() =
    ZonedDateTime.of(this, ZoneId.systemDefault()).toEpochSecond()

fun LocalDate.toEpoch() =
    ZonedDateTime.of(this, LocalTime.now(), ZoneId.systemDefault()).toEpochSecond()

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
