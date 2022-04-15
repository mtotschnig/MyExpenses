package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.CursorMatchers
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit.Companion.DebugInstance
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseUiTest
import java.util.concurrent.TimeoutException

class MyExpensesCategorySearchFilterTest : BaseUiTest<MyExpenses>() {
    private lateinit var activityScenario: ActivityScenario<MyExpenses>
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var catLabel1: String
    private lateinit var catLabel2: String
    private lateinit var catLabel1Sub: String
    private var id1Main: Long = 0
    private var id1Sub: Long = 0
    private var id2Main: Long = 0
    private lateinit var account: Account

    @Before
    fun fixture() {
        catLabel1 = "Main category 1"
        catLabel1Sub = "Sub category 1"
        catLabel2 = "Test category 2"
        val currency = DebugInstance
        account = Account(
            "Test account 1", currency, 0, "",
            AccountType.CASH, Account.DEFAULT_COLOR
        )
        account.save()
        val categoryId1 = writeCategory(catLabel1)
        val categoryId1Sub = writeCategory(catLabel1Sub, categoryId1)
        val categoryId2 = writeCategory(catLabel2)
        val op = Transaction.getNewInstance(account.id)
        op.amount = Money(currency, -1200L)
        op.catId = categoryId1
        id1Main = ContentUris.parseId(op.save()!!)
        op.catId = categoryId2
        id2Main = ContentUris.parseId(op.saveAsNew())
        op.catId = categoryId1Sub
        id1Sub = ContentUris.parseId(op.saveAsNew())
    }

    @Before
    @Throws(TimeoutException::class)
    fun startSearch() {
        ActivityScenario.launch<MyExpenses>(Intent(targetContext, MyExpenses::class.java)).also {
            activityScenario = it
        }
        waitForAdapter()
        allLabelsAreDisplayed()
        Espresso.onView(ViewMatchers.withId(R.id.SEARCH_COMMAND)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.category)).perform(ViewActions.click())
    }

    private fun allLabelsAreDisplayed() {
        isDisplayed(id1Main)
        isDisplayed(id1Sub)
        isDisplayed(id2Main)
    }

    private fun endSearch(text: String?) {
        //switch off filter
        Espresso.onView(ViewMatchers.withId(R.id.SEARCH_COMMAND)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(text)).inRoot(RootMatchers.isPlatformPopup())
            .perform(ViewActions.click())
        allLabelsAreDisplayed()
        activityScenario.close()
    }

    private fun select(label: String) {
        composeTestRule.onNodeWithText(label).performClick()
        clickMenuItem(R.id.SELECT_COMMAND, true)
    }

    @Test
    fun catFilterChildShouldHideTransaction() {
        composeTestRule.onNodeWithText(catLabel1).performSemanticsAction(SemanticsActions.Expand)
        select(catLabel1Sub)
        isDisplayed(id1Sub)
        isNotDisplayed(id1Main)
        isNotDisplayed(id2Main)
        endSearch(catLabel1Sub)
    }

    @Test
    fun catFilterMainWithChildrenShouldHideTransaction() {
        select(catLabel1)
        isDisplayed(id1Main)
        isDisplayed(id1Sub)
        isNotDisplayed(id2Main)
        endSearch(catLabel1)
    }

    @Test
    fun catFilterMainWithoutChildrenShouldHideTransaction() {
        select(catLabel2)
        isDisplayed(id2Main)
        isNotDisplayed(id1Main)
        isNotDisplayed(id1Sub)
        endSearch(catLabel2)
    }

    private fun isDisplayed(id: Long) {
        Espresso.onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, id))
            .inAdapterView(wrappedList).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun isNotDisplayed(id: Long) {
        Espresso.onView(wrappedList)
            .check(
                ViewAssertions.matches(
                    Matchers.not(
                        org.totschnig.myexpenses.testutils.Matchers.withAdaptedData(
                            CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, id)
                        )
                    )
                )
            )
    }

    override val testScenario: ActivityScenario<MyExpenses>
        get() = activityScenario
}