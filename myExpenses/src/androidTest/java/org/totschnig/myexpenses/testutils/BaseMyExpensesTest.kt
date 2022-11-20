package org.totschnig.myexpenses.testutils

import android.content.Intent
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import org.junit.After
import org.junit.Rule
import org.totschnig.myexpenses.activity.TestMyExpenses
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.provider.DatabaseConstants

abstract class BaseMyExpensesTest : BaseUiTest<TestMyExpenses>() {
    private val countingResource = CountingIdlingResource("CheckSealed")
    private var transactionPagingIdlingResource: IdlingResource? = null

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    fun launch(id: Long? = null) {
        testScenario = ActivityScenario.launch(
            Intent(targetContext, TestMyExpenses::class.java).apply {
                putExtra(DatabaseConstants.KEY_ROWID, id)
            })
        testScenario.onActivity { activity ->
            activity?.let {
                it.decoratedCheckSealedHandler =
                    DecoratedCheckSealedHandler(activity.contentResolver, countingResource)
                transactionPagingIdlingResource = (it.viewModel as DecoratingMyExpensesViewModel).countingResource
                IdlingRegistry.getInstance().register(transactionPagingIdlingResource)
            }

        }
        IdlingRegistry.getInstance().register(countingResource)
    }

    fun hasCollectionInfo(expectedColumnCount: Int, expectedRowCount: Int): SemanticsMatcher {
        return SemanticsMatcher("Collection has $expectedColumnCount columns, $expectedRowCount rows") {
            with(it.config[SemanticsProperties.CollectionInfo]) {
                columnCount == expectedColumnCount && rowCount == expectedRowCount
            }
        }
    }

    fun hasChildCount(expectedChildCount: Int): SemanticsMatcher {
        return SemanticsMatcher("has $expectedChildCount children") {
            it.children.size == expectedChildCount
        }
    }

    fun hasRowCount(expectedRowCount: Int) = hasCollectionInfo(1, expectedRowCount)
    fun hasColumnCount(expectedColumnCount: Int) = hasCollectionInfo(expectedColumnCount, 1)

    fun assertListSize(expectedSize: Int) {
        composeTestRule.waitUntil {  composeTestRule
            .onAllNodesWithTag(TEST_TAG_PAGER)
            .fetchSemanticsNodes().size == 1 }
        listNode.assert(hasRowCount(expectedSize))
    }

    val listNode: SemanticsNodeInteraction
        get() = composeTestRule.onNodeWithTag(TEST_TAG_PAGER).onChildren()
            .filter(hasTestTag(TEST_TAG_LIST)).onFirst()

    fun openCab(@IdRes command: Int?) {
        composeTestRule.onNodeWithTag(TEST_TAG_PAGER).onChildren().onFirst().onChildren().onFirst()
            .performTouchInput { longClick() }
        command?.let { clickMenuItem(it, true) }
    }

    fun clickContextItem(
        @StringRes resId: Int,
        node: SemanticsNodeInteraction = listNode,
        position: Int = 0,
        onLongClick: Boolean = false
    ) {
        node.onChildren()[position].performTouchInput {
            if (onLongClick) longClick() else click()
        }
        composeTestRule.onNodeWithText(getString(resId)).performClick()
    }

    fun assertTextAtPosition(text: String, position: Int, substring: Boolean = true) {
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).onChildren()[position].assertTextContains(
            text,
            substring = substring
        )
    }

    @After
    open fun tearDown() {
        transactionPagingIdlingResource?.let {
            IdlingRegistry.getInstance().unregister(it)
        }
        IdlingRegistry.getInstance().unregister(countingResource)
    }
}