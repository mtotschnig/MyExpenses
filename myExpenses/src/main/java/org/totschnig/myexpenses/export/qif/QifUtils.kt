/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig
package org.totschnig.myexpenses.export.qif

import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.io.ImportTransaction
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.ParseException
import java.util.*
import java.util.regex.Pattern

object QifUtils {
    private val DATE_DELIMITER_PATTERN = Pattern.compile("[/'.\\-]|(?<=\\d)(?=[a-zA-Z])|(?<=[a-zA-Z])(?=\\d)")
    private val WHITESPACE_PATTERN = Pattern.compile("(\\s|,)+")
    private val HOUR_DELIMITER_PATTERN = Pattern.compile(":")
    private val MONEY_PREFIX_PATTERN = Pattern.compile("\\D")
    private val ISO_T_PATTERN = Regex("(\\d)T(\\d)")
    @JvmStatic
    fun trimFirstChar(s: String): String {
        return if (s.length > 1) s.substring(1) else ""
    }

    @JvmStatic
    fun parseDate(sDateTime: String, format: QifDateFormat): Date {
        val s = sDateTime.trim().replace(ISO_T_PATTERN, "$1 $2")
        return try {
            parseDateInternal(s, format).time
        } catch (_: IllegalArgumentException) {
            val dateTimeChunks = WHITESPACE_PATTERN.split(s).let {
                if(isWeekday(it[0])) it.drop(1).toTypedArray() else it
            }
            // If we have at least 3 chunks, they might be the D, M, and Y parts
            if (dateTimeChunks.size >= 3) {
                try {
                    // Join them back with a standard delimiter to reuse parseDateInternal logic
                    val datePart = dateTimeChunks.slice(0..2).joinToString("/")
                    val cal = parseDateInternal(datePart, format)
                    // If there's a 4th chunk, it's likely the time
                    if (dateTimeChunks.size > 3) {
                        val timeChunks = HOUR_DELIMITER_PATTERN.split(dateTimeChunks[3])
                        cal[Calendar.HOUR_OF_DAY] = parseInt(timeChunks, 0)
                        cal[Calendar.MINUTE] = parseInt(timeChunks, 1)
                        cal[Calendar.SECOND] = parseInt(timeChunks, 2)
                    }
                    return cal.time
                } catch (_: IllegalArgumentException) {}
            }
            // Fallback for "DATE TIME" where DATE has no internal spaces
            if (dateTimeChunks.size > 1) {
                try {
                    val cal = parseDateInternal(dateTimeChunks[0], format)
                    val timeChunks = HOUR_DELIMITER_PATTERN.split(dateTimeChunks[1])
                    cal[Calendar.HOUR_OF_DAY] = parseIntSafe(timeChunks, 0)
                    cal[Calendar.MINUTE] = parseIntSafe(timeChunks, 1)
                    cal[Calendar.SECOND] = parseIntSafe(timeChunks, 2)
                    return cal.time
                } catch (_: IllegalArgumentException) {}
            }
            Date()
        }
    }

    fun parseDateInternal(sDateTime: String, format: QifDateFormat): Calendar {

        val s = sDateTime.trim().trim('"') // Also trim quotes for CSV

        // 2. Handle pure numeric YYYYMMDD
        if (s.length == 8 && s.all { it.isDigit() }) {
            val cal = Calendar.getInstance(Locale.US)
            val y = s.substring(0, 4).toInt()
            val m = s.substring(4, 6).toInt()
            val d = s.substring(6, 8).toInt()
            cal[y, m - 1, d, 0, 0] = 0
            cal.set(Calendar.MILLISECOND, 0)
            return cal
        }

        val cal = Calendar.getInstance()
        var month: Int
        var day: Int
        var year: Int
        val dateChunks = DATE_DELIMITER_PATTERN.split(s)
        when (format) {
            QifDateFormat.US -> {
                month = parseMonth(dateChunks, 0)
                day = parseInt(dateChunks, 1)
                year = parseInt(dateChunks, 2)
            }
            QifDateFormat.EU -> {
                day = parseInt(dateChunks, 0)
                month = parseMonth(dateChunks, 1)
                year = parseInt(dateChunks, 2)
            }
            QifDateFormat.YMD -> {
                year = parseInt(dateChunks, 0)
                month = parseMonth(dateChunks, 1)
                day = parseInt(dateChunks, 2)
            }
        }
        if (year < 100) {
            year += if (year < 29) {
                2000
            } else {
                1900
            }
        }
        cal[year, month - 1, day, 0, 0] = 0
        cal[Calendar.MILLISECOND] = 0
        return cal
    }

    private fun isWeekday(name: String): Boolean {
        for (locale in listOf(Locale.getDefault(), Locale.US)) {
            val names = Calendar.getInstance(locale).getDisplayNames(Calendar.DAY_OF_WEEK, Calendar.ALL_STYLES, locale)
            if (names?.keys?.any { it.equals(name, ignoreCase = true) } == true) return true
        }
        return false
    }

    private fun parseMonth(array: Array<String>, position: Int) = try {
        val s = array[position].trim()
        // Try numeric first, then look up by name
        s.toIntOrNull() ?: findMonthByName(s) ?: throw IllegalArgumentException("Invalid month: $s")
    } catch (e: IndexOutOfBoundsException) {
        throw IllegalArgumentException(e)
    }

    private fun findMonthByName(name: String): Int? {
        // Check current locale and English (US) as fallback
        for (locale in listOf(Locale.getDefault(), Locale.US)) {
            val names = Calendar.getInstance(locale).getDisplayNames(Calendar.MONTH, Calendar.ALL_STYLES, locale)
            names?.entries?.find { it.key.equals(name, ignoreCase = true) }?.value?.let {
                return it + 1
            }
        }
        return null
    }

    private fun parseIntSafe(array: Array<String>, position: Int) = try {
        parseInt(array, position)
    } catch (_: IllegalArgumentException) {
        0
    }

    @Throws(IllegalArgumentException::class)
    private fun parseInt(array: Array<String>, position: Int) = try {
        array[position].trim().toInt()
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException(e)
    } catch (e: IndexOutOfBoundsException) {
        throw IllegalArgumentException(e)
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
        val sMoney = money.trim().replace(" ", "") // to be safe
        try {
            result = BigDecimal(sMoney)
        } catch (_: NumberFormatException) {
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
                } catch (_: NumberFormatException) {
                    val formatter = NumberFormat.getNumberInstance()
                    result = try {
                        val num = formatter.parse(sMoney)
                        BigDecimal.valueOf(num.toFloat().toDouble())
                    } catch (_: ParseException) {
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
        fromAccount: ImportAccount,
        fromTransaction: ImportTransaction,
        toAccount: ImportAccount,
        toTransaction: ImportTransaction
    ) = toTransaction.isTransfer &&
            toTransaction.toAccount == fromAccount.memo &&
            fromTransaction.toAccount == toAccount.memo &&
            fromTransaction.date == toTransaction.date

    /**
     * first we transform all transfers without counterpart into normal transactions
     * second we remove one part of the transfer
     */
    fun reduceTransfers(
        accounts: List<ImportAccount>
    ) = accounts
        .map { it.copy(transactions = transformUnknownTransfers(it, it.transactions, accounts)) }
        .map { it.copy(transactions = reduceTransfers(it, accounts)) }

    private fun pickBestTransferPeer(
        fromAccount: ImportAccount,
        fromTransaction: ImportTransaction,
        toAccount: ImportAccount,
        toTransactions: List<ImportTransaction>?
    ): ImportTransaction? = toTransactions?.filter { toTransaction ->
        twoSidesOfTheSameTransfer(fromAccount, fromTransaction, toAccount, toTransaction)
    }?.let { candidates ->
        candidates.firstOrNull { it.amount.compareTo(fromTransaction.amount.negate()) == 0 } ?: candidates.firstOrNull()
    }

    /**
     * We remove one side of the transfer (either the one that is not part of a split or the one
     * that is an expense.
     */
    private fun reduceTransfers(
        fromAccount: ImportAccount,
        allAccounts: List<ImportAccount>
    ): List<ImportTransaction> {
        return fromAccount.transactions.mapNotNull { fromTransaction ->
            if (fromTransaction.isTransfer) {
                if (fromTransaction.toAccount != fromAccount.memo) {
                    allAccounts.find { it.memo == fromTransaction.toAccount }?.let { toAccount ->
                        //pair of transaction, and if it is a split part
                        val transferPeer: Pair<ImportTransaction, Boolean>? =
                           pickBestTransferPeer(fromAccount, fromTransaction, toAccount, toAccount.transactions) ?.let { it to false } ?:
                        (toAccount.transactions.firstNotNullOfOrNull { toTransaction ->
                            pickBestTransferPeer(fromAccount, fromTransaction, toAccount, toTransaction.splits)
                        })?.let { it to true }
                        if (transferPeer == null) fromTransaction
                        else if (fromTransaction.amount.signum() == 1 && !transferPeer.second) fromTransaction.copy(
                            toAmount = transferPeer.first.amount
                        ) else null
                    }
                } else fromTransaction
            }
            else if (fromTransaction.isSplit) {
                fromTransaction.copy(splits = fromTransaction.splits?.mapNotNull { split ->
                    if (split.isTransfer) {
                        if (split.toAccount != fromAccount.memo) {
                            allAccounts.find { it.memo == split.toAccount }?.let { toAccount ->
                                val transferPeer = pickBestTransferPeer(fromAccount, split, toAccount, toAccount.transactions)
                                if (transferPeer == null) split else split.copy(toAmount = transferPeer.amount)
                            }
                        } else split
                    } else split
                })
            }
            else fromTransaction
        }
    }

    private fun transformUnknownTransfers(
        fromAccount: ImportAccount,
        transactions: List<ImportTransaction>,
        allAccounts: List<ImportAccount>
    ): List<ImportTransaction> = transactions.map { fromTransaction ->
        (if (fromTransaction.isTransfer) {
            var shouldTransform = true
            if (fromTransaction.toAccount != fromAccount.memo) {
                allAccounts.find { it.memo == fromTransaction.toAccount }?.let { toAccount ->
                    val hasCounterPart = toAccount.transactions.any { toTransaction ->
                        twoSidesOfTheSameTransfer(fromAccount, fromTransaction, toAccount, toTransaction) ||
                                toTransaction.splits?.any { split ->
                                    twoSidesOfTheSameTransfer(fromAccount, fromTransaction, toAccount, split)
                                } == true
                    }
                    shouldTransform = !hasCounterPart
                }
            }
            if (shouldTransform) convertIntoRegularTransaction(fromTransaction) else fromTransaction
        } else fromTransaction).copy(splits = fromTransaction.splits?.let {
            transformUnknownTransfers(fromAccount, it, allAccounts)
        })
    }

    private fun convertIntoRegularTransaction(fromTransaction: ImportTransaction) = fromTransaction.copy(
            memo = prependMemo("Transfer: " + fromTransaction.toAccount, fromTransaction),
            toAccount = null
        )

    private fun prependMemo(prefix: String, fromTransaction: ImportTransaction): String {
        return if (fromTransaction.memo.isNullOrEmpty()) {
            prefix + " | " + fromTransaction.memo
        } else {
            prefix
        }
    }

}