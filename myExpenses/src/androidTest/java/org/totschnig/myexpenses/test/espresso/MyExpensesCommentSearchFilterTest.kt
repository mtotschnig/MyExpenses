package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model.CurrencyUnit.Companion.DebugInstance
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup

@TestShard3
class MyExpensesCommentSearchFilterTest : BaseMyExpensesTest() {
    private lateinit var account: Account

    @Before
    fun fixture() {
        val currency = DebugInstance
        account =  buildAccount("Test account 1")
        val op = Transaction.getNewInstance(account.id, homeCurrency)
        op.amount = Money(currency, 1000L)
        op.comment = comment1
        op.save(contentResolver)
        op.comment =  comment2
        op.date = op.date - 10000
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
    fun commentFilterShouldHideTransaction() {
        assertListSize(2)
        commentIsDisplayed(comment1, 0)
        commentIsDisplayed(comment2, 1)
        selectFilter(R.string.notes) {
            composeTestRule.onNodeWithText(getString(R.string.search_comment)).performTextInput(comment1)
            composeTestRule.onNodeWithText(getString(android.R.string.ok)).performClick()
        }
        assertListSize(1)
        commentIsDisplayed(comment1, 0)
        clearFilters()
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