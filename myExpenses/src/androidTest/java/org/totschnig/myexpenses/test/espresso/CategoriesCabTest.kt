package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import android.content.ContentUris.appendId
import android.content.ContentValues
import android.content.Intent
import android.widget.Button
import androidx.annotation.StringRes
import androidx.compose.ui.test.*
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import com.adevinta.android.barista.internal.matcher.HelperMatchers
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.Action
import org.totschnig.myexpenses.activity.ManageCategories
import org.totschnig.myexpenses.compose.TEST_TAG_EDIT_TEXT
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.compose.TEST_TAG_POSITIVE_BUTTON
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseComposeTest
import org.totschnig.myexpenses.viewmodel.data.Budget
import java.time.LocalDate
import java.util.*

class CategoriesCabTest : BaseComposeTest<ManageCategories>() {

    private lateinit var account: org.totschnig.myexpenses.model2.Account
    private var categoryId: Long = 0
    private val origListSize = 2

    private fun baseFixture() {
        account = buildAccount("Test account 1")
        categoryId = writeCategory(label = "TestCategory")
        writeCategory(label = "Control Category")
    }

    private fun launch() =
        ActivityScenario.launch<ManageCategories>(
            Intent(targetContext, ManageCategories::class.java).also {
                it.action = Action.MANAGE.name
            }
        ).also {
            testScenario = it
        }

    private fun fixtureWithMappedTransaction() {
        baseFixture()
        with(Transaction.getNewInstance(account.id, homeCurrency)) {
            amount = Money(homeCurrency, -1200L)
            catId = categoryId
            save(contentResolver)
        }
    }

    private fun fixtureWithMappedTemplate() {
        baseFixture()
        with(Template(contentResolver, account.id, homeCurrency, Transactions.TYPE_TRANSACTION, null)) {
            amount = Money(CurrencyUnit(Currency.getInstance("USD")), -1200L)
            catId = categoryId
            save(contentResolver)
        }
    }

    private fun fixtureWithMappedBudget() {
        baseFixture()
        val budget = Budget(0L, account.id, "TITLE", "DESCRIPTION", homeCurrency, Grouping.MONTH, -1, null as LocalDate?, null as LocalDate?, account.label, true)
        val budgetId = ContentUris.parseId(contentResolver.insert(TransactionProvider.BUDGETS_URI, budget.toContentValues(200000L))!!)
        setCategoryBudget(budgetId, categoryId, 50000)
    }

    private fun setCategoryBudget(budgetId: Long, categoryId: Long, @Suppress("SameParameterValue") amount: Long) {
        with(ContentValues(1)) {
            put(DatabaseConstants.KEY_BUDGET, amount)
            put(DatabaseConstants.KEY_YEAR, 2022)
            put(DatabaseConstants.KEY_SECOND_GROUP, 7)
            contentResolver.update(appendId(appendId(TransactionProvider.BUDGETS_URI.buildUpon(), budgetId), categoryId).build(),
                    this, null, null)
        }
    }

    @Test
    fun shouldDeleteCategoryAfterDataReload() {
        baseFixture()
        launch().use {

            assertTextAtPosition("TestCategory", 0)
            clickMenuItem(R.id.SORT_COMMAND)
            onData(HelperMatchers.menuIdMatcher(R.id.SORT_LABEL_COMMAND))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click())
            assertTextAtPosition("Control Category", 0)
            callDelete(position = 1)
            assertTextAtPosition("Control Category", 0)
            listNode.assert(hasRowCount(1))
        }
    }

    @Test
    fun shouldNotDeleteCategoryMappedToTransaction() {
        fixtureWithMappedTransaction()
        launch().use {
            callDelete()
            onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(getQuantityString(
                    R.plurals.not_deletable_mapped_transactions, 1, 1))))
            assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isEqualTo(origListSize)
        }
    }

    @Test
    fun shouldNotDeleteCategoryMappedToTemplate() {
        fixtureWithMappedTemplate()
        launch().use {
            callDelete()
            onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(getQuantityString(
                    R.plurals.not_deletable_mapped_templates, 1, 1))))
            assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isEqualTo(origListSize)
        }
    }

    @Test
    fun shouldNotDeleteCategoryMappedToBudget() {
        fixtureWithMappedBudget()
        launch().use {
            callDelete(false)
            onView(withText(containsString(getString(R.string.warning_delete_category_with_budget)))).check(matches(isDisplayed()))
            onView(withText(R.string.response_no)).perform(click())
            assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isEqualTo(origListSize)
        }
    }

    private fun callDelete(withConfirmation: Boolean = true, position: Int = 0)  {
        clickContextItem(R.string.menu_delete, position = position)
        if (withConfirmation) {
            onView(
                Matchers.allOf(
                    isAssignableFrom(Button::class.java),
                    withText(Matchers.`is`(getString(R.string.response_yes)))
                )
            ).perform(click())
        }
    }

    @Test
    fun shouldCreateSubCategory() {
        baseFixture()
        launch().use {
            composeTestRule.onNodeWithText("TestCategory").performClick()
            onContextMenu(R.string.subcategory)
            composeTestRule.onNodeWithTag(TEST_TAG_EDIT_TEXT)
                .performTextInput("Subcategory")
            composeTestRule.onNodeWithTag(TEST_TAG_POSITIVE_BUTTON).performClick()
            assertThat(repository.count(TransactionProvider.CATEGORIES_URI,
                "${DatabaseConstants.KEY_PARENTID} = ?", arrayOf(categoryId.toString()))).isEqualTo(1)
        }
    }

    private fun onContextMenu(@StringRes menuItemId: Int) =
        composeTestRule.onNodeWithText(getString(menuItemId)).performClick()

    override val listNode: SemanticsNodeInteraction
        get() = composeTestRule.onNodeWithTag(TEST_TAG_LIST)

}