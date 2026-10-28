package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.totschnig.myexpenses.next.core.model.ledger.Ledger
import org.totschnig.myexpenses.next.core.model.ledger.LedgerTransaction
import org.totschnig.myexpenses.next.core.model.ledger.OwnAccounts

/**
 * The contracts are analysed again only when [contractTransactions] change, see [ContractRepository]
 */
class ContractInputTest {

    private val giro = 1L
    private val day = LocalDate.of(2026, 9, 1)

    private fun ledger(vararg transactions: LedgerTransaction) =
        Ledger(transactions.toList(), OwnAccounts(setOf(giro), emptyMap()))

    private val payment = LedgerTransaction(1, day, amount = -1299, accountId = giro, payeeId = 7, payeeName = "Netflix")

    @Test
    fun tagsDoNotChangeTheInput() {
        assertEquals(
            contractTransactions(ledger(payment)),
            contractTransactions(ledger(payment.copy(tagIds = setOf(3L))))
        )
    }

    @Test
    fun newTransactionsAndAmountsChangeTheInput() {
        val before = contractTransactions(ledger(payment))
        assertNotEquals(before, contractTransactions(ledger(payment.copy(amount = -1499, homeAmount = -1499))))
        assertNotEquals(before, contractTransactions(ledger(payment, payment.copy(id = 2, date = day.plusMonths(1)))))
    }
}
