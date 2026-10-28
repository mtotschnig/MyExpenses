package org.totschnig.myexpenses.next.data.contracts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.totschnig.myexpenses.next.core.model.ledger.Ledger
import org.totschnig.myexpenses.next.core.model.ledger.LedgerTransaction
import org.totschnig.myexpenses.next.core.model.ledger.OwnAccounts
import java.time.LocalDate
import java.time.Period

class OwnTransferTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)
    private val giro = 1L
    private val creditCard = 2L
    private val savings = 3L
    private val daily = setOf(giro, creditCard)
    private var nextId = 1L

    private fun payment(
        amount: Long,
        account: Long = giro,
        target: Long? = null,
        payeeId: Long? = 9,
        last: LocalDate = today.minusDays(3),
        monthsBack: Int = 0,
    ) = ContractTransaction(
        id = nextId++, date = last.minus(Period.ofMonths(monthsBack)), amount = amount, accountId = account,
        payeeId = payeeId, payeeName = if (target != null) "→ Tagesgeld" else "Payee $payeeId", targetAccountId = target
    )

    @Test
    fun readsContractTransactionsFromLedgerInHomeCurrency() {
        val ledger = Ledger(
            listOf(
                LedgerTransaction(1, today, amount = -1000, homeAmount = -1100, accountId = giro, payeeName = "Shop"),
                LedgerTransaction(2, today, amount = -5000, accountId = giro, payeeName = "Ich", targetAccountId = savings),
                LedgerTransaction(3, today, amount = 5000, accountId = savings, targetAccountId = giro),
            ),
            OwnAccounts(daily, mapOf(savings to "Tagesgeld"))
        )
        val transactions = contractTransactions(ledger)
        assertEquals(listOf(1L, 2L), transactions.map { it.id })
        assertEquals(-1100, transactions[0].amount)
        assertEquals("Shop", transactions[0].payeeName)
        // Named after the account the money goes to
        assertEquals("→ Tagesgeld", transactions[1].payeeName)
    }

    @Test
    fun keepsOnlyMovementsFromDailyAccountsToOthers() {
        val toSavings = payment(-10000, target = savings)
        val receivedOnSavings = payment(10000, account = savings, target = giro)
        val payingCreditCard = payment(-50000, target = creditCard)
        val withdrawal = payment(20000, target = savings)
        val normal = payment(-4000)
        val kept = listOf(toSavings, receivedOnSavings, payingCreditCard, withdrawal, normal)
            .withoutMovementsOtherThanReserves(daily)
        assertEquals(listOf(toSavings, normal), kept)
    }

    @Test
    fun detectsStandingOrderToSavingsAccountAsReserve() {
        // Payee is the user, as imported from the bank: grouped by target account nevertheless
        val standingOrder = (0 until 6).map { payment(-10000, target = savings, monthsBack = it) }.reversed()
        val contract = ContractDetector(today).detect(standingOrder).single()
        assertEquals("a$savings|MONTHLY", contract.signature)
        assertEquals("→ Tagesgeld", contract.name)
        assertTrue(contract.isOwnTransfer)
        assertTrue(contract.isReserve)
        val rule = ContractRule.of(contract, standingOrder)
        assertEquals(setOf(savings), rule.targetAccountIds)
        assertTrue(rule.payeeIds.isEmpty())
    }

    @Test
    fun rulesForPayeesAndForAccountsDoNotMix() {
        val byPayee = ContractRule("p", ContractRule.Kind.CONTRACT, ContractDirection.EXPENSE, setOf(9), interval = ContractInterval.MONTHLY)
        val byAccount = ContractRule("a", ContractRule.Kind.CONTRACT, ContractDirection.EXPENSE, targetAccountIds = setOf(savings), interval = ContractInterval.MONTHLY)
        val toSavings = payment(-10000, target = savings)
        val normal = payment(-4000)
        assertFalse(byPayee.matches(toSavings))
        assertTrue(byPayee.matches(normal))
        assertTrue(byAccount.matches(toSavings))
        assertFalse(byAccount.matches(normal))
    }

    @Test
    fun transferWithoutPayeeCanBeMarked() {
        val transfer = payment(-10000, target = savings, payeeId = null)
        val info = ContractAnalysis.of(listOf(transfer), emptyList(), today).infoFor(transfer.id)
        assertEquals(PaymentContractInfo.Markable(isIncome = false), info)
    }

    @Test
    fun mergingPaymentsToSavingsWithMovementsKeepsBoth() {
        // Before: payments to the savings account found by the keyword, as contract of a payee
        val byPayee = (3 until 9).map {
            payment(-10000, payeeId = 7, monthsBack = it).copy(comment = "Übertrag auf Tagesgeld")
        }.reversed()
        // Since the account is connected: movements to it
        val movements = (0 until 3).map { payment(-10000, target = savings, monthsBack = it) }.reversed()
        val transactions = byPayee + movements
        val (payeeRules, _) = emptyList<ContractRule>().withRuleFor(
            ContractAnalysis.of(transactions, emptyList(), today).contracts.single { !it.isOwnTransfer }, transactions
        ) { it.copy(name = "Tagesgeld") }
        val before = ContractAnalysis.of(transactions, payeeRules, today)
        val oldContract = before.contracts.single { it.isConfirmed }
        val movementContract = before.contracts.single { it.isOwnTransfer }
        val (merged, rule) = payeeRules.merging(oldContract, movementContract, transactions)
        assertEquals(setOf(7L), rule.payeeIds)
        assertEquals(setOf(savings), rule.targetAccountIds)
        val contract = ContractAnalysis.of(transactions, merged, today).contracts.single()
        assertEquals(9, contract.transactions.size)
        assertEquals("Tagesgeld", contract.displayName)
        assertTrue(contract.isActive)
    }
}
