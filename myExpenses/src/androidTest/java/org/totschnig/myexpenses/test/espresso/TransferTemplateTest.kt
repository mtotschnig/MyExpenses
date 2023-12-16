package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
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

class TransferTemplateTest : BaseExpenseEditTest() {
    lateinit var account2: Account

    @Before
    fun fixture() {
        val currency = CurrencyUnit(Currency.getInstance("USD"))
        val accountLabel1 = "Test label 1"
        account1 = buildAccount(
            accountLabel1,
            currency = currency.code
        )
        val accountLabel2 = "Test label 2"
        account2 = buildAccount(
            accountLabel2,
            currency = currency.code
        )
    }

    private fun launch(i: Intent) = ActivityScenario.launch<TestExpenseEdit>(i).also {
        testScenario = it
    }


    private fun setTitle() {
        onView(withId(R.id.Title))
            .perform(replaceText("Espresso template"))
        Espresso.closeSoftKeyboard()
    }

    private fun assertCorrectlySaved(expectedAccount: Long, expectedAmount: Long) {
        with(Template.getInstanceFromDb(contentResolver, 1)!!) {
            assertThat(title).isEqualTo("Espresso template")
            assertThat(amount.amountMinor).isEqualTo(expectedAmount)
            assertThat(accountId).isEqualTo(expectedAccount)
        }
    }

    private fun setDefaultAction(defaultAction: Action) {
        onView(withId(R.id.DefaultAction)).perform(ViewActions.click())
        Espresso.onData(
            CoreMatchers.allOf(
                CoreMatchers.instanceOf(String::class.java),
                CoreMatchers.`is`(when(defaultAction) {
                    Action.SAVE -> getString(R.string.menu_save)
                    Action.EDIT -> getString(R.string.menu_edit)
                })
            )
        ).perform(ViewActions.click())
    }

    private fun runTheTest(
        defaultAction: Action,
        amount: Int?,
        assertion: () -> Unit
    ) {
        launch(intent.apply {
            putExtra(
                TransactionsContract.Transactions.OPERATION_TYPE,
                TransactionsContract.Transactions.TYPE_TRANSFER
            )
            putExtra(ExpenseEdit.KEY_NEW_TEMPLATE, true)
        }).use {
            setTitle()
            if (amount != null) {
                setAmount(amount)
            }
            setDefaultAction(defaultAction)
            closeKeyboardAndSave()
            assertion()
        }
    }

    @Test
    fun withAmountOnFirstAccountSave() {
        runTheTest(Action.SAVE, 3000) {
            assertCorrectlySaved(account1.id, -300000)
        }
    }

    @Test
    fun withAmountOnFirstAccountEdit() {
        runTheTest(Action.EDIT, 3000) {
            assertCorrectlySaved(account1.id, -300000)
        }
    }

    @Test
    fun withoutAmountEdit() {
        runTheTest(Action.EDIT, null) {
            assertCorrectlySaved(account1.id, 0)
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
