package org.totschnig.myexpenses.util

import android.content.Context
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.text.SimpleDateFormat

fun epochMillis2LocalDate(epochMillis: Long) = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).toLocalDate()

fun localDateTime2Epoch(localDateTime: LocalDateTime) =
        ZonedDateTime.of(localDateTime, ZoneId.systemDefault()).toEpochSecond()

fun localDateTime2EpochMillis(localDateTime: LocalDateTime) = localDateTime2Epoch(localDateTime) * 1000

fun getDateTimeFormatter(context: Context): DateTimeFormatter =
        (Utils.getDateFormatSafe(context) as? SimpleDateFormat)?.let { DateTimeFormatter.ofPattern(it.toPattern()) }
                ?: DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)