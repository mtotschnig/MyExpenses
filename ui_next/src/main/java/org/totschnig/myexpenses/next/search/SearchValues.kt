package org.totschnig.myexpenses.next.search

import androidx.annotation.StringRes
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.provider.filter.DateCriterion
import org.totschnig.myexpenses.provider.filter.Operation
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Periods that are searched often, offered before a custom one */
enum class PeriodPreset(@param:StringRes val label: Int) {
    ThisMonth(R.string.next_search_this_month),
    LastMonth(R.string.next_search_last_month),
    Last30Days(R.string.next_search_last_30_days),
    ThisYear(R.string.next_search_this_year),
    LastYear(R.string.next_search_last_year);

    fun range(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
        ThisMonth -> today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
        LastMonth -> today.minusMonths(1).let { it.withDayOfMonth(1) to it.with(TemporalAdjusters.lastDayOfMonth()) }
        Last30Days -> today.minusDays(29) to today
        ThisYear -> today.withDayOfYear(1) to today.with(TemporalAdjusters.lastDayOfYear())
        LastYear -> today.minusYears(1).let { it.withDayOfYear(1) to it.with(TemporalAdjusters.lastDayOfYear()) }
    }

    fun criterion(today: LocalDate) = range(today).let { (start, end) -> DateCriterion(start, end) }

    companion object {
        fun of(criterion: DateCriterion, today: LocalDate) = entries.find { it.criterion(today) == criterion }
    }
}

/** A period with an open start or end, as the date filter stores it */
fun periodCriterion(start: LocalDate?, end: LocalDate?): DateCriterion? = when {
    start != null && end != null -> if (start <= end) DateCriterion(start, end) else DateCriterion(end, start)
    start != null -> DateCriterion(Operation.GTE, start)
    end != null -> DateCriterion(Operation.LTE, end)
    else -> null
}

/** Start and end of [criterion], null where it is open */
fun DateCriterion.bounds(): Pair<LocalDate?, LocalDate?> = when (operation) {
    Operation.BTW -> values[0] to values[1]
    Operation.GTE, Operation.GT -> values[0] to null
    else -> null to values[0]
}

/** A whole month or year, shown as such on the chip, e.g. "October 2026" */
sealed interface PeriodShape {
    data class Month(val first: LocalDate) : PeriodShape
    data class Year(val year: Int) : PeriodShape

    companion object {
        fun of(criterion: DateCriterion): PeriodShape? {
            if (criterion.operation != Operation.BTW) return null
            val (start, end) = criterion.values
            return when {
                start.dayOfMonth == 1 && end == start.with(TemporalAdjusters.lastDayOfMonth()) -> Month(start)
                start.dayOfYear == 1 && end == start.with(TemporalAdjusters.lastDayOfYear()) -> Year(start.year)
                else -> null
            }
        }
    }
}

private val amountPattern = Regex("""[+\-−]?(\d+)(?:[.,](\d+))?""")

/**
 * An amount typed by the user, e.g. "12,50", in minor units of a currency with [fractionDigits],
 * without sign. Null if it is no number or has more decimals than the currency.
 */
fun parseAmount(text: String, fractionDigits: Int): Long? {
    val (whole, fraction) = amountPattern.matchEntire(text.trim())?.destructured ?: return null
    if (fraction.length > fractionDigits) return null
    return (whole + fraction.padEnd(fractionDigits, '0')).toBigInteger()
        .takeIf { it.bitLength() < 63 }?.toLong()
}
