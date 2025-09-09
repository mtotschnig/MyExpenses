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
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard1
import org.totschnig.myexpenses.testutils.cleanup
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.absoluteValue

@TestShard1
class ArchiveDetailWithSearchTest : BaseMyExpensesTest() {
    private lateinit var account: Account

    companion object {
        private const val amount1 = -1200L
        private const val amount2 = -3400L
        private const val archiveTotal = amount1 + amount2
    }

    @Before
    fun fixture() {
        account = buildAccount("Test account 1", openingBalance = 1000)
        val op0 = Transaction.getNewInstance(account.id, homeCurrency)
        val date = LocalDate.of(2024, 1, 22)
        val dateTime = LocalDateTime.of(date, LocalTime.of(12, 0)).toEpochSecond(ZoneOffset.UTC)
        op0.date = dateTime
        op0.amount = Money(CurrencyUnit.DebugInstance, amount1)
        op0.save(contentResolver)
        op0.amount = Money(CurrencyUnit.DebugInstance, amount2)
        op0.date = dateTime
        op0.saveAsNew(contentResolver)
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
            onView(withId(R.id.amount1)).perform(typeText("12"))
            closeSoftKeyboard()
            onView(withId(android.R.id.button1)).perform(click())
        }
        assertTextAtPosition((archiveTotal.absoluteValue / 100).toString(), 0)
        clickContextItem(R.string.details)
        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag(TEST_TAG_DIALOG_ROOT)) and
                    hasTestTag(TEST_TAG_FILTER_CARD)
        ).assertExists()
        with(composeTestRule.onNodeWithTag(TEST_TAG_PART_LIST)) {
            assertTextAtPosition((amount1.absoluteValue / 100).toString(), 0, anyDescendant = true)
            assert(hasRowCount(1))
        }
        composeTestRule.onNodeWithText(getString(android.R.string.ok)).performClick()
    }
}