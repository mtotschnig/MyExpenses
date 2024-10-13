package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.Account
import java.util.Currency
import kotlin.math.absoluteValue
import kotlin.test.Test

class CriterionReachedTest : BaseExpenseEditTest() {
    val currency = CurrencyUnit(Currency.getInstance("USD"))
    @Test
    fun savingGoalExceeded() {
        doTheTest(5000, 60, R.string.saving_goal_exceeded)
    }
    @Test
    fun savingGoalReached() {
        doTheTest(5000, 50, R.string.saving_goal_reached)
    }
    @Test
    fun savingGoalMissed() {
        doTheTest(5000, 40, null)
    }
    @Test
    fun savingGoalForgetAboutIt() {
        doTheTest(5000, -10, null)
    }

    @Test
    fun creditLimitExceeded() {
        doTheTest(-5000, -60, R.string.credit_limit_exceeded)
    }
    @Test
    fun creditLimitReached() {
        doTheTest(-5000, -50, R.string.credit_limit_reached)
    }
    @Test
    fun creditLimitMissed() {
        doTheTest(-5000, -40, null)
    }
    @Test
    fun creditLimitForgetAboutIt() {
        doTheTest(-5000, 10, null)
    }

    /**
     * @param criterion in minor units
     * @param amount amount entered in major units
     */
    private fun doTheTest(criterion: Long, amount: Int, expectedTitle: Int?) {
        account1 = Account(
            label = "Test label 1",
            currency = currency.code,
            criterion = criterion
        ).createIn(repository)
        launchForResult(intentForNewTransaction.apply {
            putExtra(ExpenseEdit.KEY_INCOME, amount > 0)
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
        }).use {
            setAmount(amount.absoluteValue)
            clickFab()
            if (expectedTitle != null) {
                onView(withText(expectedTitle)).check(matches(isDisplayed()))
            } else {
                assertFinishing()
            }
        }
    }
}