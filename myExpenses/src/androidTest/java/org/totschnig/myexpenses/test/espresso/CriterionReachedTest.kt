package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithText
import org.junit.After
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard1
import org.totschnig.myexpenses.testutils.cleanup
import java.util.Currency
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.test.Test


@TestShard1
class CriterionReachedTest : BaseExpenseEditTest() {
    val currency = CurrencyUnit(Currency.getInstance("USD"))

    fun fixture(criterion: Long, openingBalance: Long = 0) {
        account1 = Account(
            label = "Test label 1",
            currency = currency.code,
            criterion = criterion,
            openingBalance = openingBalance,
            type = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        ).createIn(repository)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
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
    fun savingGoalReachedSaveAndNew() {
        doTheTestWithNewTransaction(5000, 40, R.string.saving_goal_reached, repeat = 2)
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
        repeat: Int = 1,
    ) {
        fixture(criterion, openingBalance)
        launchForResult(intentForNewTransaction.apply {
            putExtra(ExpenseEdit.KEY_INCOME, amount > 0)
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
        }).use {
            if (repeat > 1) {
                clickMenuItem(R.id.SAVE_AND_NEW_COMMAND)
            }
            repeat(repeat) {
                setAmount(amount.absoluteValue)
                clickFab()
            }
            if (expectedTitle != null) {
                composeTestRule.onNodeWithText(getString(expectedTitle)).isDisplayed()
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
        fixture(criterion)
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
                composeTestRule.onNodeWithText(getString(expectedTitle)).isDisplayed()
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
        fixture(criterion)
        val account2 = Account(
            label = "Test label 2",
            currency = currency.code,
            criterion = criterion,
            type = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        ).createIn(repository)
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
            setAccount(account2.label)
            setAmount(editedAmount)
            clickFab()
            if (expectedTitle != null) {
                composeTestRule.onNodeWithText(getString(expectedTitle)).isDisplayed()
            } else {
                assertFinishing()
            }
        }
        cleanup {
            repository.deleteAccount(account2.id)
        }
    }
}