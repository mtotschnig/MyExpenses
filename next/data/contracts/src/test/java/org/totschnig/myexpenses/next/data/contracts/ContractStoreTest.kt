package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.totschnig.myexpenses.next.core.common.withTestDataStore

class ContractStoreTest {

    private val rule = ContractRule("r", ContractRule.Kind.CONTRACT, ContractDirection.EXPENSE, setOf(1), interval = ContractInterval.MONTHLY)

    @Test
    fun keepsRulesSalaryAreasAndNewsOfOneEditTogether() = withTestDataStore { dataStore ->
        val store = ContractStore(dataStore)
        val news = ContractNews(
            id = "n", type = ContractNews.Type.NEW_CONTRACT, ruleId = "r", transactionId = 1,
            date = LocalDate.of(2026, 10, 6), createdAt = LocalDate.of(2026, 10, 6), contractName = "Strom",
            isIncome = false, amount = 4500
        )
        store.edit { editor ->
            editor.rules = listOf(rule)
            editor.setSalary(SalaryChoice.Fixed(setOf("p:3")))
            editor.customAreas = listOf(CustomArea("a", "Hund"))
            editor.news = listOf(news)
            editor.markRulesMigrated(dropOld = true)
        }
        val settings = store.settings.first()
        assertEquals(listOf(rule), settings.rules)
        assertEquals(SalaryChoice.Fixed(setOf("p:3")), settings.salary)
        assertEquals(listOf(CustomArea("a", "Hund")), settings.customAreas)
        assertTrue(settings.rulesMigrated)
        assertEquals(listOf(news), store.news.first())
    }

    @Test
    fun replacesSalaryOfMergedContract() = withTestDataStore { dataStore ->
        val store = ContractStore(dataStore)
        store.edit { it.setSalary(SalaryChoice.Fixed(setOf("old", "other"))) }
        store.edit { it.replaceSalary("old", "new") }
        assertEquals(SalaryChoice.Fixed(setOf("new", "other")), store.settings.first().salary)
    }
}
