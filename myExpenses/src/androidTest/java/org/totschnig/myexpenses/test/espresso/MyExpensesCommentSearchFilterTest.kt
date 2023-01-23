package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit.Companion.DebugInstance
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest

class MyExpensesCommentSearchFilterTest : BaseMyExpensesTest() {
    @Before
    fun fixture() {
        val currency = DebugInstance
        val account = Account(
            "Test account 1", currency, 0, "",
            AccountType.CASH, Account.DEFAULT_COLOR
        )
        account.save()
        val op = Transaction.getNewInstance(account)
        op.amount = Money(currency, 1000L)
        op.comment = comment1
        op.save()
        op.comment =  comment2
        op.date = op.date - 10000
        op.saveAsNew()
        launch(account.id)
    }

    @Test
    fun commentFilterShouldHideTransaction() {
        assertListSize(2)
        commentIsDisplayed(comment1, 0)
        commentIsDisplayed(comment2, 1)
        Espresso.onView(ViewMatchers.withId(R.id.SEARCH_COMMAND)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.comment)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.editText))
            .perform(ViewActions.typeText(comment1), ViewActions.closeSoftKeyboard())
        Espresso.onView(ViewMatchers.withId(android.R.id.button1)).perform(ViewActions.click())
        assertListSize(1)
        commentIsDisplayed(comment1, 0)
        //switch off filter
        Espresso.onView(ViewMatchers.withId(R.id.SEARCH_COMMAND)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(comment1)).perform(ViewActions.click())
        assertListSize(2)
        commentIsDisplayed(comment2, 1)
    }

    private fun commentIsDisplayed(comment: String, position: Int) {
        assertTextAtPosition(comment, position)
    }

    companion object {
        private const val comment1 = "something"
        private const val comment2 = "different"
    }
}