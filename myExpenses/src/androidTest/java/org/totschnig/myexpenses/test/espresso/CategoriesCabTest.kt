package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.widget.Button
import androidx.annotation.StringRes
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.internal.matcher.HelperMatchers
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.Action
import org.totschnig.myexpenses.activity.ManageCategories
import org.totschnig.myexpenses.compose.TEST_TAG_EDIT_TEXT
import org.totschnig.myexpenses.compose.TEST_TAG_POSITIVE_BUTTON
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteBudget
import org.totschnig.myexpenses.db2.deleteCategory
import org.totschnig.myexpenses.db2.deleteTemplate
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseComposeTest
import org.totschnig.myexpenses.testutils.TestShard1
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.viewmodel.data.Budget
import java.time.LocalDate
import java.util.Currency

@TestShard1
class CategoriesCabTest : BaseComposeTest<ManageCategories>() {

    private lateinit var account: org.totschnig.myexpenses.model2.Account
    private var categoryId: Long = 0
    private var origListSize = 0
    private var controlCategory: Long = 0

    private fun baseFixture() {
        account = buildAccount("Test account 1")
        categoryId = writeCategory(label = "TestCategory")
        controlCategory = writeCategory(label = "Control Category")
        origListSize = repository.count(TransactionProvider.CATEGORIES_URI)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account.id)
            repository.deleteCategory(categoryId)
            repository.deleteCategory(controlCategory)
        }
    }

    private fun launch() =
        ActivityScenario.launch<ManageCategories>(
            Intent(targetContext, ManageCategories::class.java).also {
                it.action = Action.MANAGE.name
                it.putExtra(KEY_TYPE, FLAG_NEUTRAL)
            }
        ).also {
            testScenario = it
        }

    private fun fixtureWithMappedTransaction(): Long {
        baseFixture()
        return with(Transaction.getNewInstance(account.id, homeCurrency)) {
            amount = Money(homeCurrency, -1200L)
            catId = categoryId
            ContentUris.parseId(save(contentResolver)!!)
        }
    }

    private fun fixtureWithMappedTemplate(): Long {
        baseFixture()
        return with(
            Template(
                contentResolver,
                account.id,
                homeCurrency,
                Transactions.TYPE_TRANSACTION,
                null
            )
        ) {
            amount = Money(CurrencyUnit(Currency.getInstance("USD")), -1200L)
            catId = categoryId
            ContentUris.parseId(save(contentResolver)!!)
        }
    }

    private fun fixtureWithMappedBudget(): Long {
        baseFixture()
        val budget = Budget(
            0L,
            account.id,
            "TITLE",
            "DESCRIPTION",
            homeCurrency.code,
            Grouping.MONTH,
            -1,
            null as LocalDate?,
            null as LocalDate?,
            account.label,
            true
        )
        val budgetId = ContentUris.parseId(
            contentResolver.insert(
                TransactionProvider.BUDGETS_URI,
                budget.toContentValues(200000L)
            )!!
        )
        setCategoryBudget(budgetId, categoryId, 50000)
        return budgetId
    }

    private fun setCategoryBudget(
        budgetId: Long,
        categoryId: Long,
        @Suppress("SameParameterValue") amount: Long
    ) {
        with(ContentValues(1)) {
            put(DatabaseConstants.KEY_BUDGET, amount)
            put(DatabaseConstants.KEY_YEAR, 2022)
            put(DatabaseConstants.KEY_SECOND_GROUP, 7)
            contentResolver.update(
                BaseTransactionProvider.budgetAllocationUri(budgetId, categoryId),
                this, null, null
            )
        }
    }

    @Test
    fun shouldDeleteCategoryAfterDataReload() {
        baseFixture()
        launch().use {

            assertTextAtPosition("TestCategory", 0, anyDescendant = true)
            clickMenuItem(R.id.SORT_COMMAND)
            onData(HelperMatchers.menuIdMatcher(R.id.SORT_LABEL_COMMAND))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click())
            assertTextAtPosition("Control Category", 0, anyDescendant = true)
            callDelete(position = 1)
            assertTextAtPosition("Control Category", 0, anyDescendant = true)
            listNode.assert(hasRowCount(1))
        }
        cleanup {
            prefHandler.remove(PrefKey.SORT_ORDER_CATEGORIES)
        }
    }

    @Test
    fun shouldNotDeleteCategoryMappedToTransaction() {
        val transactionId = fixtureWithMappedTransaction()
        launch().use {
            assertTextAtPosition("TestCategory", 0, anyDescendant = true)
            callDelete()
            onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(
                    matches(
                        withText(
                            getQuantityString(
                                R.plurals.not_deletable_mapped_transactions, 1, 1
                            )
                        )
                    )
                )
            assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isEqualTo(origListSize)
        }
        cleanup {
            repository.deleteTransaction(transactionId)
        }
    }

    @Test
    fun shouldNotDeleteCategoryMappedToTemplate() {
        val templateId = fixtureWithMappedTemplate()
        launch().use {
            callDelete()
            onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(
                    matches(
                        withText(
                            getQuantityString(
                                R.plurals.not_deletable_mapped_templates, 1, 1
                            )
                        )
                    )
                )
            assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isEqualTo(origListSize)
        }
        cleanup {
            repository.deleteTemplate(templateId)
        }
    }

    @Test
    fun shouldNotDeleteCategoryMappedToBudget() {
        val budgetId = fixtureWithMappedBudget()
        launch().use {
            callDelete(false)
            onView(withText(containsString(getString(R.string.warning_delete_category_with_budget)))).check(
                matches(isDisplayed())
            )
            onView(withText(R.string.response_no)).perform(click())
            assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isEqualTo(origListSize)
        }
        cleanup {
            repository.deleteBudget(budgetId)
        }
    }

    private fun callDelete(withConfirmation: Boolean = true, position: Int = 0) {
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
            assertThat(
                repository.count(
                    TransactionProvider.CATEGORIES_URI,
                    "${DatabaseConstants.KEY_PARENTID} = ?", arrayOf(categoryId.toString())
                )
            ).isEqualTo(1)
        }
    }

    @Test
    fun shouldMergeCategories() {
        baseFixture()
        launch().use {
            listNode.onChildren().onFirst()
                .performTouchInput { longClick() }
            listNode.onChildren()[1].performClick()
            clickMenuItem(R.id.MERGE_COMMAND, true)
            composeTestRule.onNodeWithText(getString(R.string.menu_merge)).performClick()
            assertTextAtPosition("TestCategory", 0, anyDescendant = true)
            listNode.assert(hasRowCount(1))
        }
    }

    private fun onContextMenu(@StringRes menuItemId: Int) =
        composeTestRule.onNodeWithText(getString(menuItemId)).performClick()

}