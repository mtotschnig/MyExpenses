package org.totschnig.myexpenses.util

import android.content.Context
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.text.SimpleDateFormat

fun epochMillis2LocalDate(epochMillis: Long) = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).toLocalDate()

fun getDateTimeFormatter(context: Context): DateTimeFormatter =
        (Utils.getDateFormatSafe(context) as? SimpleDateFormat)?.let { DateTimeFormatter.ofPattern(it.toPattern()) }
                ?: DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)