/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig
package org.totschnig.myexpenses.export.qif

import android.text.TextUtils
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.ParseException
import java.util.*
import java.util.regex.Pattern

object QifUtils {
    private val DATE_DELIMITER_PATTERN = Pattern.compile("/|'|\\.|-")
    private val WHITESPACE_PATTERN = Pattern.compile("\\s+")
    private val HOUR_DELIMITER_PATTERN = Pattern.compile(":")
    private val MONEY_PREFIX_PATTERN = Pattern.compile("\\D")
    @JvmStatic
    fun trimFirstChar(s: String): String {
        return if (s.length > 1) s.substring(1) else ""
    }

    /**
     * First tries to parse input as a date in the specified format. If this fails, try to split
     * input on white space in two chunks, and tries to parse first chunk as date, second as time
     */
    @JvmStatic
    fun parseDate(sDateTime: String, format: QifDateFormat): Date {
        return try {
            parseDateInternal(sDateTime, format).time
        } catch (e: IllegalArgumentException) {
            val dateTimeChunks = WHITESPACE_PATTERN.split(sDateTime)
            if (dateTimeChunks.size > 1) {
                try {
                    val cal = parseDateInternal(dateTimeChunks[0], format)
                    val timeChunks = HOUR_DELIMITER_PATTERN.split(dateTimeChunks[1])
                    cal[Calendar.HOUR_OF_DAY] = parseInt(timeChunks, 0, 0)
                    cal[Calendar.MINUTE] = parseInt(timeChunks, 1, 0)
                    cal[Calendar.SECOND] = parseInt(timeChunks, 2, 0)
                    return cal.time
                } catch (ignored: IllegalArgumentException) {
                }
            }
            Date()
        }
    }

    /**
     * Adopted from http://jgnash.svn.sourceforge.net/viewvc/jgnash/jgnash2/trunk/src/jgnash/imports/qif/QifUtils.java
     * @param sDateTime String QIF date to parse
     * @param format    String identifier of format to parse
     * @throws IllegalArgumentException if input cannot be parsed
     * @return Returns parsed date as Calendar
     */
    fun parseDateInternal(sDateTime: String, format: QifDateFormat): Calendar {
        val cal = Calendar.getInstance()
        var month = cal[Calendar.MONTH] + 1
        var day = cal[Calendar.DAY_OF_MONTH]
        var year = cal[Calendar.YEAR]
        val hourOfDay = 0
        val minute = 0
        val second = 0
        val dateChunks = DATE_DELIMITER_PATTERN.split(sDateTime)
        if (format == QifDateFormat.US) {
            month = parseInt(dateChunks, 0)
            day = parseInt(dateChunks, 1)
            year = parseInt(dateChunks, 2)
        } else if (format == QifDateFormat.EU) {
            day = parseInt(dateChunks, 0)
            month = parseInt(dateChunks, 1)
            year = parseInt(dateChunks, 2)
        } else if (format == QifDateFormat.YMD) {
            year = parseInt(dateChunks, 0)
            month = parseInt(dateChunks, 1)
            day = parseInt(dateChunks, 2)
        }
        if (year < 100) {
            year += if (year < 29) {
                2000
            } else {
                1900
            }
        }
        cal[year, month - 1, day, hourOfDay, minute] = second
        cal[Calendar.MILLISECOND] = 0
        return cal
    }

    private fun parseInt(array: Array<String>, position: Int, defaultValue: Int): Int {
        return try {
            parseInt(array, position)
        } catch (e: IllegalArgumentException) {
            defaultValue
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun parseInt(array: Array<String>, position: Int): Int {
        return try {
            array[position].trim { it <= ' ' }.toInt()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException(e)
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalArgumentException(e)
        }
    }

    /**
     * Parse the string with a maxSize, that takes the number of fraction digits of currency into account,
     * so that the number representing the amount in the database does not exceed approximately 1/10th of [Long.MAX_VALUE]
     */
    @JvmStatic
    fun parseMoney(money: String, currency: CurrencyUnit): BigDecimal {
        return parseMoney(money, 18 - currency.fractionDigits)
    }

    /**
     * Adopted from http://jgnash.svn.sourceforge.net/viewvc/jgnash/jgnash2/trunk/src/jgnash/imports/qif/QifUtils.java
     *
     * @param money   String to be parsed
     * @param maxSize maxSize of the result, as calculated by precision - scale, i.e.  the number of digits in front of the decimal point
     * @return BigDecimal, if the result exceeds maxSize, [IllegalArgumentException] is thrown
     */
    @JvmStatic
    fun parseMoney(money: String, maxSize: Int): BigDecimal {
        var result: BigDecimal
        val sMoney = money.trim { it <= ' ' }.replace(" ", "") // to be safe
        try {
            result = BigDecimal(sMoney)
        } catch (e: NumberFormatException) {
            /* there must be commas, etc in the number.  Need to look for them
             * and remove them first, and then try BigDecimal again.  If that
             * fails, then give up and use NumberFormat and scale it down
             * */
            val split = MONEY_PREFIX_PATTERN.split(sMoney)
            if (split.size >= 2) {
                val buf = StringBuilder()
                if (sMoney.startsWith("-")) {
                    buf.append('-')
                }
                var i = 0
                while (i < split.size - 1) {
                    buf.append(split[i])
                    i++
                }
                buf.append('.')
                buf.append(split[split.size - 1])
                try {
                    result = BigDecimal(buf.toString())
                } catch (e2: NumberFormatException) {
                    val formatter = NumberFormat.getNumberInstance()
                    result = try {
                        val num = formatter.parse(sMoney)
                        BigDecimal.valueOf(num.toFloat().toDouble())
                    } catch (ignored: ParseException) {
                        BigDecimal(0)
                    }
                    CrashHandler.report(Exception("Could not parse money $sMoney"))
                }
            } else {
                result = BigDecimal(0)
            }
        }
        require(result.precision() - result.scale() <= maxSize) { "$result exceeds maximum size of $maxSize" }
        return result
    }

    @JvmStatic
    fun isTransferCategory(category: String): Boolean {
        return category.isNotEmpty() && category.startsWith("[") && category.endsWith("]")
    }

    @JvmStatic
    fun twoSidesOfTheSameTransfer(
        fromAccount: QifAccount,
        fromTransaction: QifTransaction, toAccount: QifAccount,
        toTransaction: QifTransaction
    ): Boolean {
        return (toTransaction.isTransfer
                && toTransaction.toAccount == fromAccount.memo && fromTransaction.toAccount == toAccount.memo && fromTransaction.date == toTransaction.date && fromTransaction.amount == toTransaction.amount.negate())
    }
}