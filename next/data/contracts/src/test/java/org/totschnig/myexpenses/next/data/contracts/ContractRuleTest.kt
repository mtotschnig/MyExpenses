package org.totschnig.myexpenses.next.data.contracts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Period

class ContractRuleTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)
    private var nextId = 1L

    private fun transaction(
        date: LocalDate,
        amount: Long,
        payeeId: Long? = 1,
        templateId: Long? = null,
    ) = ContractTransaction(
        id = nextId++, date = date, amount = amount, accountId = 1,
        payeeId = payeeId, payeeName = "Payee $payeeId", templateId = templateId
    )

    private fun monthly(count: Int, amount: Long, payeeId: Long? = 1, templateId: Long? = null) =
        (0 until count).map {
            transaction(today.minusDays(3).minus(Period.ofMonths(count - 1 - it)), amount, payeeId, templateId)
        }

    private fun detected(transactions: List<ContractTransaction>) =
        ContractDetector(today).detect(transactions)

    private fun rule(
        direction: ContractDirection = ContractDirection.EXPENSE,
        payeeIds: Set<Long> = setOf(1),
        templateId: Long? = null,
        amountRange: LongRange? = null,
    ) = ContractRule(
        id = "r", kind = ContractRule.Kind.CONTRACT, direction = direction, payeeIds = payeeIds,
        templateId = templateId, amountRange = amountRange, interval = ContractInterval.MONTHLY
    )

    @Test
    fun matchesByPayeeAndDirection() {
        val rule = rule()
        assertTrue(rule.matches(transaction(today, -1000)))
        assertFalse(rule.matches(transaction(today, 1000)))
        assertFalse(rule.matches(transaction(today, -1000, payeeId = 2)))
        assertFalse(rule.matches(transaction(today, -1000, payeeId = null)))
    }

    @Test
    fun matchesByTemplate() {
        val rule = rule(payeeIds = emptySet(), templateId = 7)
        assertTrue(rule.matches(transaction(today, -1000, payeeId = null, templateId = 7)))
        assertFalse(rule.matches(transaction(today, -1000, templateId = 8)))
    }

    @Test
    fun matchesAmountRange() {
        val rule = rule(amountRange = 800L..1200L)
        assertTrue(rule.matches(transaction(today, -1000)))
        assertFalse(rule.matches(transaction(today, -5000)))
    }

    @Test
    fun ruleOfContractWithOnlyItsPaymentsHasNoAmountRange() {
        val transactions = monthly(6, -80000)
        val rule = ContractRule.of(detected(transactions).single(), transactions, id = "r")
        assertEquals(setOf(1L), rule.payeeIds)
        assertNull(rule.templateId)
        assertNull(rule.amountRange)
        assertEquals(ContractInterval.MONTHLY, rule.interval)
        assertEquals(ContractDirection.EXPENSE, rule.direction)
        assertEquals("rule:r", rule.key)
    }

    @Test
    fun ruleOfContractWithOtherPaymentsOfPayeeHasAmountRange() {
        // Subscription besides single orders
        val subscription = monthly(6, -899)
        val orders = listOf(transaction(today.minusDays(40), -4599), transaction(today.minusDays(100), -2999))
        val transactions = subscription + orders
        val rule = ContractRule.of(detected(transactions).single(), transactions)
        assertEquals(719L..1079L, rule.amountRange)
        assertTrue(subscription.all(rule::matches))
        assertTrue(orders.none(rule::matches))
    }

    @Test
    fun ruleOfContractFromTemplateUsesTemplate() {
        val transactions = monthly(6, -1000, templateId = 7)
        val rule = ContractRule.of(detected(transactions).single(), transactions)
        assertEquals(7L, rule.templateId)
        assertTrue(rule.payeeIds.isEmpty())
    }

    @Test
    fun ruleKeepsDecisionsOfUser() {
        val transactions = monthly(6, 300000)
        val contract = detected(transactions).single().copy(
            customName = "Salary",
            areaChoice = AreaChoice.Fixed(null)
        )
        val rule = ContractRule.of(contract, transactions, kind = ContractRule.Kind.IGNORE)
        assertEquals(ContractRule.Kind.IGNORE, rule.kind)
        assertEquals(ContractDirection.INCOME, rule.direction)
        assertEquals("Salary", rule.name)
        assertEquals(ContractSettings.AREA_NONE, rule.areaKey)
    }

    @Test
    fun noAmountRangeIfOtherPaymentsHaveSimilarAmounts() {
        // Extra payment of the employer within the range of the salaries
        val salary = monthly(6, 150000)
        val extra = transaction(today.minusDays(50), 140000)
        val transactions = salary + extra
        val contract = detected(salary).single()
        assertNull(ContractRule.of(contract, transactions).amountRange)
    }

    @Test
    fun ruleKeepsReserveChoice() {
        val transactions = monthly(6, -15000)
        val contract = detected(transactions).single().copy(reserveChoice = true)
        assertEquals(true, ContractRule.of(contract, transactions).reserve)
    }
}
