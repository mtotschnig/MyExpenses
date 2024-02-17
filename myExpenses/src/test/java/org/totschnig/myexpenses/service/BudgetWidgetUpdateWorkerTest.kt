@file:Suppress("JUnitMalformedDeclaration", "unused")

package org.totschnig.myexpenses.service

import com.google.common.truth.Truth.assertThat
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.prefHandler
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters.next

@RunWith(JUnitParamsRunner::class)
class BudgetWidgetUpdateWorkerTest {

    private fun fixedClock(localDate: LocalDate) =
        Clock.fixed(
            ZonedDateTime.of(
                localDate.atTime(LocalTime.NOON),
                ZoneId.systemDefault()
            ).toInstant(),
            ZoneId.systemDefault()
        )

    private fun atMidnight(localDate: LocalDate) =
        ZonedDateTime.of(
            localDate.atTime(LocalTime.MIDNIGHT),
            ZoneId.systemDefault()
        ).toInstant()

    private val referenceDate = LocalDate.of(2024, 6, 15)


    @Test
    @Parameters(method = "parametersNextDay")
    fun getNextDay(now: LocalDate, next: LocalDate) {
        doTheTest(Grouping.DAY, now, next)
    }

    @Test
    @Parameters(method = "parametersNextWeek")
    fun getNextWeek(weekStart: DayOfWeek, now: LocalDate, next: LocalDate) {
        whenever(prefHandler.weekStartAsDayOfWeek).thenReturn(weekStart)
        doTheTest(Grouping.WEEK, now, next)
    }

    @Test
    @Parameters(method = "parametersNextMonth")
    fun getNextMonth(monthStart: Int, now: LocalDate, next: LocalDate) {
        whenever(prefHandler.monthStart).thenReturn(monthStart)
        doTheTest(Grouping.MONTH, now, next)
    }

    @Test
    fun getNextYear() {
        doTheTest(Grouping.YEAR, referenceDate, LocalDate.of(2025, 1, 1))
    }

    private fun doTheTest(grouping: Grouping, now: LocalDate, next: LocalDate) {
        assertThat(
            BudgetWidgetUpdateWorker.getNextScheduledTime(
                grouping, prefHandler, fixedClock(now)
            )
        ).isEqualTo(atMidnight(next))
    }

    private fun parametersNextDay(): Array<Any> = arrayOf(
        arrayOf(referenceDate, LocalDate.of(2024, 6, 16)),
        arrayOf(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 1)),
        arrayOf(LocalDate.of(2024, 12, 31), LocalDate.of(2025, 1, 1)),
    )

    private fun parametersNextWeek(): Array<Any> {
        val referenceDay = referenceDate.with(next(DayOfWeek.SUNDAY))
        return arrayOf(
            arrayOf(DayOfWeek.SUNDAY, referenceDay, referenceDay.plusDays(7)),
            arrayOf(DayOfWeek.MONDAY, referenceDay, referenceDay.plusDays(1)),
            arrayOf(DayOfWeek.TUESDAY, referenceDay, referenceDay.plusDays(2)),
            arrayOf(DayOfWeek.WEDNESDAY, referenceDay, referenceDay.plusDays(3)),
            arrayOf(DayOfWeek.THURSDAY, referenceDay, referenceDay.plusDays(4)),
            arrayOf(DayOfWeek.FRIDAY, referenceDay, referenceDay.plusDays(5)),
            arrayOf(DayOfWeek.SATURDAY, referenceDay, referenceDay.plusDays(6)),
        )
    }

    private fun parametersNextMonth() : Array<Any> {
        return arrayOf(
            arrayOf(1, referenceDate, LocalDate.of(2024, 7, 1)),
            arrayOf(20, referenceDate, LocalDate.of(2024, 6, 20)),
            arrayOf(10, referenceDate, LocalDate.of(2024, 7, 10))
        )
    }
}