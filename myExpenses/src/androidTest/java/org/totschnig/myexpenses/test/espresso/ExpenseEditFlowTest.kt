package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.createPaymentMethod
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model2.PAYMENT_METHOD_EXPENSE
import org.totschnig.myexpenses.model2.PaymentMethod
import org.totschnig.myexpenses.testutils.Espresso
import org.totschnig.myexpenses.testutils.toolbarTitle

class ExpenseEditFlowTest : BaseExpenseEditTest() {

    @Before
    fun fixture() {
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(accountLabel1)
        repository.createPaymentMethod(
            targetContext,
            PaymentMethod(0, "TEST", null, PAYMENT_METHOD_EXPENSE, true, null, listOf(AccountType.CASH))
        )
        Template.getTypedNewInstance(contentResolver, Transactions.TYPE_TRANSACTION, account1.id, homeCurrency, false, null)!!.apply {
            amount = Money(homeCurrency, 500L)
            title = "Template"
            save(contentResolver)
        }
        testScenario = ActivityScenario.launchActivityForResult(intentForNewTransaction)
    }

    /**
     * If user toggles from expense (where we have at least one payment method) to income (where there is none)
     * and then selects category, or opens calculator, and comes back, saving failed. We test here
     * the fix for this bug.
     */
    @Test
    fun testScenarioForBug5b11072e6007d59fcd92c40b() {
        onView(
            Espresso.withIdAndParent(
                R.id.AmountEditText,
                R.id.Amount
            )
        ).perform(ViewActions.typeText(10.toString()))
        onView(Espresso.withIdAndParent(R.id.TaType, R.id.Amount))
            .perform(click())
        closeSoftKeyboard()
        onView(ViewMatchers.withId(R.id.Category))
            .perform(click())
        androidx.test.espresso.Espresso.pressBack()
        onView(ViewMatchers.withId(R.id.CREATE_COMMAND))
            .perform(click())
        assertFinishing()
    }

    @Test
    fun calculatorMaintainsType() {
        onView(
            Espresso.withIdAndParent(
                R.id.AmountEditText,
                R.id.Amount
            )
        ).perform(ViewActions.typeText("123"))
        closeSoftKeyboard()
        onView(
            Espresso.withIdAndParent(
                R.id.Calculator,
                R.id.Amount
            )
        ).perform(click())
        onView(ViewMatchers.withId(R.id.bOK))
            .perform(click())
        onView(Espresso.withIdAndParent(R.id.TaType, R.id.Amount))
            .check(ViewAssertions.matches(ViewMatchers.isNotChecked()))
    }

    @Test
    fun templateMenuShouldLoadTemplateForNewTransaction() {
        clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND)
        onView(withText("Template")).perform(click())
        toolbarTitle().check(ViewAssertions.matches(withText(R.string.menu_create_transaction)))
        onView(
            Espresso.withIdAndParent(
                R.id.AmountEditText,
                R.id.Amount
            )
        ).check(ViewAssertions.matches(withText("5")))
    }
}