package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.compose.TEST_TAG_CAB
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard5
import org.totschnig.myexpenses.testutils.cleanup

@TestShard5
class SelectedSumTest : BaseMyExpensesTest() {
    lateinit var account: org.totschnig.myexpenses.model2.Account

    @Before
    fun fixture() {
        account =  buildAccount("Test account 1")
        repeat(6) {
            repository.insertTransaction(
                accountId = account.id,
                amount = -1200L
            )
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
        pressBack()
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
        composeTestRule.onNodeWithTag(TEST_TAG_CAB).assertTextContains(String.format("%.2f", sum.toFloat()), substring = true)
    }

    private fun select(position: Int) {
        listNode.onChildren()[position].performClick()
    }
}