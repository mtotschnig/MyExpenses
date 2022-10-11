package org.totschnig.myexpenses.testutils

import android.content.Intent
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import org.junit.After
import org.junit.Rule
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.activity.TestMyExpenses
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.test.espresso.MyExpensesTest

abstract class BaseMyExpensesTest: BaseUiTest<MyExpenses>() {
    private val countingResource = CountingIdlingResource("CheckSealed")
    lateinit var activityScenario: ActivityScenario<out MyExpenses>

    override val testScenario: ActivityScenario<out MyExpenses>
        get() = activityScenario

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    fun launch(id: Long? = null, clazz: Class<out MyExpenses> = MyExpenses::class.java) {
        activityScenario = ActivityScenario.launch(
            Intent(targetContext, clazz).apply {
                putExtra(DatabaseConstants.KEY_ROWID, id)
            })
        activityScenario.onActivity { activity: MyExpenses ->
            (activity as? TestMyExpenses)?.decoratedCheckSealedHandler =
                DecoratedCheckSealedHandler(activity.contentResolver, countingResource)
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

    fun hasRowCount(expectedRowCount: Int) = hasCollectionInfo(1, expectedRowCount)
    fun hasColumnCount(expectedColumnCount: Int) = hasCollectionInfo(expectedColumnCount, 1)

    fun assertListSize(expectedSize: Int) {
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

    fun clickContextItem(@StringRes resId: Int, node: SemanticsNodeInteraction = listNode, position: Int = 0) {
        node.onChildren()[position].performClick()
        composeTestRule.onNodeWithText(getString(resId)).performClick()
    }

    fun assertTextAtPosition(text: String, position: Int, substring: Boolean = true) {
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).onChildren()[position].assertTextContains(text, substring = substring)
    }

    @After
    open fun tearDown() {
        IdlingRegistry.getInstance().unregister(countingResource)
    }
}