package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.DistributionActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteCategory
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.viewmodel.DistributionViewModel
import java.time.ZonedDateTime

class DistributionTest : BaseUiTest<DistributionActivity>() {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var account: Account

    private var categoryExpenseId = 0L
    private var categoryExpenseId2 = 0L
    private var categoryIncomeId = 0L

    private fun baseFixture(
        showIncome: Boolean = false,
        showExpense: Boolean = true,
        grouping: Grouping = Grouping.NONE,
        additionalFixture: () -> Unit = {}
    ) {
        account = buildAccount("Test account 1")
        additionalFixture()
        testScenario =
            ActivityScenario.launch(Intent(targetContext, DistributionActivity::class.java).apply {
                putExtra(KEY_ACCOUNTID, account.id)
                putExtra(DistributionViewModel.SHOW_INCOME_KEY, showIncome)
                putExtra(DistributionViewModel.SHOW_EXPENSE_KEY, showExpense)
                putExtra(KEY_GROUPING, grouping)
            })
    }

    private fun fixtureWithExpenseAndIncome(
        showIncome: Boolean = false,
        showExpense: Boolean = true
    ) {
        baseFixture(showIncome, showExpense) {
            categoryExpenseId = writeCategory("Expense")
            categoryIncomeId = writeCategory("Income", type = FLAG_INCOME)
            with(Transaction.getNewInstance(account.id, homeCurrency)) {
                amount = Money(homeCurrency, -1200L)
                catId = categoryExpenseId
                save(contentResolver)
            }
            with(Transaction.getNewInstance(account.id, homeCurrency)) {
                amount = Money(homeCurrency, 3400L)
                catId = categoryIncomeId
                save(contentResolver)
            }
        }
    }

    private fun fixtureWithMultipleMonths(
        showIncome: Boolean = false,
        showExpense: Boolean = true
    ) {
        baseFixture(showIncome, showExpense, Grouping.MONTH) {
            categoryExpenseId = writeCategory("Expense 1")
            categoryExpenseId2 = writeCategory("Expense 2")
            with(Transaction.getNewInstance(account.id, homeCurrency)) {
                amount = Money(homeCurrency, -1200L)
                catId = categoryExpenseId
                save(contentResolver)
            }
            with(Transaction.getNewInstance(account.id, homeCurrency)) {
                amount = Money(homeCurrency, -3400L)
                catId = categoryExpenseId2
                save(contentResolver)
            }
            val date = ZonedDateTime.now().minusMonths(1)
            with(Transaction.getNewInstance(account.id, homeCurrency)) {
                amount = Money(homeCurrency, -3400L)
                catId = categoryExpenseId
                setDate(date)
                save(contentResolver)
            }
            with(Transaction.getNewInstance(account.id, homeCurrency)) {
                amount = Money(homeCurrency, -1200L)
                catId = categoryExpenseId2
                setDate(date)
                save(contentResolver)
            }
        }
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account.id)
            if (categoryExpenseId != 0L) {
                repository.deleteCategory(categoryExpenseId)
            }
            if (categoryIncomeId != 0L) {
                repository.deleteCategory(categoryIncomeId)
            }
        }
    }


    private fun assertIncome() {
        onView(allOf(withText(containsString("Income")), withText(containsString("34"))))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    private fun assertExpense() {
        onView(allOf(withText(containsString("Expense")), withText(containsString("12"))))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectCommandExpense() {
        launchWithContextCommand(
            R.string.menu_show_transactions,
            assertIncome = null,
            assertExpense = ::assertExpense
        )
    }

    @Test
    fun testSelectCommandIncome() {
        launchWithContextCommand(
            R.string.menu_show_transactions,
            assertIncome = ::assertIncome,
            assertExpense = null
        )
    }

    @Test
    fun testSelectBoth() {
        launchWithContextCommand(
            R.string.menu_show_transactions,
            assertIncome = ::assertIncome,
            assertExpense = ::assertExpense
        )
    }

    @Test
    fun testColorCommand() {
        launchWithContextCommand(R.string.color, null) {
            testScenario.onActivity {
                assertThat(it.supportFragmentManager.findFragmentByTag(ProtectedFragmentActivity.EDIT_COLOR_DIALOG)).isNotNull()
            }
        }
    }

    @Test
    fun shouldKeepSelectedCategory() {
        fixtureWithMultipleMonths()
        composeTestRule.onNodeWithText("Expense 2").assertIsSelected()
        clickMenuItem(R.id.BACK_COMMAND)
        composeTestRule.onNodeWithText("Expense 2").assertIsSelected()
        doWithRotation {
            composeTestRule.onNodeWithText("Expense 2").assertIsSelected()
        }
    }

    private fun launchWithContextCommand(
        @StringRes menuLabel: Int,
        assertIncome: (() -> Unit)?,
        assertExpense: (() -> Unit)?
    ) {
        fixtureWithExpenseAndIncome(assertIncome != null, assertExpense != null)
        if (assertIncome != null) {
            composeTestRule
                .onNodeWithText("Income").performTouchInput {
                    longClick()
                }
            onContextMenu(menuLabel)
            assertIncome()
            pressBack()
        }
        if (assertExpense != null) {
            composeTestRule
                .onNodeWithText("Expense").performTouchInput {
                    longClick()
                }
            onContextMenu(menuLabel)
            assertExpense()
        }
    }

    private fun onContextMenu(@StringRes menuItemId: Int) =
        composeTestRule.onNodeWithText(getString(menuItemId)).performClick()

}