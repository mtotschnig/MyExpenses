package org.totschnig.myexpenses.testutils

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import org.junit.Rule
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.compose.TEST_TAG_LIST

abstract class BaseComposeTest<A: ProtectedFragmentActivity>: BaseUiTest<A>() {
    abstract val listNode: SemanticsNodeInteraction

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    fun assertTextAtPosition(text: String, position: Int, substring: Boolean = true) {
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).onChildren()[position].assertTextContains(
            text,
            substring = substring
        )
    }

    private fun hasCollectionInfo(expectedColumnCount: Int, expectedRowCount: Int): SemanticsMatcher {
        return SemanticsMatcher("Collection has $expectedColumnCount columns, $expectedRowCount rows") {
            with(it.config[SemanticsProperties.CollectionInfo]) {
                columnCount == expectedColumnCount && rowCount == expectedRowCount
            }
        }
    }

    fun hasRowCount(expectedRowCount: Int) = hasCollectionInfo(1, expectedRowCount)
    fun hasColumnCount(expectedColumnCount: Int) = hasCollectionInfo(expectedColumnCount, 1)

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
}