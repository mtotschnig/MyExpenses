package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.After
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteParty
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup

@TestShard2
class ExpenseEditCachedDataTest: BaseExpenseEditTest() {

    private lateinit var party: Party

    //BUG: https://github.com/mtotschnig/MyExpenses/issues/1293
    //TODO: test all fields
    //fails on Tablet portrait
    @Test
    fun shouldRestoreCachedData() {
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(accountLabel1)
        party = repository.createParty(Party.create(name = "John"))!!
        launch()
        unlock()
        setAmount(200)
        setStoredPayee("John")
        typeToAndCloseKeyBoard(R.id.Comment, "Kommentar")
        setOperationType(TransactionsContract.Transactions.TYPE_SPLIT)
        onView(withId(R.id.selected_item_chip)).check(matches(withText("John")))
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