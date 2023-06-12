package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.createPaymentMethod
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model2.PAYMENT_METHOD_EXPENSE
import org.totschnig.myexpenses.model2.PaymentMethod
import org.totschnig.myexpenses.testutils.Espresso

class ExpenseEditFlowTest : BaseExpenseEditTest() {

    @Before
    fun fixture() {
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(accountLabel1)
        repository.createPaymentMethod(
            targetContext,
            PaymentMethod(0, "TEST", null, PAYMENT_METHOD_EXPENSE, true, null, listOf(AccountType.CASH))
        )
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
            .perform(ViewActions.click())
        closeSoftKeyboard()
        onView(ViewMatchers.withId(R.id.Category))
            .perform(ViewActions.click())
        androidx.test.espresso.Espresso.pressBack()
        onView(ViewMatchers.withId(R.id.CREATE_COMMAND))
            .perform(ViewActions.click())
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
        ).perform(ViewActions.click())
        onView(ViewMatchers.withId(R.id.bOK))
            .perform(ViewActions.click())
        onView(Espresso.withIdAndParent(R.id.TaType, R.id.Amount))
            .check(ViewAssertions.matches(ViewMatchers.isNotChecked()))
    }
}