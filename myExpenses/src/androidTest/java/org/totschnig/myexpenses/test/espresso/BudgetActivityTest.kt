package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.Test
import org.totschnig.myexpenses.activity.BudgetActivity
import org.totschnig.myexpenses.compose.*
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.test.R
import org.totschnig.myexpenses.testutils.BaseComposeTest
import org.totschnig.myexpenses.viewmodel.BudgetViewModel
import timber.log.Timber
import java.time.LocalDate

class BudgetActivityTest : BaseComposeTest<BudgetActivity>() {

    override val listNode: SemanticsNodeInteraction
        get() = composeTestRule.onNodeWithTag(TEST_TAG_LIST)

    fun setup(withFilter: Boolean) {
        val account = buildAccount("Account")
        //we write first a category to force category's id to be different from account id
        //in order to verify fix for https://github.com/mtotschnig/MyExpenses/issues/1189
        writeCategory("ignore")
        val mainCat1 = writeCategory("A", null)
        val mainCat2 = writeCategory("B", null)
        val op0 = Transaction.getNewInstance(account.id, homeCurrency)
        op0.amount = Money(homeCurrency, -12300L)
        op0.catId = mainCat1
        op0.save(contentResolver)

        val budgetId = createBudget(account)

        testContext.getString(R.string.testData_transaction1MainCat)
        setCategoryBudget(budgetId, mainCat1, 23000)
        if (withFilter) {
            saveBudgetFilter(budgetId, mainCat1)
        } else {
            setCategoryBudget(budgetId, mainCat2, 12000)
        }

        testScenario = ActivityScenario.launch(
            Intent(
                targetContext,
                BudgetActivity::class.java
            ).apply {
                putExtra(DatabaseConstants.KEY_ROWID, budgetId)
            })
    }

    private fun createBudget(account: Account) = ContentUris.parseId(
        repository.contentResolver.insert(
            TransactionProvider.BUDGETS_URI,
            org.totschnig.myexpenses.viewmodel.data.Budget(
                0L,
                account.id,
                "TestBudget",
                "DESCRIPTION",
                homeCurrency,
                Grouping.MONTH,
                -1,
                null as LocalDate?,
                null as LocalDate?,
                account.label,
                true
            ).toContentValues(200000L)
        )!!
    )

    private fun saveBudgetFilter(budgetId: Long, categoryId: Long) {
        val filterPersistence = FilterPersistence(prefHandler, BudgetViewModel.prefNameForCriteria(budgetId), null,
            immediatePersist = false, restoreFromPreferences = false)
        filterPersistence.addCriteria(CategoryCriterion("A", categoryId))
        filterPersistence.persistAll()
    }

    private fun setCategoryBudget(budgetId: Long, categoryId: Long, amount: Long) {
        val contentValues = ContentValues(1)
        contentValues.put(DatabaseConstants.KEY_BUDGET, amount)
        val now = LocalDate.now()
        contentValues.put(DatabaseConstants.KEY_YEAR, now.year)
        contentValues.put(DatabaseConstants.KEY_SECOND_GROUP, now.monthValue - 1)
        val budgetUri = ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, budgetId)
        val result = repository.contentResolver.update(
            ContentUris.withAppendedId(budgetUri, categoryId),
            contentValues, null, null
        )
        Timber.d("Insert category budget: %d", result)
    }

    @Test
    fun testBudgetLoadWithFilter() {
        setup(true)
        doTheTest(23000)
    }

    @Test
    fun testBudgetLoadWithoutFilter() {
        setup(false)
        doTheTest(35000)
    }

    private fun doTheTest(allocation: Long) {
        val onChildren = listNode.onChildren()
        testBudgetNumbers(
            onChildren.filterToOne(hasTestTag(TEST_TAG_HEADER)),
            200000,
            allocation,
            -12300,
            "Account" to "123"
        )
        testBudgetNumbers(
            onChildren.filter(hasTestTag(TEST_TAG_ROW)).onFirst(),
            23000,
            null,
            -12300,
            "A" to "123"
        )
    }

    private fun testBudgetNumbers(
        node: SemanticsNodeInteraction,
        budget: Long,
        allocation: Long?,
        spent: Long,
        dialogTitle: Pair<String, String>
    ) {
        //Hack: we scroll by large amount to make sure all numbers are visible
        composeTestRule.onNodeWithTag(TEST_TAG_BUDGET_ROOT).fetchSemanticsNode().config[SemanticsActions.ScrollBy].action?.invoke(1000f, 0f)
        node.onChildren().filterToOne(hasTestTag(TEST_TAG_BUDGET_BUDGET))
            .assert(hasAmount(budget))
        allocation?.let {
            node.onChildren().filterToOne(hasTestTag(TEST_TAG_BUDGET_ALLOCATION))
                .assert(hasAmount(it))
        }
        node.onChildren().filterToOne(hasTestTag(TEST_TAG_BUDGET_SPENT)).also {
            it.assert(hasAmount(spent))
            it.performClick()
        }
        onView(
            allOf(
                withText(containsString(dialogTitle.first)),
                withText(containsString(dialogTitle.second))
            )
        )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText(android.R.string.ok)).perform(click())
    }
}