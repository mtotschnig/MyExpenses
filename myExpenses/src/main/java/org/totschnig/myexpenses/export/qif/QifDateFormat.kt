/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig
package org.totschnig.myexpenses.export.qif

import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/12/11 1:57 AM
 */
enum class QifDateFormat(private val displayLabel: String) {
    US("MM/dd/yyyy, MM.dd.yy, …"),
    EU("dd/MM/yyyy, dd.MM.yy, …"),
    YMD("yyyy/MM/dd, yy.MM.dd, …");

    override fun toString() = displayLabel

    companion object {

        fun defaultForLocale(locale: Locale) = when(String(
            DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                FormatStyle.SHORT,
                null,
                IsoChronology.INSTANCE, locale
            ).toCharArray().filter { it in setOf('y', 'M', 'd') }.distinct().toCharArray()
        )) {
            "yMd" -> YMD
            "Mdy" -> US
            else -> EU
        }

        val default
            get() = defaultForLocale(Locale.getDefault())
    }
}