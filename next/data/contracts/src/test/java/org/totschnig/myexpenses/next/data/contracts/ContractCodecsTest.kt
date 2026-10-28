package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ContractCodecsTest {

    private val date = LocalDate.of(2026, 10, 5)

    @Test
    fun writesAndReadsRulesWithAllFields() {
        val rules = listOf(
            ContractRule(
                id = "r1",
                kind = ContractRule.Kind.CONTRACT,
                direction = ContractDirection.EXPENSE,
                payeeIds = setOf(3, 4),
                templateId = 7,
                amountRange = 1000L..1200L,
                interval = ContractInterval.MONTHLY,
                name = "Strom",
                areaKey = "HOUSING",
                cancelledOn = date,
                snapshot = ContractRule.Snapshot("Stadtwerke", 4500, date.minusDays(10)),
                reserve = true,
                targetAccountIds = setOf(9),
                icon = "bolt"
            ),
            ContractRule("r2", ContractRule.Kind.IGNORE, ContractDirection.INCOME, interval = ContractInterval.YEARLY)
        )
        assertEquals(rules, parseRules(serializeRules(rules)))
    }

    @Test
    fun readsTargetAccountStoredBeforeRulesHadSeveral() {
        val json = """[{"id":"r","kind":"CONTRACT","direction":"EXPENSE","interval":"MONTHLY","targetAccount":5}]"""
        assertEquals(setOf(5L), parseRules(json).single().targetAccountIds)
    }

    @Test
    fun leavesOutRulesFromLaterVersions() {
        val json = """[{"id":"r","kind":"SOMETHING_NEW","direction":"EXPENSE","interval":"MONTHLY"},""" +
                """{"id":"s","kind":"CONTRACT","direction":"EXPENSE","interval":"MONTHLY"}]"""
        assertEquals(listOf("s"), parseRules(json).map { it.id })
        assertEquals(emptyList<ContractRule>(), parseRules("broken"))
    }

    @Test
    fun writesAndReadsNews() {
        val news = listOf(
            ContractNews(
                id = "n1", type = ContractNews.Type.PRICE_CHANGE, ruleId = "r1", transactionId = 12, date = date,
                createdAt = date, contractName = "Netflix", isIncome = false, isReserve = false, amount = 1799,
                previousAmount = 1399, cancelledOn = null, interval = ContractInterval.MONTHLY, isRead = true
            ),
            ContractNews(
                id = "n2", type = ContractNews.Type.PAYMENT_AFTER_CANCELLATION, ruleId = "r2", transactionId = 13,
                date = date, createdAt = date, contractName = "Gym", isIncome = false, amount = 2900,
                cancelledOn = date.minusMonths(1)
            )
        )
        assertEquals(news, parseNews(serializeNews(news)))
    }

    @Test
    fun writesAndReadsCustomAreasAndMaps() {
        val areas = listOf(CustomArea("a1", "Hund"), CustomArea("a2", "Hobby"))
        assertEquals(areas, parseCustomAreas(serializeCustomAreas(areas)))
        val names = mapOf("p:1" to "Miete", "p:2" to "Strom")
        assertEquals(names, parseMap(serializeMap(names)))
        assertEquals(emptyMap<String, String>(), parseMap("broken"))
    }
}
