package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate
import java.time.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticDecisionsTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)
    private var nextId = 1L

    private fun monthly(count: Int, amount: Long, payeeId: Long) = (0 until count).map {
        ContractTransaction(
            id = nextId++, date = today.minusDays(3).minus(Period.ofMonths(count - 1 - it)), amount = amount,
            accountId = 1, payeeId = payeeId, payeeName = "Payee $payeeId"
        )
    }

    private val rent = monthly(12, -80000, payeeId = 1)

    private fun analysis(rules: List<ContractRule>, transactions: List<ContractTransaction> = rent) =
        ContractAnalysis.of(transactions, rules, today)

    @Test
    fun firstAnalysisTakesOverOldDecisionsAndConfirmsWithoutNews() {
        val decisions = automaticDecisions(ContractSettings(), emptyList(), analysis(emptyList()), today)
        assertTrue(decisions.marksMigration)
        assertNotNull(decisions.migratedSalary)
        assertEquals(setOf(1L), decisions.rules.flatMap { it.payeeIds }.toSet())
        // Contracts found by the first analysis are nothing new to the user
        assertTrue(decisions.news.none { it.type == ContractNews.Type.NEW_CONTRACT })
    }

    @Test
    fun laterAnalysisConfirmsNewContractWithNews() {
        val settings = ContractSettings(rulesMigrated = true)
        val decisions = automaticDecisions(settings, emptyList(), analysis(emptyList()), today)
        assertFalse(decisions.marksMigration)
        assertNull(decisions.migratedSalary)
        assertEquals(setOf(1L), decisions.rules.flatMap { it.payeeIds }.toSet())
        assertEquals(listOf(ContractNews.Type.NEW_CONTRACT), decisions.news.map { it.type })
    }

    @Test
    fun confirmedContractIsNotConfirmedAgain() {
        val first = automaticDecisions(ContractSettings(rulesMigrated = true), emptyList(), analysis(emptyList()), today)
        val settings = ContractSettings(rulesMigrated = true, rules = first.rules)
        val second = automaticDecisions(settings, first.news, analysis(first.rules), today)
        assertEquals(first.rules, second.rules)
        assertEquals(first.news, second.news)
    }
}
