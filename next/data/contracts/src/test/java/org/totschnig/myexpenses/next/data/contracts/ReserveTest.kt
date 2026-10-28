package org.totschnig.myexpenses.next.data.contracts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReserveTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)

    private fun contract(
        payee: String? = "Payee",
        category: String? = null,
        comment: String? = null,
        amount: Long = -15000,
        reserveChoice: Boolean? = null,
    ) = Contract(
        signature = "s",
        name = payee ?: "",
        interval = ContractInterval.MONTHLY,
        transactions = listOf(
            ContractTransaction(
                id = 1, date = today, amount = amount, accountId = 1,
                payeeName = payee, categoryPath = category, comment = comment
            )
        ),
        nextExpectedDate = today.plusMonths(1),
        isActive = true,
        direction = ContractDirection.of(amount),
        reserveChoice = reserveChoice
    )

    @Test
    fun suggestsReserves() {
        assertTrue(contract(payee = "Bausparkasse Schwäbisch Hall AG").isReserve)
        assertTrue(contract(comment = "Übertrag auf eigenes Tagesgeldkonto").isReserve)
        assertTrue(contract(category = "Finanzen > Sparplan").isReserve)
        assertTrue(contract(category = "Geldanlage > ETF").isReserve)
        assertTrue(contract(comment = "VL Arbeitgeber").isReserve)
        assertTrue(contract(category = "Vermögenswirksame Leistungen").isReserve)
    }

    @Test
    fun banksAreNoReserves() {
        assertFalse(contract(payee = "Frankfurter Sparkasse", comment = "Kontoführung").isReserve)
        assertFalse(contract(payee = "Sparda-Bank West").isReserve)
        assertFalse(contract(payee = "Netflix", category = "Freizeit > Streaming").isReserve)
    }

    @Test
    fun incomesAreNoReserves() {
        assertFalse(contract(payee = "Bausparkasse", amount = 15000).isReserve)
    }

    @Test
    fun choiceOfUserWins() {
        assertFalse(contract(payee = "Bausparkasse", reserveChoice = false).isReserve)
        assertTrue(contract(payee = "Mein Neffe", reserveChoice = true).isReserve)
    }
}
