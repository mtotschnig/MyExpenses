package org.totschnig.myexpenses.next.data.contracts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Period

class ContractDetectorTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)
    private val detector = ContractDetector(today)
    private var nextId = 1L

    private fun transaction(
        date: LocalDate,
        amount: Long,
        payeeId: Long? = 1,
        payeeName: String? = "Payee $payeeId",
        templateId: Long? = null,
    ) = ContractTransaction(
        id = nextId++,
        date = date,
        amount = amount,
        accountId = 1,
        payeeId = payeeId,
        payeeName = payeeName,
        templateId = templateId
    )

    /**
     * [count] debits, the last one on [last], going back by [step]
     */
    private fun series(
        count: Int,
        step: Period,
        amount: Long = -1000,
        last: LocalDate = today.minusDays(3),
        payeeId: Long? = 1,
        templateId: Long? = null,
        amountAt: (Int) -> Long = { amount },
    ) = (0 until count).map { i ->
        transaction(
            date = last.minus(step.multipliedBy(count - 1 - i)),
            amount = amountAt(i),
            payeeId = payeeId,
            templateId = templateId
        )
    }

    @Test
    fun detectsAllIntervals() {
        ContractInterval.entries.forEach { interval ->
            val contracts = detector.detect(series(interval.minOccurrences + 1, interval.step))
            assertEquals(interval.name, 1, contracts.size)
            assertEquals(interval, contracts.single().interval)
            assertTrue(contracts.single().isActive)
        }
    }

    @Test
    fun toleratesShiftedDebitDates() {
        val dates = listOf(
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 30),
            LocalDate.of(2026, 8, 3), LocalDate.of(2026, 9, 1)
        )
        val contract = detector.detect(dates.map { transaction(it, -4999) }).single()
        assertEquals(ContractInterval.MONTHLY, contract.interval)
        assertEquals(LocalDate.of(2026, 10, 1), contract.nextExpectedDate)
    }

    @Test
    fun requiresMinimumOccurrences() {
        assertTrue(detector.detect(series(2, Period.ofMonths(1))).isEmpty())
        assertEquals(1, detector.detect(series(3, Period.ofMonths(1))).size)
        assertEquals(1, detector.detect(series(2, Period.ofYears(1))).size)
    }

    @Test
    fun ignoresIrregularDebits() {
        val dates = listOf(1L, 5, 30, 32, 70, 150, 160).map { today.minusDays(200 - it) }
        assertTrue(detector.detect(dates.map { transaction(it, -2500) }).isEmpty())
    }

    @Test
    fun ignoresTransactionsWithoutPayee() {
        assertTrue(detector.detect(series(6, Period.ofMonths(1), payeeId = null)).isEmpty())
    }

    @Test
    fun detectsIncome() {
        val salary = detector.detect(series(6, Period.ofMonths(1), amount = 300000)).single()
        assertEquals(ContractDirection.INCOME, salary.direction)
        assertEquals(300000, salary.lastAmount)
        assertEquals("in:p1|MONTHLY", salary.signature)
    }

    @Test
    fun keepsContractSignatures() {
        val contract = detector.detect(series(6, Period.ofMonths(1))).single()
        assertEquals(ContractDirection.EXPENSE, contract.direction)
        assertEquals("p1|MONTHLY", contract.signature)
    }

    @Test
    fun keepsSalaryWithChristmasBonus() {
        // Bonus paid together with the salary: one booking with twice the amount, still the salary of that month
        val salary = series(12, Period.ofMonths(1)) { if (it == 6) 600000 else 300000 }
        val contract = detector.detect(salary).single()
        assertEquals(ContractInterval.MONTHLY, contract.interval)
        assertEquals(12, contract.transactions.size)
        assertEquals(300000, contract.lastAmount)
    }

    @Test
    fun separatesDebitsAndRefundsOfSamePayee() {
        val debits = series(6, Period.ofMonths(1), amount = -5000)
        val refunds = series(3, Period.ofMonths(3), amount = 5000, last = today.minusDays(20))
        val contracts = detector.detect(debits + refunds)
        assertEquals(
            setOf(ContractDirection.EXPENSE to 6, ContractDirection.INCOME to 3),
            contracts.map { it.direction to it.transactions.size }.toSet()
        )
    }

    @Test
    fun keepsPriceChangeInOneContract() {
        val contract = detector.detect(
            series(8, Period.ofMonths(1)) { if (it < 5) -1299 else -1499 }
        ).single()
        assertEquals(1499, contract.lastAmount)
        assertEquals(1499L, contract.previousAmount)
        assertEquals(8, contract.transactions.size)
    }

    @Test
    fun separatesContractsOfSamePayeeByAmount() {
        val monthly = series(6, Period.ofMonths(1), amount = -999, last = today.minusDays(10))
        val yearly = series(2, Period.ofYears(1), amount = -8900, last = today.minusDays(40))
        val contracts = detector.detect(monthly + yearly)
        assertEquals(
            setOf(ContractInterval.MONTHLY, ContractInterval.YEARLY),
            contracts.map { it.interval }.toSet()
        )
    }

    @Test
    fun ignoresOneOffDebitOfContractPayee() {
        val monthly = series(12, Period.ofMonths(1), amount = -4000)
        val oneOff = transaction(today.minusDays(100), -29900)
        val contract = detector.detect(monthly + oneOff).single()
        assertEquals(ContractInterval.MONTHLY, contract.interval)
        assertEquals(12, contract.transactions.size)
    }

    @Test
    fun groupsByTemplateRegardlessOfAmount() {
        val contract = detector.detect(
            series(6, Period.ofMonths(1), templateId = 7) { -1000L * (it + 1) }
        ).single()
        assertEquals(6, contract.transactions.size)
    }

    @Test
    fun marksEndedContracts() {
        val ended = detector.detect(series(6, Period.ofMonths(1), last = today.minusDays(60))).single()
        assertFalse(ended.isActive)
        val overdue = detector.detect(series(6, Period.ofMonths(1), last = today.minusDays(38))).single()
        assertTrue(overdue.isActive)
    }

    @Test
    fun computesMonthlyAndYearlyAmount() {
        val quarterly = detector.detect(series(4, Period.ofMonths(3), amount = -3000)).single()
        assertEquals(12000, quarterly.yearlyAmount)
        assertEquals(1000, quarterly.monthlyAmount)
    }

    @Test
    fun signatureIsStableWhenDebitsAreAdded() {
        val debits = series(6, Period.ofMonths(1), last = today.minusMonths(1)) { if (it < 3) -999 else -1299 }
        val before = detector.detect(debits).single().signature
        val after = detector.detect(debits + transaction(today.minusDays(2), -1299)).single().signature
        assertEquals("p1|MONTHLY", before)
        assertEquals(before, after)
    }

    @Test
    fun contractsOfSamePayeeAndIntervalGetDistinctSignatures() {
        val cheap = series(6, Period.ofMonths(1), amount = -500)
        val expensive = series(6, Period.ofMonths(1), amount = -5000)
        val contracts = detector.detect(cheap + expensive)
        assertEquals(
            mapOf(500L to "p1|MONTHLY", 5000L to "p1|MONTHLY|1"),
            contracts.associate { it.lastAmount to it.signature }
        )
    }

    @Test
    fun keepsSalaryAfterLargeRaise() {
        // 1.200 € → 1.750 €, more than the tolerance for similar amounts
        val salary = detector.detect(
            series(8, Period.ofMonths(1)) { if (it < 6) 120000 else 175000 }
        ).single()
        assertEquals(8, salary.transactions.size)
        assertTrue(salary.isActive)
        assertEquals(175000, salary.lastAmount)
        assertEquals(120000, salary.transactions[5].amount)
    }

    @Test
    fun keepsSalaryRightAfterRaise() {
        val salary = detector.detect(
            series(7, Period.ofMonths(1)) { if (it < 6) 120000 else 175000 }
        ).single()
        assertEquals(7, salary.transactions.size)
        assertTrue(salary.isActive)
    }

    @Test
    fun keepsContractAfterSeveralPriceChanges() {
        val contract = detector.detect(
            series(9, Period.ofMonths(1)) { -1000L - 500L * (it / 3) * (it / 3) }
        ).single()
        assertEquals(9, contract.transactions.size)
        assertEquals(3000, contract.lastAmount)
    }

    @Test
    fun doesNotJoinOneOffAfterEndedContract() {
        val ended = series(6, Period.ofMonths(1), amount = -2000, last = today.minusMonths(5))
        val oneOff = transaction(today.minusDays(10), -9000)
        val contract = detector.detect(ended + oneOff).single()
        assertEquals(6, contract.transactions.size)
        assertFalse(contract.isActive)
    }

    @Test
    fun keepsSalaryWithStronglyVaryingAmounts() {
        // Salary of a part-time job over two years: varying hours, bonus months, two raises,
        // plus extra payments in November
        val amounts = listOf(
            49618L, 129732, 115415, 49755, 52133, 48919, 39842, 47104, 42843,
            82040, 82040, 82040, 82040, 159520, 161114, 82040, 82040, 82040,
            114000, 114000, 114000, 114000, 114000, 114000, 167962, 169594
        )
        val salary = series(amounts.size, Period.ofMonths(1), last = LocalDate.of(2026, 8, 31)) { amounts[it] }
        val extras = listOf(
            transaction(LocalDate.of(2024, 11, 18), 37807),
            transaction(LocalDate.of(2025, 11, 18), 33516)
        )
        val contract = detector.detect(salary + extras).single { it.interval == ContractInterval.MONTHLY }
        assertEquals(26, contract.transactions.size)
        assertTrue(contract.isActive)
        assertEquals(169594, contract.lastAmount)
    }

    @Test
    fun detectsYearlyIncomeWithSameAmountOnSameDay() {
        val bonus = detector.detect(
            listOf(transaction(LocalDate.of(2025, 6, 30), 150000), transaction(LocalDate.of(2026, 6, 30), 150000))
        ).single()
        assertEquals(ContractInterval.YEARLY, bonus.interval)
    }

    @Test
    fun ignoresYearlyIncomeWithChangingAmount() {
        // Tax refunds and credits from annual statements differ every year
        assertTrue(
            detector.detect(
                listOf(transaction(LocalDate.of(2025, 7, 22), 50457), transaction(LocalDate.of(2026, 7, 22), 43205))
            ).isEmpty()
        )
    }

    @Test
    fun ignoresYearlyIncomeOnMovingDate() {
        // Easter: same amount, but two weeks apart from one year to the next
        assertTrue(
            detector.detect(
                listOf(transaction(LocalDate.of(2025, 4, 17), 2000), transaction(LocalDate.of(2026, 4, 2), 2000))
            ).isEmpty()
        )
    }

    @Test
    fun ignoresHalfYearlyIncomeByChance() {
        assertTrue(
            detector.detect(
                listOf(transaction(LocalDate.of(2025, 9, 1), 3193), transaction(LocalDate.of(2026, 3, 6), 2678))
            ).isEmpty()
        )
    }

    @Test
    fun keepsYearlyContractWithChangingAmount() {
        // Only incomes are checked more strictly, e.g. a yearly inspection costs different amounts
        val inspection = detector.detect(
            listOf(transaction(LocalDate.of(2025, 3, 17), -18697), transaction(LocalDate.of(2026, 3, 16), -25578))
        ).single()
        assertEquals(ContractInterval.YEARLY, inspection.interval)
    }
}
