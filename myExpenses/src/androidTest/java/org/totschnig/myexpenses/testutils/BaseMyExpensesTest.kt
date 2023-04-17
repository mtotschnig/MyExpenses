package org.totschnig.myexpenses.testutils

import android.content.Intent
import androidx.annotation.IdRes
import androidx.compose.ui.test.*
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import org.junit.After
import org.totschnig.myexpenses.activity.TestMyExpenses
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
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

    override val listNode: SemanticsNodeInteraction
        get() = composeTestRule.onNodeWithTag(TEST_TAG_PAGER)
            .onChildren()
            .onFirst()
            .onChildren()
            .filter(hasTestTag(TEST_TAG_LIST)).onFirst()

    fun openCab(@IdRes command: Int?) {
        listNode.onChildren().onFirst()
            .performTouchInput { longClick() }
        command?.let { clickMenuItem(it, true) }
    }

    @After
    open fun tearDown() {
        transactionPagingIdlingResource?.let {
            IdlingRegistry.getInstance().unregister(it)
        }
        IdlingRegistry.getInstance().unregister(countingResource)
    }
}