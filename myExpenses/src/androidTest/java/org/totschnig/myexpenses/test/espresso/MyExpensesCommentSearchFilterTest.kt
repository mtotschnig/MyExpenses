package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup
import java.time.LocalDateTime

@TestShard3
class MyExpensesCommentSearchFilterTest : BaseMyExpensesTest() {
    private lateinit var account: Account

    @Before
    fun fixture() {
        account =  buildAccount("Test account 1")
        repository.insertTransaction(
            accountId = account.id,
            amount = 1000L,
            comment = COMMENT_1
        )
        repository.insertTransaction(
            accountId = account.id,
            amount = 1000L,
            comment = COMMENT_2,
            date = LocalDateTime.now().minusMinutes(1)
        )
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
        commentIsDisplayed(COMMENT_1, 0)
        commentIsDisplayed(COMMENT_2, 1)
        selectFilter(R.string.notes) {
            composeTestRule.onNodeWithText(getString(R.string.search_comment)).performTextInput(COMMENT_1)
            composeTestRule.onNodeWithText(getString(android.R.string.ok)).performClick()
        }
        assertListSize(1)
        commentIsDisplayed(COMMENT_1, 0)
        clearFilters()
        assertListSize(2)
        commentIsDisplayed(COMMENT_2, 1)
    }

    private fun commentIsDisplayed(comment: String, position: Int) {
        assertTextAtPosition(comment, position)
    }

    companion object {
        private const val COMMENT_1 = "something"
        private const val COMMENT_2 = "different"
    }
}