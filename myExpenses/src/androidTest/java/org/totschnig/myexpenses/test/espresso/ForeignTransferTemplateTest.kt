package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Template.Action
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TestShard2
import org.totschnig.myexpenses.testutils.cleanup
import java.util.Currency

@TestShard2
class ForeignTransferTemplateTest : BaseExpenseEditTest() {
    lateinit var account2: Account

    @Before
    fun fixture() {
        val currency1 = CurrencyUnit(Currency.getInstance("USD"))
        val currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(
            accountLabel1,
            currency = currency1.code
        )
        val accountLabel2 = "Test label 2"
        account2 = buildAccount(
            accountLabel2,
            currency = currency2.code
        )
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
            repository.deleteAccount(account2.id)
        }
    }

    private fun setDefaultAction(defaultAction: Action) {
        onView(withId(R.id.DefaultAction)).perform(scrollTo(), click())
        Espresso.onData(
            CoreMatchers.allOf(
                CoreMatchers.instanceOf(String::class.java),
                CoreMatchers.`is`(when(defaultAction) {
                    Action.SAVE -> getString(R.string.menu_save)
                    Action.EDIT -> getString(R.string.menu_edit)
                })
            )
        ).perform(click())
    }

    private fun runTheTest(
        defaultAction: Action,
        amountField: Int? = R.id.Amount,
        templateAssertion: () -> Unit
    ) {
        launch(intent.apply {
            putExtra(
                TransactionsContract.Transactions.OPERATION_TYPE,
                TransactionsContract.Transactions.TYPE_TRANSFER
            )
            putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true)
        }).use {
            setTitle()
            if (amountField != null) {
                setAmount(3000, amountField)
            }
            setDefaultAction(defaultAction)
            closeKeyboardAndSave()
            templateAssertion()
        }
    }


    @Test
    fun withAmountOnFirstAccountSave() {
        runTheTest(Action.SAVE) {
            assertTemplate(account1.id, -300000)
        }
    }

    @Test
    fun withAmountOnFirstAccountEdit() {
        runTheTest(Action.EDIT) {
            assertTemplate(account1.id, -300000)
        }
    }

    @Test
    fun withAmountOnSecondAccountSave() {
        runTheTest(Action.SAVE, R.id.TransferAmount) {
            assertTemplate(account2.id, 300000)
        }
    }

    @Test
    fun withAmountOnSecondAccountEdit() {
        runTheTest(Action.EDIT, R.id.TransferAmount) {
            assertTemplate(account2.id, 300000)
        }
    }

    @Test
    fun withoutAmountEdit() {
        runTheTest(Action.EDIT, null) {
            assertTemplate(account1.id, 0)
        }
    }

    @Test
    fun withoutAmountSave() {
        runTheTest(Action.SAVE, null) {
            onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(R.string.template_default_action_without_amount_hint)))
        }
    }
}
