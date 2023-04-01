package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit.Companion.DebugInstance
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest

class MyExpensesCategorySearchFilterTest : BaseMyExpensesTest() {

    private lateinit var catLabel1: String
    private lateinit var catLabel2: String
    private lateinit var catLabel1Sub: String
    private var id1Main: Long = 0
    private var id1Sub: Long = 0
    private var id2Main: Long = 0

    @Before
    fun fixture() {
        catLabel1 = "Main category 1"
        catLabel1Sub = "Sub category 1"
        catLabel2 = "Test category 2"
        val currency = DebugInstance
        val account =  buildAccount("Test account 1")
        val categoryId1 = writeCategory(catLabel1)
        val categoryId1Sub = writeCategory(catLabel1Sub, categoryId1)
        val categoryId2 = writeCategory(catLabel2)
        val op = Transaction.getNewInstance(account.id, homeCurrency)
        op.amount = Money(currency, -1200L)
        op.catId = categoryId1
        id1Main = ContentUris.parseId(op.save()!!)
        op.catId = categoryId2
        op.date = op.date - 10000
        id2Main = ContentUris.parseId(op.saveAsNew())
        op.catId = categoryId1Sub
        op.date = op.date - 10000
        id1Sub = ContentUris.parseId(op.saveAsNew())
        launch(account.id)
        allLabelsAreDisplayed()
        Espresso.onView(ViewMatchers.withId(R.id.SEARCH_COMMAND)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(R.string.category)).perform(ViewActions.click())
    }

    private fun allLabelsAreDisplayed() {
        catIsDisplayed(catLabel1, 0)
        catIsDisplayed(catLabel2, 1)
        catIsDisplayed(catLabel1Sub, 2, true)
    }

    private fun endSearch(text: String?) {
        //switch off filter
        Espresso.onView(ViewMatchers.withId(R.id.SEARCH_COMMAND)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(text)).inRoot(RootMatchers.isPlatformPopup())
            .perform(ViewActions.click())
        allLabelsAreDisplayed()
        testScenario.close()
    }

    private fun select(label: String) {
        composeTestRule.onNodeWithText(label).performClick()
        clickMenuItem(R.id.SELECT_COMMAND, true)
    }

    @Test
    fun catFilterChildShouldHideTransaction() {
        composeTestRule.onNodeWithText(catLabel1).performSemanticsAction(SemanticsActions.Expand)
        select(catLabel1Sub)
        assertListSize(1)
        catIsDisplayed(catLabel1Sub, 0, true)
        endSearch(catLabel1Sub)
    }

    @Test
    fun catFilterMainWithChildrenShouldHideTransaction() {
        select(catLabel1)
        assertListSize(2)
        catIsDisplayed(catLabel1, 0)
        catIsDisplayed(catLabel1Sub, 1, true)
        endSearch(catLabel1)
    }

    @Test
    fun catFilterMainWithoutChildrenShouldHideTransaction() {
        select(catLabel2)
        assertListSize(1)
        catIsDisplayed(catLabel2, 0)
        endSearch(catLabel2)
    }

    private fun catIsDisplayed(label: String, position: Int, subString: Boolean = false) {
        assertTextAtPosition(label, position, subString)
    }
}