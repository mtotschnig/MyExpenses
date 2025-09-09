package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model.CurrencyUnit.Companion.DebugInstance
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup

@TestShard3
class MyExpensesAmountSearchFilterTest : BaseMyExpensesTest() {

    lateinit var account: Account

    @Before
    fun fixture() {
        val currency = DebugInstance
        account = buildAccount("Test account 1")
        val op = Transaction.getNewInstance(account.id, homeCurrency)
        op.amount = Money(currency, AMOUNT1)
        op.save(contentResolver)
        op.amount = Money(currency, AMOUNT2)
        op.date -= 10000
        op.saveAsNew(contentResolver)
        launch(account.id)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account.id)
        }
    }

    @Test
    fun amountFilterShouldHideTransaction() {
        assertListSize(2)
        amountIsDisplayed(AMOUNT1, 0)
        amountIsDisplayed(AMOUNT2, 1)
        selectFilter(R.string.amount) {
            onView(withId(R.id.amount1)).perform(typeText("12"))
            closeSoftKeyboard()
            onView(withId(android.R.id.button1)).perform(click())
        }
        assertListSize(1)
        amountIsDisplayed(AMOUNT1, 0)
        clearFilters()
        assertListSize(2)
        amountIsDisplayed(AMOUNT2, 1)
    }

    private fun amountIsDisplayed(amount: Long, position: Int) {
        composeTestRule.onNodeWithTag(TEST_TAG_LIST)
            .onChildren()[position].assert(hasAmount(amount))
    }

    companion object {
        private const val AMOUNT1 = -1200L
        private const val AMOUNT2 = -3400L
    }
}