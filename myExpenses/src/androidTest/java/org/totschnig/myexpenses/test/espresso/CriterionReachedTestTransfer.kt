package org.totschnig.myexpenses.test.espresso

import android.content.ContentUris
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import org.junit.After
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard1
import org.totschnig.myexpenses.testutils.cleanup
import java.util.Currency
import kotlin.math.absoluteValue
import kotlin.test.Test

@TestShard1
class CriterionReachedTestTransfer : BaseExpenseEditTest() {
    val currency = CurrencyUnit(Currency.getInstance("USD"))

    lateinit var account2: Account

    fun fixture() {
        val cashType = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        account1 = Account(
            label = "Credit",
            currency = currency.code,
            criterion = -10000,
            type = cashType
        ).createIn(repository)
        account2 = Account(
            label = "Saving",
            currency = currency.code,
            criterion = 5000,
            type = cashType
        ).createIn(repository)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
        }
    }


    @Test
    fun savingGoalExceeded() {
        doTheTestWithNewTransfer(60, R.string.saving_goal_exceeded)
    }

    @Test
    fun savingGoalReached() {
        doTheTestWithNewTransfer(50, R.string.saving_goal_reached)
    }

    @Test
    fun bothMissed() {
        doTheTestWithNewTransfer(40, null)
    }

    @Test
    fun creditLimitExceeded() {
        doTheTestWithNewTransfer(110, R.string.credit_limit_exceeded, R.string.saving_goal_exceeded)
    }

    @Test
    fun creditLimitReached() {
        doTheTestWithNewTransfer(100, R.string.credit_limit_reached, R.string.saving_goal_exceeded)
    }

    // editing existing

    @Test
    fun savingGoalExceededEdited() {
        doTheTestWithEditedTransfer(60, R.string.saving_goal_exceeded)
    }

    @Test
    fun savingGoalReachedEdited() {
        doTheTestWithEditedTransfer(50, R.string.saving_goal_reached)
    }

    @Test
    fun bothMissedEdited() {
        doTheTestWithEditedTransfer(45, null)
    }

    @Test
    fun creditLimitExceededEdited() {
        doTheTestWithEditedTransfer(
            110,
            R.string.credit_limit_exceeded,
            R.string.saving_goal_exceeded
        )
    }

    @Test
    fun creditLimitReachedEdited() {
        doTheTestWithEditedTransfer(
            100, R.string.credit_limit_reached,
            R.string.saving_goal_exceeded
        )
    }


    /**
     * @param amount amount entered in major units
     */
    private fun doTheTestWithNewTransfer(
        amount: Int,
        expectedTitle: Int?,
        expectedSnackBar: Int? = null,
    ) {
        fixture()
        launchForResult(intent.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSFER)
        }).use {
            setAmount(amount.absoluteValue)
            clickFab()
            if (expectedTitle != null) {
                composeTestRule.onNodeWithText(getString(expectedTitle)).isDisplayed()
                expectedSnackBar?.let {
                    onView(withId(com.google.android.material.R.id.snackbar_text))
                        .check(matches(withSubstring(getString(expectedSnackBar))))
                }
            } else {
                assertFinishing()
            }
        }
    }

    private fun doTheTestWithEditedTransfer(
        editedAmount: Int,
        expectedTitle: Int?,
        expectedSnackBar: Int? = null,
    ) {
        fixture()
        val transactionId = Transfer.getNewInstance(account1.id, currency, account2.id).let {
            it.amount = Money(currency, -4000)
            ContentUris.parseId(it.save(contentResolver)!!)
        }
        launchForResult(intentForNewTransaction.apply {
            putExtra(DatabaseConstants.KEY_ROWID, transactionId)
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
        }).use {
            setAmount(editedAmount)
            clickFab()
            if (expectedTitle != null) {
                composeTestRule.onNodeWithText(getString(expectedTitle)).isDisplayed()
                expectedSnackBar?.let {
                    onView(withId(com.google.android.material.R.id.snackbar_text))
                        .check(matches(withSubstring(getString(expectedSnackBar))))
                }
            } else {
                assertFinishing()
            }
        }
    }

    @Test
    fun doTheTestWithInvertedTransfer() {
        fixture()
        val transactionId = Transfer.getNewInstance(account1.id, currency, account2.id).let {
            it.amount = Money(currency, 6000)
            ContentUris.parseId(it.save(contentResolver)!!)
        }
        launchForResult(intentForNewTransaction.apply {
            putExtra(DatabaseConstants.KEY_ROWID, transactionId)
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
        }).use {
            clickMenuItem(R.id.INVERT_COMMAND)
            clickFab()
            composeTestRule.onNodeWithText(getString(R.string.saving_goal_exceeded)).isDisplayed()
        }
    }
}