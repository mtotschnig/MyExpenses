package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest

class SelectedSumTest : BaseMyExpensesTest() {

    @Before
    fun fixture() {
        val account = Account("Test account 1", CurrencyUnit.DebugInstance, 0, "",
                AccountType.CASH, Account.DEFAULT_COLOR)
        account.save()
        val op0 = Transaction.getNewInstance(account)
        op0.amount = Money(CurrencyUnit.DebugInstance, -1200L)
        op0.save()
        val times = 5
        for (i in 0 until times) {
            op0.saveAsNew()
        }
        launch(account.id)
    }

    @Test
    fun testSelectedSum() {
        runTheTest()
        clickMenuItem(R.id.action_mode_close_button)
        runTheTest()
    }

    private fun runTheTest() {
        openCab(null)
        var sum = 12
        for (i in 2 until 5) {
            select(i)
            sum+=12
            testTitle(sum)
        }
    }

    private fun testTitle(sum: Int) {
        Espresso.onView(withId(R.id.action_bar_title))
            .check(matches(withText(containsString(String.format("%.2f", sum.toFloat())))))
    }

    private fun select(position: Int) {
        listNode.onChildren()[position].performClick()
    }
}