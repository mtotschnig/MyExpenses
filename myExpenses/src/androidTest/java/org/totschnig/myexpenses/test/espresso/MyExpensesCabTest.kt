package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.*
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.TestMyExpenses
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.util.Utils

class MyExpensesCabTest : BaseMyExpensesTest() {
    private val origListSize = 6
    @Before
    fun fixture() {
        val home = Utils.getHomeCurrency()
        val account = Account(
            "Test account 1", home, 0, "",
            AccountType.CASH, Account.DEFAULT_COLOR
        )
        account.save()
        val op0 = Transaction.getNewInstance(account.id)
        op0.amount = Money(home, -100L)
        op0.save()
        for (i in 2 until 7) {
            op0.amount = Money(home, -100L * i)
            op0.date = op0.date - 10000
            op0.saveAsNew()
        }
        launch(account.id, TestMyExpenses::class.java)
    }

    @Test
    fun cloneCommandIncreasesListSize() {
        assertListSize(origListSize)
        clickContextItem(R.string.menu_clone_transaction)
        closeKeyboardAndSave()
        assertListSize(origListSize + 1)
    }

    @Test
    fun editCommandKeepsListSize() {
        assertListSize(origListSize)
        clickContextItem(R.string.menu_edit)
        closeKeyboardAndSave()
        assertListSize(origListSize)
    }

    @Test
    fun createTemplateCommandCreatesTemplate() {
        val templateTitle = "Espresso Template Test"
        assertListSize(origListSize)
        clickContextItem(R.string.menu_create_template_from_transaction)
        Espresso.onView(ViewMatchers.withText(Matchers.containsString(getString(R.string.menu_create_template))))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.Title)).perform(
            ViewActions.closeSoftKeyboard(),
            ViewActions.typeText(templateTitle),
            ViewActions.closeSoftKeyboard()
        )
        closeKeyboardAndSave()
        clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND)
        Espresso.onView(ViewMatchers.withText(Matchers.`is`(templateTitle)))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun deleteCommandDecreasesListSize() {
        doDelete(useCab = false, cancel = false)
    }

    @Test
    fun deleteCommandDecreasesListSizeCab() {
        doDelete(useCab = true, cancel = false)
    }

    @Test
    fun deleteCommandCancelKeepsListSize() {
        doDelete(useCab = false, cancel = true)
    }

    @Test
    fun deleteCommandCancelKeepsListSizeCab() {
        doDelete(useCab = true, cancel = true)
    }

    private fun doDelete(useCab: Boolean, cancel: Boolean) {
        assertListSize(origListSize)
        triggerDelete(useCab)
        Espresso.onView(ViewMatchers.withText(if (cancel) android.R.string.cancel else  R.string.menu_delete)).inRoot(RootMatchers.isDialog()).perform(ViewActions.click())
        assertListSize(if (cancel) origListSize else origListSize - 1)
    }

    @Test
    fun deleteCommandWithVoidOptionCab() {
        doDeleteCommandWithVoidOption(true)
    }

    @Test
    fun deleteCommandWithVoidOption() {
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
        Espresso.onView(ViewMatchers.withId(R.id.checkBox)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.menu_delete)).perform(ViewActions.click())
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
        openCab(R.id.SPLIT_TRANSACTION_COMMAND)
        handleContribDialog(ContribFeature.SPLIT_TRANSACTION)
        Espresso.onView(ViewMatchers.withText(R.string.menu_split_transaction))
            .perform(ViewActions.click())
        composeTestRule.onNodeWithTag(TEST_TAG_LIST).onChildren().onFirst().assertTextContains(getString(R.string.split_transaction))
    }

    @Test
    fun cabIsRestoredAfterOrientationChange() {
        openCab(null)
        rotate()
        Espresso.onView(ViewMatchers.withId(R.id.action_mode_bar))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}