package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.withAccount
import java.util.Currency
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.test.Test

class CriterionReachedTest : BaseExpenseEditTest() {
    val currency = CurrencyUnit(Currency.getInstance("USD"))

    lateinit var account2: Account

    fun fixture(criterion: Long, withAccount2: Boolean, openingBalance: Long = 0) {
        account1 = Account(
            label = "Test label 1",
            currency = currency.code,
            criterion = criterion,
            openingBalance = openingBalance
        ).createIn(repository)
        if (withAccount2) {
            account2 = Account(
                label = "Test label 2",
                currency = currency.code,
                criterion = criterion
            ).createIn(repository)
        }
    }

    @Test
    fun savingGoalExceeded() {
        doTheTestWithNewTransaction(5000, 60, R.string.saving_goal_exceeded)
    }

    @Test
    fun savingGoalReached() {
        doTheTestWithNewTransaction(5000, 50, R.string.saving_goal_reached)
    }

    @Test
    fun savingGoalMissed() {
        doTheTestWithNewTransaction(5000, 40, null)
    }

    @Test
    fun creditIsLargerThanSavingGoal() {
        doTheTestWithNewTransaction(5000, -60, null)
    }

    @Test
    fun creditLimitExceeded() {
        doTheTestWithNewTransaction(-5000, -60, R.string.credit_limit_exceeded)
    }

    @Test
    fun creditLimitReached() {
        doTheTestWithNewTransaction(-5000, -50, R.string.credit_limit_reached)
    }

    @Test
    fun creditLimitMissed() {
        doTheTestWithNewTransaction(-5000, -40, null)
    }

    @Test
    fun savingGoalAlreadyExceeded() {
        doTheTestWithNewTransaction(5000, 60, null, 6000)
    }

    @Test
    fun creditLimitAlreadyExceeded() {
        doTheTestWithNewTransaction(-5000, -60, null, -6000)
    }

    // editing existing

    @Test
    fun savingGoalExceededEdited() {
        doTheTestWithEditedTransaction(5000, 4000, 60, R.string.saving_goal_exceeded)
    }

    @Test
    fun savingGoalReachedEdited() {
        doTheTestWithEditedTransaction(5000, 4000, 50, R.string.saving_goal_reached)
    }

    @Test
    fun savingGoalMissedEdited() {
        doTheTestWithEditedTransaction(5000, 4000, 45, null)
    }

    @Test
    fun creditLimitExceededEdited() {
        doTheTestWithEditedTransaction(-5000, -4000, -60, R.string.credit_limit_exceeded)
    }

    @Test
    fun creditLimitReachedEdited() {
        doTheTestWithEditedTransaction(-5000, -4000, -50, R.string.credit_limit_reached)
    }

    @Test
    fun creditLimitMissedEdited() {
        doTheTestWithEditedTransaction(-5000, -4000, -45, null)
    }

    // editing existing and change account

    @Test
    fun savingGoalExceededEditedAccountChanged() {
        doTheTestWithEditedTransactionAndChangedAccount(
            5000,
            4000,
            60,
            R.string.saving_goal_exceeded
        )
    }

    @Test
    fun savingGoalReachedEditedAccountChanged() {
        doTheTestWithEditedTransactionAndChangedAccount(
            5000,
            4000,
            50,
            R.string.saving_goal_reached
        )
    }

    @Test
    fun savingGoalMissedEditedAccountChanged() {
        doTheTestWithEditedTransactionAndChangedAccount(5000, 4000, 40, null)
    }

    @Test
    fun creditLimitExceededEditedAccountChanged() {
        doTheTestWithEditedTransactionAndChangedAccount(
            -5000,
            -4000,
            -60,
            R.string.credit_limit_exceeded
        )
    }

    @Test
    fun creditLimitReachedEditedAccountChanged() {
        doTheTestWithEditedTransactionAndChangedAccount(
            -5000,
            -4000,
            -50,
            R.string.credit_limit_reached
        )
    }

    @Test
    fun creditLimitMissedEditedAccountChanged() {
        doTheTestWithEditedTransactionAndChangedAccount(-5000, -4000, -40, null)
    }

    /**
     * @param criterion in minor units
     * @param amount amount entered in major units
     */
    private fun doTheTestWithNewTransaction(
        criterion: Long,
        amount: Int,
        expectedTitle: Int?,
        openingBalance: Long = 0,
    ) {
        fixture(criterion, false, openingBalance)
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

    private fun doTheTestWithEditedTransaction(
        criterion: Long,
        existingAmount: Long,
        editedAmount: Int,
        expectedTitle: Int?,
    ) {
        fixture(criterion, false)
        val transactionId = Transaction.getNewInstance(account1.id, currency).let {
            it.amount = Money(currency, existingAmount)
            ContentUris.parseId(it.save(contentResolver)!!)
        }
        launchForResult(intentForNewTransaction.apply {
            putExtra(DatabaseConstants.KEY_ROWID, transactionId)
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
        }).use {
            if (editedAmount.sign != existingAmount.sign) {
                toggleType()
            }
            setAmount(editedAmount)
            clickFab()
            if (expectedTitle != null) {
                onView(withText(expectedTitle)).check(matches(isDisplayed()))
            } else {
                assertFinishing()
            }
        }
    }

    private fun doTheTestWithEditedTransactionAndChangedAccount(
        criterion: Long,
        existingAmount: Long,
        editedAmount: Int,
        expectedTitle: Int?,
    ) {
        fixture(criterion, true)
        val transactionId = Transaction.getNewInstance(account1.id, currency).let {
            it.amount = Money(currency, existingAmount)
            ContentUris.parseId(it.save(contentResolver)!!)
        }
        launchForResult(intentForNewTransaction.apply {
            putExtra(DatabaseConstants.KEY_ROWID, transactionId)
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
        }).use {
            if (editedAmount.sign != existingAmount.sign) {
                toggleType()
            }
            onView(withId(R.id.Account)).perform(click())
            onData(allOf(instanceOf(IdHolder::class.java), withAccount(account2.label))).perform(
                click()
            )
            setAmount(editedAmount)
            clickFab()
            if (expectedTitle != null) {
                onView(withText(expectedTitle)).check(matches(isDisplayed()))
            } else {
                assertFinishing()
            }
        }
    }
}