package org.totschnig.myexpenses.next.feature.balance

import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.next.core.model.FakeLedgerSource
import org.totschnig.myexpenses.next.data.contracts.ContractFacts
import org.totschnig.myexpenses.next.core.model.ledger.AnalysisState
import org.totschnig.myexpenses.next.core.model.ledger.Ledger
import org.totschnig.myexpenses.next.core.model.ledger.LedgerTransaction
import org.totschnig.myexpenses.next.core.model.ledger.OwnAccounts
import org.totschnig.myexpenses.next.data.balance.BalanceRepository

@OptIn(ExperimentalCoroutinesApi::class)
class BalanceViewModelTest {

    private val giro = 1L
    private val facts = ContractFacts(emptyList(), emptyList(), emptyList(), emptySet(), emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun asksForConsentAndShowsDeclined() = runTest {
        val source = FakeLedgerSource(consent = null)
        val repository = BalanceRepository(source, MutableStateFlow(facts), backgroundScope)
        val viewModel = BalanceViewModel(repository)
        assertEquals(AnalysisState.AskConsent, viewModel.balance.first { it != AnalysisState.Loading })
        source.setConsent(false)
        assertEquals(AnalysisState.Declined, viewModel.balance.first { it != AnalysisState.AskConsent })
    }

    @Test
    fun computesBalanceOfTheMonthFromTheLedger() = runTest {
        val today = LocalDate.now()
        val ledger = Ledger(
            listOf(LedgerTransaction(1, today, amount = -2500, accountId = giro)),
            OwnAccounts(setOf(giro), emptyMap())
        )
        val viewModel = BalanceViewModel(BalanceRepository(FakeLedgerSource(ledger = ledger), MutableStateFlow(facts), backgroundScope))
        val state = viewModel.balance.first { it is AnalysisState.Ready } as AnalysisState.Ready
        // Without salary the balance runs over the calendar month
        assertEquals(today.withDayOfMonth(1), state.value.balance.period.start)
        assertEquals(listOf(1L), state.value.balance.otherTransactionIds)
    }

    @Test
    fun computesAgainOnlyForANewInput() = runTest {
        val today = LocalDate.now()
        val transaction = LedgerTransaction(1, today, amount = -2500, accountId = giro)
        val accounts = OwnAccounts(setOf(giro), emptyMap())
        val source = FakeLedgerSource(ledger = Ledger(listOf(transaction), accounts), today = today)
        val repository = BalanceRepository(source, MutableStateFlow(facts), backgroundScope)
        val viewModel = BalanceViewModel(repository)
        suspend fun balanceOf(day: LocalDate) =
            viewModel.balance.first { (it as? AnalysisState.Ready)?.value?.today == day }
        balanceOf(today)
        // Read again unchanged, e.g. after the app was in the background
        source.ledger.value = Ledger(listOf(transaction.copy()), accounts)
        balanceOf(today)
        assertEquals(1, repository.memo.computations)
        source.today.value = today.plusDays(1)
        balanceOf(today.plusDays(1))
        assertEquals(2, repository.memo.computations)
    }
}
