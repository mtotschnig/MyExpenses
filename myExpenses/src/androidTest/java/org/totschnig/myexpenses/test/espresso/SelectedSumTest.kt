package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.testutils.TestShard5
import org.totschnig.myexpenses.testutils.cleanup

@TestShard5
class SelectedSumTest : BaseMyExpensesTest() {
    lateinit var account: org.totschnig.myexpenses.model2.Account

    @Before
    fun fixture() {
        account =  buildAccount("Test account 1")
        val op0 = Transaction.getNewInstance(account.id, homeCurrency)
        op0.amount = Money(CurrencyUnit.DebugInstance, -1200L)
        op0.save(contentResolver)
        repeat(5) {
            op0.saveAsNew(contentResolver)
        }
        launch(account.id)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account.id)
        }
    }

    @Test
    fun testSelectedSum() {
        runTheTest()
        clickMenuItem(androidx.appcompat.R.id.action_mode_close_button)
        runTheTest()
    }

    private fun runTheTest() {
        openCab(null)
        var sum = 12
        for (i in 1 .. 5) {
            select(i)
            sum+=12
            testTitle(sum)
        }
    }

    private fun testTitle(sum: Int) {
        Espresso.onView(withId(androidx.appcompat.R.id.action_bar_title))
            .check(matches(withText(containsString(String.format("%.2f", sum.toFloat())))))
    }

    private fun select(position: Int) {
        listNode.onChildren()[position].performClick()
    }
}