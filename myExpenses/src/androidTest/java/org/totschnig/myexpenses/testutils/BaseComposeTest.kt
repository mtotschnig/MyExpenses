package org.totschnig.myexpenses.testutils

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import org.junit.Rule
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.compose.amountProperty
import org.totschnig.myexpenses.compose.headerProperty
import timber.log.Timber

abstract class BaseComposeTest<A : ProtectedFragmentActivity> : BaseUiTest<A>() {
    val listNode: SemanticsNodeInteraction
        get() = composeTestRule.onNodeWithTag(TEST_TAG_LIST)

    val listNodeUnmerged: SemanticsNodeInteraction
        get() = composeTestRule.onNodeWithTag(TEST_TAG_LIST, useUnmergedTree = true)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    fun assertTextAtPosition(
        text: String,
        position: Int,
        substring: Boolean = true,
        anyDescendant: Boolean = false,
    ) {
        composeTestRule.onNodeWithTag(TEST_TAG_LIST)
            .assertTextAtPosition(text, position, substring, anyDescendant)
    }

    fun SemanticsNodeInteraction.assertTextAtPosition(
        text: String,
        position: Int,
        substring: Boolean = true,
        anyDescendant: Boolean = false,
    ) {
        val nodeInteraction = onChildren()[position]
        if (anyDescendant) {
            nodeInteraction.assert(
                hasAnyDescendant(
                    hasText(text, substring)
                )
            )
        } else {
            nodeInteraction.assertTextContains(text, substring)
        }
    }

    private fun hasCollectionInfo(expectedColumnCount: Int, expectedRowCount: Int) =
        SemanticsMatcher("Collection has $expectedColumnCount columns, $expectedRowCount rows") {
            with(it.config[SemanticsProperties.CollectionInfo]) {
                val result = columnCount == expectedColumnCount && rowCount == expectedRowCount
                if (!result) {
                    Timber.d("Actual colums/rows: %d/%d", columnCount, rowCount)
                }
                result
            }
        }

    fun hasRowCount(expectedRowCount: Int) = hasCollectionInfo(1, expectedRowCount)
    fun hasColumnCount(expectedColumnCount: Int) = hasCollectionInfo(expectedColumnCount, 1)

    fun hasAmount(amount: Long) = SemanticsMatcher.expectValue(amountProperty, amount)

    fun hasHeaderId(headerId: Int) = SemanticsMatcher.expectValue(headerProperty, headerId)

    fun clickContextItem(
        @StringRes resId: Int,
        listNode: SemanticsNodeInteraction = this.listNode,
        position: Int = 0,
        onLongClick: Boolean = false,
    ) {
        clickContextItem(resId, listNode.onChildren()[position], onLongClick)
    }

    fun clickContextItem(
        @StringRes resId: Int,
        itemNode: SemanticsNodeInteraction,
        onLongClick: Boolean = false,
    ) {
        itemNode.performTouchInput {
            if (onLongClick) longClick() else click()
        }
        if (!isOrchestrated) {
            Thread.sleep(200)
        }
        composeTestRule.onNodeWithText(getString(resId)).performClick()
    }


}