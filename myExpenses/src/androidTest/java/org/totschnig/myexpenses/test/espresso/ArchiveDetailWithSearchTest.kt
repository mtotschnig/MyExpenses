package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.TEST_TAG_DIALOG_ROOT
import org.totschnig.myexpenses.compose.TEST_TAG_FILTER_CARD
import org.totschnig.myexpenses.compose.TEST_TAG_PART_LIST
import org.totschnig.myexpenses.db2.archive
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard1
import org.totschnig.myexpenses.testutils.cleanup
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.absoluteValue

@TestShard1
class ArchiveDetailWithSearchTest : BaseMyExpensesTest() {
    private lateinit var account: Account

    companion object {
        private const val AMOUNT1 = -1200L
        private const val AMOUNT2 = -3400L
        private const val ARCHIVE_TOTAL = AMOUNT1 + AMOUNT2
    }

    @Before
    fun fixture() {
        account = buildAccount("Test account 1", openingBalance = 1000)
        val date = LocalDate.of(2024, 1, 22)
        val dateTime = LocalDateTime.of(date, LocalTime.of(12, 0))
        repository.insertTransaction(
            amount = AMOUNT1,
            accountId = account.id,
            date = dateTime
        )
        repository.insertTransaction(
            amount = AMOUNT2,
            accountId = account.id,
            date = dateTime
        )
        repository.archive(account.id, date to date)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account.id)
        }
    }

    @Test
    fun showDetailWithSearch() {
        launch(account.id)
        selectFilter(R.string.amount) {
            onView(withId(R.id.amount1)).inRoot(isDialog()).perform(typeText("12"))
            closeSoftKeyboard()
            onView(withId(android.R.id.button1)).perform(click())
        }
        assertTextAtPosition((ARCHIVE_TOTAL.absoluteValue / 100).toString(), 0)
        clickContextItem(R.string.details)
        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag(TEST_TAG_DIALOG_ROOT)) and
                    hasTestTag(TEST_TAG_FILTER_CARD)
        ).assertExists()
        with(composeTestRule.onNodeWithTag(TEST_TAG_PART_LIST)) {
            assertTextAtPosition((AMOUNT1.absoluteValue / 100).toString(), 0, anyDescendant = true)
            assert(hasRowCount(1))
        }
        composeTestRule.onNodeWithText(getString(android.R.string.ok)).performClick()
    }
}