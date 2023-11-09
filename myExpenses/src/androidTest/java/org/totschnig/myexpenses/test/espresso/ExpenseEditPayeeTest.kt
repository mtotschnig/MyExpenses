package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.model2.Party


class ExpenseEditPayeeTest: BaseExpenseEditTest() {

    private lateinit var party: Party

    fun fixture(withIban: String?) {
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(accountLabel1)
        party = repository.createParty(Party.create(name = "John", iban = withIban))
        testScenario = ActivityScenario.launch(intentForNewTransaction)
        assertThat(repository.loadTransactions(account1.id)).isEmpty()
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
        fixture(withIban)
        setStoredPayee("John")
        setAmount(101)
        onView(ViewMatchers.withId(R.id.CREATE_COMMAND)).perform(click())
        onIdle()
        assertThat(repository.loadTransactions(account1.id).first().party).isEqualTo(party.id)
    }
}