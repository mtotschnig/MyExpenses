package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteParty
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.DEBT_LABEL
import org.totschnig.myexpenses.testutils.PARTY_NAME
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.viewmodel.data.Debt

// fails on Nexus 7 emulator Portrait
@TestShard2
class ExpenseEditPayeeTest : BaseExpenseEditTest() {

    private var partyId: Long = 0

    private var debtId: Long = 0

    suspend fun fixture(withIban: String? = null, withDebt: Boolean = false) {
        account1 = buildAccount(ACCOUNT_LABEL_1)
        partyId = repository.createParty(Party.create(name = PARTY_NAME, iban = withIban)!!)!!.id
        if (withDebt) {
            debtId = repository.saveDebt(
                Debt(
                    payeeId = partyId,
                    amount = 100,
                    id = 0L,
                    label = DEBT_LABEL,
                    description = "",
                    currency = homeCurrency,
                    date = System.currentTimeMillis() / 1000,
                )
            )
        }
        launch()
        assertThat(load()).isEmpty()
    }

    private suspend fun load() = repository.loadTransactions(account1.id)

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteParty(partyId)
            prefHandler.remove(PrefKey.AUTO_FILL_SWITCH)
            prefHandler.remove(PrefKey.AUTO_FILL_HINT_SHOWN)
        }
    }

    @Test
    fun shouldSelectPayee() {
        doTheTest(null)
    }


    @Test
    fun shouldSelectPayeeWithIban() {
        doTheTest("iban")
    }

    private fun doTheTest(withIban: String?) {
        runTest {
            fixture(withIban)
            setStoredPayee("John")
            setAmount(101)
            clickFab()
            onIdle()
            assertThat(load().first().payeeId).isEqualTo(partyId)
        }
    }

    @Test
    fun shouldSaveDebt() {
        runTest {
            fixture(withDebt = true)
            setAmount(101)
            setDebt()
            clickFab()
            onIdle()
            with(load().first()) {
                assertThat(payeeId).isEqualTo(partyId)
                assertThat(debtId).isEqualTo(this@ExpenseEditPayeeTest.debtId)
            }
        }
    }
}