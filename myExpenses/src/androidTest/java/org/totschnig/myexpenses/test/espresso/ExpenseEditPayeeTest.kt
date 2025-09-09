package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.onIdle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteParty
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup

// fails on Nexus 7 emulator Portrait
@TestShard2
class ExpenseEditPayeeTest: BaseExpenseEditTest() {

    private lateinit var party: Party

    suspend fun fixture(withIban: String?) {
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(accountLabel1)
        party = repository.createParty(Party.create(name = "John", iban = withIban))!!
        launch()
        assertThat(load()).isEmpty()
    }

    private suspend fun load() =  repository.loadTransactions(account1.id)

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteParty(party.id)
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
            assertThat(load().first().party).isEqualTo(party.id)
        }
    }
}