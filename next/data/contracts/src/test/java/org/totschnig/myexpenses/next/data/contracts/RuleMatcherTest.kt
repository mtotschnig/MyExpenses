package org.totschnig.myexpenses.next.data.contracts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Period

class RuleMatcherTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)
    private val matcher = RuleMatcher(today)
    private var nextId = 1L

    private fun transaction(date: LocalDate, amount: Long, payeeId: Long = 1) = ContractTransaction(
        id = nextId++, date = date, amount = amount, accountId = 1, payeeId = payeeId, payeeName = "Payee $payeeId"
    )

    private fun monthly(amounts: List<Long>, payeeId: Long = 1, last: LocalDate = today.minusDays(3)) =
        amounts.mapIndexed { i, amount ->
            transaction(last.minus(Period.ofMonths(amounts.size - 1 - i)), amount, payeeId)
        }

    private fun rule(
        id: String,
        kind: ContractRule.Kind = ContractRule.Kind.CONTRACT,
        direction: ContractDirection = ContractDirection.EXPENSE,
        payeeId: Long = 1,
        amountRange: LongRange? = null,
        interval: ContractInterval = ContractInterval.MONTHLY,
        name: String? = null,
    ) = ContractRule(
        id = id, kind = kind, direction = direction, payeeIds = setOf(payeeId),
        amountRange = amountRange, interval = interval, name = name
    )

    @Test
    fun confirmedContractContainsAllPaymentsWhateverTheAmount() {
        // Heuristics would split this, the rule does not care
        val salary = monthly(listOf(40000, 130000, 49000, 82000, 82000, 160000, 114000, 170000))
        val result = matcher.apply(listOf(rule("salary", direction = ContractDirection.INCOME, name = "Job")), salary)
        val contract = result.confirmed.single()
        assertEquals(8, contract.transactions.size)
        assertTrue(contract.isConfirmed)
        assertTrue(contract.isActive)
        assertEquals("rule:salary", contract.signature)
        assertEquals("Job", contract.displayName)
        assertEquals("Payee 1", contract.name)
        assertEquals(ContractDirection.INCOME, contract.direction)
        assertEquals(today.minusDays(3).plusMonths(1), contract.nextExpectedDate)
        assertTrue(result.remaining.isEmpty())
    }

    @Test
    fun confirmedContractWithFewPaymentsUsesConfirmedInterval() {
        // Only one payment so far, the detector would not suggest it
        val payment = transaction(today.minusDays(100), -23000)
        val contract = matcher.apply(
            listOf(rule("insurance", interval = ContractInterval.YEARLY)), listOf(payment)
        ).confirmed.single()
        assertEquals(ContractInterval.YEARLY, contract.interval)
        assertTrue(contract.isActive)
    }

    @Test
    fun confirmedContractEndsWhenNoLongerPaid() {
        val contract = matcher.apply(
            listOf(rule("gym")), monthly(listOf(-3000, -3000, -3000), last = today.minusMonths(4))
        ).confirmed.single()
        assertFalse(contract.isActive)
    }

    @Test
    fun leavesOtherPaymentsForDetection() {
        val subscription = monthly(listOf(-899, -899, -899))
        val order = transaction(today.minusDays(20), -4599)
        val otherPayee = transaction(today.minusDays(20), -1000, payeeId = 2)
        val refund = transaction(today.minusDays(10), 899)
        val result = matcher.apply(
            listOf(rule("prime", amountRange = 719L..1079L)),
            subscription + order + otherPayee + refund
        )
        assertEquals(3, result.confirmed.single().transactions.size)
        assertEquals(setOf(order, otherPayee, refund), result.remaining.toSet())
    }

    @Test
    fun separatesIgnoredPayments() {
        val shop = monthly(listOf(-4296, -4535), payeeId = 2)
        val result = matcher.apply(listOf(rule("shop", kind = ContractRule.Kind.IGNORE, payeeId = 2)), shop)
        assertTrue(result.confirmed.isEmpty())
        assertEquals(shop, result.ignored.getValue("shop"))
        assertTrue(result.remaining.isEmpty())
    }

    @Test
    fun contractWinsOverIgnoreAndSpecificOverGeneral() {
        val subscription = monthly(listOf(-899, -899, -899))
        val order = transaction(today.minusDays(20), -4599)
        val result = matcher.apply(
            listOf(
                // A general rule from dismissing all other payments of the payee
                rule("orders", kind = ContractRule.Kind.IGNORE),
                rule("prime", amountRange = 719L..1079L),
            ),
            subscription + order
        )
        assertEquals(subscription, result.confirmed.single().transactions)
        assertEquals(listOf(order), result.ignored.getValue("orders"))
    }

    @Test
    fun ruleWithoutPaymentsGivesNoContract() {
        val result = matcher.apply(listOf(rule("old", payeeId = 9)), monthly(listOf(-1000, -1000, -1000)))
        assertTrue(result.confirmed.isEmpty())
        assertEquals(3, result.remaining.size)
    }

    @Test
    fun cancelledContractIsNotActive() {
        val payments = monthly(listOf(-3000, -3000, -3000))
        val contract = matcher.apply(
            listOf(rule("gym").copy(cancelledOn = today.minusDays(1))), payments
        ).confirmed.single()
        assertTrue(contract.isCancelled)
        assertFalse(contract.isActive)
        assertTrue(contract.hasPayments)
        assertEquals(payments, contract.transactions)
    }

    @Test
    fun cancelledContractWithoutRecentPaymentsComesFromSnapshot() {
        val cancelled = rule("old", payeeId = 9, name = "Old gym").copy(
            cancelledOn = LocalDate.of(2024, 1, 10),
            snapshot = ContractRule.Snapshot("Gym GmbH", 2990, LocalDate.of(2024, 1, 1))
        )
        val contract = matcher.apply(listOf(cancelled), emptyList()).confirmed.single()
        assertFalse(contract.hasPayments)
        assertEquals("Gym GmbH", contract.name)
        assertEquals("Old gym", contract.displayName)
        assertEquals(2990, contract.lastAmount)
        assertEquals(LocalDate.of(2024, 1, 1), contract.lastDate)
        assertFalse(contract.isIncome)
    }
}
