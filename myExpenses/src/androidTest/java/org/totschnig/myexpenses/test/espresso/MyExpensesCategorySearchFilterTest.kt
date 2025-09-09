package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteCategory
import org.totschnig.myexpenses.model.CurrencyUnit.Companion.DebugInstance
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup

@TestShard3
class MyExpensesCategorySearchFilterTest : BaseMyExpensesTest() {

    private lateinit var account: Account
    private lateinit var catLabel1: String
    private lateinit var catLabel2: String
    private lateinit var catLabel1Sub: String
    private var categoryId1: Long = 0
    private var categoryId1Sub: Long = 0
    private var categoryId2: Long = 0
    private var id1Main: Long = 0
    private var id1Sub: Long = 0
    private var id2Main: Long = 0

    @Before
    fun fixture() {
        catLabel1 = "Main category 1"
        catLabel1Sub = "Sub category 1"
        catLabel2 = "Test category 2"
        val currency = DebugInstance
        account = buildAccount("Test account 1")
        categoryId1 = writeCategory(catLabel1)
        categoryId1Sub = writeCategory(catLabel1Sub, categoryId1)
        categoryId2 = writeCategory(catLabel2)
        val op = Transaction.getNewInstance(account.id, homeCurrency)
        op.amount = Money(currency, -1200L)
        op.catId = categoryId1
        id1Main = ContentUris.parseId(op.save(contentResolver)!!)
        op.catId = categoryId2
        op.date -= 10000
        id2Main = ContentUris.parseId(op.saveAsNew(contentResolver))
        op.catId = categoryId1Sub
        op.date -= 10000
        id1Sub = ContentUris.parseId(op.saveAsNew(contentResolver))
        launch(account.id)
        allLabelsAreDisplayed()
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account.id)
            repository.deleteCategory(categoryId1)
            repository.deleteCategory(categoryId1Sub)
            repository.deleteCategory(categoryId2)
        }
    }

    private fun allLabelsAreDisplayed() {
        catIsDisplayed(catLabel1, 0)
        catIsDisplayed(catLabel2, 1)
        catIsDisplayed(catLabel1Sub, 2, true)
    }

    private fun endSearch() {
        clearFilters()
        allLabelsAreDisplayed()
        testScenario.close()
    }

    private fun select(label: String) {
        composeTestRule.onNodeWithText(label).performClick()
        clickMenuItem(R.id.SELECT_COMMAND, true)
    }

    @Test
    fun catFilterChildShouldHideTransaction() {
        selectFilter(R.string.category) {
            composeTestRule.onNodeWithText(catLabel1)
                .performSemanticsAction(SemanticsActions.Expand)
            select(catLabel1Sub)
        }
        assertListSize(1)
        catIsDisplayed(catLabel1Sub, 0, true)
        endSearch()
    }

    @Test
    fun catFilterMainWithChildrenShouldHideTransaction() {
        selectFilter(R.string.category) {
            select(catLabel1)
        }
        assertListSize(2)
        catIsDisplayed(catLabel1, 0)
        catIsDisplayed(catLabel1Sub, 1, true)
        endSearch()
    }

    @Test
    fun catFilterMainWithoutChildrenShouldHideTransaction() {
        selectFilter(R.string.category) {
            select(catLabel2)
        }
        assertListSize(1)
        catIsDisplayed(catLabel2, 0)
        endSearch()
    }

    private fun catIsDisplayed(label: String, position: Int, subString: Boolean = false) {
        assertTextAtPosition(label, position, subString)
    }
}