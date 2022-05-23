package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.Espresso
import java.util.*

class ExpenseEditTest : BaseExpenseEditTest() {
    lateinit var activityScenario: ActivityScenario<TestExpenseEdit>
    private lateinit var account1: Account
    private lateinit var account2: Account
    private lateinit var currency1: CurrencyUnit
    private lateinit var currency2: CurrencyUnit

    @Before
    fun fixture() {
        configureLocale(Locale.GERMANY)
        currency1 = CurrencyUnit(Currency.getInstance("USD"))
        currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        val accountLabel1 = "Test label 1"
        account1 = Account(accountLabel1, currency1, 0, "", AccountType.CASH, Account.DEFAULT_COLOR)
        account1.save()
        val accountLabel2 = "Test label 2"
        account2 = Account(accountLabel2, currency2, 0, "", AccountType.BANK, Account.DEFAULT_COLOR)
        account2.save()
    }

    private fun launch(i: Intent) = ActivityScenario.launch<TestExpenseEdit>(i).also {
        activityScenario = it
    }

    @Test
    fun formForTransactionIsPrepared() {
        launch(intent.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
        }).use {
            Espresso.checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
                R.id.PayeeRow, R.id.AccountRow
            )
            Espresso.checkEffectiveGone(R.id.Status, R.id.Recurrence, R.id.TitleRow)
            clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND)
            Espresso.checkEffectiveVisible(R.id.TitleRow, R.id.Recurrence)
            checkAccountDependents()
        }
    }

    private fun checkAccountDependents() {
        onView(ViewMatchers.withId(R.id.AmountLabel)).check(
            ViewAssertions.matches(
                ViewMatchers.withText(
                    String.format(Locale.ROOT, "%s (%s)", getString(R.string.amount), "$")
                )
            )
        )
        onView(ViewMatchers.withId(R.id.DateTimeLabel)).check(
            ViewAssertions.matches(
                ViewMatchers.withText(
                    String.format(
                        Locale.ROOT,
                        "%s / %s",
                        getString(R.string.date),
                        getString(R.string.time)
                    )
                )
            )
        )
    }

    @Test
    fun statusIsShownWhenBankAccountIsSelected() {
        launch(intent.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
            putExtra(DatabaseConstants.KEY_ACCOUNTID, account2.id)
        }).use {
            Espresso.checkEffectiveVisible(R.id.Status)
        }
    }

    @Test
    fun formForTransferIsPrepared() {
        launch(intent.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSFER)
        }).use {
            Espresso.checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow,
                R.id.TransferAccountRow
            )
            Espresso.checkEffectiveGone(R.id.Status, R.id.Recurrence, R.id.TitleRow)
            clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND)
            Espresso.checkEffectiveVisible(R.id.TitleRow, R.id.Recurrence)
            checkAccountDependents()

        }
    }

    @Test
    fun formForSplitIsPrepared() {
        launch(intent.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_SPLIT)
        }).use {
            Espresso.checkEffectiveVisible(
                R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.SplitRow,
                R.id.PayeeRow, R.id.AccountRow
            )
            Espresso.checkEffectiveGone(R.id.Status, R.id.Recurrence, R.id.TitleRow)
            clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND)
            Espresso.checkEffectiveVisible(R.id.TitleRow, R.id.Recurrence)
            checkAccountDependents()
        }
    }

    @Test
    fun formForTemplateIsPrepared() {
        launch(intent.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
            putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true)
        }).use {
            Espresso.checkEffectiveVisible(
                R.id.TitleRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
                R.id.PayeeRow, R.id.AccountRow, R.id.Recurrence, R.id.DefaultActionRow
            )
            Espresso.checkEffectiveGone(R.id.PB)
        }
    }

    @Test
    fun accountIdInExtraShouldPopulateSpinner() {
        val allAccounts = arrayOf(account1, account2)
        for (a in allAccounts) {
            val i = intent.apply {
                putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
                putExtra(DatabaseConstants.KEY_ACCOUNTID, a.id)
            }
            launch(i).use {
                onView(ViewMatchers.withId(R.id.Account)).check(
                    ViewAssertions.matches(
                        ViewMatchers.withSpinnerText(
                            a.label
                        )
                    )
                )
            }
        }
    }

    @Test
    fun currencyInExtraShouldPopulateSpinner() {
        val allCurrencies = arrayOf(currency1, currency2)
        for (c in allCurrencies) {
            //we assume that Fixture has set up the default account with id 1
            val i = intent.apply {
                putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
                putExtra(DatabaseConstants.KEY_CURRENCY, c.code)
            }
            launch(i).use {
                it.onActivity { activity: TestExpenseEdit ->
                    Assert.assertEquals(
                        "Selected account has wrong currency",
                        c.code,
                        activity.currentAccount!!.currency.code
                    )
                }
            }
        }
    }

    @Test
    fun saveAsNewWorksMultipleTimesInARow() {
        launch(intent.apply {
            putExtra(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSACTION)
            putExtra(DatabaseConstants.KEY_ACCOUNTID, account1.id)
        }).use {
            val success = getString(R.string.save_transaction_and_new_success)
            val times = 5
            val amount = 2
            clickMenuItem(R.id.SAVE_AND_NEW_COMMAND, false) //toggle save and new on
            for (j in 0 until times) {
                onView(
                    Espresso.withIdAndParent(
                        R.id.AmountEditText,
                        R.id.Amount
                    )
                ).perform(ViewActions.typeText(amount.toString()))
                onView(ViewMatchers.withId(R.id.CREATE_COMMAND))
                    .perform(ViewActions.click())
                onView(ViewMatchers.withText(success))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            }
            //we assume two fraction digits
            Assert.assertEquals(
                "Transaction sum does not match saved transactions",
                account1.getTransactionSum(null),
                (-amount * times * 100).toLong()
            )
        }
    }

    @Test
    fun shouldSaveTemplateWithAmount() {
        val template =
            Template.getTypedNewInstance(Transactions.TYPE_TRANSFER, account1.id, false, null)
        template!!.setTransferAccountId(account2.id)
        template.title = "Test template"
        template.save()
        launch(intent.apply {
            putExtra(DatabaseConstants.KEY_TEMPLATEID, template.id)
        }).use {
            val amount = 2
            onView(
                Espresso.withIdAndParent(
                    R.id.AmountEditText,
                    R.id.Amount
                )
            ).perform(ViewActions.click(), ViewActions.typeText(amount.toString()))
            onView(ViewMatchers.withId(R.id.CREATE_COMMAND))
                .perform(ViewActions.click())
            val restored = Template.getInstanceFromDb(template.id)
            Assert.assertEquals(Transactions.TYPE_TRANSFER, restored!!.operationType())
            Assert.assertEquals((-amount * 100).toLong(), restored.amount.amountMinor)
        }
    }

    override val testScenario: ActivityScenario<TestExpenseEdit>
        get() = activityScenario
}