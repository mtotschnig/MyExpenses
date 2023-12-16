package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.contract.TransactionsContract
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Template.Action
import org.totschnig.myexpenses.model2.Account
import java.util.Currency

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

    private fun launch(i: Intent) = ActivityScenario.launch<TestExpenseEdit>(i).also {
        testScenario = it
    }


    private fun setTitle() {
        onView(
            ViewMatchers.withId(
                R.id.Title
            )
        ).perform(
            replaceText("Espresso template")
        )
        androidx.test.espresso.Espresso.closeSoftKeyboard()
    }

    private fun assertCorrectlySaved(expectedAccount: Long, expectedAmount: Long) {
        with(Template.getInstanceFromDb(contentResolver, 1)!!) {
            assertThat(title).isEqualTo("Espresso template")
            assertThat(amount.amountMinor).isEqualTo(expectedAmount)
            assertThat(accountId).isEqualTo(expectedAccount)
        }
    }

    private fun setDefaultAction(defaultAction: Template.Action) {
        onView(ViewMatchers.withId(R.id.DefaultAction)).perform(ViewActions.click())
        Espresso.onData(
            CoreMatchers.allOf(
                CoreMatchers.instanceOf(String::class.java),
                CoreMatchers.`is`(when(defaultAction) {
                    Template.Action.SAVE -> getString(R.string.menu_save)
                    Template.Action.EDIT -> getString(R.string.menu_edit)
                })
            )
        ).perform(ViewActions.click())
    }

    private fun runTheTest(defaultAction: Action, amountField: Int = R.id.Amount) {
        launch(intent.apply {
            putExtra(
                TransactionsContract.Transactions.OPERATION_TYPE,
                TransactionsContract.Transactions.TYPE_TRANSFER
            )
            putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true)
        }).use {
            setTitle()
            setAmount(3000, amountField)
            setDefaultAction(defaultAction)
            closeKeyboardAndSave()
        }
    }


    @Test
    fun withAmountOnFirstAccountSave() {
        runTheTest(Action.SAVE)
        assertCorrectlySaved(account1.id, -300000)
    }

    @Test
    fun withAmountOnFirstAccountEdit() {
        runTheTest(Action.EDIT)
        assertCorrectlySaved(account1.id, -300000)
    }

    @Test
    fun withAmountOnSecondAccountSave() {
        runTheTest(Action.SAVE, R.id.TransferAmount)
        assertCorrectlySaved(account2.id, 300000)
    }

    @Test
    fun withAmountOnSecondAccountEdit() {
        runTheTest(Action.EDIT, R.id.TransferAmount)
        assertCorrectlySaved(account2.id, 300000)
    }
}
