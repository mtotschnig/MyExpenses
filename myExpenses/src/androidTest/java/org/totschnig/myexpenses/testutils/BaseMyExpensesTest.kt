package org.totschnig.myexpenses.testutils

import android.content.Intent
import androidx.annotation.IdRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
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
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.After
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpensesV2
import org.totschnig.myexpenses.compose.TEST_TAG_DIALOG
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.provider.KEY_ROWID
import timber.log.Timber

abstract class BaseMyExpensesTest : BaseComposeTest<MyExpensesV2>() {
    private var transactionPagingIdlingResource: IdlingResource? = null

    fun launch(id: Long? = null) {
        testScenario = ActivityScenario.launch(
            Intent(targetContext, MyExpensesV2::class.java).apply {
                putExtra(KEY_ROWID, id)
            })
        testScenario.onActivity { activity ->
            activity?.let {
                transactionPagingIdlingResource =
                    (it.viewModel as DecoratingMyExpensesViewModel).countingResource
                IdlingRegistry.getInstance().register(transactionPagingIdlingResource)
            }
        }
    }

    fun hasChildCount(expectedChildCount: Int): SemanticsMatcher {
        return SemanticsMatcher("has $expectedChildCount children") {
            it.children.size == expectedChildCount
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun assertListSize(expectedSize: Int) {
        composeTestRule.waitUntilNodeCount(
            hasTestTag(TEST_TAG_LIST) and hasRowCount(expectedSize),
            count = 1
        )
    }

    fun openCab(@IdRes command: Int?) {
        listNode.onChildren().onFirst()
            .performTouchInput { longClick() }
        command?.let { clickMenuItem(it, true) }
    }

    fun selectFilter(column: Int, dialogActions: () -> Unit) {
        composeTestRule.onNodeWithTag("Search").performClick()
        composeTestRule.onNodeWithText(getString(column), useUnmergedTree = true)
            .performClick()
        dialogActions()
        composeTestRule.onNodeWithContentDescription(getString(R.string.apply)).performClick()
        waitForDialogClosed()
    }

    fun clearFilters() {
        composeTestRule.onNodeWithTag("Search").performClick()
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