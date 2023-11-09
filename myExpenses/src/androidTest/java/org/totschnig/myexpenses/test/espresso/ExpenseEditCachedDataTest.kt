package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.model2.Party

class ExpenseEditCachedDataTest: BaseExpenseEditTest() {

    //BUG: https://github.com/mtotschnig/MyExpenses/issues/1293
    //TODO: test all fields
    @Test
    fun shouldRestoreCachedData() {
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(accountLabel1)
        repository.createParty(Party.create(name = "John"))
        testScenario = ActivityScenario.launch(intentForNewTransaction)
        unlock()
        setAmount(200)
        setStoredPayee("John")
        closeSoftKeyboard()
        onView(withId(R.id.Comment)).perform(scrollTo(), typeText("Kommentar"))
        setOperationType(TransactionsContract.Transactions.TYPE_SPLIT)
        onView(withId(R.id.Payee)).check(matches(withText("John")))
        checkAmount(200)
        onView(withId(R.id.Comment)).check(matches(withText("Kommentar")))
    }
}