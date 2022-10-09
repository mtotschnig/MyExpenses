package org.totschnig.myexpenses.testutils

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import org.junit.Rule
import org.totschnig.myexpenses.activity.TestMyExpenses

abstract class BaseMyExpensesTest: BaseUiTest<TestMyExpenses>() {
    lateinit var activityScenario: ActivityScenario<TestMyExpenses>

    override val testScenario: ActivityScenario<out TestMyExpenses>
        get() = activityScenario

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    fun assertListSize(expectedSize: Int) {
        composeTestRule.onNodeWithTag("LIST").onChildren().filter(hasTestTag("ITEM")).assertCountEquals(expectedSize)
    }

    fun openCab() {
        composeTestRule.onNodeWithTag("LIST").onChildren().onFirst().performTouchInput { longClick() }
    }

    fun openContext() {
        composeTestRule.onNodeWithTag("LIST").onChildren().onFirst().performClick()
    }

    fun clickContextItem(resId: Int) {
        openContext()
        composeTestRule.onNodeWithText(getString(resId)).performClick()
    }
}