package org.totschnig.myexpenses.util

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.totschnig.myexpenses.R
import java.text.SimpleDateFormat

fun epoch2LocalDate(epochSecond: Long): LocalDate = ZonedDateTime.ofInstant(
    Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault()).toLocalDate()

fun epochMillis2LocalDate(epochMillis: Long, zoneId: ZoneId?): LocalDate = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(epochMillis), zoneId).toLocalDate()

fun epoch2ZonedDateTime(epoch: Long): ZonedDateTime = ZonedDateTime.ofInstant(
        Instant.ofEpochSecond(epoch), ZoneId.systemDefault())

fun localDateTime2Epoch(localDateTime: LocalDateTime) =
        ZonedDateTime.of(localDateTime, ZoneId.systemDefault()).toEpochSecond()

fun localDate2Epoch(localDate: LocalDate) =
    ZonedDateTime.of(localDate, LocalTime.now(), ZoneId.systemDefault()).toEpochSecond()

fun localDateTime2EpochMillis(localDateTime: LocalDateTime) = localDateTime2Epoch(localDateTime) * 1000

fun getDateTimeFormatter(context: Context): DateTimeFormatter =
        (Utils.getDateFormatSafe(context) as? SimpleDateFormat)?.let { DateTimeFormatter.ofPattern(it.toPattern()) }
                ?: DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

@SuppressLint("SimpleDateFormat")
fun validateDateFormat(dateFormat: String) = when {
    TextUtils.isEmpty(dateFormat) -> null
    Regex("[^\\p{P}Mdy]").containsMatchIn(dateFormat) -> R.string.date_format_unsupported_character
    !(dateFormat.contains("d") && dateFormat.contains("M") && dateFormat.contains("y")) -> R.string.date_format_specifier_missing
    else -> try {
        SimpleDateFormat(dateFormat)
        null
    } catch (e: IllegalArgumentException) {
        R.string.date_format_illegal
    }
}
