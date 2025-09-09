package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.annotation.StringRes
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
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.DistributionActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.deleteCategory
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.TestShard3
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.isOrchestrated
import org.totschnig.myexpenses.viewmodel.DistributionViewModel
import java.util.Currency

@TestShard3
class GrandTotalDistributionTest : BaseUiTest<DistributionActivity>() {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    val currency1 = CurrencyUnit(Currency.getInstance("USD"))
    val currency2 = CurrencyUnit(Currency.getInstance("EUR"))
    private lateinit var account1: Account
    private lateinit var account2: Account
    var categoryExpenseId: Long = 0
    var categoryIncomeId: Long = 0

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            // For unidentified reason, tests in this class fail when run with the whole package
            // "am instrument -e package org.totschnig.myexpenses.test.espresso"
            // but work when run on class level
            // "am instrument -e class org.totschnig.myexpenses.test.espresso.GrandTotalDistributionTest"
            Assume.assumeTrue(isOrchestrated)
        }
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
            repository.deleteCategory(categoryExpenseId)
            repository.deleteCategory(categoryIncomeId)
        }
    }

    private fun baseFixture(
        showIncome: Boolean = false,
        showExpense: Boolean = true,
        additionalFixture: () -> Unit = {}
    ) {
        account1 = buildAccount("Test account 1", currency = currency1.code)
        account2 = buildAccount("Test account 1", currency = currency2.code)
        additionalFixture()
        testScenario =
            ActivityScenario.launch(Intent(targetContext, DistributionActivity::class.java).apply {
                putExtra(KEY_ACCOUNTID, HOME_AGGREGATE_ID)
                putExtra(DistributionViewModel.SHOW_INCOME_KEY, showIncome)
                putExtra(DistributionViewModel.SHOW_EXPENSE_KEY, showExpense)
            })
    }

    private fun fixtureWithMappedTransactions(
        showIncome: Boolean = false,
        showExpense: Boolean = true
    ) {
        baseFixture(showIncome, showExpense) {
            categoryExpenseId = writeCategory("Expense")
            categoryIncomeId = writeCategory("Income", type = FLAG_INCOME)
            with(Transaction.getNewInstance(account1.id, currency1)) {
                amount = Money(homeCurrency, -1200L)
                catId = categoryExpenseId
                save(contentResolver)
            }
            with(Transaction.getNewInstance(account2.id, currency2)) {
                amount = Money(homeCurrency, 3400L)
                catId = categoryIncomeId
                save(contentResolver)
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

    private fun launchWithContextCommand(
        @StringRes menuLabel: Int,
        assertIncome: (() -> Unit)?,
        assertExpense: (() -> Unit)?
    ) {
        fixtureWithMappedTransactions(assertIncome != null, assertExpense != null)
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