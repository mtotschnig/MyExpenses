package org.totschnig.myexpenses.next.data.contracts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Period

class ContractNewsTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)
    private var nextId = 1L

    private fun monthly(amounts: List<Long>, payeeId: Long = 1, last: LocalDate = today.minusDays(3)) =
        amounts.mapIndexed { i, amount ->
            ContractTransaction(
                id = nextId++, date = last.minus(Period.ofMonths(amounts.size - 1 - i)), amount = amount,
                accountId = 1, payeeId = payeeId, payeeName = "Payee $payeeId"
            )
        }

    private fun rule(payeeId: Long = 1, direction: ContractDirection = ContractDirection.EXPENSE, cancelledOn: LocalDate? = null) =
        ContractRule(
            id = "r$payeeId", kind = ContractRule.Kind.CONTRACT, direction = direction, payeeIds = setOf(payeeId),
            interval = ContractInterval.MONTHLY, name = "Contract $payeeId", cancelledOn = cancelledOn
        )

    private fun detect(rules: List<ContractRule>, transactions: List<ContractTransaction>) =
        ContractAnalysis.of(transactions, rules, today).detectNews(today)

    @Test
    fun paymentAfterCancellationReactivates() {
        val payments = monthly(listOf(-4000, -4000, -4000))
        val detection = detect(listOf(rule(cancelledOn = today.minusDays(10))), payments)
        val news = detection.news.single()
        assertEquals(ContractNews.Type.PAYMENT_AFTER_CANCELLATION, news.type)
        assertEquals(payments.last().id, news.transactionId)
        assertEquals(today.minusDays(10), news.cancelledOn)
        assertEquals(4000, news.amount)
        assertEquals("Contract 1", news.contractName)
        assertEquals(setOf("r1"), detection.reactivated)
        val reactivated = listOf(rule(cancelledOn = today.minusDays(10))).reactivating(detection.reactivated).single()
        assertNull(reactivated.cancelledOn)
    }

    @Test
    fun noNewsWithoutPaymentAfterCancellation() {
        val detection = detect(listOf(rule(cancelledOn = today.minusDays(1))), monthly(listOf(-4000, -4000, -4000)))
        assertTrue(detection.news.isEmpty())
        assertTrue(detection.reactivated.isEmpty())
    }

    @Test
    fun paymentsBookedInAdvanceDoNotCount() {
        val advance = monthly(listOf(-4000, -4000, -4000), last = today.plusDays(5))
        val detection = detect(listOf(rule(cancelledOn = today.minusDays(1))), advance)
        assertTrue(detection.reactivated.isEmpty())
    }

    @Test
    fun priceChangeAfterStablePrice() {
        val news = detect(listOf(rule()), monthly(listOf(-1399, -1399, -1399, -1799))).news.single()
        assertEquals(ContractNews.Type.PRICE_CHANGE, news.type)
        assertEquals(1799, news.amount)
        assertEquals(1399L, news.previousAmount)
        assertFalse(news.isIncome)
    }

    @Test
    fun raiseOfSalaryIsPriceChange() {
        val news = detect(
            listOf(rule(direction = ContractDirection.INCOME)),
            monthly(listOf(114000, 114000, 114000, 167962))
        ).news.single()
        assertTrue(news.isIncome)
        assertEquals(114000L, news.previousAmount)
    }

    @Test
    fun noPriceChangeForVaryingAmounts() {
        // A phone bill that differs slightly every month
        assertTrue(detect(listOf(rule()), monthly(listOf(-4712, -4695, -4803, -4750))).news.isEmpty())
        // Differences below the tolerance, e.g. from exchange rates
        assertTrue(detect(listOf(rule()), monthly(listOf(-1000, -1000, -1005))).news.isEmpty())
    }

    @Test
    fun noPriceChangeFromHistory() {
        val old = monthly(listOf(-1399, -1399, -1799), last = today.minusDays(60))
        assertTrue(detect(listOf(rule()), old).news.none { it.type == ContractNews.Type.PRICE_CHANGE })
    }

    @Test
    fun suggestionsHaveNoNews() {
        assertTrue(detect(emptyList(), monthly(listOf(-1399, -1399, -1399, -1799))).news.isEmpty())
    }

    @Test
    fun addingKeepsKnownNewsAndReadState() {
        val found = detect(listOf(rule()), monthly(listOf(-1399, -1399, -1399, -1799))).news
        val stored = emptyList<ContractNews>().adding(found, today).map { it.copy(isRead = true) }
        val again = stored.adding(found, today)
        assertEquals(stored, again)
        assertTrue(again.single().isRead)
    }

    @Test
    fun addingDropsOldNewsAndLimitsCount() {
        val template = detect(listOf(rule()), monthly(listOf(-1399, -1399, -1399, -1799))).news.single()
        val old = template.copy(id = "old", createdAt = today.minusMonths(25))
        val many = (1..ContractNews.MAX_COUNT + 5).map { template.copy(id = "n$it", createdAt = today.minusDays(it.toLong())) }
        val result = listOf(old).adding(many, today)
        assertEquals(ContractNews.MAX_COUNT, result.size)
        assertTrue(result.none { it.id == "old" })
        assertEquals("n1", result.first().id)
    }

    @Test
    fun missingPaymentAfterTolerance() {
        // Last payment 36 days ago, expected 5 days ago: beyond the tolerance of 4 days
        val late = detect(listOf(rule()), monthly(listOf(-4000, -4000, -4000), last = today.minusDays(36))).news.single()
        assertEquals(ContractNews.Type.PAYMENT_MISSING, late.type)
        assertEquals(today.minusDays(36).plusMonths(1), late.date)
        // Expected 2 days ago: within the tolerance, e.g. moved by a weekend
        assertTrue(detect(listOf(rule()), monthly(listOf(-4000, -4000, -4000), last = today.minusDays(33))).news.isEmpty())
    }

    @Test
    fun missingPaymentIsSettledWhenItArrives() {
        val before = monthly(listOf(-4000, -4000, -4000), last = today.minusDays(36))
        val missing = detect(listOf(rule()), before).news
        val stored = emptyList<ContractNews>().adding(missing, today)
        val arrived = before + ContractTransaction(
            id = 999, date = today.minusDays(1), amount = -4000, accountId = 1, payeeId = 1, payeeName = "Payee 1"
        )
        val settled = stored.settling(ContractAnalysis.of(arrived, listOf(rule()), today))
        assertTrue(settled.single().isRead)
    }

    @Test
    fun stoppedContractRecentlyEnded() {
        // Monthly contract ends 42 days after the last payment
        val stopped = detect(listOf(rule()), monthly(listOf(-3000, -3000, -3000), last = today.minusDays(50))).news.single()
        assertEquals(ContractNews.Type.CONTRACT_STOPPED, stopped.type)
        // Ended long ago: history, no news
        assertTrue(detect(listOf(rule()), monthly(listOf(-3000, -3000, -3000), last = today.minusDays(120))).news.isEmpty())
        // Cancelled by the user: expected to stop
        assertTrue(
            detect(listOf(rule(cancelledOn = today.minusDays(60))), monthly(listOf(-3000, -3000, -3000), last = today.minusDays(70)))
                .news.isEmpty()
        )
    }

    @Test
    fun stoppedContractIsSettledWhenCancelled() {
        val payments = monthly(listOf(-3000, -3000, -3000), last = today.minusDays(50))
        val stored = emptyList<ContractNews>().adding(detect(listOf(rule()), payments).news, today)
        val settled = stored.settling(ContractAnalysis.of(payments, listOf(rule(cancelledOn = today)), today))
        assertTrue(settled.single().isRead)
    }

    @Test
    fun newContractNews() {
        val analysis = ContractAnalysis.of(monthly(List(8) { -1500L }), emptyList(), today)
        val news = newContractNews(analysis.automaticConfirmations(emptyList()), today).single()
        assertEquals(ContractNews.Type.NEW_CONTRACT, news.type)
        assertEquals(ContractInterval.MONTHLY, news.interval)
        assertEquals(1500, news.amount)
    }

    @Test
    fun newsOfReserveAreMarked() {
        val news = detect(listOf(rule().copy(reserve = true)), monthly(listOf(-10000, -10000, -10000, -15000))).news.single()
        assertTrue(news.isReserve)
        assertFalse(detect(listOf(rule()), monthly(listOf(-1399, -1399, -1399, -1799))).news.single().isReserve)
    }
}
