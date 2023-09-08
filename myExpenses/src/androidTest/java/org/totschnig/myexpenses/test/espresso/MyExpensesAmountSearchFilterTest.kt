package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit.Companion.DebugInstance
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import kotlin.math.absoluteValue

class MyExpensesAmountSearchFilterTest : BaseMyExpensesTest() {

    @Before
    fun fixture() {
        val currency = DebugInstance
        val account = buildAccount("Test account 1")
        val op = Transaction.getNewInstance(account.id, homeCurrency)
        op.amount =  Money(currency, amount1)
        op.save(contentResolver)
        op.amount = Money(currency, amount2)
        op.date = op.date - 10000
        op.saveAsNew(contentResolver)
        launch(account.id)
    }

    @Test
    fun amountFilterShouldHideTransaction() {
        assertListSize(2)
        amountIsDisplayed(amount1, 0)
        amountIsDisplayed(amount2, 1)
        onView(withId(R.id.SEARCH_COMMAND)).perform(click())
        onView(ViewMatchers.withText(R.string.amount)).perform(click())
        onView(withId(R.id.amount1)).perform(typeText("12"))
        closeSoftKeyboard()
        onView(withId(android.R.id.button1)).perform(click())
        assertListSize(1)
        amountIsDisplayed(amount1, 0)
        //switch off filter
        onView(withId(R.id.SEARCH_COMMAND)).perform(click())
        onView(withSubstring(getString(R.string.expense))).perform(click())
        assertListSize(2)
        amountIsDisplayed(amount2, 1)
    }

    private fun amountIsDisplayed(amount: Long, position: Int) {
        assertTextAtPosition((amount.absoluteValue /100).toString(), position)
    }

    companion object {
        private const val amount1 = -1200L
        private const val amount2 = -3400L
    }
}