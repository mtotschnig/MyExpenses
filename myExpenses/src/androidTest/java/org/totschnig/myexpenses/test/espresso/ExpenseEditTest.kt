package org.totschnig.myexpenses.test.espresso

import android.widget.Button
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.PREDEFINED_NAME_BANK
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.dateButtonHasDate
import java.time.LocalDate
import java.util.Currency

@TestShard2
class ExpenseEditTest : BaseExpenseEditTest() {
    private lateinit var account2: Account
    private lateinit var yenAccount: Account
    private lateinit var currency1: CurrencyUnit
    private lateinit var currency2: CurrencyUnit

    @Before
    fun fixture() {
        val accountTypeCash = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        val accountTypeBank = repository.findAccountType(PREDEFINED_NAME_BANK)!!
        currency1 = CurrencyUnit(Currency.getInstance("USD"))
        currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        account1 = Account(label = "Test label 1", currency = currency1.code, type = accountTypeCash).createIn(repository)
        account2 =
            Account(label = "Test label 2", currency = currency2.code, type = accountTypeBank)
                .createIn(repository)
        yenAccount =
            Account(label = "Japan", currency = "JPY", type = accountTypeCash).createIn(repository)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
            repository.deleteAccount(yenAccount.id)
        }
    }

    @Test
    fun formForTransactionIsPrepared() {
        launch(intentForNewTransaction.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
        }).use {
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
                R.id.PayeeRow, R.id.AccountRow
            )
            checkEffectiveGone(R.id.Status, R.id.Recurrence, R.id.TitleRow)
            clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND)
            checkEffectiveVisible(R.id.TitleRow, R.id.Recurrence)
            checkAccountDependents()
        }
    }

    private fun checkAccountDependents() {
        onView(withId(R.id.AmountLabel)).check(
            matches(withText("${getString(R.string.amount)} (${currency1.symbol})"))
        )
        onView(withId(R.id.DateTimeLabel)).check(
            matches(withText("${getString(R.string.date)} / ${getString(R.string.time)}"))
        )
    }

    @Test
    fun statusIsShownWhenBankAccountIsSelected() {
        launch(intent.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
            putExtra(KEY_ACCOUNTID, account2.id)
        }).use {
            checkEffectiveVisible(R.id.Status)
        }
    }

    @Test
    fun amountInputWithFractionDigitLessCurrency() {
        launch(intent.apply {
            putExtra(KEY_ACCOUNTID, yenAccount.id)
        }).use {
            setAmount(100)
        }
    }

    @Test
    fun helpDialogIsOpened() {
        launch()
        clickMenuItem(R.id.HELP_COMMAND)
        onView(withText(containsString(getString(R.string.help_ExpenseEdit_transaction_title))))
            .check(matches(isDisplayed()))
        onView(
            allOf(
                isAssignableFrom(Button::class.java),
                withText(`is`(app.getString(android.R.string.ok)))
            )
        )
            .check(matches(isDisplayed()))
    }

    @Test
    fun formForTransferIsPrepared() {
        launch(intentForNewTransaction.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSFER)
        }).use {
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow,
                R.id.TransferAccountRow
            )
            checkEffectiveGone(R.id.Status, R.id.Recurrence, R.id.TitleRow)
            clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND)
            checkEffectiveVisible(R.id.TitleRow, R.id.Recurrence)
            checkAccountDependents()

        }
    }

    @Test
    fun formForSplitIsPrepared() {
        launch(intentForNewTransaction.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_SPLIT)
        }).use {
            checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.SplitRow,
                R.id.PayeeRow, R.id.AccountRow
            )
            checkEffectiveGone(R.id.Status, R.id.Recurrence, R.id.TitleRow)
            clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND)
            checkEffectiveVisible(R.id.TitleRow, R.id.Recurrence)
            checkAccountDependents()
        }
    }

    @Test
    fun formForTemplateIsPrepared() {
        //New Templates are created without account_id passed in via intent
        launch(intent.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
            putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true)
        }).use {
            checkEffectiveVisible(
                R.id.TitleRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
                R.id.PayeeRow, R.id.AccountRow, R.id.Recurrence, R.id.DefaultActionRow
            )
            checkEffectiveGone(R.id.PB)
        }
    }

    @Test
    fun accountIdInExtraShouldPopulateSpinner() {
        val allAccounts = arrayOf(account1, account2)
        for (a in allAccounts) {
            val i = intent.apply {
                putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
                putExtra(KEY_ACCOUNTID, a.id)
            }
            launch(i).use {
                checkAccount(a.label)
            }
        }
    }

    @Test
    fun saveAsNewWorksMultipleTimesInARow() {
        //We test with an account that is not sorted first, in order to verify that account is kept
        //after save and new fab is clicked
        launch(intent.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
            putExtra(KEY_ACCOUNTID, account2.id)
        }).use {
            val success = getString(R.string.save_transaction_and_new_success)
            val times = 5
            val amount = 2
            clickMenuItem(R.id.SAVE_AND_NEW_COMMAND) //toggle save and new on
            repeat(times) {
                setAmount(amount)
                clickFab()
                checkAccount(account2.label)
                onView(withText(success)).check(matches(isDisplayed()))
            }
            //we assume two fraction digits
            Truth.assertWithMessage("Transaction sum does not match saved transactions")
                .that(repository.getTransactionSum(account2))
                .isEqualTo(-amount * times * 100L)
        }
    }

    @Test
    fun shouldSaveTemplateWithAmount() {
        val template =
            Template.getTypedNewInstance(contentResolver, Transactions.TYPE_TRANSFER, account1.id, currency1, false, null)
        template!!.setTransferAccountId(account2.id)
        template.title = "Test template"
        template.save(contentResolver)
        launch(intent.apply {
            putExtra(KEY_TEMPLATEID, template.id)
        }).use {
            val amount = 2
            setAmount(amount)
            clickFab()
            val restored = Template.getInstanceFromDb(contentResolver, template.id)!!
            assertThat(restored.operationType()).isEqualTo(Transactions.TYPE_TRANSFER)
            assertThat(restored.amount.amountMinor).isEqualTo(-amount * 100L)
        }
    }

    @Test
    fun shouldChangeDate() {
        launch(intentForNewTransaction).use {
            val today = LocalDate.now()
            onView(withId(R.id.DateButton))
                .check(matches(dateButtonHasDate(today)))
            onView(withId(R.id.DateButton)).perform(click())
            val newDate = if (today.dayOfMonth == 1) today.plusDays(1) else today.minusDays(1)
            onView(
                allOf(
                    withText(newDate.dayOfMonth.toString()), // The day number
                    ViewMatchers.isDescendantOfA(withId(com.google.android.material.R.id.month_grid)), // Ensure it's within the calendar grid
                    isDisplayed() // Ensure it's currently visible
                )
            ).perform(click())
            onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())
            onView(withId(R.id.DateButton))
                .check(matches(dateButtonHasDate(newDate)))
        }
    }
}