package org.totschnig.myexpenses.next.feature.contracts

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.ContractInterval
import org.totschnig.myexpenses.next.data.contracts.ContractTransaction

class ContractTileTest {

    private fun contract(name: String, categoryPath: String?) = Contract(
        signature = "s",
        name = name,
        interval = ContractInterval.MONTHLY,
        transactions = listOf(
            ContractTransaction(1, LocalDate.of(2026, 10, 1), -1000, accountId = 1, categoryPath = categoryPath, categoryIcon = "folder")
        ),
        nextExpectedDate = LocalDate.of(2026, 11, 1),
        isActive = true
    )

    @Test
    fun anIconOfTheNameComesBeforeThatOfTheArea() {
        assertEquals("umbrella", contract("Hausratversicherung", "Versicherungen").automaticIcon)
        assertEquals("shield-halved", contract("Allianz", "Versicherungen").automaticIcon)
        assertNull(contract("Allianz", null).automaticIcon)
    }

    @Test
    fun suggestsTheIconOfTheCategoryAndAreaFirst() {
        assertEquals(listOf("folder", "tv"), contract("Netflix", "Freizeit > Streaming").iconSuggestions.take(2))
    }
}
