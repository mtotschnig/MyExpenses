package org.totschnig.myexpenses.testutils

import android.content.Intent
import androidx.annotation.IdRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
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
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import org.junit.After
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpensesV2
import org.totschnig.myexpenses.compose.TEST_TAG_ACCOUNT_LABEL
import org.totschnig.myexpenses.compose.TEST_TAG_BALANCE_AMOUNT
import org.totschnig.myexpenses.compose.TEST_TAG_DIALOG
import org.totschnig.myexpenses.compose.TEST_TAG_FAB
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.compose.TEST_TAG_NAV_ACCOUNTS
import org.totschnig.myexpenses.compose.TEST_TAG_NAV_OVERFLOW
import org.totschnig.myexpenses.compose.TEST_TAG_NAV_TRANSACTIONS
import org.totschnig.myexpenses.compose.TEST_TAG_OVERFLOW_MENU
import org.totschnig.myexpenses.compose.TEST_TAG_PAGER
import org.totschnig.myexpenses.dialog.MenuItem
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.util.formatMoney

abstract class BaseMyExpensesTest : BaseComposeTest<MyExpensesV2>() {
    private var transactionPagingIdlingResource: IdlingResource? = null

    fun launch(id: Long? = null, currency: String? = null) {
        testScenario = ActivityScenario.launch(
            Intent(targetContext, MyExpensesV2::class.java).apply {
                putExtra(KEY_ROWID, id)
                putExtra(KEY_CURRENCY, currency)
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

    fun openCab(command: Int?) {
        listNode.onChildren().onFirst()
            .performTouchInput { longClick() }
        command?.let {
            clickMenuItemOverflowCompose(command)
        }
    }

    fun selectNavigationItem(tag: String) {
        // Check if the item is already present in the tree
        val nodes = composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes()

        if (nodes.isEmpty()) {
            // Not found, open the overflow menu first
            composeTestRule.onNodeWithTag(TEST_TAG_NAV_OVERFLOW).performClick()
        }

        // Now the item should be available to click
        composeTestRule.onNodeWithTag(tag).performClick()
    }

    fun clickMenuItemOverflowCompose(@IdRes menuItemId: Int) {
        clickMenuItemOverflowCompose(targetContext.resources.getResourceName(menuItemId))
    }

    fun clickMenuItemCompose(@IdRes menuItemId: Int) {
        clickMenuItemCompose(targetContext.resources.getResourceName(menuItemId))
    }

    fun clickMenuItemCompose(testTag: String) {
        composeTestRule.onNodeWithTag(testTag).performClick()
    }

    fun clickMenuItemOverflowCompose(testTag: String) {
        // 1. Check if the item is already visible (e.g. on tablet or as a quick action)
        val nodes = composeTestRule.onAllNodesWithTag(testTag).fetchSemanticsNodes()

        if (nodes.isEmpty()) {
            // 2. Not found, open the overflow menu
            composeTestRule.onNodeWithTag(TEST_TAG_OVERFLOW_MENU).performClick()
        }

        // 3. Click the item
        composeTestRule.onNodeWithTag(testTag).performClick()
    }

    fun selectFilter(column: Int, dialogActions: () -> Unit) {
        composeTestRule.onNodeWithTag(MenuItem.Search.testTag).performClick()
        composeTestRule.onNodeWithText(getString(column), useUnmergedTree = true)
            .performClick()
        dialogActions()
        composeTestRule.onNodeWithContentDescription(getString(R.string.apply)).performClick()
        waitForDialogClosed()
    }

    fun clearFilters() {
        composeTestRule.onNodeWithTag(MenuItem.Search.testTag).performClick()
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

    protected fun checkTitle(label: String, currency: CurrencyUnit = homeCurrency) {
        val balance = currencyFormatter.formatMoney(Money(currency, 0))
        composeTestRule.onNodeWithTag(TEST_TAG_ACCOUNT_LABEL, useUnmergedTree = true).assertTextEquals(label)
        composeTestRule.onNodeWithTag(TEST_TAG_BALANCE_AMOUNT, useUnmergedTree = true).assertTextEquals(balance)
    }

    fun clickFabCompose() {
        composeTestRule.onNodeWithTag(TEST_TAG_FAB).performClick()
    }

    fun navigateToAccounts() {
        composeTestRule.onNodeWithTag(TEST_TAG_NAV_ACCOUNTS).performClick()
    }

    fun navigateToTransactions() {
        composeTestRule.onNodeWithTag(TEST_TAG_NAV_TRANSACTIONS).performClick()
    }
}