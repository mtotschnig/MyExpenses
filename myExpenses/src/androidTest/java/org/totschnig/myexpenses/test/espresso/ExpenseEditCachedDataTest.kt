package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions
import org.junit.After
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteParty
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.testutils.cleanup

class ExpenseEditCachedDataTest: BaseExpenseEditTest() {

    private lateinit var party: Party

    //BUG: https://github.com/mtotschnig/MyExpenses/issues/1293
    //TODO: test all fields
    @Test
    fun shouldRestoreCachedData() {
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(accountLabel1)
        party = repository.createParty(Party.create(name = "John"))
        testScenario = ActivityScenario.launch(intentForNewTransaction)
        unlock()
        setAmount(200)
        setStoredPayee("John")
        typeToAndCloseKeyBoard(R.id.Comment, "Kommentar")
        setOperationType(TransactionsContract.Transactions.TYPE_SPLIT)
        onView(withId(R.id.Payee)).check(matches(withText("John")))
        checkAmount(200)
        onView(withId(R.id.Comment)).check(matches(withText("Kommentar")))
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteParty(party.id)
            prefHandler.remove(PrefKey.AUTO_FILL_SWITCH)
            prefHandler.remove(PrefKey.AUTO_FILL_HINT_SHOWN)
        }
    }
}