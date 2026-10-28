package org.totschnig.myexpenses.next.data.balance

import org.junit.Assert.assertEquals
import org.junit.Test
import org.totschnig.myexpenses.next.core.model.ledger.Ledger
import org.totschnig.myexpenses.next.core.model.ledger.LedgerTransaction
import org.totschnig.myexpenses.next.core.model.ledger.OwnAccounts
import java.time.LocalDate

class BalanceTransactionsTest {

    private val giro = 1L
    private val savings = 2L
    private val period = BalancePeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), false)

    @Test
    fun takesTransactionsOfPeriodInHomeCurrency() {
        val ledger = Ledger(
            listOf(
                LedgerTransaction(1, LocalDate.of(2026, 8, 31), amount = -500, accountId = giro),
                LedgerTransaction(2, LocalDate.of(2026, 9, 1), amount = -1000, homeAmount = -1100, accountId = giro),
                LedgerTransaction(3, LocalDate.of(2026, 9, 15), amount = -5000, accountId = giro, targetAccountId = savings),
                LedgerTransaction(4, LocalDate.of(2026, 10, 1), amount = -700, accountId = giro),
            ),
            OwnAccounts(setOf(giro), emptyMap())
        )
        assertEquals(
            listOf(
                BalanceTransaction(2, LocalDate.of(2026, 9, 1), -1100, giro),
                // Movements between own accounts count as transfers
                BalanceTransaction(3, LocalDate.of(2026, 9, 15), -5000, giro, transferAccountId = savings),
            ),
            balanceTransactions(ledger, period)
        )
    }
}
