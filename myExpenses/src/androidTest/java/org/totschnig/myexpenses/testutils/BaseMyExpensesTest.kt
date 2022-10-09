package org.totschnig.myexpenses.testutils

import android.util.Log
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
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

    fun hasCollectionInfo(expectedColumnCount: Int, expectedRowCount: Int): SemanticsMatcher {
        return SemanticsMatcher("Collection has $expectedColumnCount columns, $expectedRowCount rows") {
            with(it.config[SemanticsProperties.CollectionInfo]) {
                Log.e("hasCollectionInfo", "Count the beast: $columnCount x $rowCount")
                columnCount == expectedColumnCount && rowCount == expectedRowCount
            }
        }
    }

    fun hasRowCount(expectedRowCount: Int) = hasCollectionInfo(1, expectedRowCount)

    fun assertListSize(expectedSize: Int) {
        composeTestRule.onNodeWithTag("LIST").assert(hasRowCount(expectedSize))
    }

    fun openCab(@IdRes command: Int?) {
        composeTestRule.onNodeWithTag("LIST").onChildren().onFirst()
            .performTouchInput { longClick() }
        command?.let { clickMenuItem(it, true) }
    }

    fun clickContextItem(@StringRes resId: Int) {
        composeTestRule.onNodeWithTag("LIST").onChildren().onFirst().performClick()
        composeTestRule.onNodeWithText(getString(resId)).performClick()
    }
}