package org.totschnig.myexpenses.test.espresso

import android.database.Cursor
import android.os.Debug
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.CursorMatchers
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.Utils

class MyExpensesCabTest : BaseMyExpensesCabTest() {
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
        op0.amount = Money(home, -1200L)
        op0.save()
        val times = 5
        for (i in 0 until times) {
            op0.saveAsNew()
        }
        launch(account.id)
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
        assertListSize(origListSize)
        openCab()
        clickMenuItem(R.id.DELETE_COMMAND, true)
        Espresso.onView(ViewMatchers.withText(R.string.menu_delete)).perform(ViewActions.click())
        assertListSize(origListSize - 1)
    }

    @Test
    fun deleteCommandWithVoidOption() {
        assertListSize(origListSize)
        openCab()
        clickMenuItem(R.id.DELETE_COMMAND, true)
        Espresso.onView(ViewMatchers.withId(R.id.checkBox)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.menu_delete)).perform(ViewActions.click())
        Espresso.onData(
            Matchers.`is`(
                Matchers.instanceOf(
                    Cursor::class.java
                )
            )
        ).inAdapterView(wrappedList).atPosition(1)
            .check(
                ViewAssertions.matches(
                    ViewMatchers.hasDescendant(
                        Matchers.both(
                            ViewMatchers.withId(
                                R.id.voidMarker
                            )
                        ).and(ViewMatchers.isDisplayed())
                    )
                )
            )
        assertListSize(origListSize)
        openCab()
        clickMenuItem(R.id.UNDELETE_COMMAND, true)
        Espresso.onView(wrappedList)
            .check(
                ViewAssertions.matches(
                    CoreMatchers.not(
                        org.totschnig.myexpenses.testutils.Matchers.withAdaptedData(
                            CursorMatchers.withRowString(DatabaseConstants.KEY_CR_STATUS, "VOID")
                        )
                    )
                )
            )
        assertListSize(origListSize)
    }

    @Test
    fun deleteCommandCancelKeepsListSize() {
        assertListSize(origListSize)
        openCab()
        clickMenuItem(R.id.DELETE_COMMAND, true)
        Espresso.onView(ViewMatchers.withText(android.R.string.cancel)).perform(ViewActions.click())
        assertListSize(origListSize)
    }

    @Test
    fun splitCommandCreatesSplitTransaction() {
        openCab()
        clickMenuItem(R.id.SPLIT_TRANSACTION_COMMAND, true)
        handleContribDialog(ContribFeature.SPLIT_TRANSACTION)
        Espresso.onView(ViewMatchers.withText(R.string.menu_split_transaction))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.split_transaction))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun cabIsRestoredAfterOrientationChange() {
        openCab()
        rotate()
        Espresso.onView(ViewMatchers.withId(R.id.action_mode_bar))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}