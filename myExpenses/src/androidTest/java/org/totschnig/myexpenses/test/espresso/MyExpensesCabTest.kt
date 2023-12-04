package org.totschnig.myexpenses.test.espresso

import android.net.Uri
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.TEST_TAG_CONTEXT_MENU
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.db2.addAttachments
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest

class MyExpensesCabTest : BaseMyExpensesTest() {
    private val origListSize = 6
    private lateinit var account: Account

    private fun launch(excludeFromTotals: Boolean = false) {
        account = buildAccount("Test account 1", excludeFromTotals = excludeFromTotals)
        val op0 = Transaction.getNewInstance(account.id, homeCurrency)
        op0.amount = Money(homeCurrency, -100L)
        op0.save(contentResolver)
        for (i in 2 until 7) {
            repository.addAttachments(op0.id, listOf(Uri.parse("file:///android_asset/screenshot.jpg")))
            op0.amount = Money(homeCurrency, -100L * i)
            op0.date = op0.date - 10000
            op0.saveAsNew(contentResolver)
        }
        launch(account.id)
    }

    @Test
    fun cloneCommandIncreasesListSize() {
        launch()
        assertListSize(origListSize)
        clickContextItem(R.string.menu_clone_transaction)
        closeKeyboardAndSave()
        assertListSize(origListSize + 1)
    }

    @Test
    fun editCommandKeepsListSize() {
        launch()
        assertListSize(origListSize)
        clickContextItem(R.string.menu_edit)
        closeKeyboardAndSave()
        assertListSize(origListSize)
    }

    @Test
    fun createTemplateCommandCreatesTemplate() {
        launch()
        val templateTitle = "Espresso Template Test"
        assertListSize(origListSize)
        clickContextItem(R.string.menu_create_template_from_transaction)
        onView(withId(R.id.Title)).perform(
            closeSoftKeyboard(),
            typeText(templateTitle),
            closeSoftKeyboard()
        )
        closeKeyboardAndSave()
        clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND)
        onView(withText(Matchers.`is`(templateTitle)))
            .check(matches(isDisplayed()))
    }

    @Test
    fun deleteCommandDecreasesListSize() {
        launch()
        doDelete(useCab = false, cancel = false)
    }

    @Test
    fun deleteCommandDecreasesListSizeCab() {
        launch()
        doDelete(useCab = true, cancel = false)
    }

    @Test
    fun deleteCommandCancelKeepsListSize() {
        launch()
        doDelete(useCab = false, cancel = true)
    }

    @Test
    fun deleteCommandCancelKeepsListSizeCab() {
        launch()
        doDelete(useCab = true, cancel = true)
    }

    private fun doDelete(useCab: Boolean, cancel: Boolean) {
        assertListSize(origListSize)
        triggerDelete(useCab)
        onView(withText(if (cancel) android.R.string.cancel else  R.string.menu_delete))
            .inRoot(isDialog())
            .perform(click())
        assertListSize(if (cancel) origListSize else origListSize - 1)
    }

    @Test
    fun deleteCommandWithVoidOptionCab() {
        launch()
        doDeleteCommandWithVoidOption(true)
    }

    @Test
    fun deleteCommandWithVoidOption() {
        launch()
        doDeleteCommandWithVoidOption(false)
    }

    private fun triggerDelete(useCab: Boolean) {
        if (useCab) {
            openCab(R.id.DELETE_COMMAND)
        } else {
            clickContextItem(R.string.menu_delete)
        }
    }

    private fun doDeleteCommandWithVoidOption(useCab: Boolean) {
        assertListSize(origListSize)
        triggerDelete(useCab)
        onView(withId(R.id.checkBox)).perform(click())
        onView(withText(R.string.menu_delete)).perform(click())
        val voidStatus = getString(R.string.status_void)
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).onChildren().onFirst()
            .assertContentDescriptionEquals(voidStatus)
        assertListSize(origListSize)
        clickContextItem(R.string.menu_undelete_transaction)
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).onChildren().onFirst()
            .assert(hasContentDescription(voidStatus).not())
        assertListSize(origListSize)
    }

    @Test
    fun splitCommandCreatesSplitTransaction() {
        launch()
        doSplitCommandTest()
    }

    @Test
    fun withAccountExcludedFromTotalsSplitCommandCreatesSplitTransaction() {
        launch(true)
        doSplitCommandTest()
    }

    private fun doSplitCommandTest() {
        openCab(R.id.SPLIT_TRANSACTION_COMMAND)
        handleContribDialog(ContribFeature.SPLIT_TRANSACTION)
        onView(withText(R.string.menu_split_transaction))
            .perform(click())
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).onChildren().onFirst().assertTextContains(getString(R.string.split_transaction))
    }

    @Test
    fun cabIsRestoredAfterOrientationChange() {
        launch()
        openCab(null)
        rotate()
        onView(withId(androidx.appcompat.R.id.action_mode_bar)).check(matches(isDisplayed()))
    }

    @Test
    fun contextForSealedAccount() {
        launch()
        testScenario.onActivity {
            it.viewModel.setSealed(account.id, true)
        }
        openCab(null)
        onView(withId(androidx.appcompat.R.id.action_mode_bar)).check(doesNotExist())
        //context menu should only have the details entry
        composeTestRule.onNodeWithTag(TEST_TAG_CONTEXT_MENU).assert(hasChildCount(1))
    }
}