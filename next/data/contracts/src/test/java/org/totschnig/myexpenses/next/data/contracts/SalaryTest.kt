package org.totschnig.myexpenses.next.data.contracts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SalaryTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)

    private fun income(
        signature: String,
        amount: Long,
        interval: ContractInterval = ContractInterval.MONTHLY,
        isActive: Boolean = true,
    ) = Contract(
        signature = signature,
        name = signature,
        interval = interval,
        transactions = listOf(ContractTransaction(id = 1, date = today.minusDays(5), amount = amount, accountId = 1)),
        nextExpectedDate = today.minusDays(5).plus(interval.step),
        isActive = isActive,
        direction = ContractDirection.INCOME
    )

    private val salary = income("salary", 300000)
    private val rent = income("rent", 400000)
    private val childBenefit = income("child", 25000)
    private val bonus = income("bonus", 500000, interval = ContractInterval.YEARLY)
    private val oldJob = income("oldJob", 600000, isActive = false)
    private val incomes = listOf(salary, rent, childBenefit, bonus, oldJob)

    @Test
    fun automaticChoosesHighestActiveMonthlyIncome() {
        assertEquals(listOf(rent), incomes.salaries(SalaryChoice.Automatic))
    }

    @Test
    fun userCanChooseSeveralSalariesHighestFirst() {
        assertEquals(listOf(salary, childBenefit), incomes.salaries(SalaryChoice.Fixed(setOf("child", "salary"))))
    }

    @Test
    fun userCanChooseNoSalary() {
        assertTrue(incomes.salaries(SalaryChoice.None).isEmpty())
    }

    @Test
    fun incomesNoLongerReceivedAreLeftOut() {
        assertEquals(listOf(salary), incomes.salaries(SalaryChoice.Fixed(setOf("salary", "oldJob"))))
    }

    @Test
    fun fallsBackToAutomaticWhenNoChosenIncomeIsLeft() {
        assertEquals(listOf(rent), incomes.salaries(SalaryChoice.Fixed(setOf("oldJob", "dismissed"))))
    }
}
