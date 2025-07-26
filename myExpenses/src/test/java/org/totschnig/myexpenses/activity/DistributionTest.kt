package org.totschnig.myexpenses.activity

import android.content.Context
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.viewmodel.DistributionViewModel

@Ignore("For unknown reason, testing for expense works, while testing for income fails. We run this test connected for the moment.")
@RunWith(AndroidJUnit4::class)
class DistributionTest : BaseTestWithRepository() {
    private lateinit var scenario: ActivityScenario<DistributionActivity>

    @get:Rule
    val composeTestRule = createEmptyComposeRule()
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val currency = CurrencyUnit.DebugInstance
    private var accountId: Long = 0

    private fun baseFixture(
        showIncome: Boolean = false,
        showExpense: Boolean = true,
        additionalFixture: () -> Unit = {}
    ) {
        accountId = insertAccount("Test account 1", currency = currency.code)
        additionalFixture()
        scenario =
            ActivityScenario.launch(Intent(targetContext, DistributionActivity::class.java).apply {
                putExtra(KEY_ACCOUNTID, accountId)
                putExtra(DistributionViewModel.SHOW_INCOME_KEY, showIncome)
                putExtra(DistributionViewModel.SHOW_EXPENSE_KEY, showExpense)
            })
    }

    private fun fixtureWithMappedTransactions(
        showIncome: Boolean = false,
        showExpense: Boolean = true
    ) {
        baseFixture(showIncome, showExpense) {
            val categoryExpenseId = writeCategory("Expense", type = FLAG_EXPENSE)
            val categoryIncomeId = writeCategory("Income", type = FLAG_INCOME)
            with(Transaction.getNewInstance(accountId, homeCurrency)) {
                amount = Money(homeCurrency, 3400L)
                catId = categoryIncomeId
                save(contentResolver)
            }
            with(Transaction.getNewInstance(accountId, homeCurrency)) {
                amount = Money(homeCurrency, -1200L)
                catId = categoryExpenseId
                save(contentResolver)
            }
        }
    }

    private val homeCurrency: CurrencyUnit by lazy { currencyContext.homeCurrencyUnit }

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
            scenario.onActivity {
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
                .onNodeWithText("Income", useUnmergedTree = true).performTouchInput {
                    longClick()
                }
            onContextMenu(menuLabel)
            assertIncome()
            pressBack()
        }
        if (assertExpense != null) {
            composeTestRule
                .onNodeWithText("Expense", useUnmergedTree = true).performTouchInput {
                    longClick()
                }
            onContextMenu(menuLabel)
            assertExpense()
        }
    }

    private fun onContextMenu(@StringRes menuItemId: Int) =
        composeTestRule.onNodeWithText(targetContext.getString(menuItemId)).performClick()

}