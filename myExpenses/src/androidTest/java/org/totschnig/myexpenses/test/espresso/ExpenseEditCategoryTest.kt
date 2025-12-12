package org.totschnig.myexpenses.test.espresso

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteAllCategories
import org.totschnig.myexpenses.db2.insertTemplate
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.CATEGORY_ICON
import org.totschnig.myexpenses.testutils.CATEGORY_LABEL
import org.totschnig.myexpenses.testutils.TEMPLATE_TITLE
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.withCategoryIcon
import org.totschnig.shared_test.TransactionData


class ExpenseEditCategoryTest : BaseExpenseEditTest() {

    var categoryId: Long = 0

    @Before
    fun baseFixture() {
        account1 = buildAccount(ACCOUNT_LABEL_1)
        categoryId = writeCategory(label = CATEGORY_LABEL, icon = CATEGORY_ICON)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAllCategories()
        }
    }

    @Test
    fun shouldSaveCategory() = runTest {
        launch()
        setAmount(101)
        onView(withId(R.id.Category)).perform(click())
        composeTestRule.onNodeWithText(CATEGORY_LABEL).performClick()
        verifyCategory()
        clickFab() // save transaction
        assertTransaction(
            id = repository.loadTransactions(account1.id).first().id,
            TransactionData(
                accountId = account1.id,
                amount = -10100,
                category = categoryId
            )
        )
    }

    private fun verifyCategory() {
        onView(withId(R.id.Category)).check(
            matches(
                allOf(
                    withText(CATEGORY_LABEL),
                    withCategoryIcon(CATEGORY_ICON)
                )
            )
        )
    }

    @Test
    fun shouldLoadCategory() = runTest {
        val transaction = repository.insertTransaction(
            accountId = account1.id,
            amount = 100,
            categoryId = categoryId,
        )
        launch(getIntentForEditTransaction(transaction.id))
        verifyCategory()
    }

    @Test
    fun shouldLoadCategoryForTemplate() = runTest {
        val template = repository.insertTemplate(
            title = TEMPLATE_TITLE,
            accountId = account1.id,
            amount = 100,
            categoryId = categoryId,
        )
        launch(getIntentForEditTemplate(template.id))
        verifyCategory()
    }

    @Test
    fun shouldLoadCategoryForTransactionFromTemplate() = runTest {
        val template = repository.insertTemplate(
            title = TEMPLATE_TITLE,
            accountId = account1.id,
            amount = 100,
            categoryId = categoryId,
        )
        launch(getIntentForTransactionFromTemplate(template.id))
        verifyCategory()
    }

    @Test
    fun shouldLoadCategoryForTemplateFromTransaction() = runTest {
        val transaction = repository.insertTransaction(
            accountId = account1.id,
            amount = 100,
            categoryId = categoryId,
        )
        launch(getIntentForTemplateFromTransaction(transaction.id))
        checkToolbarTitleForTemplate()
        verifyCategory()
    }
}