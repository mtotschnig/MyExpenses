package org.totschnig.myexpenses.testutils

import android.content.Intent
import androidx.annotation.IdRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.After
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.TestMyExpenses
import org.totschnig.myexpenses.compose.TEST_TAG_DIALOG
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.provider.DatabaseConstants

abstract class BaseMyExpensesTest : BaseComposeTest<TestMyExpenses>() {
    private val countingResource = CountingIdlingResource("CheckSealed")
    private var transactionPagingIdlingResource: IdlingResource? = null

    fun launch(id: Long? = null) {
        testScenario = ActivityScenario.launch(
            Intent(targetContext, TestMyExpenses::class.java).apply {
                putExtra(DatabaseConstants.KEY_ROWID, id)
            })
        testScenario.onActivity { activity ->
            activity?.let {
                it.decoratedCheckSealedHandler =
                    DecoratedCheckSealedHandler(activity.contentResolver, countingResource)
                transactionPagingIdlingResource =
                    (it.viewModel as DecoratingMyExpensesViewModel).countingResource
                IdlingRegistry.getInstance().register(transactionPagingIdlingResource)
            }

        }
        IdlingRegistry.getInstance().register(countingResource)
    }

    fun hasChildCount(expectedChildCount: Int): SemanticsMatcher {
        return SemanticsMatcher("has $expectedChildCount children") {
            it.children.size == expectedChildCount
        }
    }

    fun assertListSize(expectedSize: Int) {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodesWithTag(TEST_TAG_PAGER)
                .fetchSemanticsNodes().size == 1
        }
        listNode.assert(hasRowCount(expectedSize))
    }

    fun openCab(@IdRes command: Int?) {
        listNode.onChildren().onFirst()
            .performTouchInput { longClick() }
        command?.let { clickMenuItem(it, true) }
    }

    fun selectFilter(column: Int, dialogActions: () -> Unit) {
        onView(withId(R.id.SEARCH_COMMAND)).perform(ViewActions.click())
        composeTestRule.onNodeWithText(getString(column), useUnmergedTree = true)
            .performClick()
        dialogActions()
        composeTestRule.onNodeWithContentDescription(getString(R.string.apply)).performClick()
        waitForDialogClosed()
    }

    fun clearFilters() {
        onView(withId(R.id.SEARCH_COMMAND)).perform(ViewActions.click())
        composeTestRule.onNodeWithContentDescription(getString(R.string.clear_all_filters)).performClick()
        composeTestRule.onNodeWithText(getString(android.R.string.ok)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.apply)).performClick()
        waitForDialogClosed()
    }

    private fun waitForDialogClosed() {
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(TEST_TAG_DIALOG).isNotDisplayed()
        }
    }

    @After
    fun tearDown() {
        transactionPagingIdlingResource?.let {
            IdlingRegistry.getInstance().unregister(it)
        }
        IdlingRegistry.getInstance().unregister(countingResource)
    }

    protected fun assertDataSize(size: Int) {
        with(composeTestRule.onNodeWithTag(TEST_TAG_PAGER)) {
            if (size > 0) {
                assert(hasColumnCount(size))
            } else {
                assertDoesNotExist()
            }
        }
    }
}