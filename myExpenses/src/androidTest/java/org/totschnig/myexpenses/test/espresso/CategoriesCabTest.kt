package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import android.content.ContentUris.appendId
import android.content.ContentValues
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.containsString
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.ACTION_MANAGE
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageCategories
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.viewmodel.data.Budget
import java.time.LocalDate
import java.util.*

class CategoriesCabTest : BaseUiTest<ManageCategories>() {
    private lateinit var activityScenario: ActivityScenario<ManageCategories>
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val contentResolver
        get() = targetContext.contentResolver

    val currency = CurrencyUnit.DebugInstance
    private lateinit var account: Account
    private var categoryId: Long = 0
    private val origListSize = 1

    private fun baseFixture() {
        account = Account("Test account 1", currency, 0, "",
                AccountType.CASH, Account.DEFAULT_COLOR)
        account.save()
        categoryId = writeCategory(label = "TestCategory")
    }

    private fun launch() =
        ActivityScenario.launch<ManageCategories>(
            Intent(InstrumentationRegistry.getInstrumentation().targetContext, ManageCategories::class.java).also {
                it.action = ACTION_MANAGE
            }
        ).also {
            activityScenario = it
        }

    private fun fixtureWithMappedTransaction() {
        baseFixture()
        with(Transaction.getNewInstance(account.id)) {
            amount = Money(CurrencyUnit(Currency.getInstance("USD")), -1200L)
            catId = categoryId
            save()
        }
    }

    private fun fixtureWithMappedTemplate() {
        baseFixture()
        with(Template(account, Transactions.TYPE_TRANSACTION, null)) {
            amount = Money(CurrencyUnit(Currency.getInstance("USD")), -1200L)
            catId = categoryId
            save()
        }
    }

    private fun fixtureWithMappedBudget() {
        baseFixture()
        val budget = Budget(0L, account.id, "TITLE", "DESCRIPTION", currency, Grouping.MONTH, -1, null as LocalDate?, null as LocalDate?, account.label, true)
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
    fun shouldDeleteCategory() {
        baseFixture()
        launch().use {
            cabAndDelete()
            assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isEqualTo(origListSize - 1)
        }
    }

    @Test
    fun shouldNotDeleteCategoryMappedToTransaction() {
        fixtureWithMappedTransaction()
        launch().use {
            cabAndDelete()
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
            cabAndDelete()
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
            cabAndDelete()
            onView(withText(containsString(getString(R.string.warning_delete_category_with_budget)))).check(matches(isDisplayed()))
            onView(withText(R.string.response_no)).perform(click())
            assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isEqualTo(origListSize)
        }
    }

    private fun cabAndDelete()  {
        //Interestingly, we have to use the SemanticsActions, since performTouchInput { longClick() } does not work
        composeTestRule.onNodeWithText("TestCategory").performSemanticsAction(SemanticsActions.OnLongClick)
        clickMenuItem(R.id.DELETE_COMMAND, true)
    }

    @Test
    fun shouldCreateSubCategory() {
        baseFixture()
        launch().use {
            composeTestRule.onNodeWithText("TestCategory").performClick()
            onContextMenu(R.string.subcategory)
            composeTestRule.onNodeWithTag("editText")
                .performTextInput("Subcategory")
            composeTestRule.onNodeWithTag("positive").performClick()
            assertThat(repository.count(TransactionProvider.CATEGORIES_URI,
                "${DatabaseConstants.KEY_PARENTID} = ?", arrayOf(categoryId.toString()))).isEqualTo(1)
        }
    }

    private fun onContextMenu(@StringRes menuItemId: Int) =
        composeTestRule.onNodeWithText(getString(menuItemId)).performClick()

    override val testScenario: ActivityScenario<ManageCategories>
        get() = activityScenario
}