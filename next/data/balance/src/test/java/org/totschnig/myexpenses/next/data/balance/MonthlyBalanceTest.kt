package org.totschnig.myexpenses.next.data.balance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.ContractDirection
import org.totschnig.myexpenses.next.data.contracts.ContractInterval
import org.totschnig.myexpenses.next.data.contracts.ContractTransaction
import java.time.LocalDate

class MonthlyBalanceTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)
    private val giro = 1L
    private val creditCard = 2L
    private val savingsAccount = 3L
    private val daily = setOf(giro, creditCard)

    private fun contract(
        amount: Long,
        vararg dates: LocalDate,
        interval: ContractInterval = ContractInterval.MONTHLY,
        firstId: Long = 100,
        account: Long = giro,
    ) = Contract(
        signature = "s$firstId",
        name = "c$firstId",
        interval = interval,
        transactions = dates.mapIndexed { i, date ->
            ContractTransaction(id = firstId + i, date = date, amount = amount, accountId = account)
        },
        nextExpectedDate = dates.last().plus(interval.step),
        isActive = true,
        direction = ContractDirection.of(amount)
    )

    private fun salary(vararg dates: LocalDate) = contract(300000, *dates, firstId = 1)

    @Test
    fun periodRunsFromSalaryToSalary() {
        val period = BalancePeriod.of(salary(LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 28)), today)
        assertEquals(BalancePeriod(LocalDate.of(2026, 8, 28), LocalDate.of(2026, 9, 28), true), period)
        assertEquals(3, period.daysLeft(today))
    }

    @Test
    fun salaryBookedInAdvanceDoesNotStartPeriod() {
        val period = BalancePeriod.of(salary(LocalDate.of(2026, 8, 28), LocalDate.of(2026, 9, 28)), today)
        assertEquals(LocalDate.of(2026, 8, 28), period.start)
    }

    @Test
    fun lateSalaryIsExpectedTomorrow() {
        val period = BalancePeriod.of(salary(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 20)), today)
        assertEquals(today.plusDays(1), period.end)
    }

    @Test
    fun fallsBackToCalendarMonth() {
        val period = BalancePeriod.of(null, today)
        assertEquals(BalancePeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), false), period)
        assertFalse(period.isSalaryCycle)
    }

    @Test
    fun computesBalanceOfDailyAccounts() {
        val period = BalancePeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), true)
        val rent = contract(-80000, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), firstId = 10)
        // Next debit on 10.10., outside the period
        val phone = contract(-4000, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 9, 10), firstId = 20)
        // Next debit on 28.09., still to come
        val gym = contract(-3000, LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 28), firstId = 30)
        val transactions = listOf(
            BalanceTransaction(1, LocalDate.of(2026, 9, 1), 300000, giro),
            BalanceTransaction(11, LocalDate.of(2026, 9, 1), -80000, giro),
            BalanceTransaction(21, LocalDate.of(2026, 9, 10), -4000, giro),
            BalanceTransaction(50, LocalDate.of(2026, 9, 12), -6000, creditCard),
            // Refund counts as income
            BalanceTransaction(51, LocalDate.of(2026, 9, 13), 1000, creditCard),
            // Paying the credit card: between daily accounts, ignored
            BalanceTransaction(52, LocalDate.of(2026, 9, 20), -5000, giro, transferAccountId = creditCard),
            BalanceTransaction(53, LocalDate.of(2026, 9, 20), 5000, creditCard, transferAccountId = giro),
            // Saving
            BalanceTransaction(54, LocalDate.of(2026, 9, 2), -35000, giro, transferAccountId = savingsAccount),
            // Not a daily account
            BalanceTransaction(55, LocalDate.of(2026, 9, 2), 35000, savingsAccount, transferAccountId = giro),
            // Outside of the period
            BalanceTransaction(56, LocalDate.of(2026, 8, 31), -9999, giro),
        )
        val balance = MonthlyBalance.compute(period, transactions, daily, listOf(rent, phone, gym))
        assertEquals(301000, balance.income)
        assertEquals(-84000, balance.contractsBooked)
        assertEquals(-3000, balance.contractsUpcoming)
        assertEquals(-35000, balance.savings)
        assertEquals(-6000, balance.other)
        assertEquals(128000, balance.expenses)
        assertEquals(173000, balance.available)
        assertEquals(listOf(1L, 51L), balance.incomeTransactionIds)
        assertEquals(listOf(50L), balance.otherTransactionIds)
    }

    @Test
    fun countsWeeklyContractForEachRemainingWeek() {
        val period = BalancePeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), true)
        val weekly = contract(
            -1000, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 10),
            interval = ContractInterval.WEEKLY
        )
        // 17.09. and 24.09.
        val balance = MonthlyBalance.compute(period, emptyList(), daily, listOf(weekly))
        assertEquals(-2000, balance.contractsUpcoming)
    }

    @Test
    fun ignoresUpcomingDebitsOfOtherAccounts() {
        val period = BalancePeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), true)
        val fromSavings = contract(-1000, LocalDate.of(2026, 8, 28), account = savingsAccount)
        assertTrue(MonthlyBalance.compute(period, emptyList(), daily, listOf(fromSavings)).contractsUpcoming == 0L)
    }

    @Test
    fun expectsFurtherSalariesWithinPeriod() {
        val period = BalancePeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), true)
        // Determines the period, next payment on 01.10.
        val jobA = contract(300000, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), firstId = 1)
        // Next payment on 15.09., already booked
        val jobB = contract(80000, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 9, 15), firstId = 10)
        // Next payment on 28.09., still expected
        val jobC = contract(20000, LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 28), firstId = 20)
        val transactions = listOf(
            BalanceTransaction(2, LocalDate.of(2026, 9, 1), 300000, giro),
            BalanceTransaction(11, LocalDate.of(2026, 9, 15), 80000, giro),
        )
        val balance = MonthlyBalance.compute(period, transactions, daily, emptyList(), listOf(jobA, jobB, jobC))
        assertEquals(380000, balance.incomeBooked)
        assertEquals(20000, balance.incomeUpcoming)
        assertEquals(400000, balance.available)
    }

    @Test
    fun cancelledContractIsPaidButNotExpected() {
        val period = BalancePeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), true)
        // Last payment on 10.09. after cancelling, the next one on 10.10. is not expected
        val cancelled = contract(-4000, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 9, 10), firstId = 20)
            .copy(cancelledOn = LocalDate.of(2026, 9, 5), isActive = false)
        val weekly = contract(-1000, LocalDate.of(2026, 9, 3), interval = ContractInterval.WEEKLY, firstId = 30)
            .copy(cancelledOn = LocalDate.of(2026, 9, 5), isActive = false)
        val balance = MonthlyBalance.compute(
            period,
            listOf(BalanceTransaction(21, LocalDate.of(2026, 9, 10), -4000, giro)),
            daily,
            listOf(cancelled, weekly)
        )
        assertEquals(-4000, balance.contractsBooked)
        assertEquals(0, balance.contractsUpcoming)
        assertEquals(0, balance.other)
    }

    @Test
    fun reserveContractsCountAsReserves() {
        val period = BalancePeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), true)
        // Paid on 01.09.
        val buildingSavings = contract(-15000, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), firstId = 10)
            .copy(reserveChoice = true)
        // Next on 20.09., still to come
        val savingsPlan = contract(-10000, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 20), firstId = 20)
            .copy(reserveChoice = true)
        val rent = contract(-80000, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), firstId = 30)
        val balance = MonthlyBalance.compute(
            period,
            listOf(
                BalanceTransaction(11, LocalDate.of(2026, 9, 1), -15000, giro),
                BalanceTransaction(31, LocalDate.of(2026, 9, 1), -80000, giro),
            ),
            daily,
            listOf(buildingSavings, savingsPlan, rent)
        )
        assertEquals(-80000, balance.contracts)
        assertEquals(-15000, balance.savings)
        assertEquals(-10000, balance.savingsUpcoming)
        assertEquals(-25000, balance.reserves)
        assertEquals(105000, balance.expenses)
        assertEquals(listOf(11L), balance.savingsTransactionIds)
    }
}
