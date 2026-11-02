package org.totschnig.myexpenses.next.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.totschnig.myexpenses.provider.filter.DateCriterion
import org.totschnig.myexpenses.provider.filter.Operation
import java.time.LocalDate

class SearchValuesTest {

    private val today = LocalDate.of(2026, 3, 15)

    @Test
    fun presetsCoverWholeMonthsAndYears() {
        assertEquals(LocalDate.of(2026, 3, 1) to LocalDate.of(2026, 3, 31), PeriodPreset.ThisMonth.range(today))
        assertEquals(LocalDate.of(2026, 2, 1) to LocalDate.of(2026, 2, 28), PeriodPreset.LastMonth.range(today))
        assertEquals(LocalDate.of(2026, 2, 14) to today, PeriodPreset.Last30Days.range(today))
        assertEquals(LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 12, 31), PeriodPreset.LastYear.range(today))
    }

    @Test
    fun recognizesAPreset() {
        assertEquals(PeriodPreset.ThisYear, PeriodPreset.of(PeriodPreset.ThisYear.criterion(today), today))
        assertNull(PeriodPreset.of(DateCriterion(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 31)), today))
    }

    @Test
    fun customPeriodsMayBeOpen() {
        val start = LocalDate.of(2026, 1, 5)
        val end = LocalDate.of(2026, 2, 5)
        assertEquals(DateCriterion(start, end), periodCriterion(end, start))
        assertEquals(DateCriterion(Operation.GTE, start), periodCriterion(start, null))
        assertEquals(start to null, DateCriterion(Operation.GTE, start).bounds())
        assertEquals(null to end, periodCriterion(null, end)!!.bounds())
        assertNull(periodCriterion(null, null))
    }

    @Test
    fun showsWholeMonthsAndYears() {
        assertEquals(
            PeriodShape.Month(LocalDate.of(2024, 2, 1)),
            PeriodShape.of(DateCriterion(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)))
        )
        assertEquals(PeriodShape.Year(2025), PeriodShape.of(PeriodPreset.LastYear.criterion(today)))
        assertNull(PeriodShape.of(PeriodPreset.Last30Days.criterion(today)))
        assertNull(PeriodShape.of(DateCriterion(Operation.LTE, today)))
    }

    @Test
    fun parsesAmounts() {
        assertEquals(1250L, parseAmount("12,5", 2))
        assertEquals(1250L, parseAmount(" 12.50 ", 2))
        assertEquals(5L, parseAmount("5", 0))
        assertNull(parseAmount("12,505", 2))
        assertNull(parseAmount("abc", 2))
        assertNull(parseAmount("", 2))
    }
}
