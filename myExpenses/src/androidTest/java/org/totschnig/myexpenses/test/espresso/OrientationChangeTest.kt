package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.core.widget.NestedScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ExpenseEdit
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.PREDEFINED_NAME_BANK
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_2
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone
import org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible
import org.totschnig.myexpenses.testutils.TestShard4
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.toolbarTitle
import org.totschnig.myexpenses.testutils.withStatus
import java.util.Currency


@TestShard4
class OrientationChangeTest : BaseExpenseEditTest() {
    private lateinit var currency1: CurrencyUnit
    private lateinit var account2: Account
    private lateinit var currency2: CurrencyUnit

    @Before
    fun fixture() {
        val accountTypeCash = repository.findAccountType(PREDEFINED_NAME_CASH)!!
        val accountTypeBank = repository.findAccountType(PREDEFINED_NAME_BANK)!!
        currency1 = CurrencyUnit(Currency.getInstance("USD"))
        account1 = Account(
            label = ACCOUNT_LABEL_1,
            currency = currency1.code,
            type = accountTypeBank,
            ).createIn(repository)
        currency2 = CurrencyUnit(Currency.getInstance("EUR"))
        account2 = Account(
            label = ACCOUNT_LABEL_2,
            currency = currency2.code,
            type = accountTypeCash
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
    fun shouldKeepAccountAfterOrientationChange() {
        val id = repository.insertTransaction(
            accountId = account1.id,
            amount = 500L
        ).id
        val i = Intent(targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, id)
        testScenario = ActivityScenario.launch(i)
        setAccount(ACCOUNT_LABEL_2)
        checkAccount(ACCOUNT_LABEL_2)
        doWithRotation {
            onIdle()
            checkAccount(ACCOUNT_LABEL_2)
        }
    }

    @Test
    fun shouldKeepMethodAfterOrientationChange() {
        val id = repository.insertTransaction(
            accountId = account1.id,
            amount = -500L,
            methodId = repository.findPaymentMethod(PreDefinedPaymentMethod.DIRECTDEBIT.name)
        ).id
        val i = Intent(targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, id)
        testScenario = ActivityScenario.launch(i)
        closeSoftKeyboard()
        setMethod(PreDefinedPaymentMethod.CREDITCARD)
        checkMethod(PreDefinedPaymentMethod.CREDITCARD)
        doWithRotation {
            onIdle()
            checkMethod(PreDefinedPaymentMethod.CREDITCARD)        }
    }


    @Test
    fun shouldKeepStatusAfterOrientationChange() {
        val id = repository.insertTransaction(
            accountId = account1.id,
            amount = -500L,
            methodId = repository.findPaymentMethod(PreDefinedPaymentMethod.DIRECTDEBIT.name)
        ).id
        val i = Intent(targetContext, ExpenseEdit::class.java)
        i.putExtra(DatabaseConstants.KEY_ROWID, id)
        testScenario = ActivityScenario.launch(i)
        closeSoftKeyboard()
        //onView(withId(R.id.Status)).perform(nestedScrollToAction(), click()) leads to Status spinner
        //being overlayed by Floating Action Button on phone landscape
        onView(isAssignableFrom(NestedScrollView::class.java)).perform(swipeUp())
        onView(withId(R.id.Status)).perform(click())
        onData(
            allOf(
                instanceOf(CrStatus::class.java),
                withStatus(CrStatus.CLEARED)
            )
        ).perform(click())
        //withSpinnerText matches toString of object
        val string = CrStatus.CLEARED.toString()
        onView(withId(R.id.Status)).check(matches(withSpinnerText(`is`(string))))
        doWithRotation {
            onIdle()
            onView(withId(R.id.Status)).check(matches(withSpinnerText(`is`(string))))
        }
    }

    @Test
    fun shouldHandleNewInstanceAfterOrientationChange() {
        launch()
        doWithRotation {
            onIdle()
            toolbarTitle().check(doesNotExist())
            checkEffectiveVisible(R.id.OperationType)
        }
    }

    @Test
    fun shouldHandleExistingInstanceAfterOrientationChange() {
        val id = repository.insertTransaction(
            accountId = account1.id,
            amount = -500L,
            methodId = repository.findPaymentMethod(PreDefinedPaymentMethod.DIRECTDEBIT.name)
        ).id
        testScenario = ActivityScenario.launch(
            Intent(targetContext, ExpenseEdit::class.java).apply {
                putExtra(DatabaseConstants.KEY_ROWID, id)
            })
        doWithRotation {
            onIdle()
            checkEffectiveGone(R.id.OperationType)
            checkToolbarTitle(R.string.menu_edit_transaction)
        }
    }
}